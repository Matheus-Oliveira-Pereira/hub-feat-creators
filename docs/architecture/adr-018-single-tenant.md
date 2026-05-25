# ADR-018: Arquitetura Single-Tenant

- **Status**: Accepted
- **Date**: 2026-05-25
- **Supersedes**: [[adr-009-multi-tenant-strategy]], [[adr-017-admin-platform-role]]
- **Driver**: [[18-arquitetura-single-tenant]] (PRD)

## Contexto

ADR-009 estabeleceu multi-tenancy compartilhado via coluna `assessoria_id` em todas as tabelas, `TenantAspect` (Spring AOP) filtrando queries no service layer, e `TenantContext` propagado por request.

ADR-017 adicionou um tipo de usuário `ADM` (super-administrador da plataforma) com bypass do filtro multi-tenant, auditado em `admin_audit_log`.

Após implementação completa (PRDs 1–17), três sinais convergem para remoção do multi-tenancy:

1. **Custo cognitivo** — toda feature nova exige raciocínio sobre isolamento, ownership e cross-tenant.
2. **Política free-tier (ADR-016)** — v1 não monetiza, não há demanda real por servir múltiplas assessorias simultâneas.
3. **Realidade operacional** — o sistema é operado por uma única assessoria (a do dono do projeto).

## Decisão

**Adotar arquitetura single-tenant**: remover toda noção de `assessoria_id`, `TenantAspect`, `TenantContext`, `Assessoria` entity, e tipo `ADM`. Manter RBAC (ADR-015) como **única camada de autorização**.

### Consequências práticas

- **DB**: V20 dropa coluna `assessoria_id` de ~24 tabelas, dropa `assessorias` e `admin_audit_log`. Unique indexes recriados como globais.
- **Backend**: `TenantAspect` removido, `JwtAuthFilter` simplificado (sem branch ADM/INTERNO; só USUARIO + CREATOR), `RequirePermissionAspect` simplificado (sem bypass ADM).
- **JWT**: payload deixa de ter `ass`. Mantém `usuarioId`, `role`, `permissions`, opcionalmente `tipo` (`USUARIO` ou `CREATOR`).
- **Frontend**: route group `(admin)` removido. Páginas globais movem para `(app)/configuracoes/**` com permissão `OWNR`. Portal sai de `/[slug]/...` para `/portal/...`.
- **Autorização**: OWNR vira super-perfil com todas as permissões explicitamente concedidas (não mais via bypass implícito).
- **Audit log**: tabela `admin_audit_log` removida. Auditoria geral continua via `eventos` (PRD-010).

## Alternativas consideradas

- **Manter ADR-009 e simplesmente cadastrar uma única assessoria** — rejeitada: deixa código pago (TenantAspect, ADM, slug portal) sem valor.
- **Adotar schema-per-tenant** — rejeitada: não resolve o problema (ainda multi-tenant).
- **Esperar v2 para decidir** — rejeitada: cada PR multi-tenant é débito que acumula.

## Riscos

- **Defesa em profundidade reduzida** — antes, mesmo se RBAC falhasse, TenantAspect protegia cross-tenant. Agora RBAC é único gate. Mitigação: revisar TODOS os `@RequirePermission` em endpoints; auditoria estática garantida pelo `PermissionAnnotationValidator` (ADR-015).
- **Reintrodução de multi-tenancy é cara** — se v2 quiser tenants, restaurar de zero. Aceito: opção explícita.
- **Migration destrutiva** — V20 não tem rollback automático. Mitigação: `pg_dump` antes; testar em DB recriado primeiro.

## Status de outros ADRs

| ADR | Status pós-018 |
|---|---|
| ADR-009 multi-tenant strategy | **Superseded by ADR-018** |
| ADR-017 admin platform role | **Superseded by ADR-018** |
| ADR-015 RBAC roles perfis | Mantém — vira ÚNICA camada de autorização |
| ADR-008 auth JWT | Mantém — payload simplifica (sem `ass`) |
| ADR-005 email SMTP multi-conta | Mantém — contas viram globais (não por assessoria) |
| ADR-006 WhatsApp Cloud API | Mantém — contas viram globais |
| ADR-011 LGPD baseline | Mantém |
| ADR-016 v1 free-tier policy | Mantém — reforça a decisão |

## Referências

- PRD: [[18-arquitetura-single-tenant]]
- Migration: `apps/api/src/main/resources/db/migration/V20__drop_multi_tenant.sql`
- Branch sugerida: `refactor/single-tenant`
