## [4.4.0](https://github.com/quizup-organization/quizup-bff/compare/v4.3.0...v4.4.0) (2026-10-05)

### Features

* **bff:** web push des notifications (abonnements VAPID + envoi) ([7ac039e](https://github.com/quizup-organization/quizup-bff/commit/7ac039ec9468cc7324a5cfc8592f3241a0d2eaf5))

## [4.3.0](https://github.com/quizup-organization/quizup-bff/compare/v4.2.0...v4.3.0) (2026-10-04)

### Features

* **bff:** suppression des notifications (DELETE + push NOTIFICATION_DELETED) ([d83d598](https://github.com/quizup-organization/quizup-bff/commit/d83d5980c46d2bf64df4e724f0e41e4ca04c8c6e))

## [4.2.0](https://github.com/quizup-organization/quizup-bff/compare/v4.1.1...v4.2.0) (2026-10-03)

### Features

* **bff:** topic authoring surface ([3e3fb01](https://github.com/quizup-organization/quizup-bff/commit/3e3fb01fd3b468047d548b9c863c85b6441953c0))

## [4.1.1](https://github.com/quizup-organization/quizup-bff/compare/v4.1.0...v4.1.1) (2026-10-03)

### Bug Fixes

* **bff:** invalidate profile read cache on updates ([cda159c](https://github.com/quizup-organization/quizup-bff/commit/cda159c2c9847ce08f3547d684f383508243edde))

## [4.1.0](https://github.com/quizup-organization/quizup-bff/compare/v4.0.0...v4.1.0) (2026-10-03)

### Features

* **bff:** notification inbox, nominative challenge and released domain pins ([769bb6a](https://github.com/quizup-organization/quizup-bff/commit/769bb6ae127618436b1613be52f2cb4f8ecc079e))

## [4.0.0](https://github.com/quizup-organization/quizup-bff/compare/v3.0.0...v4.0.0) (2026-10-02)

### ⚠ BREAKING CHANGES

* **bff:** matchmaking tickets + private lobbies surface

### Code Refactoring

* **bff:** matchmaking tickets + private lobbies surface ([c4f2d55](https://github.com/quizup-organization/quizup-bff/commit/c4f2d552c23ede7c614bcbf5f3051df166257372))

## [3.0.0](https://github.com/quizup-organization/quizup-bff/compare/v2.2.1...v3.0.0) (2026-09-30)

### ⚠ BREAKING CHANGES

* **bff:** profile updates are per-field endpoints (/pseudonym, /bio, /country, /avatar-options, /language); displayName renamed to pseudonym; MeView exposes language; RoundStartedNotification exposes translations; game creation passes the creator language (pins SDK 4.1.0, profile 3.0.0, theme 4.0.0, game 4.0.0, social 3.0.0, matchmaking 3.0.0, leaderboard 3.0.0).

### Features

* **bff:** per-field profile endpoints, pseudonym rename and language-aware games ([2dfea1c](https://github.com/quizup-organization/quizup-bff/commit/2dfea1c51cc084a15f49f04d869254542d5aaeac))

## [2.2.1](https://github.com/quizup-organization/quizup-bff/compare/v2.2.0...v2.2.1) (2026-09-28)

### Bug Fixes

* **bff:** reject stale sessions with 401 ([67429c1](https://github.com/quizup-organization/quizup-bff/commit/67429c1e2b47291276c11e452820141feec31373))

## [2.2.0](https://github.com/quizup-organization/quizup-bff/compare/v2.1.0...v2.2.0) (2026-09-27)

### Features

* **bff:** expose topic cover image in topic views ([f60af56](https://github.com/quizup-organization/quizup-bff/commit/f60af5648d626dbadafaa6b7955710e60a27d328))

## [2.1.0](https://github.com/quizup-organization/quizup-bff/compare/v2.0.0...v2.1.0) (2026-09-27)

### Features

* **bff:** hybrid REST/WS surface, dedicated views and resilient presence ([448f918](https://github.com/quizup-organization/quizup-bff/commit/448f918beb7d92967604d949319ed92a1106d881))

## [2.0.0](https://github.com/quizup-organization/quizup-bff/compare/v1.2.4...v2.0.0) (2026-09-25)

### ⚠ BREAKING CHANGES

* **bff:** all application traffic now goes through the BFF; the business
services are headless.

### Features

* **bff:** unique REST/WS surface for the headless services ([bb3f2a6](https://github.com/quizup-organization/quizup-bff/commit/bb3f2a65ddf26caefe75fa09c89cee4dad19b09c))

## [1.2.4](https://github.com/quizup-organization/quizup-bff/compare/v1.2.3...v1.2.4) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.4 (typed PageResult over query transport) ([862a3de](https://github.com/quizup-organization/quizup-bff/commit/862a3de16e71df2fca300feb98458673d7dc345c))

## [1.2.3](https://github.com/quizup-organization/quizup-bff/compare/v1.2.2...v1.2.3) (2026-09-24)

### Bug Fixes

* **bff:** read untyped rows for paginated followers (query-bus element erasure) ([a5fc431](https://github.com/quizup-organization/quizup-bff/commit/a5fc43130051c620780c727d953cbd3dc3b489fa))

## [1.2.2](https://github.com/quizup-organization/quizup-bff/compare/v1.2.1...v1.2.2) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.3 (bus-only search criteria type info) ([13a97b4](https://github.com/quizup-organization/quizup-bff/commit/13a97b467176550cf7c3744d216c6c7da3231da9))

## [1.2.1](https://github.com/quizup-organization/quizup-bff/compare/v1.2.0...v1.2.1) (2026-09-24)

### Bug Fixes

* **deps:** bump quizup-parent to 2.4.2 (search criteria type info) ([adbb16d](https://github.com/quizup-organization/quizup-bff/commit/adbb16d8a7abbe7d6996fa7628a3d69b2d02cb11))

## [1.2.0](https://github.com/quizup-organization/quizup-bff/compare/v1.1.0...v1.2.0) (2026-09-24)

### Features

* **bff:** resolve profiles in batch (GetProfilesByIds) with cache ([1abff2d](https://github.com/quizup-organization/quizup-bff/commit/1abff2d1e370e13f99219cf560558de742a06a48))

## [1.1.0](https://github.com/quizup-organization/quizup-bff/compare/v1.0.0...v1.1.0) (2026-09-24)

### Features

* **bff:** matchmaking queue endpoints (enqueue/ticket/cancel) ([46903af](https://github.com/quizup-organization/quizup-bff/commit/46903afd925e4561967464c032315fdce9e65222))

## 1.0.0 (2026-09-24)

### Features

* **bff:** initial quizup-bff facade (REST resources + realtime fan-out) ([e308de0](https://github.com/quizup-organization/quizup-bff/commit/e308de05c2d2f1eccfd2cc8d4c9a07f1a2755abd))
