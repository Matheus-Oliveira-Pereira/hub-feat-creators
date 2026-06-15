# ADR-019: E-mail Single System Account

- **Status**: Accepted
- **Date**: 2026-06-15
- **Supersedes**: [[adr-005-email-smtp-multi-conta]]
- **Driver**: [[19-email-single-account]] (PRD)

## Contexto

ADR-005 definiu SMTP multi-conta: assessoria cadastrava N contas SMTP com pool de conexão por conta, circuit breaker por conta e roteamento por envio.

A justificativa original era multi-tenant: cada assessoria usaria o e-mail dela (ADR-009). Com a migração para single-tenant (ADR-018/PRD-018), essa razão desaparece. O sistema serve uma única assessoria com um único remetente.

Manter multi-conta pós-single-tenant cria:
- Complexidade de seleção de conta por envio (qual das N contas usar?)
- CRUD de contas, pool por conta, circuit breaker por conta
- Dívida técnica concreta: `DigestJobHandler` nunca enviou e-mail por não conseguir resolver a conta correta

## Decisão

**Uma única conta SMTP de sistema**, configurada pelo OWNR e persistida em `system_email_config` (tabela single-row). Fallback: env vars `SMTP_*` se tabela vazia.

`SystemMailService` substitui o roteamento multi-conta como ponto único de envio. Instancia `JavaMailSender` dinamicamente; cache com invalidação em mudança de config.

## Consequências práticas

- **DB**: V24 dropa `smtp_accounts`, cria `system_email_config` (single-row). Migra linha ATIVA se existir.
- **Backend**: `EmailAccountService` / `EmailAccountRepository` removidos ou reduzidos a `SystemEmailConfigService`. `DigestJobHandler` resolve email via `UsuarioRepository` e envia de verdade.
- **Frontend**: página multi-conta (`Configurações > E-mail > Contas`) substituída por formulário único de configuração.
- **Segurança**: cifragem AES-GCM mantida (`password_enc` + `password_nonce`). Chave `EMAIL_KEY` permanece. Nunca logar senha.
- **Circuit breaker**: mantido — única conta; 3 falhas auth em 10min → `FALHA_AUTH` + notificação OWNER.
- **Opt-out**: mantido; chave agora é só `email` (sem `assessoria_id`, já removido em V20).
- **Templates / tracking**: sem mudança.

## Alternativas consideradas

- **Manter multi-conta com single-tenant** — sem justificativa: uma assessoria raramente precisa de N contas de saída. Complexidade gratuita.
- **Migrar para provedor transacional (Resend/SES)** — rejeitado: motivação do ADR-005 ainda válida (assessoria usa o e-mail dela, não o nosso domínio).
- **Configurar apenas via env vars** — rejeita: OWNR não tem acesso ao Railway para mudar env; UI necessária.

## Riscos

- **Ponto único de falha** — antes, conta com falha poderia rotacionar para outra. Agora: falha única = digest e envios param. Mitigação: circuit breaker + notificação imediata + fallback env vars.
- **Migration destrutiva** — `smtp_accounts` é dropada. Dados de contas extras (> 1) se perdem. Aceito: single-tenant não tem caso de uso para múltiplas.

## Status de outros ADRs

| ADR | Status pós-019 |
|---|---|
| ADR-005 SMTP multi-conta | **Superseded by ADR-019** |
| ADR-011 LGPD baseline | Mantém — opt-out, List-Unsubscribe obrigatórios |
| ADR-018 single-tenant | Motivação desta decisão |

## Referências

- PRD: [[19-email-single-account]]
- Migration: `apps/api/src/main/resources/db/migration/V24__single_email_account.sql`
- Branch sugerida: `feature/single-system-email`
