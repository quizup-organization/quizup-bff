# AGENTS.md — quizup-bff

> **BFF unique** : façade REST + WebSocket des services QuizUp (devenus headless).
> Architecture : Axon Framework (query/command bus distribué) + consommation Kafka + STOMP.
> Pour les règles de patterns :
> [`../../best-practices/.backend/hexagonal-architecture.md`](../../best-practices/.backend/hexagonal-architecture.md).

---

## 1. Rôle

- Expose **la seule surface HTTP/WS** applicative (`/api`, `/ws`).
- Compose les lectures via le **query bus** Axon (`QueryGateway`) et les écritures via le
  **command bus** (`CommandGateway`).
- Consomme le **flux d'événements Kafka** (`@ProcessingGroup`) et **fan-out** les notifications
  vers un socket client unique (`/topic/games/{id}`, `/topic/lobbies/{id}`, `/topic/social/{userId}`,
  `/topic/presence/{userId}`).
- Porte la **présence joueur** (sessions STOMP client → `quizup-profile`).

**Package** : `io.github.quizup.bff` · **Port** : `8092` (local) / `8080` (prod) · **DB** : `quizup_bff`

---

## 2. Conventions

- **REST orienté ressources** : pluriel kebab-case, `GET /{id}`, `POST /{collection}/search`,
  transitions `POST /{id}/{action}`, `DELETE` pour la suppression/retrait.
- **Enrichissements autorisés** : sous-collections renvoyant la ressource liée complète
  (`/api/profiles/{userId}/followers`) et **embeds lecture seule** (opposant, sujet) — pour
  supprimer le fan-out côté client.
- **Actor** : le `userId` provient du JWT (`SecurityHelper`) et est injecté dans les commandes
  (le bus ne propage pas le contexte de sécurité).
- Ne jamais joindre plusieurs ressources côté client : la composition vit ici.

---

## 3. Non périmètre

- Aucune logique métier d'agrégat (elle reste dans les services headless).
- Pas d'event store propre (seul le token store Axon, fourni par le SDK).
