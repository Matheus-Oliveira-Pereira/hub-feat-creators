# ADR-015: RBAC com roles 4-letter + Perfis por assessoria

## Status
Accepted — 2026-05-01

## Context

Sistema (PRD-001) tem só 2 roles coarse (`OWNER`/`ASSESSOR`) suficientes pra demo, insuficientes pra produção:

- PRD-002 (prospecção) exige isolar visibilidade entre assessores
- PRD-003 (tarefas) e PRD-004 (e-mail) virão com permissões granulares por feature
- Assessorias reais têm "estagiário só lê", "comercial sem deletar", "OWNER" — composições que ENUM coarse não cobre

Critérios:
- **Granularidade** sem matar performance — checagem em hot path tem que ser rápida
- **Padrão claro de nomenclatura** — devs não inventam código paralelo
- **Backward compat** com PRD-001 (não quebrar OWNER que já existe)
- **Gestão pelo OWNER da assessoria** sem precisar de support engineer
- **Defense in depth** — frontend esconde, backend rejeita
- **Tenant isolation** mantido (perfis por assessoria, não globais)

## Decision

### Modelo de roles

**Role = string de 4 caracteres**: `[ação][entidade]`

- **Ação (1 letra)**: `B` Browse, `C` Change (criar), `E` Edit (modificar), `D` Delete
- **Entidade (3 letras)**: PRO Prospecção, MAR Marca, INF Influenciador, CON Contato, USU Usuário (CRUD de equipe), ASS Assessoria (config), TAR Tarefa, EML E-mail, REL Relatório, PRF Perfil

**Roles especiais não-CRUD** (4 letras quando faz sentido fonético):
- `OWNR` — owner da assessoria, bypassa qualquer check
- `INVT` — convidar usuário (não é CRUD)
- `EXPT` — exportar CSV/relatórios (sensível LGPD)
- `BLLG` — browse audit log (auditoria)

**Princípio**: códigos atômicos, fáceis de combinar. Profile = `string[]` desses códigos.

### Entidade `Perfil`

```sql
CREATE TABLE perfis (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  assessoria_id UUID NOT NULL REFERENCES assessorias(id),
  nome TEXT NOT NULL,
  descricao TEXT,
  roles TEXT[] NOT NULL DEFAULT '{}',
  is_system BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  deleted_at TIMESTAMPTZ NULL,
  UNIQUE (assessoria_id, nome)
);

CREATE INDEX idx_perfis_tenant ON perfis(assessoria_id, deleted_at);
CREATE INDEX idx_perfis_roles_gin ON perfis USING GIN (roles);

ALTER TABLE usuarios ADD COLUMN profile_id UUID REFERENCES perfis(id);
CREATE INDEX idx_usuarios_profile ON usuarios(profile_id);
```

`is_system = TRUE` em "Owner", "Assessor", "Leitor" — não-deletáveis. Permitem editar nome/descrição mas não as roles seed (admin futuro pode quebrar isso intencionalmente).

### Perfis seed (auto-criados no signup da assessoria)

| Nome | is_system | Roles |
|---|---|---|
| Owner | true | `OWNR INVT EXPT BLLG` + todas `B/C/E/D` × todas entidades |
| Assessor | true | `BPRO CPRO EPRO BMAR BINF BCON CCON ECON BUSU BREL` |
| Leitor | true | `BPRO BMAR BINF BCON BUSU BREL` |

Owner é redundante (`OWNR` já bypassa) mas mantém explícito pra UI mostrar "permissões granulares" quando admin quiser auditar.

### Enforcement — backend

**Anotação `@RequirePermission`** + AspectJ:

```java
@RequirePermission("BPRO")
@GetMapping("/api/v1/prospeccoes")
public PageResponse<ProspeccaoResponse> listar(...) { ... }

@RequirePermission(value = {"EPRO"}, mode = AnyOf)
@PatchMapping("/{id}/status")
public ProspeccaoResponse mudarStatus(...) { ... }
```

Aspect `@Around`:
1. Recupera `AuthPrincipal` do `SecurityContext`
2. Se `OWNR` em `principal.permissions` → libera
3. Se intersecção `principal.permissions ∩ requiredRoles` ≠ ∅ → libera (mode AnyOf default)
4. Senão → throw `AccessDeniedException` (mapped pra 403)

Performance: lista de roles vem do JWT (claim `permissions`), zero lookup.

**Visibilidade row-level** (não é o aspect — é predicate em service/repository):

```java
public List<Prospeccao> listar(AuthPrincipal principal, FiltroProspeccao filtro) {
  if (principal.hasPermission("OWNR")) {
    return repo.findByAssessoriaId(principal.assessoriaId(), filtro);
  }
  return repo.findVisibleToAssessor(
      principal.assessoriaId(), principal.usuarioId(), filtro);
}
```

`findVisibleToAssessor` query nativa:
```sql
WHERE assessoria_id = :tenant
  AND deleted_at IS NULL
  AND (
       created_by = :userId
    OR assessor_responsavel_id = :userId
  )
```

### JWT claims

```json
{
  "sub": "<usuarioId>",
  "ass": "<assessoriaId>",
  "role": "ASSESSOR",       // coarse legado, mantido
  "perms": ["BPRO","CPRO","EPRO","BMAR","BINF","BCON","CCON","ECON","BUSU","BREL"],
  "iat": ..., "exp": ...
}
```

Trade-off: token cresce ~200 bytes. Aceitável.

**Mudança de perfil** propaga só após próximo refresh (60 min default). Documentado como limitação MVP. Mitigação futura: invalidação push por websocket.

### Frontend

```ts
// lib/auth.ts
export function decodeJwt(token: string): AuthPayload { ... }
export const usePermissions = () => useAuthStore(s => s.permissions);

// components/can.tsx
export function Can({ role, children }: { role: string|string[]; children: ReactNode }) {
  const perms = usePermissions();
  const required = Array.isArray(role) ? role : [role];
  if (perms.includes('OWNR')) return <>{children}</>;
  return required.some(r => perms.includes(r)) ? <>{children}</> : null;
}

// uso
<Can role="CPRO"><Button>+ Adicionar</Button></Can>
<Can role={['BPRO','BREL']}><SidebarItem ... /></Can>
```

Sidebar dinâmica: itens escondidos quando perfil não tem `B<entidade>`. Defense in depth: backend rejeita 403 mesmo se frontend errar.

### Hierarquia OWNR

`OWNR` na lista de permissions = godmode (bypass total). Distinto do `Usuario.role` coarse (que continua existindo pra compat). Migração:

- Usuários existentes com `role=OWNER` recebem perfil "Owner" (que tem `OWNR` na lista)
- `Usuario.role` continua sendo carregado no JWT (claim `role`) — código legado de PRD-001 que checa `role==OWNER` continua funcionando

## Alternatives considered

1. **Spring Security `@PreAuthorize` SpEL** com authorities standard
   - + nativo, expressões poderosas
   - − verboso pra strings 4-letter; SpEL é overkill pro caso
   - Descartado: anotação custom é mais legível

2. **Bitmask numérico** (`permissions BIGINT`)
   - + compacto, comparação O(1) bitwise
   - − ilegível em log/debug; limite de 64 permissões
   - Descartado: 4-letter codes são auto-documentados; perf não é gargalo

3. **CASL (frontend)** ou OPA (backend)
   - + DSL madura, condicionais ricas
   - − over-engineering pro MVP; CASL não tem equivalente Java barato
   - Reavaliar Fase 3 se virar pesadelo

4. **Permissão por ação stringless** (ex: `prospeccao:read`, `prospeccao:write`)
   - + descritivo
   - − verboso; usuário pediu 4 letras; padrão fica menos uniforme
   - Descartado em favor da convenção do usuário

5. **OWNER coarse permanece como fonte da verdade**, perfil só pra UI
   - + simples
   - − não cobre granular real
   - Descartado: PRD-005 explicitamente exige fine-grained

## Consequences

**Positivas**:
- OWNER da assessoria gerencia equipe sem suporte técnico
- Isolamento entre assessores (PRD-002) cabe num predicado simples
- 4-letter codes são fáceis de aprender e logar
- JWT-driven check é zero-DB no hot path
- `OWNR` bypass mantém código existente funcionando sem refactor amplo

**Negativas**:
- JWT cresce (~200 bytes) — irrelevante exceto em mobile lento
- Mudança de perfil propaga só no próximo refresh — pode confundir usuário ("liberei e ele ainda não vê")
- Frontend e backend duplicam lógica de check (defense in depth) — risco de drift
- Aspect AOP é mágica; novos devs precisam doc pra entender ordem de execução

**Riscos**:
- **Bypass acidental** se algum endpoint novo esquecer `@RequirePermission` — mitigar com lint/teste de integração que itera controllers e exige a anotação
- **OWNR como single-point-of-bypass** — se vazar pra perfil errado, libera tudo. Mitigar com confirmação dupla ao adicionar OWNR a perfil novo + audit log

## Impact on specs

- **security**: regra "todo controller público tem `@RequirePermission` ou `@PublicEndpoint`"; teste automatizado verifica
- **api**: documentar required roles no OpenAPI (custom annotation processor exporta tag `x-required-roles`)
- **observability**: métrica `auth_permission_denied_total{role,endpoint}`; log de 403 com `usuarioId`, `roles`, `requested_role`
- **testing**: matriz parametrizada (perfil × endpoint × resultado); Spring `@WithMockUser` extension custom carrega permissions
- **data-architecture**: `perfis` segue padrão multi-tenant (assessoria_id + soft-delete + auditoria); novo módulo `rbac/`
- **versioning**: `/api/v1/perfis` em vNext; `/api/v1/usuarios/{id}/profile` PATCH

## References

- PRD-005 (RBAC MVP) — driver desta ADR
- PRD-002 (prospecção) — primeiro consumidor de visibilidade row-level
- ADR-008 (auth JWT) — claim `permissions` adicionado
- ADR-009 (multi-tenant strategy) — perfis são tenant-scoped, herdam `TenantAspect`
- Código de referência: Spring AOP — https://docs.spring.io/spring-framework/reference/core/aop.html
- OWASP Authorization Cheat Sheet — https://cheatsheetseries.owasp.org/cheatsheets/Authorization_Cheat_Sheet.html
