# PRD-013: Portal do Influenciador (Web) — MVP

## Context

Influenciador hoje recebe brief/tarefa por e-mail/WA solto. Sem central, perde prazo, perde anexo, assessora vira intermediária de cada update. Vision: Fase 2 — "Portal do influenciador (visualizar tarefas, pipeline)".
Vision: Fase 2.
Depende de: PRDs 01, 02, 03, 06 (auth), 07.

## Objective

Portal web com login dedicado para influenciador acompanhar campanhas atribuídas, brief, prazos, entregar materiais e trocar mensagens com assessoria — preservando isolamento (não vê dados de outros).

## Scope

### Includes
- [ ] **Identidade externa**: usuário tipo `INFLUENCIADOR_USER` (separado de `usuario` interno); login/senha + MFA opcional
- [ ] **Convite**: assessora envia convite por e-mail/WA → influenciador define senha
- [ ] **Tela "Minhas campanhas"**: lista de prospecções `GANHA` em que está vinculado; cards com status entrega
- [ ] **Detalhe de campanha**: brief (texto + anexos), prazos (`tarefas` atribuídas via novo flag `visivel_para_creator`), histórico mensagens
- [ ] **Upload entregáveis**: vídeo/foto/print por tarefa; status `EM_REVISAO|APROVADO|SOLICITADA_REVISAO`
- [ ] **Comentários** na tarefa: thread bidirecional (assessora ↔ influenciador)
- [ ] **Notificações**: in-app + e-mail quando tarefa muda status, comentário novo, prazo vencendo (reusa PRD-009 com tipo `INFLUENCIADOR_*`)
- [ ] **Privacidade**: vê só prospecções/tarefas onde é o influenciador titular; NUNCA vê marca real do concorrente, valor de outras prospecções, comentários internos
- [ ] **Marca whitelabel-leve**: assessoria escolhe logo + cor primária do portal (subdomínio `<slug>.portal.feat-creators.com` ou path `/portal/<slug>`)
- [ ] **Termos de uso e privacidade** específicos influenciador (LGPD, base legal "execução de contrato")

### Excludes
- [ ] Marketplace / influenciador descobre marca — Fase 4
- [ ] Pagamentos diretos no portal — fora MVP
- [ ] Editor de proposta — fora MVP
- [ ] App nativo iOS/Android — PRD-014

## Not Doing

- **Whitelabel completo** (DNS custom, e-mail no domínio cliente) — complexidade alta. Subdomínio compartilhado MVP.
- **Múltiplas assessorias por influenciador na mesma conta** — login separado por assessoria; gerenciar identidade unificada é Fase 2 do portal.

## User Stories

- Como influenciador, quero ver todas campanhas ativas em um lugar
- Como influenciador, quero subir vídeo final e receber aprovação
- Como assessora, quero saber quando influenciador entregou
- Como dona, quero meu logo no portal

## Acceptance Criteria

- [ ] **AC-1**: Login `INFLUENCIADOR_USER` é separado: token JWT com claim `tipo=CREATOR`; aspect bloqueia acesso a APIs internas
- [ ] **AC-2**: Visibilidade: query SQL filtra por `influenciador_id = current_user.influenciador_id`; cross-tenant tentativa retorna 404
- [ ] **AC-3**: Convite cria `INFLUENCIADOR_USER` linkado a `influenciador_id` específico; aceitar exige verificar e-mail e definir senha
- [ ] **AC-4**: Tarefa precisa flag `visivel_para_creator=true` para aparecer no portal; default false (assessora marca explicitamente)
- [ ] **AC-5**: Upload entregável: máx 500MB, formatos vídeo/imagem/pdf; storage abstrato (S3-compat); URL assinada 7d
- [ ] **AC-6**: Comentários têm flag `interno=false` por default no portal; assessora pode marcar `interno=true` e some no portal
- [ ] **AC-7**: Notificação para influenciador respeita preferências (default: tudo on)
- [ ] **AC-8**: Whitelabel: logo + cor primária aplicados via CSS variable; fallback default
- [ ] **AC-9**: Eventos no portal geram entrada em `eventos` (PRD-010) com tipo `INFLUENCIADOR_*`
- [ ] **AC-NF-1**: Acessibilidade WCAG 2.1 AA — portal pode ser usado por creator com leitor de tela
- [ ] **AC-NF-2**: Cobertura ≥ 80% em `PortalService`, `CreatorAuthService`
- [ ] **AC-NF-3**: Performance: lista de campanhas < 500ms p95

## Technical Decisions

- **Auth separado**: `creator_users` table; subject JWT prefixado `creator:` para aspect distinguir
- **Roteamento**: `apps/web/app/(portal)/` paralelo a `(app)`; AppShell distinto (sem sidebar admin)
- **Storage entregáveis**: `AttachmentStorage` (mesma abstração e-mail PRD-004)
- **Whitelabel**: tabela `assessoria_branding(logo_url, cor_primaria)`; fetch no SSR via slug do path
- **Subdomain ou path?**: começar com path (`/portal/<slug>`) — TLS wildcard fica para Fase 2

### Schema

```sql
creator_users (id, influenciador_id FK UNIQUE, email CITEXT UNIQUE, senha_hash,
  email_verificado_em, mfa_secret_enc, mfa_ativo, status, created_at, deleted_at)

creator_invites (id, influenciador_id, email, token_hash, expires_at, aceito_em, criado_por_id)

creator_entregaveis (id, tarefa_id FK, creator_user_id FK, arquivo_path, content_type, size_bytes,
  status CHECK ('ENVIADO','EM_REVISAO','APROVADO','SOLICITADA_REVISAO'),
  feedback TEXT, criado_em, atualizado_em)

assessoria_branding (assessoria_id PK, logo_url, cor_primaria, atualizado_em)

-- mudanças
ALTER TABLE tarefas ADD COLUMN visivel_para_creator BOOL DEFAULT FALSE;
ALTER TABLE comentarios ADD COLUMN interno BOOL DEFAULT TRUE;  -- inverte default no contexto portal
```

## Impact on Specs

- **Compliance**: novos titulares (influenciadores como `creator_user`); base legal "execução de contrato"; DSR específico
- **Security**: superfície ampliada → review OWASP; rate limit login portal; CSP estrito
- **Scalability**: portal possivelmente cresce mais que admin (N influenciadores por assessoria); cache de listagem
- **Observability**: `portal_login_total`, `portal_entregavel_uploaded_total`, `portal_visita_total`
- **Accessibility**: WCAG AA — público externo, exigência alta
- **API**: `/api/v1/portal/*` (creator JWT)
- **Testing**: e2e Playwright em fluxo creator; testes de isolamento

## Rollout

- **Feature flag**: `feature.portal.enabled` por tenant
- **Migrations**: V0013_*
- **Rollback**: flag off bloqueia rotas portal; usuários criados ficam latentes
- **Onboarding**: assessoria liga portal nas configurações; convida creators aos poucos

## Métricas

- ≥ 30% influenciadores convidados aceitam em 7d
- ≥ 50% das tarefas marcadas visíveis recebem entrega via portal
- Tempo entrega → aprovação reduzido vs e-mail (baseline)

## Open questions

- [ ] Influenciador com 2 assessorias = 2 logins ou unificado? — MVP 2 logins; unificar Fase 2
- [ ] Permitir creator iniciar conversa fora de tarefa? — não MVP, evita ruído
