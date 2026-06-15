# PRD-019: E-mail Single Account — Simplificação

## Context

PRD-004 (ADR-005) implementou SMTP multi-conta: assessoria cadastrava N contas (Gmail, Outlook, M365…), cada envio escolhia uma conta, circuit breaker por conta, rate limit por conta, pool de conexão por conta.

Após PRD-018 (single-tenant), a razão principal do multi-conta deixou de existir. Não há mais múltiplas assessorias competindo por contas. O sistema serve **uma única assessoria** com **um único remetente**.

Manter SMTP multi-conta cria complexidade sem retorno:
- CRUD de contas, seleção por envio, pool por conta, circuit breaker por conta, rotação de credenciais por conta.
- DigestJobHandler nunca enviou e-mail de verdade por falta do `setTo()` — dívida técnica que só existe por causa da complexidade multi-conta.

Decisão: **uma conta SMTP de sistema**, configurada pelo OWNR via página de configurações (persiste em DB, cifrada) ou via env vars (fallback). Todos os e-mails saem deste único remetente.

## Objective

Simplificar o stack de e-mail: única conta SMTP de saída, configurável pelo OWNR, com as mesmas garantias de segurança (AES-GCM), idempotência, opt-out e tracking.

## Scope

### Includes

- [ ] **Remover CRUD multi-conta** — página `Configurações > E-mail > Contas` some; substituída por formulário único "Conta de e-mail do sistema"
- [ ] **Single-row settings**: tabela `system_email_config` (1 linha) com `host`, `port`, `username`, `password_enc`, `password_nonce`, `from_address`, `from_name`, `tls_mode`, `daily_quota`, `status` — gerenciada via `PATCH /api/v1/configuracoes/email`
- [ ] **Fallback env vars**: se `system_email_config` vazia, lê `SMTP_HOST` / `SMTP_PORT` / `SMTP_USER` / `SMTP_PASS` / `SMTP_FROM` / `SMTP_FROM_NAME` — útil no boot antes do OWNR configurar
- [ ] **Migration V24**: drop tabela `smtp_accounts`, create `system_email_config`; se existia exatamente 1 linha em `smtp_accounts` (status ATIVA), migrar para `system_email_config`
- [ ] **SystemMailService** substitui `EmailAccountService` como ponto único de envio — instancia `JavaMailSender` dinamicamente quando config muda (cache com TTL 5min ou invalidação manual)
- [ ] **DigestJobHandler fix**: usar `UsuarioRepository` para resolver `email` do `responsavelId`; chamar `mailSender.send(msg)` de verdade
- [ ] **Templates e tracking** — mantidos sem mudança (não dependem de multi-conta)
- [ ] **Opt-out** — mantido; agora a chave é `email` (sem `assessoria_id`)
- [ ] **Circuit breaker** — mantido; única conta; 3 falhas auth em 10min → status `FALHA_AUTH` + notificação para OWNER
- [ ] **Botão "Testar conexão"** — mantido; testa a conta única
- [ ] **Web**: página `(app)/configuracoes/email/page.tsx` — formulário único (host, port, user, senha, from, from_name, tls_mode, quota) + badge de status + botão testar
- [ ] **Permissão**: `OWNR` para ler/editar config; `EMLR` (já existe) para enviar

### Excludes

- [ ] OAuth SMTP (Gmail OAuth2 flow) — env vars / app password suficiente no MVP
- [ ] Múltiplos remetentes por template — fora de escopo; único `from`
- [ ] IMAP sync (respostas) — Fase 2

## Not Doing

- **Manter multi-conta como opção** — complexidade > valor no single-tenant. Se futuro precisar, restaurar de ADR-005 + PRD-004 (mantidos como histórico).
- **Provedor transacional (Resend/SES)** — user quer SMTP próprio (ADR-005 motivação ainda válida).

## User Stories

- Como OWNR, quero configurar o e-mail de saída do sistema uma vez, sem gerenciar múltiplas contas.
- Como OWNR, quero testar a conexão antes de salvar para detectar erros de credencial.
- Como OWNR, quero ser alertado se a conta falhar para trocar a senha rapidamente.
- Como assessora, quero receber o digest diário de tarefas no meu e-mail (fix do DigestJobHandler).

## Acceptance Criteria

- **AC-1** — Migration V24 aplica sem erro; `smtp_accounts` não existe; `system_email_config` existe com 0 ou 1 linha.
- **AC-2** — `PATCH /api/v1/configuracoes/email` (OWNR) salva config cifrada; `GET` retorna sem expor senha (campo `passwordSet: true/false`).
- **AC-3** — `POST /api/v1/configuracoes/email/test` testa handshake SMTP; retorna 200 OK ou 422 com mensagem de erro legível.
- **AC-4** — Todos os envios (1-to-1 de prospecção, digest, notificações de sistema) usam `SystemMailService`; não existe mais referência a `EmailAccountService` como ponto de roteamento multi-conta.
- **AC-5** — DigestJobHandler envia e-mail real para o endereço do responsável. Log confirma `digest.send email=xxx`.
- **AC-6** — Opt-out bloqueia envio independente de conta (não há mais "conta" como dimensão).
- **AC-7** — Circuit breaker: 3 falhas auth em 10min → `status = FALHA_AUTH` + notificação in-app para OWNER.
- **AC-8** — Fallback env vars: se `system_email_config` vazia, sistema usa `SMTP_*` env vars sem erro.
- **AC-9** — `pnpm lint && pnpm test` (web) + `./mvnw verify` (api) passam.

## Migration Plan (V24)

```sql
-- Se existe exatamente 1 conta ATIVA em smtp_accounts, migrar
INSERT INTO system_email_config (host, port, username, password_enc, password_nonce,
  from_address, from_name, tls_mode, daily_quota, status)
SELECT host, port, username, password_enc, password_nonce,
  from_address, from_name, tls_mode, daily_quota, status
FROM smtp_accounts
WHERE status = 'ATIVA'
LIMIT 1
ON CONFLICT DO NOTHING;

-- Drop multi-account
DROP TABLE smtp_accounts;
```

## Technical Decisions

- Related ADR: [[adr-019-single-system-email]] (nova — supersedes [[adr-005-email-smtp-multi-conta]])
- Mantém: [[adr-008-auth-jwt]], [[adr-011-lgpd-baseline]], specs `email/` (tracking, templates, opt-out, fila)
- Impacto em PRD-004: parcialmente superseded — infra de envio simplifica; templates/tracking/opt-out mantidos.
