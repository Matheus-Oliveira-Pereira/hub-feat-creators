# ADR-017: Administrador de Plataforma — tipo ADM cross-tenant

> **SUPERSEDED by [[adr-018-single-tenant]] em 2026-05-25.** Sistema migrou para single-tenant — tipo ADM deixa de fazer sentido. Permissões consolidam em OWNR (perfil RBAC). Feature flags continuam acessíveis via permissão `FLAG`. Audit log de ADM removido.

## Status
Superseded by [[adr-018-single-tenant]] — 2026-05-25 (originalmente Proposed — 2026-05-22)

## Context

[[17-admin-plataforma]] introduz um perfil de operador da plataforma, sem vínculo com nenhuma assessoria, capaz de configurar integrações globais (e-mail, WhatsApp, AI worker, social OAuth) e dar suporte cross-tenant. Hoje:

- `Usuario.assessoria_id` é `NOT NULL` — modelo presume usuário pertence a uma assessoria
- `TenantAspect` (`@Order(3)`) bloqueia acesso fora do tenant do principal — bypass não existe
- `audit_log` (PRD-001) é mutável via UPDATE/DELETE — não serve para registro privilegiado
- Permissions 4-letter (ADR-015) são por assessoria; não há gate global
- Onboarding (PRD-006) só cria usuários OWNER vinculados a uma assessoria recém-criada

Critérios da decisão:

- **Bootstrap zero-touch** — ambiente novo precisa nascer sem SQL/SSH manual
- **Trilha LGPD imutável** — ADM acessa dados de titulares de qualquer tenant; legítimo interesse exige rastro
- **MFA-first** — privilégio máximo não pode ficar atrás de só senha
- **Mínima invasão no modelo** — não reescrever `Usuario`/`TenantAspect`; estender com flag
- **Backward compat** — usuários existentes (OWNER/ASSESSOR) seguem inalterados
- **Sem aumentar superfície de ataque acidentalmente** — primeiro signup vira ADM só em sistema vazio

## Decision

### 1. Tipo `ADM` em `Usuario.tipo`

Adicionar `ADM` ao enum existente `Usuario.tipo` (hoje `INTERNO`). `CreatorUser` permanece tipo separado.

- `Usuario.assessoria_id` vira **nullable** (migração V19); validação: `tipo = 'ADM' → assessoria_id IS NULL` AND `tipo = 'INTERNO' → assessoria_id IS NOT NULL` (check constraint)
- JWT do ADM: claim `tipo: "ADM"`, `assessoria_id: null`, permissions inclui `ADMN`

### 2. Bootstrap "primeiro signup vira ADM"

`AuthService.signup()` executa em transação:

```
IF count(usuarios WHERE tipo='ADM' AND deleted_at IS NULL) == 0 THEN
  cria Usuario(tipo=ADM, assessoria_id=null, mfa_ativo=false)
  retorna { user, mfa_required: true }
ELSE
  fluxo normal: cria assessoria + OWNER
END
```

Ambientes pré-existentes (com assessorias já criadas) — primeiro ADM por script `scripts/create-first-admin.sh`, documentado em runbook. Sem self-service de ADM pós-bootstrap (evita escalação acidental).

ADMs adicionais: convite ADM-a-ADM (`POST /admin/usuarios/convidar` com `tipo=ADM`), reusando infra de convite do PRD-006 mas com `aceitar-convite` forçando MFA setup.

### 3. Bypass de `TenantAspect`

`TenantAspect` no `@Before` checa `principal.tipo`:

```java
if (principal.tipo == ADM) {
    // ADM passa sem checar tenant — log de bypass em admin_audit_log
    return;
}
// fluxo atual: rejeita se tenant != principal.assessoriaId
```

ADM pode chamar qualquer endpoint, com qualquer `assessoria_id` em path/payload. Não há "trocar de tenant" — `assessoria_id` vem do recurso, não do principal.

### 4. Permission `ADMN`

- Adicionar `ADMN` em `PermissionCodes.java` + `lib/rbac.ts`
- `@RequirePermission("ADMN")` em todos endpoints `/admin/**`
- `RequirePermissionAspect` reconhece: `tipo=ADM` tem `ADMN` implicitamente (não precisa de `Perfil` atribuído)
- `OWNR` da assessoria não tem `ADMN` — explícito: OWNER de assessoria ≠ ADM de plataforma

### 5. MFA TOTP obrigatório para ADM

- Login ADM com `mfa_ativo=false` → resposta `{ next: "mfa_setup", token_temp: "..." }` (token só vale para endpoints de setup)
- `JwtAuthFilter` recusa qualquer outro endpoint com token de ADM sem MFA confirmado (exceção: `/auth/mfa-setup`, `/auth/mfa-verify`, `/auth/logout`)
- Reuso da infra existente (campos `mfa_secret_enc`, `mfa_ativo`, `MFA_KEY`) — sem código novo de TOTP

### 6. `admin_audit_log` append-only

Tabela dedicada:

```sql
CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES usuarios(id),
    acao TEXT NOT NULL,  -- LOGIN, MFA_SETUP, CONFIG_UPDATE, ASSESSORIA_EDIT, USUARIO_EDIT, INTEGRATION_UPDATE, FEATURE_FLAG_CHANGE, TENANT_BYPASS_READ, TENANT_BYPASS_WRITE
    assessoria_id_afetada UUID NULL REFERENCES assessorias(id),  -- NULL = ação global
    recurso TEXT NULL,         -- ex: "influenciador", "email_account"
    recurso_id UUID NULL,
    payload_antes JSONB NULL,  -- estado antes da mudança
    payload_depois JSONB NULL, -- estado após
    ip TEXT NULL,
    user_agent TEXT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_admin_audit_admin_created ON admin_audit_log(admin_id, created_at DESC);
CREATE INDEX idx_admin_audit_acao ON admin_audit_log(acao, created_at DESC);
CREATE INDEX idx_admin_audit_assessoria ON admin_audit_log(assessoria_id_afetada, created_at DESC) WHERE assessoria_id_afetada IS NOT NULL;
CREATE INDEX idx_admin_audit_payload ON admin_audit_log USING GIN (payload_depois);

-- Trigger anti-mutação
CREATE OR REPLACE FUNCTION admin_audit_log_immutable() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'admin_audit_log is append-only (no UPDATE/DELETE)';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER admin_audit_log_no_update BEFORE UPDATE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION admin_audit_log_immutable();
CREATE TRIGGER admin_audit_log_no_delete BEFORE DELETE ON admin_audit_log
    FOR EACH ROW EXECUTE FUNCTION admin_audit_log_immutable();
```

Aspect `@AdminAudit` registra automaticamente em controllers `/admin/**`. Bypass de TenantAspect também loga (linhas `acao=TENANT_BYPASS_READ/WRITE`).

### 7. Feature flags runtime via DB

Tabela `platform_feature_flags`:

```sql
CREATE TABLE platform_feature_flags (
    key TEXT PRIMARY KEY,
    enabled BOOLEAN NOT NULL,
    updated_by UUID NULL REFERENCES usuarios(id),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

`AppProperties.Features` consulta DB com cache 30s (Caffeine); fallback para env var se DB indisponível ou key ausente. ADM altera via `/admin/feature-flags`.

### 8. Decisão de NÃO-impersonation

ADM não "vira" OWNER de assessoria. Edita via UI `/admin/assessorias/{id}/**` que internamente chama os mesmos services, mas mantém `principal = ADM`. Audit fica claro: linha é `acao=ASSESSORIA_EDIT` por ADM, não confunde com OWNER real.

## Alternatives considered

1. **Coluna `is_platform_admin` em vez de tipo ADM** — mais simples (um booleano), mas espalha checks em vários lugares (`if (user.assessoriaId == null || user.isPlatformAdmin)`). Tipo no enum é semanticamente mais limpo e força tratamento explícito em switches.

2. **Tenant fake `__platform__`** — criar uma assessoria-fantasma onde ADM mora. Reusa modelo existente sem nullable, mas espalha lógica condicional ("se assessoria_id = __platform__, é ADM") em todo lugar. Pior que nullable + check constraint.

3. **Impersonation com sessão temporária** (`/admin/impersonate/{assessoriaId}` emite JWT temporário com `acting_as_admin: true`) — UX melhor para suporte, mas embaralha audit (ação foi do ADM ou do OWNER?), risco de vazar JWT. Adiado para Fase 2 se virar dor.

4. **Audit em `audit_log` existente com flag `is_admin_action`** — menos código, mas mistura escopos (audit de assessoria e audit de plataforma têm retenção, ACL e queries diferentes). Separar evita esses choques.

5. **MFA opcional para ADM** — mais simples no MVP, mas inseguro: conta com poder total atrás de só senha viola princípio de defense in depth. Custo do TOTP é pequeno (infra existe), benefício alto.

6. **Sem feature flag `FEATURE_ADMIN_ENABLED`** — default sempre ativo. Mas em ambientes onde ADM já existe e não vai ser usado, manter flag permite desativar superfície sem remover código.

## Consequences

### Positive

- **Bootstrap zero-touch**: ambientes novos sobem sem intervenção manual
- **Trilha LGPD imutável**: trigger no DB garante audit append-only (defesa em profundidade — mesmo SQL ad-hoc não consegue alterar)
- **Modelo mínimo**: única mudança estrutural é `usuarios.assessoria_id` nullable + check constraint + tabelas novas
- **MFA TOTP** já existia (PRD-006) — sem código novo de cripto
- **Feature flags runtime**: ADM ativa/desativa funcionalidades sem deploy

### Negative

- **`assessoria_id` nullable** — código existente que assumia NOT NULL precisa revisar (services, queries). Mitigação: check constraint força a invariante; testes de integração cobrem.
- **TenantAspect com branch** — adiciona ramo no hot path. Custo: um `if (tipo == ADM) return;`. Insignificante.
- **Duas tabelas de audit** (`audit_log` e `admin_audit_log`) — duplica esquema. Aceitável: escopos e retenção diferentes.
- **Bootstrap "primeiro signup vira ADM"** só funciona em sistema 100% vazio. Em ambientes existentes, runbook obriga script. Mitigação: documentar claramente.

### Risks

- **Escalação acidental**: bug em `JwtAuthFilter` que infere `tipo=ADM` erradamente vira RCE de privilégio. Mitigação: testes específicos (`AdminAuthIT`) + check constraint no DB + auditoria de cada PR que toca esse arquivo.
- **Bypass TenantAspect mal logado**: ação de ADM em assessoria sem entrada em `admin_audit_log` = vazamento sem trilha. Mitigação: aspect `@AdminAudit` em **todos** controllers `/admin/**`; teste de integração que verifica linha de audit por ação.
- **Cache de feature flags stale**: ADM desativa flag, mas processos em-vôo seguem habilitados por até 30s. Aceitável: documentar no painel. Para flag crítica de segurança, reduzir TTL ou invalidar explicitamente.
- **Trigger no DB confunde dev local**: dev tenta `DELETE FROM admin_audit_log` em ambiente local e leva erro. Documentar; truncar pelo `psql` com `ALTER TABLE ... DISABLE TRIGGER` em scripts de reset.

## Impact on specs

- **Compliance (`docs/specs/compliance/`)** — adicionar base legal para ADM acessar dados cross-tenant (legítimo interesse: suporte operacional). Atualizar ROPA na V19 (registro de tratamento `admin_platform_access`).
- **Security (`docs/specs/security/`)** — novo perfil de risco alto; rate-limit dedicado `/admin/**` (60 req/min/admin), CSP mais restrito no painel, logs de tentativas de login ADM em alerta separado.
- **RBAC (`docs/specs/rbac/`)** — `ADMN` no catálogo; observação que tipo `ADM` tem `ADMN` implícito (não via Perfil).
- **Observability (`docs/specs/observability/`)** — métricas `admin_actions_total{acao,admin_id}`, `tenant_bypass_total{admin_id,assessoria_id}`; alerta `rate(admin_actions_total[1m]) > 100` (ação massiva inesperada).
- **API (`docs/specs/api/`)** — convenção: prefixo `/api/v1/admin/**` requer `ADMN`; respostas seguem padrão de erro existente.
- **Data-architecture** — particionamento de `admin_audit_log` por mês quando atingir 1M linhas (não MVP).

## References

- PRD: [[17-admin-plataforma]]
- ADRs relacionados: [[adr-008-auth-jwt]] (JWT + MFA), [[adr-009-multi-tenant-strategy]] (TenantAspect — estendido), [[adr-011-lgpd-baseline]] (audit imutável, base legal), [[adr-015-rbac-roles-perfis]] (`ADMN`)
- Migração: V19 — schema `admin_audit_log`, `platform_feature_flags`, tipo ADM, nullable em `usuarios.assessoria_id`
- Runbook: `docs/runbooks/admin-bootstrap.md` (criar primeiro ADM em ambientes existentes)
