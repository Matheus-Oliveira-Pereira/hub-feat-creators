# Module: RBAC (PRD-005 + ADR-015)

Controle de acesso fino via perfis com roles 4-letter atribuídas a usuários.

## Roles 4-letter

Formato: `[B|C|E|D] + 3 letras de entidade`. Roles especiais 4-letras quando justificado.

| Letra inicial | Significado |
|---|---|
| `B` | Browse — listar/ver |
| `C` | Change — criar |
| `E` | Edit — modificar (incluindo mudança de status) |
| `D` | Delete — remover (soft-delete) |

| Entidade | Codigo |
|---|---|
| Prospecção | `PRO` |
| Marca | `MAR` |
| Influenciador | `INF` |
| Contato | `CON` |
| Tarefa (PRD-003) | `TAR` |
| E-mail (PRD-004) | `EML` |
| Usuário | `USU` |
| Perfil | `PRF` |
| Relatório/dashboard | `REL` (apenas `BREL`) |

Roles especiais:

| Code | Significado |
|---|---|
| `OWNR` | Godmode — bypassa todas as verificações da assessoria |
| `INVT` | Convidar usuário |
| `EXPT` | Exportar CSV / relatórios (sensível LGPD) |
| `BLLG` | Browse audit log |

Lista canônica: `PermissionCodes.ALL` (Java) e `lib/rbac.ts` (TS — espelhar manualmente).

## Perfis seed

Criados automaticamente no signup da assessoria + via backfill da V3:

| Nome | `is_system` | Roles |
|---|---|---|
| **Owner** | true | todas (≈37 codes incluindo `OWNR INVT EXPT BLLG`) |
| **Assessor** | true | `BPRO CPRO EPRO BMAR BINF BCON CCON ECON BUSU BREL BTAR CTAR ETAR BEML CEML EEML` |
| **Leitor** | true | `BPRO BMAR BINF BCON BUSU BREL BTAR BEML` |

Perfis `is_system=true`:
- Não podem ser deletados (HTTP 400)
- Não podem ter nome ou roles alterados (HTTP 400)
- Aceitam edit de descrição

## Enforcement

### Backend

```java
@RequirePermission("BPRO")
@GetMapping("/api/v1/prospeccoes")
public ... { ... }

@RequirePermission(value = {"EPRO", "CPRO"}, mode = RequirePermission.Mode.ANY_OF)
@PatchMapping("/{id}")
public ... { ... }
```

Aspect `RequirePermissionAspect` (`@Order(1)`) intercepta `@annotation` ou `@within`, lê `AuthPrincipal.permissions` (vem do JWT claim `perms`), aplica `hasAny/AllPermissions`. Bypass: `Usuario.role=OWNER` ou `OWNR` na lista.

Falha → `AccessDeniedException` (mapped pra HTTP 403) + counter `auth_permission_denied_total{required, mode}`.

### Frontend

```tsx
import { Can } from '@/components/auth/can';
import { usePermission } from '@/lib/auth';

<Can role="CPRO"><Button>+ Nova</Button></Can>
<Can role={['BPRO','BREL']} fallback={null}>...</Can>

const can = usePermission('DPRO');
```

`AuthProvider` decoda JWT no mount + escuta evento `storage` (sintético disparado por `setTokens`/`clearTokens`).
Sidebar dinâmica filtra itens via `requires?: string[]`.
Sempre defesa em profundidade — backend revalida.

## Visibilidade row-level

Independente do permission gate. Implementada por entidade no service.

**Prospecção** (PRD-002):
- OWNER: `findAllOwner()` — toda a assessoria.
- ASSESSOR: `findAllAssessor()` — `created_by = me OR assessor_responsavel_id = me`.

Tentativa fora do escopo → `404 NOT_FOUND` (não vaza existência).

## JWT

```json
{
  "sub": "<usuarioId>",
  "ass": "<assessoriaId>",
  "role": "OWNER|ASSESSOR",
  "perms": ["BPRO","CPRO","EPRO","BMAR",...],
  "iat": ...,
  "exp": ...
}
```

Mudança de perfil propaga **somente após refresh do token** (60min default). Documentado como limitação MVP em ADR-015.

## Endpoints administrativos

| Método | Path | Roles |
|---|---|---|
| GET | `/api/v1/perfis` | `BPRF` |
| GET | `/api/v1/perfis/{id}` | `BPRF` |
| POST | `/api/v1/perfis` | `CPRF` |
| PUT | `/api/v1/perfis/{id}` | `EPRF` |
| DELETE | `/api/v1/perfis/{id}` | `DPRF` (409 se em uso, 400 se sistema) |
| PATCH | `/api/v1/usuarios/{id}/profile` | `EUSU` |

## Schema

```sql
perfis (
  id UUID PK, assessoria_id UUID FK CASCADE,
  nome TEXT NOT NULL, descricao TEXT,
  roles TEXT[] NOT NULL DEFAULT '{}',
  is_system BOOLEAN DEFAULT FALSE,
  created_at, updated_at, deleted_at,
  UNIQUE (assessoria_id, nome)
);
ALTER TABLE usuarios ADD COLUMN profile_id UUID FK NULL;
```

GIN em `roles` (busca futura "quem tem permissão X"). Tenant scoped via RLS.

## Tech debt rastreado

- **Override por usuário** — não suportado; criar perfil novo. Aceito.
- **Field-level permissions** — Fase 2.
- **Audit log de mudanças no perfil** — adiável; quando demandar, plug no `AuditLogService` existente.
- **Custom profiles fora dos seed** — assessoria pode criar; mas UI atual não permite **clonar** seed → criar do zero. Adicionar quando demandado.
- **Invalidação push do token em mudança de perfil** — websocket Fase 2; hoje aguarda refresh.

## References
- PRD-005 `docs/product/05-rbac-mvp.md`
- ADR-015 `docs/architecture/adr-015-rbac-roles-perfis.md`
- ADR-008 (auth — claim `perms` adicionado)
- ADR-009 (multi-tenant — perfis tenant-scoped)
