package io.github.quizup.bff.application;

import io.github.quizup.game.domain.model.GameQuestion;
import io.github.quizup.game.domain.model.GameQuestionChoice;
import io.github.quizup.game.domain.model.GameQuestionContent;
import io.github.quizup.game.domain.model.GameRules;
import io.github.quizup.microservice.core.domain.model.i18n.Language;
import io.github.quizup.microservice.core.infrastructure.axon.QueryResponseTypes;
import io.github.quizup.theme.domain.model.Question;
import io.github.quizup.theme.domain.model.QuestionChoice;
import io.github.quizup.theme.domain.model.QuestionContent;
import io.github.quizup.theme.domain.query.QuestionQuery;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;

/**
 * Prépare les questions d'une partie bot : tirage aléatoire strict dans les langues demandées,
 * mappé vers le modèle du domaine game. Appelé <b>avant</b> l'envoi de la commande : aucun I/O
 * inter-service ne s'exécute dans un handler de commande.
 * <p>Le mapping theme → game est volontairement dupliqué côté matchmaking : un {@code *-domain}
 * ne dépend jamais d'un autre service.
 */
@Service
public class GameQuestionDrawer {

    private final QueryGateway queryGateway;

    public GameQuestionDrawer(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    public CompletableFuture<List<GameQuestion>> draw(String topicId, Set<Language> languages) {
        if (languages == null || languages.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        return queryGateway
                .query(new QuestionQuery.GetRandomApprovedQuestionsQuery(
                                topicId, GameRules.TOTAL_ROUNDS, languages),
                        QueryResponseTypes.multipleInstancesOf(Question.class))
                .thenApply(questions -> questions.stream()
                        .map(GameQuestionDrawer::toGameQuestion)
                        .toList());
    }

    private static GameQuestion toGameQuestion(Question question) {
        Map<Language, GameQuestionContent> translations = new EnumMap<>(Language.class);
        if (question.contents() != null) {
            for (Map.Entry<Language, QuestionContent> content : question.contents().entrySet()) {
                translations.put(content.getKey(), toGameQuestionContent(content.getValue()));
            }
        }
        return new GameQuestion(
                question.questionId(),
                translations,
                question.imageUrl(),
                question.difficulty() != null ? question.difficulty().name() : null,
                toGameQuestionChoice(question.correctAnswer()));
    }

    private static GameQuestionContent toGameQuestionContent(QuestionContent content) {
        return new GameQuestionContent(content.text(), toGameQuestionChoices(content.answers()));
    }

    private static GameQuestionChoice toGameQuestionChoice(QuestionChoice questionChoice) {
        return isNull(questionChoice) ? null : GameQuestionChoice.valueOf(questionChoice.name());
    }

    private static Map<GameQuestionChoice, String> toGameQuestionChoices(Map<QuestionChoice, String> questionChoices) {
        Map<GameQuestionChoice, String> gameQuestionChoices = new EnumMap<>(GameQuestionChoice.class);
        if (isNull(questionChoices) || questionChoices.isEmpty()) {
            return gameQuestionChoices;
        }
        for (Map.Entry<QuestionChoice, String> entry : questionChoices.entrySet()) {
            GameQuestionChoice choice = toGameQuestionChoice(entry.getKey());
            if (!isNull(choice)) {
                gameQuestionChoices.put(choice, entry.getValue());
            }
        }
        return gameQuestionChoices;
    }
}
