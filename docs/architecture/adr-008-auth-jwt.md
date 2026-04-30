# ADR-008: Autenticação via JWT com refresh rotation

## Status
Accepted — 2026-04-29

## Context
PRD-001 (cadastros MVP) exige autenticação multi-usuário multi-tenant: assessora cria conta, faz login, convida equipe. Stack já decidiu Spring Security (ADR-002). Falta escolher esquema de sessão/credencial.

Critérios:
- **Stateless**: api roda em Railway com possibilidade de múltiplas réplicas; sessão server-side exige store compartilhado (Redis), custo extra no MVP
- **Mobile-ready**: Expo (Fase 2) consome mesma API — JWT em SecureStore é padrão estabelecido
- **Multi-tenant**: claim `assessoria_id` no token elimina lookup extra a cada request
- **Revogação**: token roubado precisa ser invalidado sem esperar expiração natural
- **Lib madura em Java 21**: assinatura/verificação sem CVEs recentes

## Decision

### Esquema
- **Access token JWT** (HS256, segredo de 256 bits via `JWT_SECRET`) — vida curta (60 min default, configurável via `JWT_EXPIRATION_MINUTES`)
- **Refresh token opaco** (UUID v7, hash SHA-256 no DB) — vida 30 dias, **rotation a cada uso** (refresh antigo invalidado, novo emitido)
- **Refresh family**: cada login gera `family_id`; reuso detectado de refresh já consumido → invalida família inteira (proteção contra roubo)
- Tabela `refresh_token`: `token_hash`, `family_id`, `usuario_id`, `assessoria_id`, `expires_at`, `revoked_at`, `replaced_by`, `user_agent`, `ip`

### Claims do access token
```
sub: <usuario_id UUID>
ass: <assessoria_id UUID>     # tenant claim — usado pelo filtro multi-tenant (ADR-009)
role: OWNER|ASSESSOR
jti: <uuid>                    # para denylist se necessário
iat, exp
```

### Library
- **`io.jsonwebtoken:jjwt`** (versão **0.11.5**) — API estável; 0.12.x quebrou compatibilidade de API (`parserBuilder()` removido) durante impl → pinado em 0.11.5
- Descartado `auth0/java-jwt`: API mais antiga, menos opinião sobre claims types

### Hashing de senha
- **BCrypt** (`BCryptPasswordEncoder`, fator 12) — **Argon2id descartado na implementação**: `de.mkammerer:argon2-jvm` não existe no Maven Central sem binários nativos; Spring Security's `Argon2PasswordEncoder` requer Bouncy Castle com binding nativo indisponível em Railway (JVM pura). BCrypt com fator 12 atende OWASP para MVP (min 100ms em hardware moderno).
- Reavaliar Argon2id quando tiver infra dedicada ou lib pura consolidada.

### Revogação
- Logout: revoga refresh family
- "Sair de todas sessões": revoga todas families do usuário
- Access token NÃO tem denylist por padrão (vida curta) — se necessário no futuro, adicionar Redis com TTL = expiração

### Endpoints
- `POST /api/v1/auth/login` → `{ access, refresh }`
- `POST /api/v1/auth/refresh` → rotation
- `POST /api/v1/auth/logout` → revoga family atual
- `POST /api/v1/auth/signup` → cria assessoria + OWNER

## Alternatives considered

1. **Sessão server-side (Spring Session + Redis)**
   - + revogação trivial, segredo nunca sai do servidor
   - − exige Redis no MVP (custo + complexidade)
   - − mobile precisa cookies cross-origin (CORS + SameSite=None) — mais frágil
   - Descartado

2. **JWT sem refresh (só access longo)**
   - + simples
   - − roubo de token sobrevive até expiração (horas/dias)
   - Descartado: viola padrão LGPD (mitigação de comprometimento)

3. **OAuth2 / OIDC com provider externo (Keycloak, Auth0)**
   - + features prontas (MFA, social login, audit)
   - − custo $$$ (Auth0) ou ops (Keycloak self-hosted)
   - Reavaliar quando precisarmos de SSO/SAML para clientes enterprise

4. **PASETO em vez de JWT**
   - + sem armadilhas de algoritmo (alg=none, RS256/HS256 confusion)
   - − ecosistema Java escasso, menos integração Spring
   - Descartado pelo custo de adoção

## Consequences

**Positivas**:
- API stateless → escalar Railway horizontalmente sem Redis
- Mobile (Expo) usa mesmo fluxo
- Tenant claim no token → filtro multi-tenant zero-lookup (ADR-009)
- Refresh rotation + family detection é state-of-the-art OWASP

**Negativas**:
- Segredo HS256 compartilhado entre todas réplicas — rotação de segredo é evento coordenado
- Tabela de refresh tokens cresce; precisa job de purga (`expires_at < now() - 7 days`)

**Riscos**:
- HS256 escolhido por simplicidade no MVP. Se múltiplos serviços precisarem validar token, migrar para RS256 (par chave pública/privada). Rastreado como tech debt.
- `JWT_SECRET` em variável de ambiente — vazamento implica re-emissão de todos tokens. Mitigação: rotacionar trimestralmente; alarme em logs `auth_failed_signature_invalid`.
- JJWT pinado em 0.11.5 — migração para 0.12+ exige refactor da API de parsing (`getBody()` → `getPayload()`, builders). Rastreado como tech debt.
- BCrypt (em lugar de Argon2id) é adequado para MVP mas menos resistente a ASIC que Argon2id. Migrar em versão futura se performance do servidor permitir Bouncy Castle nativo.

## Impact on specs
- **security**: definir parâmetros BCrypt (fator 12), denylist policy, rotação de segredo
- **api**: endpoints `/auth/*`, header `Authorization: Bearer <jwt>`
- **observability**: métricas `auth_login_total`, `auth_refresh_total`, `auth_revoked_total`; log de tentativa falha (sem PII)
- **data-architecture**: tabela `refresh_token` + índice `(token_hash)` UNIQUE, `(usuario_id, family_id)`, `(expires_at)` para purga
- **compliance**: log de auth não persiste senha nem token claro; LGPD ok

## References
- PRD: `docs/product/01-cadastros-mvp.md`
- ADR-002 (stack base)
- ADR-009 (multi-tenant strategy) — consome `ass` claim
- jjwt: https://github.com/jwtk/jjwt
- OWASP JWT cheatsheet: https://cheatsheetseries.owasp.org/cheatsheets/JSON_Web_Token_for_Java_Cheat_Sheet.html
- Refresh rotation reuse detection: https://auth0.com/docs/secure/tokens/refresh-tokens/refresh-token-rotation
