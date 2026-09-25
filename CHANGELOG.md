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
