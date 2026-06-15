# Module: Admin Platform (PRD-017 + ADR-017)

Super-administrador da plataforma — usuário sem vínculo com assessoria, acessa `/admin/**` para configurar integrações globais e gerenciar todas as assessorias.

Referência: [[17-admin-plataforma]], [[adr-017-admin-platform-role]].

## Tipo `ADM` e bootstrap

- `Usuario.tipo` aceita `INTERNO` ou `ADM`
- `Usuario.assessoria_id` é nullable; check constraint força `tipo=ADM ⇒ assessoria_id IS NULL` e `tipo=INTERNO ⇒ assessoria_id IS NOT NULL`
- **Primeiro signup em sistema vazio** (`count(usuarios WHERE tipo=ADM AND deleted_at IS NULL) = 0`) cria ADM em vez de assessoria. Próximos signups voltam ao fluxo normal
- ADMs adicionais via convite ADM-a-ADM (`POST /admin/usuarios/convidar` com `tipo=ADM`)
- Ambientes pré-existentes: rodar `scripts/create-first-admin.sh` (ver `docs/runbooks/admin-bootstrap.md`)

## Autenticação

- JWT do ADM: `{ tipo: "ADM", assessoria_id: null, permissions: ["ADMN", ...] }`
- MFA TOTP **obrigatório** — login com `mfa_ativo=false` retorna `{ next: "mfa_setup", token_temp }`
- Token temp de setup só acessa `/auth/mfa-setup`, `/auth/mfa-verify`, `/auth/logout`
- Pós-MFA confirm, login retorna JWT completo (60min) + refresh token (30d)
- Reuso de `MFA_KEY` para cifra do `mfa_secret_enc` (AES-GCM)

## Permission `ADMN`

- Adicionada em `PermissionCodes.java` + `lib/rbac.ts`
- Usuários `tipo=ADM` têm `ADMN` **implícito** (não precisam de Perfil atribuído)
- `OWNR` de assessoria **não** tem `ADMN` — explícito: OWNER ≠ ADM
- `@RequirePermission("ADMN")` em todos endpoints `/api/v1/admin/**`

## Bypass de TenantAspect

```java
// TenantAspect.before()
if (principal.tipo == TipoUsuario.ADM) {
    adminAuditService.logTenantBypass(principal.id, requestPath, tenantIdEffetivo);
    return; // sem checar tenant
}
// fluxo atual: 403 se tenant != principal.assessoriaId
```

ADM passa por qualquer endpoint com qualquer `assessoria_id` em path/payload. Cada bypass gera linha em `admin_audit_log` com `acao=TENANT_BYPASS_READ` ou `TENANT_BYPASS_WRITE` (decidido pelo HTTP method).

## Endpoints `/api/v1/admin/**`

Todos exigem `ADMN`. Todos registram em `admin_audit_log`.

| Método | Path | Descrição |
|---|---|---|
| GET | `/admin/dashboard` | Contagens agregadas: assessorias, usuários, e-mails, status integrações |
| GET | `/admin/integrations/email` | Lista contas SMTP globais |
| POST | `/admin/integrations/email` | Cria conta SMTP global (senha cifrada via `EmailCipherService`) |
| PUT | `/admin/integrations/email/{id}` | Edita conta SMTP global |
| DELETE | `/admin/integrations/email/{id}` | Remove conta SMTP global (soft-delete) |
| GET/POST/PUT/DELETE | `/admin/integrations/whatsapp[/{id}]` | CRUD credenciais Meta Cloud API (tokens cifrados via `WhatsappCipherService`) |
| GET/PUT | `/admin/integrations/ai-worker` | URL + healthcheck do AI worker |
| GET/POST/PUT | `/admin/integrations/social/{provider}` | App credentials Instagram/YouTube/TikTok |
| GET | `/admin/assessorias?page=&filter=` | Lista paginada de assessorias |
| GET | `/admin/assessorias/{id}` | Drill-down: usuários, prospecções, marcas, influenciadores |
| PUT | `/admin/assessorias/{id}` | Edita (status, plano, etc.) |
| POST | `/admin/assessorias/{id}/anonymize` | Anonimiza assessoria (LGPD) |
| GET/POST/PUT/DELETE | `/admin/assessorias/{id}/influenciadores[/{infId}]` | CRUD cross-tenant (TenantAspect bypassed) |
| (idem para `marcas`, `contatos`, `prospeccoes`, `tarefas`, `email-accounts`) | | |
| GET | `/admin/usuarios?tipo=&assessoria_id=` | Lista global de usuários |
| POST | `/admin/usuarios/convidar` | Convida novo ADM (body: `{ email, tipo: "ADM" }`) |
| PUT | `/admin/usuarios/{id}/status` | Ativa/desativa qualquer usuário |
| GET | `/admin/feature-flags` | Lista flags atuais |
| PUT | `/admin/feature-flags/{key}` | Toggle de flag (persistido em `platform_feature_flags`) |
| GET | `/admin/audit?cursor=&filter=` | Cursor-paginated `admin_audit_log` |

Convenções de erro seguem `docs/specs/api/`: 401 (token ausente/inválido), 403 (falta `ADMN` ou MFA não confirmado), 404 (recurso não existe), 422 (validação).

## Tabela `admin_audit_log`

```sql
CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES usuarios(id),
    acao TEXT NOT NULL,
    assessoria_id_afetada UUID NULL REFERENCES assessorias(id),
    recurso TEXT NULL,
    recurso_id UUID NULL,
    payload_antes JSONB NULL,
    payload_depois JSONB NULL,
    ip TEXT NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Ações registradas (`acao`)

| Código | Quando |
|---|---|
| `LOGIN` | Login bem-sucedido de ADM |
| `LOGIN_FAIL` | Falha de senha ou MFA |
| `MFA_SETUP` | ADM ativou MFA |
| `MFA_RESET` | ADM (outro) resetou MFA de um ADM |
| `CONFIG_UPDATE` | Mudança em integração global |
| `INTEGRATION_UPDATE` | Cifra de token de integração rotacionada |
| `ASSESSORIA_EDIT` | CRUD em recurso de assessoria (cross-tenant) |
| `USUARIO_EDIT` | Status/perfil alterado |
| `USUARIO_INVITE` | Convite enviado |
| `FEATURE_FLAG_CHANGE` | Toggle de flag |
| `TENANT_BYPASS_READ` | GET cross-tenant via bypass |
| `TENANT_BYPASS_WRITE` | POST/PUT/DELETE cross-tenant |
| `EXPORT` | Export CSV/JSON de dados |

### Imutabilidade

Triggers `BEFORE UPDATE` e `BEFORE DELETE` levantam exceção:

```sql
RAISE EXCEPTION 'admin_audit_log is append-only (no UPDATE/DELETE)';
```

Dev local que precisa truncar usa `ALTER TABLE admin_audit_log DISABLE TRIGGER ALL; TRUNCATE; ALTER TABLE admin_audit_log ENABLE TRIGGER ALL;` — documentado no runbook de reset de DB.

### Retenção

5 anos (consistente com LGPD para logs de tratamento por interesse legítimo). Particionamento por mês quando atingir 1M linhas (não MVP).

## Aspect `@AdminAudit`

```java
@AdminAudit(acao = "ASSESSORIA_EDIT", recurso = "influenciador")
@PutMapping("/admin/assessorias/{assessoriaId}/influenciadores/{id}")
public ResponseEntity<?> updateInfluenciador(...) { ... }
```

Aspect intercepta:
- `payload_antes`: snapshot do recurso pré-mudança (via repository.findById)
- `payload_depois`: corpo da resposta ou estado pós-save
- `assessoria_id_afetada`: lê do path variable ou do recurso
- `ip` + `user_agent`: do `HttpServletRequest`
- Falha em logar **não aborta a operação** — só logback WARN (decisão consciente: disponibilidade > completude da trilha em runtime; LGPD coberto por trigger no DB)

## Feature flags runtime

Tabela `platform_feature_flags`:

```sql
CREATE TABLE platform_feature_flags (
    key TEXT PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_by UUID NULL REFERENCES usuarios(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

`AppProperties.Features.isEnabled(key)`:

1. Cache Caffeine TTL 30s
2. Cache miss → query DB
3. DB miss ou erro → fallback `${ENV_VAR:default}` do `application.yml`

Mudança no painel → `INSERT ... ON CONFLICT (key) DO UPDATE` + invalida cache (em-memória; cluster: aceitar 30s de stale entre instâncias).

Flags suportadas inicialmente: `FEATURE_PORTAL_ENABLED`, `FEATURE_WHATSAPP_ENABLED`, `FEATURE_SOCIAL_ENABLED`, `FEATURE_AI_MATCH_ENABLED`, `FEATURE_MOBILE_ENABLED`, `FEATURE_SIGNUP_ENABLED`, `FEATURE_IMPORT_ENABLED`, `FEATURE_COMPLIANCE_STRICT`. `FEATURE_ADMIN_ENABLED` **não** é runtime — desativar painel exige restart (boot-time).

## Rate limit

`/api/v1/admin/**` tem bucket dedicado: 60 req/min por `admin_id` (não por IP). Aspect existente de rate-limit consulta `X-Admin-Id` derivado do JWT. Excedeu → 429.

## Segurança adicional

- **CSP no painel `/admin`** mais restrito: `default-src 'self'`; sem CDN externa (vs painel de assessoria que aceita Google Fonts)
- **Sem links cross-origin** dentro de `/admin` que não passem por confirmação
- **Logout invalida refresh token** (reusa infra existente)
- **Sessão concorrente**: ADM pode ter múltiplas sessões (laptop + celular); cada uma com refresh token próprio. Listagem de sessões ativas em `/admin/conta/sessoes` (Fase 2)

## Métricas e alertas

- `admin_actions_total{acao, admin_id}` (counter)
- `tenant_bypass_total{admin_id, assessoria_id}` (counter)
- `admin_login_failures_total{admin_id}` (counter)
- Alerta: `rate(admin_actions_total[1m]) > 100` — ação massiva inesperada (possível abuso ou bug)
- Alerta: `rate(admin_login_failures_total[5m]) > 5` — possível brute-force em conta ADM

## UI

Rota `apps/web/app/(admin)/` (route group separado de `(app)`):

```
(admin)/
  layout.tsx              ← AdminShell (sidebar específica, badge "ADMIN")
  page.tsx                ← dashboard
  integrations/
    email/page.tsx
    whatsapp/page.tsx
    ai-worker/page.tsx
    social/page.tsx
  assessorias/
    page.tsx              ← lista
    [id]/
      page.tsx            ← drill-down
      influenciadores/page.tsx
      marcas/page.tsx
      ...
  usuarios/page.tsx
  feature-flags/page.tsx
  audit/page.tsx
```

Acentos visuais distintos do painel de assessoria (cor de aviso, badge persistente). Tokens em `docs/specs/design-system/`.

`apps/web/middleware.ts` redireciona ADM logado em `/` para `/admin`; INTERNO em `/admin/*` para `/` (sem acesso).

## Gotchas

- **Primeira `Usuario.assessoria_id` nullable**: queries que assumem NOT NULL (ex: `JOIN assessoria ON u.assessoria_id = a.id`) viram outer join ou exigem filtro `WHERE u.tipo = 'INTERNO'`. Revisar repositories existentes.
- **`@RequirePermission("ADMN")`** não funciona via Perfil — aspect precisa checar `tipo=ADM` primeiro. Atualizar `RequirePermissionAspect` em conjunto com a migração.
- **Trigger anti-UPDATE em `admin_audit_log`** quebra `flyway repair` se algum dia precisar re-checksumar a tabela. Não usar `repair` em prod; ambiente local desabilita trigger temporariamente.
- **Cache de feature flags em-memória**: cluster com >1 instância tem janela de 30s de inconsistência entre nós. Para flag crítica de segurança, baixar TTL para 5s ou usar pub/sub (Fase 2).
- **Bootstrap em sistema com seed de testes**: integração que cria assessoria mock no setup pode "queimar" a porta do primeiro signup. Solução: `IntegrationTestBase` cria ADM via Repository direto (não via signup) antes de tudo.
- **Convite ADM-a-ADM**: e-mail de convite passa por SMTP configurado pelo ADM — se SMTP não estiver configurado ainda, convite falha. Fallback: aceitar convite via token mostrado na UI do ADM convidante (copy/paste). Documentar.
- **Anonimização cross-tenant**: ADM rodando `/admin/assessorias/{id}/anonymize` em assessoria com dados ativos é destrutivo. Confirmação dupla na UI + `payload_antes` completo no audit para reversão manual em até 30 dias.
