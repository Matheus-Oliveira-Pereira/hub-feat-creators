# PRD-020: Bootstrap de Admin — Criação Automática do OWNR no Cold Start

## Context

No modelo single-tenant (PRD-018), o sistema só tem um conjunto de usuários. O primeiro
`signup` vira OWNER (`usuarioRepo.count() == 0`). Porém:

- Em Railway (prod), `FEATURE_SIGNUP_ENABLED=false` por segurança → ninguém consegue fazer
  signup → sistema fica inacessível no primeiro deploy.
- Se o DB for dropado/recriado (disaster recovery), o admin se perde e não há como recuperar
  sem acesso manual ao SQL ou reabrir signup.
- Não há garantia de que o perfil "Owner" com todas as permissões exista antes do primeiro login.

Solução: um listener `ApplicationReadyEvent` que roda no boot e, se nenhum OWNER existir,
cria automaticamente um usuário admin com credenciais configuradas por env vars, já com o
perfil Owner e todas as permissões (`PermissionCodes.OWNER_DEFAULT`).

## Objective

Garantir que ao subir o sistema em qualquer ambiente (prod, staging, DR) sempre exista ao
menos um usuário OWNER capaz de logar, sem intervenção manual em SQL.

## Scope

### Includes

- [ ] **`AdminBootstrap`** — `@Component` que escuta `ApplicationReadyEvent`
- [ ] Checagem: `usuarioRepo.findByRole(OWNER)` — se vazio → criar admin
- [ ] Credenciais via env vars:
  - `ADMIN_EMAIL` (default: `admin@hub.local`)
  - `ADMIN_PASSWORD` (default: `changeme-set-in-prod`) — hash Argon2id igual ao signup normal
- [ ] Perfil: atribuir perfil "Owner" via `RbacBootstrap.seedFor(OWNER)` (já existente)
- [ ] Email verificado: `emailVerificadoEm = now()` (admin não precisa verificar e-mail)
- [ ] Log obrigatório: `log.warn("admin.bootstrap.created email={}", email)` — visível em prod
- [ ] Idempotente: se OWNER já existe, **não faz nada** (nem loga em nível alto)
- [ ] `FEATURE_ADMIN_BOOTSTRAP_ENABLED` (default `true`) — permite desativar se quiser forçar
  fluxo manual

### Excludes

- [ ] Troca de senha do admin via UI (já existe `/forgot-password`)
- [ ] Múltiplos admins bootstrap (só 1)
- [ ] Admin criado via migration SQL (Flyway não tem acesso a env vars de senha em runtime)

## Acceptance Criteria

- **AC-1** — DB vazio + boot → usuário `ADMIN_EMAIL` criado com `role=OWNER`, `emailVerificadoEm != null`, perfil Owner atribuído.
- **AC-2** — Login com `ADMIN_EMAIL` + `ADMIN_PASSWORD` retorna JWT válido com `role=OWNER` e todas as permissions de `PermissionCodes.OWNER_DEFAULT`.
- **AC-3** — Boot com OWNER já existente → nenhum usuário novo criado, log em nível DEBUG.
- **AC-4** — `FEATURE_ADMIN_BOOTSTRAP_ENABLED=false` → bootstrap não executa mesmo sem OWNER.
- **AC-5** — `ADMIN_PASSWORD` padrão `changeme-set-in-prod` é hash Argon2id (nunca plaintext em DB).
- **AC-6** — `./mvnw verify` passa.

## Technical Decisions

- Usar `ApplicationReadyEvent` (não `@PostConstruct`) — garante que Flyway já rodou e JPA está pronto.
- `@Transactional` no handler → rollback se qualquer etapa falhar.
- Não criar novo endpoint — bootstrap é interno ao boot, sem surface de ataque.
- Senha padrão deve ser trocada imediatamente em prod (documentar no runbook).

## Related

- `RbacBootstrap` (já existente) — `seedFor(OWNER)` retorna ou cria o perfil Owner
- `PermissionCodes.OWNER_DEFAULT` — `Set<String>` com todos os códigos 4-letter
- `AuthService.signup()` — padrão a replicar (Argon2id hash, emailVerificadoEm)
- ADR-015 (RBAC), ADR-018 (single-tenant)
