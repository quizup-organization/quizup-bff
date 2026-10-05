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
  vers un socket client unique (`/topic/games/{id}`, `/topic/lobbies/{id}`,
  `/topic/notifications/{userId}`, `/topic/presence/{userId}`).
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
- **Notifications web** : DTOs d'infrastructure BFF (`GameNotification`, `LobbyNotification`,
  `MatchmakingNotification`, `NotificationView` pour l'inbox personnelle) enveloppés dans
  `EventEnvelopeResponse` (ossature Jackson du web). Les événements reçus du bus sont des
  `EventEnvelope` SDK au payload typé (`eventType`) ;
  un `*-domain` ne porte **ni notification ni annotation framework**. `RoundStartedNotification`
  expose `questionText`/`answers` (langue source) **et** `translations` (toutes les langues du
  snapshot, clé = code ISO 639-1) : le client choisit sa langue, avec repli sur la source.
  `GameCreatedNotification` expose `questionImageUrls` (images des questions, ordre des rounds)
  pour le préchargement client dès la création de la partie.
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
- **Cache de lecture profil** (`ProfileLookup`, Caffeine 30 s) : purgé par
  `ProfileUpdateService` après chaque écriture de profil réussie (y compris no-op) — sinon
  `GET /api/me` sert l'ancien profil jusqu'à l'expiration du TTL. Invalidation locale : suffisante
  avec le réplica unique du BFF en prod.
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
| `GET /api/me` | `MeView` | Profil (+ `language` fr/en) + progression + stats de duel + `followingCount` + `followersCount` |
| `GET /api/suggestions?q=&limit=` | `List<SuggestionView>` | Palette ⌘K : sujets + joueurs (`type`, `id`, libellé, visuel) |
| `GET /api/clock` | `ServerTimeView` | `serverTime`, `epochMillis` |

### Accueil

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/home` | `HomeView` | `followedTopics` (récents) + `trendingTopics` (les plus joués, via `game`) |

### Sujets (theme + social + leaderboard + profile)

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/topics?q=&category=&followed=&mine=&sort=&page=&size=` | `PageResponse<TopicCardView>` | `sort` ∈ `POPULAR\|ALPHA` (défaut `POPULAR`) ; `followed=true` filtre côté serveur ; `mine=true` = sujets créés par le joueur courant (brouillons + publiés, tri `updatedAt desc`), exclusif de `followed`/`q`/`category`/`sort` (400 sinon). `TopicCardView.status` expose le statut |
| `GET /api/topics/facets?q=&followed=` | `TopicFacetsView` | `total` + `categories[{ category, label, count }]` |
| `GET /api/topic-categories` | `List<TopicCategoryView>` | `category`, `label` |
| `GET /api/topics/{topicId}/overview` | `TopicOverviewView` | `topic`, `follow`, `followersCount`, `myRank`, `myProgress`, `canManage` (créateur) |
| `PUT /api/topics/{topicId}/follow` | `204` | Idempotent |
| `DELETE /api/topics/{topicId}/follow` | `204` | |
| `GET /api/topics/{topicId}/leaderboard?period=&month=&scope=&page=&size=` | `TopicLeaderboardView` | `entries` paginées + `me` ; `period` ∈ `ALL_TIME\|MONTHLY`, `month` (`YYYY-MM`, mensuel uniquement, défaut = mois courant), `scope` ∈ `WORLD\|FOLLOWING\|COUNTRY` |

### Auteur (sujets + questions)

Surface d'écriture **propriétaire uniquement** : le créateur du sujet gère son brouillon, ses
questions et la publication. Les commandes du sujet portent la vérification dans
`TopicAggregate` (`requestedBy == creatorId`) ; les questions sont vérifiées côté BFF (l'agrégat
question ne connaît pas le créateur du sujet) et répondent `403` `PERMISSION` sinon.

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/topics` | `201 + Location` | `{ name ≤25, description ≤500, category, emoji?, color?, imageUrl? }` → DRAFT |
| `PUT /api/topics/{topicId}/name\|description\|category\|emoji\|color\|image-url` | `204` | un champ par route (description/image/emoji/color `null` efface) |
| `POST /api/topics/{topicId}/publish` | `200` | garde ≥ 7 questions approuvées |
| `GET /api/topics/{topicId}/questions?page=&size=` | `PageResponse<QuestionEditorView>` | tous statuts, contenus FR/EN + statut + difficulté |
| `POST /api/topics/{topicId}/questions` | `201 + Location` | contenus localisés `[{ language, text, answers[A-D] }]` + `correctAnswer` + `imageUrl?` → PENDING |
| `POST /api/questions/{questionId}/translations` | `200` | ajout d'une langue absente (contenu complet) |
| `PUT /api/questions/{questionId}/text\|answers\|correct-answer\|image-url` | `204` | `language` dans le body pour text/answers |
| `POST /api/questions/{questionId}/approve` | `200` | |
| `POST /api/questions/{questionId}/reject` | `200` | `{ reason ≤500 }` optionnel |


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
| `PUT /api/profiles/{userId}/pseudonym` | `204` | `{ pseudonym }` |
| `PUT /api/profiles/{userId}/bio` | `204` | `{ bio }` (`null` efface) |
| `PUT /api/profiles/{userId}/country` | `204` | `{ country }` (`null` efface) |
| `PUT /api/profiles/{userId}/avatar-options` | `204` | `{ avatarOptions }` (`null` efface) |
| `PUT /api/profiles/{userId}/language` | `204` | `{ language }` ∈ `fr\|en` (enum strict) |
| `GET /api/presence/{userId}` | `PresenceView` | `404` si le joueur ne s'est jamais connecté (l'absence vaut hors ligne) ; poussé aussi en WS |

### Appariement public (matchmaking)

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/matchmaking/tickets` | `201 + Location` | Body `{ topicId }` — lance la recherche ; 5 s sans adversaire → partie bot |
| `GET /api/matchmaking/tickets/{ticketId}` | `MatchmakingTicketView` | Statut `SEARCHING\|MATCHED\|CANCELLED\|FAILED`, `gameId`, `opponentId`, `vsBot` |
| `POST /api/matchmaking/tickets/{ticketId}/cancel` | `200` | Annule la recherche |
| `GET /api/matchmaking/tickets/{ticketId}/notifications` | `List<EventEnvelopeResponse>` (payload `MatchmakingNotification`) | Même contrat que le push WS |

### Défis nominatifs (matchmaking)

Intention **asynchrone** (TTL 1 h) distincte de la salle temps réel : l'acceptation crée la salle
(saga matchmaking) qui porte présence, compte à rebours et partie.

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/challenges` | `201 + Location` | Body `{ topicId, opponentId }` — l'adversaire doit exister (`404` sinon) |
| `GET /api/challenges/mine` | `List<ChallengeView>` | Défis `PENDING` où le joueur est challenger ou opposant |
| `GET /api/challenges/{challengeId}` | `ChallengeView` | Sujet, challenger/opponent, statut (`PENDING\|ACCEPTED\|DECLINED\|CANCELLED\|EXPIRED`), `roomId` dès l'acceptation |
| `POST /api/challenges/{challengeId}/accept` | `200` | Invité uniquement ; la salle est créée par la saga (`roomId`) |
| `POST /api/challenges/{challengeId}/decline` | `200` | Invité uniquement |
| `POST /api/challenges/{challengeId}/cancel` | `200` | Lanceur uniquement (défi sans réponse) |

### Salons privés (matchmaking)

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/lobbies` | `201 + Location` | Body `{ topicId }` — **salon partagé** par lien `/join/{lobbyId}` ; le défi nominatif passe par `POST /api/challenges` (`400` si `opponentId`) |
| `GET /api/lobbies/mine` | `List<LobbyView>` | Salons `CREATED` du joueur (initiateur **ou** invité) — filet de reprise |
| `GET /api/lobbies/{lobbyId}` | `LobbyView` | Sujet, statut, `phase` (`WAITING_PARTICIPANT\|WAITING_PRESENCE\|READY\|COMPLETED\|MISSED\|CLOSED\|FAILED`), adversaire, `nominative`, `awaitingMe`, présences, `readyDeadlineAt`, `missedReason`, `gameId` |
| `POST /api/lobbies/{lobbyId}/join` | `200` | Rejoint le salon (idempotent, acceptation) ; nominatif : invité uniquement (403 sinon) |
| `POST /api/lobbies/{lobbyId}/enter` | `200` | **Présence temps réel** (idempotent) ; quand les deux sont entrés, compte à rebours de 3 s puis création de la partie |
| `POST /api/lobbies/{lobbyId}/decline` | `200` | Refus d'un défi nominatif (invité uniquement) |
| `POST /api/lobbies/{lobbyId}/leave` | `200` | Sortie **non destructive** (idempotente) : le salon reste ouvert, retour possible ; seul `cancel` (initiateur) ferme le salon |
| `POST /api/lobbies/{lobbyId}/cancel` | `200` | Annulation par l'initiateur |
| `GET /api/lobbies/{lobbyId}/notifications` | `List<EventEnvelopeResponse>` (payload `LobbyNotification`) | Même contrat que le push WS |

> Un état terminal (`CLOSED`/`FAILED`) est conservé le temps de la rétention (2 min) puis purgé :
> les commandes tardives répondent 404 (pré-vérification BFF) ou un `Problem` métier, jamais un 500.

### Notifications personnelles (quizup-notification)

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/notifications?unreadOnly=&page=&size=` | `PageResponse<NotificationView>` | Inbox du joueur courant |
| `GET /api/notifications/unread-count` | `{ count }` | Badge de la cloche |
| `POST /api/notifications/{notificationId}/read` | `200` | Propriétaire uniquement (404/403 sinon) |
| `DELETE /api/notifications/{notificationId}` | `204` | Hard delete — propriétaire uniquement (404/403 sinon) |
| `DELETE /api/notifications` | `204` | Vide l'inbox du joueur courant (hard delete, fan-out de commandes unitaires idempotentes) |
| `POST /api/notifications/read-all` | `200` | Fan-out de commandes unitaires idempotentes |
| `GET /api/notification-preferences` | `List<{ category, enabled }>` | `category` ∈ `FOLLOW\|LOBBY` (défaut activé) |
| `PUT /api/notification-preferences/{category}` | `204` | `{ enabled }` |

### Web Push (navigateur)

Abonnements par navigateur portés par le BFF (`push_subscription`, migration `V2__push_subscriptions.sql`) :
l'envoi est branché sur le processing group `notification-push` (même flux que le fan-out STOMP,
hors thread du processor). Payload structuré (`type`, `actorPseudonym`, `sourceId`, `path`…) ;
le Service Worker web compose le texte et route le clic. Configuration `quizup.push.vapid.*`
(clé publique dans le ConfigMap, privée dans le secret sealed) : clés absentes ⇒ push désactivé.

| Endpoint | Réponse | Notes |
|---|---|---|
| `GET /api/push/vapid-public-key` | `{ publicKey }` | `404` si Web Push non configuré ; clé = `applicationServerKey` du `PushManager.subscribe` |
| `PUT /api/push/subscriptions` | `204` | idempotent, body `{ endpoint, keys: { p256dh, auth } }`, actor = JWT (upsert/rebind) |
| `DELETE /api/push/subscriptions?endpoint=…` | `204` | désabonnement au logout (propriétaire uniquement) |

### Duel (game)

| Endpoint | Réponse | Notes |
|---|---|---|
| `POST /api/games` | `201 + Location` | Body `{ topicId, difficulty? }` — duel contre un bot uniquement |
| `GET /api/games/current` | `CurrentGameView` | Partie en attente/en cours la plus récente (`CREATED\|READY\|IN_PROGRESS`) ; `204` s'il n'y en a aucune (reprise — pas d'erreur pour une absence normale) |
| `POST /api/games/{gameId}/join` | `200` | Entrée dans la salle d'attente de l'arène (idempotent) |
| `POST /api/games/{gameId}/leave` | `200` | Quitte la salle d'attente avant démarrage (annule la partie) |
| `POST /api/games/{gameId}/answer` | `200` | Body `{ choice }` |
| `POST /api/games/{gameId}/abandon` | `200` | Forfait en cours (`ForfeitGameCommand`) — l'adversaire gagne |
| `POST /api/games/{gameId}/cancel` | `200` | Annulation avant démarrage |
| `GET /api/games/{gameId}/notifications` | `List<EventEnvelopeResponse>` (payload `GameNotification`) | Même contrat que le push WS |

---

## 4. WebSocket (STOMP)

Endpoint `/ws` (SockJS) ; broker `/topic`. Une connexion par client, JWT en `CONNECT`.

| Destination | Payload |
|---|---|
| `/topic/games/{gameId}` | `EventEnvelopeResponse` (payload `GameNotification`) |
| `/topic/lobbies/{lobbyId}` | `EventEnvelopeResponse` (payload `LobbyNotification`) (`CREATED`, `JOINED`, `DECLINED`, `COMPLETED`, `CANCELLED`, `EXPIRED`, `FAILED`, `ROOM_ENTERED`, `LEFT`, `ALL_PRESENT`, `MISSED`) |
| `/topic/matchmaking/tickets/{ticketId}` | `EventEnvelopeResponse` (payload `MatchmakingNotification`) (`SEARCHING`, `MATCHED`, `CANCELLED`, `FAILED`) |
| `/topic/notifications/{userId}` | `EventEnvelopeResponse` (payload `NotificationView`) — inbox personnelle (invitations de défi, follows) ; suppression poussée sous `NOTIFICATION_DELETED` (payload `{ notificationId }`) |
| `/topic/presence/{userId}` | `PresenceView` — **exception assumée** : événements de session non séquencés, pas d'enveloppe |
| `/topic/follow-presence/{userId}` | `FollowPresenceView` (`actorId`, `pseudonym`, `avatarOptions`, `at`) — **fan-out éphémère** : quand un joueur passe en ligne, chaque **abonné** reçoit « X est en ligne » ; jamais persisté (pas d'inbox), poussé par le groupe `follow-presence-notification` |

`EventEnvelopeResponse` : `aggregateId`, `sequenceNumber`, `timestamp`, `eventType` (type web),
`payload` (DTO de notification). L'historique REST (`GET .../notifications`) et le push WS
partagent exactement le même contrat (dédup par `sequenceNumber` côté client). Les DTOs de
notification (`GameNotification`, `LobbyNotification`, `MatchmakingNotification`, `NotificationView`)
vivent dans `infrastructure/out/messaging/response/` ou `in/api/response/` ; **aucune notification
ni annotation Jackson dans un `*-domain`**. Le transport des événements du query bus est
l'`EventEnvelope` du SDK (payload typé via `eventType`, sans annotation), mappé ici vers
`EventEnvelopeResponse`.

---

## 5. Non périmètre

- Aucune logique métier d'agrégat (elle reste dans les services headless).
- Pas d'event store propre (seul le token store Axon, fourni par le SDK).
- Pas de back-office global : la surface d'**auteur** (`/api/topics` écriture, `/api/questions`)
  est ouverte à tout joueur pour **ses propres** sujets ; les use cases de recherche génériques
  restent réservés aux futures surfaces d'administration.
- Erreurs de façade : les `BaseProblem` levés hors handlers Axon (gardes propriétaire, chaînage
  asynchrone) sont mappés par `BffProblemExceptionHandler` (RFC 7807, `PERMISSION` → 403).

---

## 6. Tests

- Un test `WebMvcTest` par contrôleur (gateways mockés) : chemin, paramètres, mapping DTO.
- Un test unitaire par service de composition (`application/`).
- `mvn -pl quizup-bff-infrastructure test` avant tout commit.
