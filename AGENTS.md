# AGENTS.md — quizup-bff

> **BFF unique** : façade REST + WebSocket des services QuizUp (headless), personnalisée pour
> `quizup-web`. Architecture : Axon (query/command bus distribués) + Kafka + STOMP.
> Ce fichier est **normatif** pour la surface BFF ↔ web : toute route ajoutée ou modifiée doit
> s'y conformer et y être documentée.
> Patterns backend : [`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

- Expose **la seule surface HTTP/WS** applicative (`/api`, `/ws`).
- Compose les lectures via le **query bus** Axon (`QueryGateway`) et les écritures via le
  **command bus** (`CommandGateway`).
- Consomme le **flux d'événements Kafka** (`@ProcessingGroup`) et **fan-out** les notifications
  vers un socket client unique (`/topic/games/{id}`, `/topic/matchmaking/tickets/{id}`,
  `/topic/social/{userId}`, `/topic/presence/{userId}`).
- Porte la **présence joueur** (sessions STOMP client → `quizup-profile`) : chaque instance BFF a
  un identifiant stable (`application:host`), purge au démarrage ses sessions antérieures et
  retente les commandes de présence tant que le routage distribué n'est pas prêt.

**Package** : `io.github.quizup.bff` · **Port** : `8092` (local) / `8080` (prod) · **DB** : `quizup_bff`

Le `quizup-mobile` n'est **pas** branché sur cette surface (migration dédiée ultérieure).

---

## 2. Conventions de surface

- **Surface hybride** :
  - ressources explicites + query params typés pour les collections
    (`GET /api/topics?category=&sort=&page=&size=`) ;
  - endpoints d'**agrégation nommés** pour les écrans multi-services
    (`GET /api/me`, `GET /api/home`, `GET /api/topics/{id}/overview`).
- **Aucun `POST /{collection}/search` exposé.** Les use cases de recherche restent dans les
  services headless et sont réservés aux futures surfaces d'administration. Le BFF ne compose
  jamais un compteur, une facette, un historique ou une liste de page à partir d'une recherche
  générique : il utilise une requête dédiée (batch / count / facettes).
- **DTOs de façade uniquement** : `*View` (lecture) et `*Request` (écriture). Jamais de read
  model interne exposé (`Challenge`, `Game`, `Lobby`, `Profile`, `Topic`…), jamais de `Map`,
  `Object` ou cast non typé.
- **Notifications web** : DTOs d'infrastructure BFF (`GameNotification`, `TicketNotification`,
  `SocialNotification`) enveloppés dans `EventEnvelopeResponse` (ossature Jackson du web). Les
  événements reçus du bus sont des `EventEnvelope` SDK au payload typé (`eventType`) ; un
  `*-domain` ne porte **ni notification ni annotation framework**.
- **Pagination** : `?page=&size=` → `PageResponse<T> { content, page, size, totalElements,
  totalPages, first, last }`. **Tris par enum documenté** (phrase d'URL/valeurs fermées), jamais
  de nom de propriété arbitraire.
- **Relations** : `PUT /api/{resource}/{id}/follow` (idempotent) puis
  `DELETE /api/{resource}/{id}/follow` (`204`).
- **Transitions d'état** : `POST /api/{resource}/{id}/{action}`. `DELETE` est réservé à la
  disparition effective de la ressource.
- **Composition côté BFF** : enrichissements (profils, niveaux, présences, compteurs, noms,
  avatars) systématiquement résolus ici, jamais côté client. Les lectures batch passent par des
  requêtes dédiées (`GetProfilesByIdsQuery`, `GetProgressionsByIdsQuery`,
  `GetPresencesByIdsQuery`…).
- **Actor** : le `userId` provient du JWT (`SecurityHelper`) ; jamais dans le body.
- **Session périmée** : un JWT encore valide dont le `user_id` n'existe plus dans identity (base
  purgée, compte supprimé) est rejeté en `401` par `CurrentUserExistsFilter` sur `/api/**` —
  le client purge sa session et repasse par le login. Existence résolue via
  `UserExistsByIdQuery` (cache court 30 s) ; identity injoignable ⇒ fail-open.
- **Entrées** : `@Valid`, enums stricts, `ProblemDetail` en erreur. Aucun parsing permissif,
  aucune valeur par défaut inventée (`level=1`, `OFFLINE` fabriqué…).
- **Composition asynchrone** : combiner les `QueryGateway.query()` (pas de `.join()` bloquant
  sur le thread HTTP).
- Contrôleurs **minces** : aucune composition ni règle métier dans un `*Controller` ; tout vit
  dans un service d'`application/`.

---

## 3. Surface cible (normative)

### Compte / coquille

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/me` | `MeView` | Profil + progression + stats de duel + `followingCount` + `followersCount` + `pendingChallengesCount` |
| `GET /api/suggestions?q=&limit=` | `List<SuggestionView>` | Palette ⌘K : sujets + joueurs (`type`, `id`, libellé, visuel) |
| `GET /api/clock` | `ServerTimeView` | `serverTime`, `epochMillis` |

### Accueil

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/home` | `HomeView` | `followedTopics` (récents) + `trendingTopics` (les plus joués, via `game`) |

### Sujets (theme + social + leaderboard + profile)

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/topics?q=&category=&followed=&sort=&page=&size=` | `PageResponse<TopicCardView>` | `sort` ∈ `POPULAR\|ALPHA` ; `followed=true` filtre côté serveur |
| `GET /api/topics/facets?q=&followed=` | `TopicFacetsView` | `total` + `categories[{ category, label, count }]` |
| `GET /api/topic-categories` | `List<TopicCategoryView>` | `category`, `label` |
| `GET /api/topics/{topicId}/overview` | `TopicOverviewView` | `topic`, `follow`, `followersCount`, `myRank`, `myProgress`, `questionsCount` |
| `PUT /api/topics/{topicId}/follow` | `204` | Idempotent |
| `DELETE /api/topics/{topicId}/follow` | `204` | |
| `GET /api/topics/{topicId}/leaderboard?period=&month=&scope=&page=&size=` | `TopicLeaderboardView` | `entries` paginées + `me` ; `period` ∈ `ALL_TIME\|MONTHLY`, `month` (`YYYY-MM`, mensuel uniquement, défaut = mois courant), `scope` ∈ `WORLD\|FOLLOWING\|COUNTRY` |

### Joueurs / personnes (profile + social + leaderboard + game)

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/profiles/{userId}` | `PlayerProfileView` | Profil + progression + stats + counts + présence + `follow` + `isMe` |
| `GET /api/profiles/{userId}/following?q=&sort=&page=&size=` | `PageResponse<PlayerCardView>` | `sort` ∈ `RECENT\|LEVEL\|ALPHA` |
| `GET /api/profiles/{userId}/followers?q=&sort=&page=&size=` | `PageResponse<PlayerCardView>` | idem |
| `PUT /api/profiles/{userId}/follow` | `204` | Idempotent |
| `DELETE /api/profiles/{userId}/follow` | `204` | |
| `GET /api/profiles/{userId}/games?topicId=&opponentId=&page=&size=` | `PageResponse<GameHistoryItemView>` | Historique enrichi (adversaire, sujet, résultat, XP) |
| `GET /api/profiles/{userId}/head-to-head?against=` | `HeadToHeadView` | V/N/D entre deux joueurs |
| `GET /api/profiles/{userId}/activity?from=&to=` | `ActivityView` | Streak + graphe |
| `PUT /api/profiles/{userId}` | `200` | `displayName`, `bio`, `country`, `avatarOptions` |
| `GET /api/presence/{userId}` | `PresenceView` | `404` si le joueur ne s'est jamais connecté (l'absence vaut hors ligne) ; poussé aussi en WS |

### Défis (social)

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/challenges?box=&status=&page=&size=` | `PageResponse<ChallengeCardView>` | `box` ∈ `RECEIVED\|SENT\|ALL` ; enrichi adversaire + sujet + actions |
| `GET /api/challenges/pending-count` | `PendingCountView` | Badge de navigation |
| `GET /api/challenges/{challengeId}` | `ChallengeDetailView` | Participants + sujet + runs + `gameId` + scores + `winnerId` + `completedAt` |
| `POST /api/challenges` | `201 + Location` | Body `{ challengedId, topicId }` |
| `POST /api/challenges/{challengeId}/accept\|decline\|cancel` | `200` | |
| `POST /api/challenges/{challengeId}/runs` | `200` | Body `{ gameId }` |

### Duel (game)

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/games` | `201 + Location` | Body `{ topicId, mode: BOT\|ASYNC, difficulty?, opponentId?, ghostGameId? }` |
| `POST /api/games/{gameId}/answer` | `200` | Body `{ choice }` |
| `POST /api/games/{gameId}/abandon` | `200` | **Toujours valide** : le BFF route `cancel` si la partie n'a pas démarré |
| `POST /api/games/{gameId}/cancel` | `200` | Avant démarrage |
| `GET /api/games/{gameId}/notifications` | `List<EventEnvelopeResponse>` (payload `GameNotification`) | Même contrat que le push WS |

### Matchmaking (matchmaking)

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/matchmaking/tickets` | `201 + Location` + `MatchmakingTicketView` | Body `{ topicId }` |
| `GET /api/matchmaking/tickets/{ticketId}` | `MatchmakingTicketView` | Lit le **read model ticket** du service (statuts `SEARCHING\|MATCHED\|CANCELLED`, `gameId`, `opponentId`, `vsBot`) |
| `POST /api/matchmaking/tickets/{ticketId}/cancel` | `200` | |
| `GET /api/matchmaking/tickets/{ticketId}/notifications` | `List<EventEnvelopeResponse>` (payload `TicketNotification`) | Même contrat que le push WS |

---

## 4. WebSocket (STOMP)

Endpoint `/ws` (SockJS) ; broker `/topic`. Une connexion par client, JWT en `CONNECT`.

| Destination | Payload |
|---|---|
| `/topic/games/{gameId}` | `EventEnvelopeResponse` (payload `GameNotification`) |
| `/topic/matchmaking/tickets/{ticketId}` | `EventEnvelopeResponse` (payload `TicketNotification`) (`SEARCHING`, `MATCHED`, `CANCELLED`) |
| `/topic/social/{userId}` | `EventEnvelopeResponse` (payload `SocialNotification`) |
| `/topic/presence/{userId}` | `PresenceView` — **exception assumée** : événements de session non séquencés, pas d'enveloppe |

`EventEnvelopeResponse` : `aggregateId`, `sequenceNumber`, `timestamp`, `eventType` (type web),
`payload` (DTO de notification). L'historique REST (`GET .../notifications`) et le push WS
partagent exactement le même contrat (dédup par `sequenceNumber` côté client). Les DTOs de
notification (`GameNotification`, `TicketNotification`, `SocialNotification`) vivent dans
`infrastructure/out/messaging/response/` ; **aucune notification ni annotation Jackson dans un
`*-domain`**. Le transport des événements du query bus est l'`EventEnvelope` du SDK (payload typé
via `eventType`, sans annotation), mappé ici vers `EventEnvelopeResponse`.

---

## 5. Non périmètre

- Aucune logique métier d'agrégat (elle reste dans les services headless).
- Pas d'event store propre (seul le token store Axon, fourni par le SDK).
- Pas de surface d'administration (les use cases de recherche restent dans les services).

---

## 6. Tests

- Un test `WebMvcTest` par contrôleur (gateways mockés) : chemin, paramètres, mapping DTO.
- Un test unitaire par service de composition (`application/`).
- `mvn -pl quizup-bff-infrastructure test` avant tout commit.
