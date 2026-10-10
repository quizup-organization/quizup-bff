package io.github.quizup.bff.application;

import io.github.quizup.bff.infrastructure.in.api.request.PeopleSort;
import io.github.quizup.bff.infrastructure.in.api.response.GameHistoryItemView;
import io.github.quizup.bff.infrastructure.in.api.response.HeadToHeadView;
import io.github.quizup.bff.infrastructure.in.api.response.PageResponse;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerCardView;
import io.github.quizup.bff.infrastructure.in.api.response.PlayerProfileView;
import io.github.quizup.bff.infrastructure.in.api.response.PresenceView;
import io.github.quizup.bff.infrastructure.in.api.response.ProgressionView;
import io.github.quizup.bff.infrastructure.in.api.response.TopicRefView;
import io.github.quizup.bff.infrastructure.in.api.response.UserRefView;
import io.github.quizup.game.domain.model.Game;
import io.github.quizup.game.domain.model.GameStatus;
import io.github.quizup.game.domain.model.PlayerGamesPage;
import io.github.quizup.game.domain.query.GameQuery;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.profile.domain.model.GameXp;
import io.github.quizup.profile.domain.model.PlayerPresence;
import io.github.quizup.profile.domain.model.PlayerProgress;
import io.github.quizup.profile.domain.model.Profile;
import io.github.quizup.profile.domain.query.PresenceQuery;
import io.github.quizup.profile.domain.query.ProgressionQuery;
import io.github.quizup.social.domain.model.FollowDirection;
import io.github.quizup.social.domain.model.UserFollowCounts;
import io.github.quizup.social.domain.model.UserFollower;
import io.github.quizup.social.domain.query.UserFollowerQuery;
import io.github.quizup.theme.domain.model.Topic;
import io.github.quizup.theme.domain.query.TopicQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Composition des vues joueur : profil public, listes de personnes, historique de duels,
 * bilan de confrontations, activité.
 */
@Service
public class ProfileViewService {

    private static final int MAX_PEOPLE_SCAN = FollowLookup.MAX_LIST_SIZE;
    private static final int HEAD_TO_HEAD_PAGE_SIZE = 200;
    private static final int HEAD_TO_HEAD_MAX_PAGES = 5;

    private final QueryGateway queryGateway;
    private final ProfileLookup profileLookup;
    private final FollowLookup followLookup;

    public ProfileViewService(QueryGateway queryGateway, ProfileLookup profileLookup, FollowLookup followLookup) {
        this.queryGateway = queryGateway;
        this.profileLookup = profileLookup;
        this.followLookup = followLookup;
    }

    public CompletableFuture<PlayerProfileView> profileView(String viewerId, String userId) {
        CompletableFuture<Profile> profileFuture = profileLookup.get(userId);
        CompletableFuture<PlayerProgress> progressFuture = queryGateway.query(
                new ProgressionQuery.GetProgressionQuery(userId),
                QueryResponseTypes.instanceOf(PlayerProgress.class));
        CompletableFuture<UserFollowCounts> countsFuture = followLookup.userCounts(userId);
        CompletableFuture<List<PlayerPresence>> presenceFuture = queryGateway.query(
                new PresenceQuery.GetPresencesByIdsQuery(List.of(userId)),
                QueryResponseTypes.multipleInstancesOf(PlayerPresence.class));
        CompletableFuture<Boolean> followingFuture = userId.equals(viewerId)
                ? CompletableFuture.completedFuture(false)
                : followLookup.userFollowed(viewerId, userId);

        return CompletableFuture.allOf(profileFuture, progressFuture, countsFuture, presenceFuture, followingFuture)
                .thenApply(_ -> {
                    Profile profile = profileFuture.join();
                    PlayerProgress progress = progressFuture.join();
                    UserFollowCounts counts = countsFuture.join();
                    PlayerPresence presence = presenceFuture.join().stream().findFirst().orElse(null);
                    return new PlayerProfileView(
                            profile.userId(),
                            profile.pseudonym(),
                            profile.bio(),
                            profile.country(),
                            profile.avatarOptions(),
                            profile.userId().equals(viewerId),
                            followingFuture.join(),
                            presence == null ? null : toPresence(presence),
                            ProgressionViews.toView(progress),
                            ProgressionViews.toStats(progress),
                            counts.followers(),
                            counts.following());
                });
    }

    public CompletableFuture<Optional<PresenceView>> presence(String userId) {
        return queryGateway.query(
                        new PresenceQuery.GetPresencesByIdsQuery(List.of(userId)),
                        QueryResponseTypes.multipleInstancesOf(PlayerPresence.class))
                .thenApply(presences -> presences.stream().findFirst().map(ProfileViewService::toPresence));
    }

    /**
     * Liste des abonnements/abonnés enrichie (profil, niveau, présence, suivi du lecteur).
     * Le périmètre est borné côté service social ({@value FollowLookup#MAX_LIST_SIZE}), puis
     * filtré/trié/paginé ici.
     */
    public CompletableFuture<PageResponse<PlayerCardView>> people(String viewerId,
                                                                  String userId,
                                                                  FollowDirection direction,
                                                                  String query,
                                                                  PeopleSort sort,
                                                                  int page,
                                                                  int size) {
        CompletableFuture<List<UserFollower>> followsFuture = queryGateway.query(
                new UserFollowerQuery.GetUserFollowsQuery(userId, direction, MAX_PEOPLE_SCAN),
                QueryResponseTypes.multipleInstancesOf(UserFollower.class));
        CompletableFuture<List<String>> viewerFollowingFuture = followLookup.followingIds(viewerId, MAX_PEOPLE_SCAN);

        return followsFuture.thenCompose(follows -> {
            List<String> ids = follows.stream()
                    .map(follow -> direction == FollowDirection.FOLLOWING ? follow.followedId() : follow.followerId())
                    .distinct()
                    .toList();

            CompletableFuture<List<Profile>> profilesFuture = profileLookup.getAll(ids);
            CompletableFuture<List<PlayerProgress>> progressionsFuture = ids.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : queryGateway.query(new ProgressionQuery.GetProgressionsByIdsQuery(ids),
                            QueryResponseTypes.multipleInstancesOf(PlayerProgress.class));
            CompletableFuture<List<PlayerPresence>> presencesFuture = ids.isEmpty()
                    ? CompletableFuture.completedFuture(List.of())
                    : queryGateway.query(new PresenceQuery.GetPresencesByIdsQuery(ids),
                            QueryResponseTypes.multipleInstancesOf(PlayerPresence.class));

            return CompletableFuture.allOf(profilesFuture, progressionsFuture, presencesFuture, viewerFollowingFuture)
                    .thenApply(_ -> {
                        Map<String, Profile> profileById = byKey(profilesFuture.join(), Profile::userId);
                        Map<String, PlayerProgress> progressById = byKey(progressionsFuture.join(), PlayerProgress::userId);
                        Map<String, PlayerPresence> presenceById = byKey(presencesFuture.join(), PlayerPresence::userId);
                        Set<String> viewerFollowing = Set.copyOf(viewerFollowingFuture.join());
                        String normalizedQuery = query == null || query.isBlank() ? null : normalize(query);

                        List<PlayerCardView> cards = ids.stream()
                                .map(id -> toCard(id, profileById, progressById, presenceById, viewerFollowing))
                                .filter(Optional::isPresent)
                                .map(Optional::get)
                                .filter(card -> normalizedQuery == null
                                        || normalize(card.pseudonym()).contains(normalizedQuery))
                                .sorted(peopleComparator(sort))
                                .toList();

                        return PageResponse.of(slice(cards, page, size), page, size, cards.size());
                    });
        });
    }

    /** Historique paginé des duels d'un joueur, enrichi adversaire + sujet + XP réelle. */
    public CompletableFuture<PageResponse<GameHistoryItemView>> games(String userId,
                                                                      String topicId,
                                                                      String opponentId,
                                                                      int page,
                                                                      int size) {
        return queryGateway.query(
                        new GameQuery.GetPlayerGamesQuery(userId, topicId, opponentId, page, size),
                        QueryResponseTypes.instanceOf(PlayerGamesPage.class))
                .thenCompose(gamesPage -> enrichGames(userId, gamesPage.games())
                        .thenApply(items -> PageResponse.of(items, gamesPage.page(), gamesPage.size(),
                                gamesPage.totalElements())));
    }

    /** Bilan des duels communs entre deux joueurs (borné à 1000 parties). */
    public CompletableFuture<HeadToHeadView> headToHead(String userId, String against) {
        return allGamesAgainst(userId, against, 0, new ArrayList<>())
                .thenApply(games -> {
                    int wins = 0;
                    int losses = 0;
                    int draws = 0;
                    for (Game game : games) {
                        if (game.winnerId() == null) {
                            draws++;
                        } else if (game.winnerId().equals(userId)) {
                            wins++;
                        } else {
                            losses++;
                        }
                    }
                    return new HeadToHeadView(games.size(), wins, losses, draws);
                });
    }

    private CompletableFuture<List<GameHistoryItemView>> enrichGames(String userId, List<Game> games) {
        List<String> gameIds = games.stream().map(Game::gameId).toList();
        List<String> topicIds = games.stream().map(Game::topicId).distinct().toList();
        List<String> opponentIds = games.stream()
                .map(game -> opponentIdOf(game, userId))
                .filter(java.util.Objects::nonNull)
                .distinct()
                .toList();

        CompletableFuture<List<Topic>> topicsFuture = topicIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : queryGateway.query(new TopicQuery.GetTopicsByIdsQuery(topicIds),
                        QueryResponseTypes.multipleInstancesOf(Topic.class));
        CompletableFuture<List<Profile>> opponentsFuture = opponentIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : profileLookup.getAll(opponentIds);
        CompletableFuture<List<GameXp>> xpFuture = gameIds.isEmpty()
                ? CompletableFuture.completedFuture(List.of())
                : queryGateway.query(new ProgressionQuery.GetGamesXpQuery(userId, gameIds),
                        QueryResponseTypes.multipleInstancesOf(GameXp.class));

        return CompletableFuture.allOf(topicsFuture, opponentsFuture, xpFuture)
                .thenApply(_ -> {
                    Map<String, Topic> topicById = byKey(topicsFuture.join(), Topic::topicId);
                    Map<String, Profile> opponentById = byKey(opponentsFuture.join(), Profile::userId);
                    Map<String, GameXp> xpByGame = byKey(xpFuture.join(), GameXp::gameId);
                    return games.stream()
                            .map(game -> toHistoryItem(userId, game, topicById, opponentById, xpByGame))
                            .toList();
                });
    }

    private CompletableFuture<List<Game>> allGamesAgainst(String userId,
                                                          String against,
                                                          int page,
                                                          List<Game> accumulator) {
        return queryGateway.query(
                        new GameQuery.GetPlayerGamesQuery(userId, null, against, page, HEAD_TO_HEAD_PAGE_SIZE),
                        QueryResponseTypes.instanceOf(PlayerGamesPage.class))
                .thenCompose(gamesPage -> {
                    accumulator.addAll(gamesPage.games());
                    boolean morePages = page + 1 < gamesPage.totalPages() && page + 1 < HEAD_TO_HEAD_MAX_PAGES;
                    return morePages
                            ? allGamesAgainst(userId, against, page + 1, accumulator)
                            : CompletableFuture.completedFuture(accumulator);
                });
    }

    private static Optional<PlayerCardView> toCard(String userId,
                                                   Map<String, Profile> profileById,
                                                   Map<String, PlayerProgress> progressById,
                                                   Map<String, PlayerPresence> presenceById,
                                                   Set<String> viewerFollowing) {
        Profile profile = profileById.get(userId);
        if (profile == null) {
            return Optional.empty();
        }
        ProgressionView progression = ProgressionViews.toView(
                progressById.getOrDefault(userId, PlayerProgress.empty(userId)));
        PlayerPresence presence = presenceById.get(userId);
        return Optional.of(new PlayerCardView(
                profile.userId(),
                profile.pseudonym(),
                profile.avatarOptions(),
                progression.level(),
                progression.title(),
                viewerFollowing.contains(userId),
                presence == null ? null : toPresence(presence)));
    }

    private static Comparator<PlayerCardView> peopleComparator(PeopleSort sort) {
        if (sort == PeopleSort.LEVEL) {
            return Comparator.comparingInt(PlayerCardView::level).reversed()
                    .thenComparing(PlayerCardView::pseudonym, String.CASE_INSENSITIVE_ORDER);
        }
        if (sort == PeopleSort.ALPHA) {
            return Comparator.comparing(PlayerCardView::pseudonym, String.CASE_INSENSITIVE_ORDER);
        }
        return (first, second) -> 0;
    }

    private static GameHistoryItemView toHistoryItem(String userId,
                                                     Game game,
                                                     Map<String, Topic> topicById,
                                                     Map<String, Profile> opponentById,
                                                     Map<String, GameXp> xpByGame) {
        boolean isPlayer1 = game.player1Id().equals(userId);
        String opponentId = opponentIdOf(game, userId);
        Profile opponent = opponentId == null ? null : opponentById.get(opponentId);
        Topic topic = topicById.get(game.topicId());
        GameXp xp = xpByGame.get(game.gameId());
        return new GameHistoryItemView(
                game.gameId(),
                TopicViews.toRef(game.topicId(), topic),
                opponent == null ? null : new UserRefView(opponent.userId(), opponent.pseudonym(), opponent.avatarOptions()),
                game.opponent() == null ? null : game.opponent().name(),
                outcomeOf(game, userId),
                isPlayer1 ? game.player1Score() : game.player2Score(),
                isPlayer1 ? game.player2Score() : game.player1Score(),
                xp == null ? null : xp.xp(),
                game.endedAt() != null ? game.endedAt() : game.createdAt());
    }

    private static GameHistoryItemView.Outcome outcomeOf(Game game, String userId) {
        return switch (game.status()) {
            case IN_PROGRESS -> GameHistoryItemView.Outcome.IN_PROGRESS;
            case CANCELED -> GameHistoryItemView.Outcome.CANCELLED;
            case FINISHED -> finishedOutcome(game, userId);
        };
    }

    private static GameHistoryItemView.Outcome finishedOutcome(Game game, String userId) {
        if (game.winnerId() == null) {
            return GameHistoryItemView.Outcome.DRAW;
        }
        return game.winnerId().equals(userId)
                ? GameHistoryItemView.Outcome.WIN
                : GameHistoryItemView.Outcome.LOSS;
    }

    private static String opponentIdOf(Game game, String userId) {
        if (game.player1Id().equals(userId)) {
            return game.player2Id();
        }
        return game.player1Id();
    }

    private static PresenceView toPresence(PlayerPresence presence) {
        return new PresenceView(presence.userId(), presence.status(), presence.lastSeenAt());
    }

    private static <T> Map<String, T> byKey(List<T> items, Function<T, String> key) {
        return items.stream().collect(Collectors.toMap(key, Function.identity(), (first, _) -> first, LinkedHashMap::new));
    }

    private static <T> List<T> slice(List<T> items, int page, int size) {
        int from = Math.min(page * size, items.size());
        int to = Math.min(from + size, items.size());
        return items.subList(from, to);
    }

    private static String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .trim();
    }
}
