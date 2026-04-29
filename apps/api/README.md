# API — HUB Feat Creators

Spring Boot 3.3 + Java 21.

## Setup wrapper (uma vez)
```
mvn -N wrapper:wrapper
```
Gera `mvnw` / `mvnw.cmd` / `.mvn/wrapper/`.

## Comandos
- `./mvnw spring-boot:run` — dev
- `./mvnw test` — unit + integration (Testcontainers exige Docker)
- `./mvnw verify` — build + spotless + tests
- `./mvnw spotless:apply` — format

## Endpoints
- `http://localhost:8080/actuator/health`
- `http://localhost:8080/swagger-ui.html`

## Próximo passo
Implementar PRD-001 (`docs/product/01-cadastros-mvp.md`) — schema Flyway V1 + auth JWT + multi-tenant.
