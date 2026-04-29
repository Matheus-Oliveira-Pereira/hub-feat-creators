# ADR-005: E-mail via SMTP relay externo multi-conta (Jakarta Mail)

## Status
Accepted — 2026-04-29

## Context
Sistema precisa enviar e-mails de prospecção. Versão inicial do CLAUDE.md previa provedores transacionais (Resend/SES/Postmark). Usuário decidiu: implementação própria em Java permitindo cadastro de múltiplas contas SMTP pelo cliente final (assessoria usa o e-mail dela, não o nosso).

## Decision
- Stack: **Jakarta Mail** (`jakarta.mail`) via `spring-boot-starter-mail`.
- **SMTP relay externo**: usuário cadastra credenciais (Gmail, Outlook, M365, servidor próprio do cliente). Nunca MTA local (Postfix/James).
- Tabela `email_account` com: `host`, `port`, `username`, `password_encrypted`, `from_address`, `from_name`, `tls_mode` (STARTTLS/SSL), `daily_quota`, `status`.
- Credenciais cifradas em repouso com **AES-GCM** + chave gerenciada (Railway secret / KMS futuro). Nunca logar senha.
- Envio assíncrono via fila (Spring `@Async` + Postgres-backed queue ou Redis futuro) com idempotência por `message_id`.
- Connection pool por conta SMTP. Circuit breaker em falha auth.
- Webhook/parser de bounces via IMAP do mesmo `email_account` (opcional fase 2).

## Alternatives considered
1. **Provedor transacional (Resend/SES)** — fácil, deliverability gerenciada. Rejeitado: assessorias querem usar e-mail próprio (branding, lista de contatos histórica, deliverability já estabelecida da conta delas).
2. **MTA próprio (Postfix/Apache James)** — controle total. Rejeitado: deliverability nova-do-zero é inferno (warmup IP, blacklists, reputação). Custo operacional alto.
3. **SMTP relay externo (escolhido)** — usuário traz a conta, sistema só envia. Deliverability é responsabilidade do dono da conta.

## Consequences
- Positive: zero custo de provedor; multi-tenant natural; cliente reutiliza reputação do domínio dele.
- Negative: suporte sofre com configurações erradas (porta, TLS, app password Gmail); cliente precisa criar app password / OAuth.
- Risks: credencial vazada = dano direto na conta do cliente — cifragem forte é obrigatória; rate limits do provedor (Gmail 500/dia, M365 10k/dia) — sistema deve respeitar e expor ao cliente.

## Impact on specs
- **Security**: cifragem AES-GCM, KMS, zero log de credencial, OWASP A02.
- **Compliance** (LGPD): base legal documentada por finalidade; opt-out honrado em todo envio; `List-Unsubscribe` header obrigatório.
- **Scalability**: fila com retry exponencial; rate limit por conta; backpressure.
- **Observability**: métricas por conta (sent/bounced/failed/quota_used); alerta em queda de auth.
- **Data-architecture**: `email_account`, `email_message`, `email_event` (envio/bounce/complaint).
- Novo módulo: `docs/specs/email/` (a criar).

## References
- PRD: `docs/product/vision.md`
- Jakarta Mail: https://eclipse-ee4j.github.io/mail/
- Spring Mail: https://docs.spring.io/spring-boot/docs/current/reference/html/io.html#io.email
