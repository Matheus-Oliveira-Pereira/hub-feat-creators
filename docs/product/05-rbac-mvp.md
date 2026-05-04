# PRD-005: RBAC e perfis de acesso — MVP

## Context

Hoje o sistema tem só 2 roles coarse — `OWNER` e `ASSESSOR`. Funciona pra demo, mas:

- Não dá pra ter um assessor que **só lê** (estagiário, contador) sem dar poder de criar/deletar
- Não dá pra impedir um assessor de mexer em prospecções de colega (ver PRD-002 que pediu isolamento entre assessores)
- Não dá pra liberar acesso parcial a relatórios sem dar acesso a cadastros
- Donas de assessoria querem composição flexível: "essa pessoa só vê marca, essa só edita influenciador" — sem precisar mudar código a cada caso

Sem RBAC fino, ou liberamos demais (segurança fraca, conflito de equipe), ou liberamos de menos (assessora pede pra OWNER fazer tudo, vira gargalo).

Vision: `docs/product/vision.md` — Fase 1.
Depende de: PRD-001 (usuário, assessoria, convite).
Habilita: PRD-002 (prospecção exige visibilidade granular ASSESSOR vs OWNER), PRD-003, PRD-004.

## Objective

Permitir que **OWNER** componha **perfis** de acesso usando **roles atômicas de 4 letras** (`{B|C|E|D}{ENT}`) e atribua um perfil a cada usuário, controlando: (1) **acesso a tela/funcionalidade** (permission gate) e (2) **visibilidade de dados** (row-level scoping para alguns recursos como prospecção).

## Scope

### Includes

- [ ] **Entidade `Perfil`** (assessoria-scoped) com nome, descrição, lista de roles (4-letter codes)
- [ ] **Enum `Role`** com códigos 4-letter: ação (`B`/`C`/`E`/`D`) + entidade (3 letras estáveis):
  - `B` = Browse (ler lista, buscar)
  - `C` = Change (criar registro novo)
  - `E` = Edit (modificar existente, mudar status)
  - `D` = Delete (remover, soft-delete)
- [ ] **Mapa de entidades** (3 letras): `PRO` (prospecção), `MAR` (marca), `INF` (influenciador), `CON` (contato), `USU` (usuário), `ASS` (assessoria), `TAR` (tarefa, futuro), `EML` (e-mail, futuro), `REL` (relatório/dashboard)
- [ ] Roles especiais (não-4-letras quando justificado): `OWNR` (godmode da assessoria), `INVT` (convidar usuário)
- [ ] **Atribuição** de perfil ao usuário (1 perfil por usuário; FK `profile_id` em `usuarios`)
- [ ] **Perfis seed** automaticamente criados ao signup da assessoria:
  - `Owner` — todas as roles, incluindo `OWNR` e `INVT`
  - `Assessor` — `BPRO CPRO EPRO BMAR BINF BCON CCON ECON BUSU` (browse próprio time + edita prospecção + browse marca/influenciador/contato + cria contato; sem delete; sem convidar; sem editar marca/influenciador)
  - `Leitor` — só `B*` (read-only) — útil pra estagiário/contador
- [ ] **OWNER coarse role** (`Usuario.role`) preservado como gate primário no JWT — quando `role=OWNER`, todas verificações passam (OWNR implícito)
- [ ] **Anotação `@RequirePermission("BPRO")`** em controllers — aspect intercepta e checa contra `principal.profile.roles`. 403 se faltar. JWT carrega lista de roles (claim novo) pra evitar lookup a cada request
- [ ] **Visibilidade row-level** para prospecção (definida em PRD-002):
  - OWNER vê tudo da assessoria
  - ASSESSOR vê: prospecções **criadas por ele** ∪ prospecções onde ele é `assessor_responsavel`
  - Implementado via predicado dinâmico em service/repository (não via Hibernate `@Filter` — depende do principal)
- [ ] **CRUD de perfis** via UI pelo OWNER: criar, editar nome/roles, atribuir a usuários, deletar (impedir delete de perfil em uso)
- [ ] **Atribuir perfil** pela tela de usuários (futura — MVP minimum: aceitar perfil no convite)

### Excludes

- [ ] Permissões de campo (field-level — ex: ASSESSOR vê valor mas não custo) — Fase 2
- [ ] Permissões temporais (ex: acesso vence em 30 dias) — Fase 2
- [ ] Múltiplos perfis por usuário — adia; 1 perfil cobre 95% dos casos. Se precisar, modela "Profile groups" depois
- [ ] Scope cross-assessoria (ex: consultor que atende várias assessorias) — Fase 3
- [ ] Audit log de mudanças no perfil (quem mudou, de o que pra o que) — útil mas adiável
- [ ] UI completa de gestão de perfis com drag/drop de roles — MVP usa lista de checkboxes
- [ ] Importar perfis de templates da indústria — Fase 2

## Not Doing (and why)

- **Field-level**: complexidade explode (regras por campo × por entidade × por role); domínio não pediu ainda
- **Múltiplos perfis**: composição já cabe num perfil; UI fica simples
- **Audit de perfil**: registrar via audit_log existente é trivial mas não bloqueia release; faz quando demandar
- **Override por usuário** (ex: "esse assessor tem perfil X mas pode também deletar"): vira espaguete; cria perfil novo

## User Stories

- **Como OWNER**, quero criar um perfil "Comercial Sênior" com `BPRO CPRO EPRO DPRO BMAR EMAR BINF EINF BCON CCON ECON DCON` para dar acesso quase total a quem cuida de vendas, sem deixar mexer em usuários
- **Como OWNER**, quero atribuir o perfil "Leitor" a um estagiário para ele só ler dados, sem risco de apagar nada
- **Como assessor**, espero não conseguir abrir a tela de marcas se meu perfil não tiver `BMAR` (item da sidebar some, rota retorna 403)
- **Como assessor**, espero ver no kanban só as prospecções minhas e dos influenciadores que assessoro
- **Como OWNER**, quero ver todas as prospecções da assessoria, independente de quem criou
- **Como dev**, espero documentação clara de cada role 4-letter para não inventar códigos paralelos

## Design

- **Tela "Perfis"** (`/perfis`) — listagem (cards), modal create/edit com checkboxes agrupados por entidade (PRO, MAR, INF, CON, USU, REL); badge de quantos usuários usam o perfil
- **Tela "Usuários"** (futura — fora do MVP visual; via tela de convite incrementada) — coluna "Perfil" + ação "Mudar perfil"
- **Sidebar dinâmica** — itens escondidos quando perfil não tem `B<entidade>`
- **Botões "Criar/Editar/Remover"** dentro de páginas — escondidos sem `C/E/D<entidade>`
- **403** com mensagem amigável + sugestão "fale com o OWNER pra liberar"

## Acceptance Criteria

### Funcional
- [ ] **AC-1**: Signup cria automaticamente perfis "Owner", "Assessor", "Leitor" na assessoria; usuário OWNER recebe perfil "Owner"; convite default = "Assessor"
- [ ] **AC-2**: `@RequirePermission("XYZA")` em controller bloqueia request com 403 quando perfil do usuário não contém a role; permite quando contém
- [ ] **AC-3**: Usuário com `Usuario.role = OWNER` (coarse) bypassa todas as checagens (mantém compat com PRD-001 pre-RBAC)
- [ ] **AC-4**: OWNER pode criar perfil novo com nome, descrição, lista de roles selecionadas
- [ ] **AC-5**: Tentativa de deletar perfil em uso retorna 409 com lista de usuários afetados
- [ ] **AC-6**: JWT inclui claim `permissions` (array de roles) — frontend decodifica e ajusta UI; backend re-valida em cada request (não confia só no JWT)
- [ ] **AC-7**: Frontend esconde itens da sidebar e botões quando role faltante; tentativa direta via URL retorna 403 no backend (defense in depth)
- [ ] **AC-8**: Listagem de prospecções (e endpoint `/api/v1/prospeccoes`) aplica filtro de visibilidade — ASSESSOR vê só próprias + onde é responsável; OWNER vê todas; mesmo via cursor/paginação

### Não-funcional
- [ ] **AC-NF-1**: Verificação de permissão adiciona < 5ms p99 ao request (in-memory check, sem DB)
- [ ] **AC-NF-2**: Mudança de perfil reflete na próxima emissão de token (logout/login ou refresh) — documentar trade-off vs invalidação imediata
- [ ] **AC-NF-3**: Cobertura ≥ 85% no aspect, service de perfis e checagem de visibilidade
- [ ] **AC-NF-4**: Cross-tenant: assessoria A não vê perfis de B (multi-tenant herdado)

## Technical Decisions

Detalhes em **ADR-015** (RBAC technical).

Tabelas:
```sql
perfis (
  id UUID PK,
  assessoria_id UUID FK,
  nome TEXT NOT NULL,
  descricao TEXT NULL,
  roles TEXT[] NOT NULL DEFAULT '{}',  -- ex: ['BPRO','CPRO','EPRO','BMAR']
  is_system BOOLEAN NOT NULL DEFAULT FALSE, -- seed perfis (Owner/Assessor/Leitor) — não deletáveis
  created_at, updated_at, deleted_at
);

ALTER TABLE usuarios ADD COLUMN profile_id UUID FK → perfis(id) NULL;
```

Migration backfill: para cada assessoria existente cria os 3 perfis seed; OWNER recebe "Owner", ASSESSOR recebe "Assessor".

## Impact on Specs

- **Security**: surface ampliada; teste exaustivo do aspect; risco de bypass exige revisão; documentar tabela de roles em `docs/specs/rbac/`
- **Compliance**: facilita LGPD (princípio de mínimo privilégio); audit log já cobre operações sensíveis
- **API**: convenção `@RequirePermission` em todos os controllers públicos (exceto auth); doc OpenAPI com tag `requires:`
- **Observability**: métrica `auth_permission_denied_total{role,endpoint}` pra detectar permissões mal configuradas
- **Testing**: matriz parametrizada (perfil × endpoint × resultado esperado)
- **Frontend**: hook `usePermission('BPRO')` retorna boolean; componente `<Can role="BPRO">{...}</Can>` esconde filhos

## Rollout

- **Feature flag**: `feature.rbac.enabled` global — quando off, sistema cai no comportamento legado (Usuario.role coarse)
- **Data migration**: V?_perfis.sql + backfill — irreversível em forward; rollback via flag off
- **Comunicação**: nota em release notes; OWNER vê banner "novos perfis criados; revise atribuições"

## Métricas de sucesso

- Zero incidente de bypass detectado em 90 dias
- ≥ 50% das assessorias com mais de 1 usuário criam ao menos 1 perfil customizado em 60 dias
- p99 do aspect < 5ms em load test

## Open questions

- [ ] Mudança de perfil exige logout/refresh ou propaga em tempo real? — MVP: refresh do token (até 60 min); se virar dor, evento via websocket Fase 2
- [ ] Perfil tem limite de roles? — não no MVP; perfil "godmode customizado" é OK
- [ ] Quem pode criar perfil? — só `OWNER` no MVP. Liberar `INVT`-equivalente Fase 2

## References
- PRD-001 (cadastros, usuário, convite)
- PRD-002 (prospecção — consumidor primário do row-level scoping)
- ADR-008 (auth/JWT — claim `permissions` adicionado)
- ADR-009 (multi-tenant — perfis são scoped por assessoria)
- ADR-015 (RBAC technical) — a criar

---

## Notas de implementação (post-mortem)

> Adicionado em 2026-05-03, após implementação da Fase R (commits `c5be1ab..744d41d`).

### O que foi entregue
- **Backend** (R1–R3): migration V3 com perfis + `usuarios.profile_id` + `influenciadores.assessor_responsavel_id` + backfill de seeds, entidade `Perfil` + `RbacBootstrap`, aspect `@RequirePermission` + `RequirePermissionAspect`, JWT claim `perms`, `AuthPrincipal.hasPermission`, `PerfilController` CRUD + `PATCH /usuarios/{id}/profile`, IT smoke.
- **Frontend** (R4–R5): `lib/auth.ts` (decodeJwt + AuthProvider + usePermissions), `<Can>`, sidebar dinâmica, tela `/perfis` com checkboxes agrupados por entidade.

### Decisões implementadas
- 3 perfis seed (`Owner`, `Assessor`, `Leitor`) criados no signup + via backfill. Marcados `is_system=true` (não deletáveis, roles imutáveis).
- `OWNER` coarse + role `OWNR` bypassam aspect — preserve compat PRD-001.
- Token cresce ~200 bytes por causa do claim `perms`. Aceito.
- `JwtService.generateAccessToken(...)` agora aceita `Collection<String> permissions` — compat com `AuthService` reescrito.
- Mudança de perfil propaga só após próximo refresh do token (não imediato). Limitação MVP.

### Códigos sincronizados manualmente
`PermissionCodes.java` (backend) e `lib/rbac.ts` (frontend) listam roles. Adicionar role nova em ambos os lugares + spec `docs/specs/rbac/`. Sem code-gen.

### Pendências
- Override por usuário (negado por design).
- Field-level (Fase 2).
- Audit log de mudanças no perfil (plug fácil no `AuditLogService`).
- Clonar perfil seed como ponto de partida (UI hoje força criar do zero).
- Invalidação push do token via websocket.

### Métricas
- 6 commits R + 1 doc commit. Build limpo backend + frontend.
