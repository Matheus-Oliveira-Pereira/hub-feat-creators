# PRD-017: Administrador de Plataforma — MVP

> **SUPERSEDED by [[18-arquitetura-single-tenant]] em 2026-05-25.** Sistema migrou para single-tenant — não há mais "plataforma cross-tenant". Permissões consolidam em perfis RBAC (ADR-015). Feature flags acessíveis via permissão `FLAG`. Audit log de ADM removido. Telas de feature flags movem para `/configuracoes/feature-flags` com permissão `OWNR` (ou `FLAG`).

## Context

Hoje o sistema exige que uma assessoria exista antes de qualquer configuração ser possível. Integrações globais (e-mail transacional do sistema, WhatsApp Cloud API, AI worker, OAuth de redes sociais, feature flags) são configuradas via env vars ou SQL manual. Resultado:

- **Bootstrap manual** — toda subida nova de ambiente exige dev/devops rodar SQL ou setar env vars para conectar integrações
- **Sem visibilidade cross-tenant** — não há quem possa diagnosticar problema de uma assessoria específica, dar suporte, ou auditar abuso sem acesso direto ao DB
- **Configuração rígida** — feature flags só mudam com restart; credenciais de integração ficam em texto plano nas env vars do host
- **Onboarding bloqueado** — primeiro usuário do sistema precisa criar assessoria *e* configurar integrações *e* convidar time, tudo em um único fluxo

Sem um perfil de plataforma separado, suporte/operação dependem de acesso SSH ou DB direto — não escala e fere LGPD (acesso a dados sem trilha).

Vision: `docs/product/vision.md` — Fase 1 (operação saudável).
Depende de: [[05-rbac-mvp]] (Usuario, perfis), [[06-onboarding-multi-tenant-mvp.md]] (signup, MFA TOTP), [[04-email-outbound-mvp.md]] (contas SMTP), [[08-whatsapp-outbound-mvp.md]] (Meta tokens), [[16-match-marca-influenciador-ia.md]] (AI worker URL).
Habilita: ambientes novos sem intervenção manual; suporte com trilha LGPD.

## Objective

Criar um **tipo de usuário ADM (administrador de plataforma)**, sem vínculo com nenhuma assessoria, que (1) é criado automaticamente no primeiro signup do sistema, (2) acessa um painel `/admin` para configurar integrações globais e gerenciar todas as assessorias, (3) tem MFA TOTP obrigatório e (4) tem toda ação rastreada em `admin_audit_log` imutável.

## Scope

### Includes

- [ ] **Tipo `ADM`** no `Usuario.tipo` (enum: `INTERNO`, `ADM`; complementa `CreatorUser` que continua tipo separado)
  - ADM não tem `assessoria_id` (NULL)
  - ADM bypassa `TenantAspect` — pode acessar qualquer endpoint com qualquer `assessoria_id` no path/payload
  - JWT do ADM carrega claim `tipo: "ADM"` (espelha contrato existente em `JwtAuthFilter`)
- [ ] **Auto-criação no primeiro signup** — `AuthService.signup()` detecta `count(usuarios WHERE tipo='ADM') == 0`:
  - Primeiro signup: cria `Usuario(tipo=ADM, assessoriaId=null)`, **não cria assessoria**, força fluxo de MFA setup
  - Demais signups: comportamento atual (cria assessoria + OWNER)
  - Idempotência: tentativa de criar segundo ADM via signup vira signup normal de assessoria
- [ ] **MFA TOTP obrigatório** — login ADM bloqueado até `mfa_ativo=true`:
  - Primeiro login do ADM redireciona para `/admin/mfa-setup` (QR code + recovery codes)
  - Login subsequente exige código TOTP; sem MFA → 403
  - Reuso de infra existente em [[06-onboarding-multi-tenant-mvp.md]] (campos `mfa_secret_enc`, `mfa_ativo`)
- [ ] **Painel `/admin`** (rota Next.js separada, layout próprio sem AppShell de assessoria):
  - `/admin` → dashboard: contagem de assessorias, usuários, e-mails enviados, status das integrações
  - `/admin/integrations/email` → CRUD de contas SMTP globais (sistema) + override por assessoria
  - `/admin/integrations/whatsapp` → CRUD de credenciais Meta Cloud API (waba_id, phone_number_id, tokens cifrados via `WHATSAPP_KEY`)
  - `/admin/integrations/ai-worker` → URL + healthcheck do AI worker (PRD-016)
  - `/admin/integrations/social` → app credentials Instagram/YouTube/TikTok (PRD-015)
  - `/admin/assessorias` → lista todas com filtros (status, criada em, último login); editar (ativar/desativar, mudar plano, anonimizar)
  - `/admin/assessorias/{id}` → drill-down: usuários, prospecções, marcas, influenciadores (read + write conforme escopo definido)
  - `/admin/usuarios` → lista global de usuários (todas as assessorias + ADMs)
  - `/admin/feature-flags` → toggle runtime de `FEATURE_*` (persiste em DB, sobrescreve env var; default mantém env)
  - `/admin/audit` → leitura paginada de `admin_audit_log` com filtros (data, ADM, ação, assessoria afetada)
- [ ] **Cross-tenant edit completo** — ADM pode CRUD em:
  - Configurações globais (integrações, feature flags, perfis-padrão)
  - Qualquer recurso de qualquer assessoria (influenciadores, marcas, contatos, prospecções, tarefas, e-mails, etc.) — comportamento equivalente a `OWNR` no contexto da assessoria escolhida via path
- [ ] **Tabela `admin_audit_log`** (append-only):
  - Colunas: `id`, `admin_id`, `acao` (enum: LOGIN, MFA_SETUP, CONFIG_UPDATE, ASSESSORIA_EDIT, USUARIO_EDIT, INTEGRATION_UPDATE, FEATURE_FLAG_CHANGE, IMPERSONATE_READ, …), `assessoria_id_afetada` (NULL se ação global), `recurso` (entidade afetada), `recurso_id`, `payload_antes` JSONB, `payload_depois` JSONB, `ip`, `user_agent`, `created_at`
  - Sem UPDATE/DELETE permitidos (trigger bloqueia); só INSERT
  - Aspect `@AdminAudit` em controllers `/admin/**` registra automaticamente
- [ ] **Permission 4-letter `ADMN`** — gate p/ qualquer endpoint `/admin/**`:
  - Aspect `RequirePermissionAspect` reconhece `ADMN`; só usuário com `tipo=ADM` tem
  - Sincronizar `lib/rbac.ts` (catálogo TS) com `PermissionCodes.java`
- [ ] **Cifra de credenciais de integração** — tokens SMTP/Meta/OAuth salvos em DB cifrados AES-GCM, reusando `EmailCipherService`/`WhatsappCipherService`/`SocialCipherService` por chave dedicada
- [ ] **Convite de ADM** (ADM-a-ADM) — ADM existente pode convidar outro ADM via e-mail (token 7 dias); convidado também passa por MFA setup forçado
- [ ] **Feature flag `FEATURE_ADMIN_ENABLED`** — default `true` (necessário para bootstrap); permite desativar painel sem remover código

### Excludes

- [ ] **Hierarquia de ADMs** (super-admin vs admin-suporte com escopo limitado) — todo ADM é igual no MVP
- [ ] **Impersonation visual** (ADM "vira" OWNER de assessoria X com sessão temporária) — Fase 2; MVP edita via UI separada
- [ ] **Aprovação de templates WhatsApp globalmente** — Fase 2; MVP cada assessoria aprova suas templates
- [ ] **UI de gerenciar perfis-padrão por assessoria** — perfis seed continuam vindo do signup; MVP só edita perfis existentes via `/admin/assessorias/{id}/perfis`
- [ ] **Bilhetagem/uso por assessoria** — métricas detalhadas de cobrança ficam para PRD futuro
- [ ] **Painel ADM no mobile** (Expo) — desktop-only no MVP
- [ ] **2FA por hardware key (WebAuthn)** — só TOTP no MVP

## Not Doing (and why)

- **Hierarquia de ADMs (super-admin vs admin-suporte)** — todo ADM tem mesmo poder no MVP. **Why:** complexidade alta para benefício marginal enquanto o time de plataforma for de 1-3 pessoas. Revisitar quando houver terceirização de suporte. **How to apply:** se requisito surgir, criar novo campo `Usuario.admin_role` (string) e dividir permissions ADMN_* em sub-categorias.

- **Impersonation visual (ADM logar como OWNER X)** — MVP edita dados de assessoria via UI `/admin/assessorias/{id}/**` separada, não trocando sessão. **Why:** impersonation embaralha audit log (quem fez a ação?), risco de vazar JWT da assessoria, complica testes. Edição direta deixa claro no log que foi ADM. **How to apply:** se UX virar dor para suporte, adicionar Fase 2 com `?as=admin&impersonate=<assessoriaId>` que mantém claim `acting_as_admin: true` no JWT.

- **Self-service de novo ADM via signup público** — após primeiro ADM existir, novos signups sempre criam assessoria. **Why:** evita escalação de privilégio acidental ou maliciosa. ADMs adicionais só por convite ADM-a-ADM. **How to apply:** `AuthService.signup()` checa contagem de ADMs; > 0 → fluxo normal.

- **Configuração via UI das chaves AES (`EMAIL_KEY`, `WHATSAPP_KEY` etc.)** — chaves de cifra continuam em env vars. **Why:** rotacionar chave de cifra exige re-cifrar dados existentes — operação offline com runbook. Não é configuração runtime. **How to apply:** documentar em runbook de operação; ADM só vê *fingerprint* da chave ativa, não o valor.

- **Aprovação centralizada de templates WhatsApp / app review Meta** — cada assessoria continua dona dos seus templates. **Why:** Meta exige business verification por número, não por plataforma. Tentar centralizar viola termos. **How to apply:** ADM vê lista global de templates de todas as assessorias (read-only) para auditoria, mas não submete.

## User Stories

- Como **operador de plataforma**, quero criar a primeira conta admin sem precisar de SQL, para subir um ambiente novo em minutos
- Como **ADM**, quero configurar a conta SMTP do sistema dentro do painel, para os e-mails transacionais (verificação, convite, reset senha) saírem sem rodar comando no servidor
- Como **ADM**, quero cadastrar credenciais Meta WhatsApp Cloud API no painel, para que assessorias usem o número da plataforma ou tragam o próprio
- Como **ADM**, quero ver todas as assessorias e seus usuários em uma lista, para dar suporte e diagnosticar problema sem acesso ao DB
- Como **ADM em suporte**, quero abrir uma assessoria específica e editar/corrigir um dado (ex: contato com e-mail errado), para resolver ticket sem pedir senha pro cliente
- Como **ADM**, quero ver o log de tudo que outros ADMs fizeram, para auditoria interna e prestação de contas LGPD
- Como **operador**, quero alternar uma feature flag sem reiniciar o backend, para liberar funcionalidade gradualmente

## Design

- Layout próprio em `apps/web/app/(admin)/` (rota group separada do `(app)`), com sidebar específica:
  - Dashboard / Integrações (E-mail, WhatsApp, AI, Social) / Assessorias / Usuários / Feature Flags / Audit Log / Configurações
- Visual distinto do AppShell de assessoria — fundo `bg-muted/30`, badge "ADMIN" no topo, cor de acento vermelha (`#E11D48`) para reforçar contexto privilegiado
- Tokens: usar `docs/specs/design-system/` existente; criar apenas tokens específicos `admin-accent` e `admin-warning-banner`
- Componentes reutilizáveis: `EntityFormModal`, `PageHeader`, `FilterBar`, `Sheet` drawer — sem criar primitives novas
- Sem Figma — design driven pelos tokens (Flow A)

## Acceptance Criteria

- [ ] **AC-1**: Primeiro `POST /api/v1/auth/signup` em sistema vazio (zero assessorias, zero ADMs) cria `Usuario(tipo=ADM, assessoriaId=null)` e retorna 201 com flag `mfa_required=true`; segundo signup cria assessoria normalmente
- [ ] **AC-2**: ADM sem `mfa_ativo=true` recebe 403 em qualquer endpoint exceto `/auth/login`, `/auth/mfa-setup`, `/auth/mfa-verify`
- [ ] **AC-3**: Endpoint `/admin/**` com JWT de usuário tipo `INTERNO` (não-ADM) retorna 403; com JWT de ADM retorna 200
- [ ] **AC-4**: ADM faz `PUT /api/v1/admin/assessorias/{id}/influenciadores/{infId}` sem header `X-Tenant-Id` e a edição é aceita (TenantAspect bypassed)
- [ ] **AC-5**: Toda ação em endpoint `/admin/**` gera 1 linha em `admin_audit_log` com `admin_id`, `acao`, `assessoria_id_afetada`, `payload_antes`, `payload_depois` populados; tentativa de `UPDATE admin_audit_log` retorna erro de trigger
- [ ] **AC-6**: ADM cria/edita credencial SMTP global em `/admin/integrations/email`; senha persiste cifrada (`EmailCipherService`); GET retorna senha mascarada (`****`)
- [ ] **AC-7**: ADM convida segundo ADM via `/admin/usuarios/convidar` com `tipo=ADM`; convidado faz signup-pelo-convite, é criado como ADM e é forçado para MFA setup
- [ ] **AC-8**: `/admin/audit` retorna logs paginados, ordenados por `created_at DESC`, com filtros funcionando (data range, admin_id, acao); resposta inclui cursor opaco para próxima página
- [ ] **AC-9**: Toggle de feature flag em `/admin/feature-flags` persiste em tabela `platform_feature_flags`; `AppProperties.Features` consulta DB antes de env var (cache 30s); fallback para env se DB indisponível
- [ ] **AC-10**: Tela `/admin/dashboard` carrega em < 2s com 100 assessorias e mostra contagens corretas (queries agregadas indexadas)

## Technical Decisions

- ADR novo: `adr-017-admin-platform-role` — bypass de TenantAspect, modelo de audit imutável, decisão de não-impersonation
- Reusa: [[adr-008-auth-jwt]] (JWT + MFA), [[adr-009-multi-tenant-strategy]] (estende escopo do TenantAspect), [[adr-015-rbac-permission-aspect]] (permission gate)
- Cifras: chaves AES por integração (`EMAIL_KEY`, `WHATSAPP_KEY`, `SOCIAL_KEY`, `MFA_KEY`) continuam vindas de env var — ADM **não** edita chaves de cifra
- `admin_audit_log` JSONB para `payload_*` (GIN index para query por chave) + trigger `BEFORE UPDATE OR DELETE` que faz `RAISE EXCEPTION`

## Impact on Specs

- **Compliance ([[07-compliance-lgpd-mvp]])** — ADM acessa dados pessoais cross-tenant; base legal: **legítimo interesse** (suporte operacional contratado). Atualizar ROPA (registros de tratamento) em V19. Toda leitura/edição ADM logada (AC-5)
- **Security** — privilégio máximo → MFA obrigatório (AC-2), audit imutável (AC-5), rate-limit dedicado em `/admin/**` (60 req/min/admin), CSP no painel `/admin` mais restrito (sem CDN externa)
- **Observability** — métrica `admin_actions_total{acao, admin_id}` (Prometheus); alerta em `admin_actions_total > 100/min` (ação massiva inesperada)
- **Scalability** — `admin_audit_log` cresce indefinidamente; particionar por mês após 1M de linhas (não no MVP)
- **RBAC ([[05-rbac-mvp]])** — adiciona `ADMN` ao catálogo `PermissionCodes.java` + `lib/rbac.ts`; tipo `ADM` em `Usuario.tipo`
- **API ([[docs/specs/api]])** — novos endpoints `/admin/**` documentados; versão `v1` reusada
- **Accessibility** — painel `/admin` herda padrões WCAG 2.1 AA do design system
- **i18n** — pt-BR apenas (MVP)

## Rollout

- **Feature flag**: `FEATURE_ADMIN_ENABLED=true` por default (necessário para bootstrap em ambiente novo); pode desativar via env em ambientes onde ADM já existe e não precisa do painel
- **Migration V19** — `usuarios.tipo` adiciona valor `ADM`; cria tabela `admin_audit_log` com trigger anti-UPDATE/DELETE; cria tabela `platform_feature_flags`; adiciona `usuarios.assessoria_id` como nullable (para ADM); adiciona `ADMN` no enum de perfis ou apenas trata via tipo
- **Backfill** — ambiente staging/prod existentes que já têm assessorias: primeiro ADM deve ser criado via script `scripts/create-first-admin.sh` (ou SQL manual com aviso no runbook) porque "primeiro signup vira ADM" só vale para sistema vazio
- **Rollback** — `FEATURE_ADMIN_ENABLED=false` desativa rotas `/admin/**`; usuários `tipo=ADM` continuam podendo login mas só veem dashboard vazio. Drop de tabelas via migration reversa apenas em ambiente dev

## Documentação a atualizar

- `CLAUDE.md` → seção Modular Specifications: novo módulo `admin/`
- `docs/architecture/adr-017-admin-platform-role.md` → ADR novo
- `docs/specs/admin/README.md` → spec técnico (endpoints, audit log schema, fluxo de bootstrap)
- `docs/runbooks/admin-bootstrap.md` → runbook: como criar primeiro ADM em ambientes existentes
- `.env.example` → remover credenciais que migrarão para DB; manter chaves AES
- `docs/runbooks/lgpd-dsr.md` → adicionar fluxo ADM acessar dados de titular sob solicitação
