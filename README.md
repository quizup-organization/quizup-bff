# quizup-bff

**Backend For Frontend** unique de QuizUp : façade REST + WebSocket des services headless.

- Agrège les services via les **bus distribués Axon** (query/command).
- Diffuse les **notifications temps réel** (consommation Kafka, fan-out STOMP) sur un socket unique.
- Porte la **présence joueur**.
- Expose une API REST orientée ressources sous `/api`.

## Build

```bash
mvn install -DskipTests
```

## Configuration

- `application.yml` importe `quizup-defaults.yml` (SDK).
- `application-local.yml` : port `8092`, datasource `quizup_bff`, importe `quizup-local.yml`.
- `application-prod.yml` : port `8080`, importe `quizup-prod.yml`.

## Contrats

Dépend des `*-domain` publiés (GitHub Packages) : identity, theme, social, game, profile,
matchmaking, leaderboard.
