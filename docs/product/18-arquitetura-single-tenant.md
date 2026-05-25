# PRD-018: Arquitetura Single-Tenant — Simplificação

## Context

O sistema foi originalmente projetado como SaaS multi-tenant — múltiplas assessorias compartilhando a mesma infraestrutura, isoladas via `assessoria_id` em todas as tabelas, `TenantAspect` filtrando queries, e um tipo `ADM` para administradores de plataforma com bypass cross-tenant.

Após implementação completa dos PRDs 1–17, ficou claro que:

- **Sobrecarga conceitual** — toda nova feature exige pensar em isolamento, owner vs assessor, permissões cross-tenant.
- **Complexidade no front** — portal usa `/[slug]/...`, listas filtram por `assessoria_id` implícito, RBAC + tenant filter convivem.
- **Sobrecarga operacional** — admin de plataforma + admin de assessoria + perfis RBAC = três níveis de autorização.
- **Free-tier policy (ADR-016)** — v1 não monetiza; não há demanda real por servir múltiplas assessorias simultâneas.
- **Roadmap real** — o produto será operado por uma única assessoria (a do próprio dono do projeto).

Decisão: **remover multi-tenancy**. Sistema vira single-tenant. Todos os usuários cadastrados acessam todos os dados. Autorização passa a ser puramente baseada em perfil RBAC (4-letter codes).

## Objective

Eliminar a camada de isolamento por `assessoria_id` em todo o sistema (DB, backend, frontend, mobile), mantendo o controle de acesso via perfis RBAC já existente.

## Scope

### Includes

- [x] Migration Flyway V20 — drop coluna `assessoria_id` de todas as tabelas, drop tabela `assessorias`, drop `admin_audit_log` (não faz mais sentido).
- [x] Backend — remover `TenantAspect`, `TenantContext`, `Assessoria` entity/repository/service/controller.
- [x] Backend — remover `Usuario.tipo` (não há mais ADM/INTERNO; só USUARIO). `assessoria_id` deixa de existir no JWT (`ass` claim).
- [x] Backend — simplificar `JwtAuthFilter` (sem branch INTERNO/ADM/CREATOR; só USUARIO + CREATOR).
- [x] Backend — `RequirePermissionAspect` simplificado (sem bypass implícito de ADM; OWNR ganha todas as permissões explicitamente).
- [x] Backend — remover endpoints `/api/v1/admin/assessorias`, `/api/v1/admin/usuarios` (ADM-scoped). `/api/v1/usuarios` e `/api/v1/perfis` continuam, agora com permissão `OWNR` ou `USRW`.
- [x] Backend — `platform_feature_flags` continua, agora protegida por permissão `FLAG` (nova).
- [x] Frontend — remover route group `(admin)/admin/**`; mover páginas úteis (feature-flags, audit) para `(app)/configuracoes/**` com permissão `OWNR`.
- [x] Frontend — remover route group `(portal)/[slug]/...` → vira `(portal)/...` (sem slug).
- [x] Frontend — remover `isAdmClaims`, simplificar `useAuth` (sem checagem de tipo).
- [x] Mobile — atualizar API client se houver `assessoriaId` em alguma chamada.
- [x] Documentação — atualizar todos os ADRs e PRDs com referência a multi-tenant. Marcar ADR-009 e ADR-017 como **Superseded by ADR-018**.

### Excludes

- [ ] **Voltar ao multi-tenant no futuro** — se v2 monetizar multi-tenant, restaurar de ADR-009 + PRD-006 (mantidos como registro histórico).
- [ ] **Renomear `Assessoria` em UI** — termo já era genérico; pode virar "Organização" no copy se fizer sentido (não no escopo deste PRD).
- [ ] **Remover perfis RBAC** — RBAC é exatamente a camada que sobrevive.
- [ ] **Remover Compliance LGPD** — base legal e DSR continuam (são por dado, não por assessoria).

## Not Doing (and why)

- **Múltiplas instâncias em paralelo** — se outro cliente precisar do sistema, sobe nova instância (Railway deploy). Não é multi-tenant via DB.
- **Branding por organização** — sem múltiplos tenants, não há porque parametrizar logo/cores no DB. Brand fixo (ADR-014).
- **Sub-organizações / equipes** — perfis RBAC já modelam grupos de permissão. Não criar tabela `equipes`.
- **Migration reversível** — drop de `assessoria_id` é destrutivo. Backup do DB antes de aplicar V20 (runbook).

## User Stories

- Como **operador da assessoria**, quero entrar no sistema sem precisar pensar em qual "tenant" estou, porque só existe uma realidade.
- Como **OWNR**, quero conceder acesso a um colaborador apenas selecionando um perfil RBAC, sem pensar em ADM/INTERNO.
- Como **desenvolvedor**, quero escrever uma feature nova sem precisar adicionar filtro de `assessoria_id` em cada query.
- Como **creator no portal**, quero acessar `/portal/login` sem precisar saber o slug da assessoria.

## Acceptance Criteria

- **AC-1** — Migration V20 dropa `assessoria_id` de todas as tabelas listadas (ver Migration Plan abaixo) sem erro. DB volta a subir.
- **AC-2** — Após migration, `SELECT * FROM information_schema.columns WHERE column_name='assessoria_id';` retorna 0 linhas.
- **AC-3** — Tabela `assessorias` deixa de existir.
- **AC-4** — Backend compila sem `Assessoria`, `TenantAspect`, `TenantContext`, `AssessoriaRepository`, `AssessoriaService`, `AssessoriaController`.
- **AC-5** — JWT gerado no login não contém claim `ass`. Contém `tipo: "USUARIO"` (ou nenhum tipo) + `role` + `permissions`.
- **AC-6** — Login + listagem de influenciadores funciona para todos os usuários (independente de qualquer "assessoria").
- **AC-7** — Endpoint `/api/v1/admin/*` retorna 404 ou foi movido para `/api/v1/configuracoes/*`.
- **AC-8** — Tela `/admin/**` no frontend deixa de existir. Equivalentes em `/configuracoes/**`.
- **AC-9** — Portal acessa em `/portal/login` (sem slug); creator faz login direto.
- **AC-10** — Suite de testes unitários passa sem alterações de mock relacionadas a `assessoriaId` (testes podem precisar refactor — aceitar).
- **AC-11** — Documentação atualizada: CLAUDE.md, ADR-009, ADR-017 marcados Superseded by ADR-018; PRD-006 marcado partially-superseded.

## Migration Plan (V20)

Tabelas a serem alteradas — drop coluna `assessoria_id`:

```sql
ALTER TABLE influenciadores       DROP COLUMN assessoria_id CASCADE;
ALTER TABLE marcas                DROP COLUMN assessoria_id CASCADE;
ALTER TABLE contatos              DROP COLUMN assessoria_id CASCADE;
ALTER TABLE prospeccoes           DROP COLUMN assessoria_id CASCADE;
ALTER TABLE tarefas               DROP COLUMN assessoria_id CASCADE;
ALTER TABLE emails_envios         DROP COLUMN assessoria_id CASCADE;
ALTER TABLE email_templates       DROP COLUMN assessoria_id CASCADE;
ALTER TABLE smtp_accounts         DROP COLUMN assessoria_id CASCADE;
ALTER TABLE email_layouts         DROP COLUMN assessoria_id CASCADE;
ALTER TABLE whatsapp_accounts     DROP COLUMN assessoria_id CASCADE;
ALTER TABLE whatsapp_templates    DROP COLUMN assessoria_id CASCADE;
ALTER TABLE whatsapp_envios       DROP COLUMN assessoria_id CASCADE;
ALTER TABLE eventos               DROP COLUMN assessoria_id CASCADE;
ALTER TABLE relatorios            DROP COLUMN assessoria_id CASCADE;
ALTER TABLE notificacoes          DROP COLUMN assessoria_id CASCADE;
ALTER TABLE perfis                DROP COLUMN assessoria_id CASCADE;
ALTER TABLE convites              DROP COLUMN assessoria_id CASCADE;
ALTER TABLE creator_users         DROP COLUMN assessoria_id CASCADE;
ALTER TABLE social_accounts       DROP COLUMN assessoria_id CASCADE;
ALTER TABLE briefings             DROP COLUMN assessoria_id CASCADE;
ALTER TABLE match_sugestoes       DROP COLUMN assessoria_id CASCADE;
ALTER TABLE jobs                  DROP COLUMN assessoria_id CASCADE;
ALTER TABLE refresh_tokens        DROP COLUMN assessoria_id CASCADE;
ALTER TABLE eventos_compliance    DROP COLUMN assessoria_id CASCADE;

-- Drop unique indices que tinham assessoria_id
DROP INDEX IF EXISTS uq_influenciador_handle_assessoria;
DROP INDEX IF EXISTS uq_marca_nome_assessoria;
-- (e outros)

-- Recriar unique globais
CREATE UNIQUE INDEX uq_influenciador_handle ON influenciadores
  USING btree ((handles->>'instagram')) WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_marca_nome ON marcas
  USING btree (lower(nome)) WHERE deleted_at IS NULL;

-- Usuario: drop tipo + assessoria_id
ALTER TABLE usuarios DROP COLUMN tipo;
ALTER TABLE usuarios DROP COLUMN assessoria_id CASCADE;

-- Drop tabelas inteiras
DROP TABLE admin_audit_log;
DROP TABLE assessorias CASCADE;
```

Verificar antes de aplicar: nenhuma FK órfã (CASCADE limpa). Backup: `pg_dump` antes.

## Technical Decisions

- Related ADR: [[adr-018-single-tenant]] (nova — supersedes [[adr-009-multi-tenant-strategy]] e [[adr-017-admin-platform-role]])
- Mantém: [[adr-008-auth-jwt]], [[adr-015-rbac-roles-perfis]], [[adr-011-lgpd-baseline]], [[adr-016-v1-free-tier-policy]]
- Modifica: [[adr-005-email-smtp-multi-conta]] (contas SMTP continuam multi-conta, mas globais), [[adr-006-whatsapp-cloud-api]] (idem).

## Impact on Specs

- **Compliance** — base_legal continua obrigatória. Sem mudança.
- **Security** — superfície reduzida (menos código = menos bugs). Cuidado: usuário malicioso autenticado agora vê tudo — RBAC vira ÚNICA defesa. Revisar todos os endpoints `@RequirePermission`.
- **Scalability** — sem mudança. Sem `assessoria_id` no índice composto, alguns ganhos de performance.
- **Observability** — remover labels de `assessoria_id` em métricas Prometheus.
- **Accessibility** — sem mudança.
- **i18n** — sem mudança.
- **Versioning** — bump API minor (`v1.x` → `v1.(x+1)`) — endpoints removidos, mas mesmo path/versão.

## PRDs Afetados — Resumo das Alterações

| PRD | O que muda |
|---|---|
| **PRD-001** Cadastros | Remove `assessoriaId` de payloads. Handles/nomes uniqueness vira global. |
| **PRD-002** Prospecção | Remove `assessoriaId`. Visibilidade ASSESSOR (created_by OR responsavel) continua via RBAC. |
| **PRD-003** Tarefas | Remove `assessoriaId`. Digest 07:00 BRT continua global. |
| **PRD-004** E-mail outbound | Contas SMTP viram globais. Templates globais. Layout único. |
| **PRD-005** RBAC | Mantém integral. **Vira a ÚNICA camada de autorização.** Adiciona permissão `FLAG` (gerenciar feature flags) e `USRW` (gerenciar usuários). |
| **PRD-006** Onboarding multi-tenant | **Partially-superseded** — signup vira único fluxo (sem auto-criar assessoria). Convite continua. MFA continua. Lockout continua. |
| **PRD-007** Compliance LGPD | Mantém. ROPA já documenta finalidade, não tenant. |
| **PRD-008** WhatsApp | Contas WhatsApp globais. Templates globais. |
| **PRD-009** Notificações | Mantém. Notificações por usuário. |
| **PRD-010** Histórico | Mantém. Eventos sem `assessoriaId`. |
| **PRD-011** Importação CSV | Mantém. Sem `assessoriaId` em payload. |
| **PRD-012** Relatórios | Mantém. MVs sem `assessoria_id`. |
| **PRD-013** Portal Creator | URL muda: `/portal/login` (sem slug). Branding fixo do sistema. CreatorUser continua. |
| **PRD-014** Mobile | Sem mudança no app (já não tinha noção de tenant — só JWT de creator). |
| **PRD-015** Social OAuth | Mantém. Tokens globais por creator. |
| **PRD-016** Match IA | Mantém. Sem `assessoriaId` no scoring. |
| **PRD-017** Admin Plataforma | **Superseded por este PRD-018.** Permissões `ADMN`/`OWNR` consolidam. Feature flags continuam (permissão `FLAG`). Audit log de ADM **removido** (não há mais ADM cross-tenant). |

## Rollout

- [x] **Feature flag**: nenhuma. Mudança destrutiva, sem rollback parcial.
- [x] **Data migration**: V20 dropa colunas/tabelas. Backup obrigatório (`pg_dump`) antes.
- [x] **Rollback plan**: restaurar dump pré-V20 + revert dos commits da branch `refactor/single-tenant`.
- [x] **Comunicação**: nenhuma — sistema ainda não está em produção pública. Operador único.
- [x] **Ordem de deploy**:
    1. Branch `refactor/single-tenant` aberta.
    2. Backup DB.
    3. Commit V20 + código backend simplificado + frontend.
    4. Rodar `./mvnw verify` + `pnpm lint test`.
    5. Deploy local. Smoke test (login, listar, criar, deletar).
    6. Se OK → merge para main.
