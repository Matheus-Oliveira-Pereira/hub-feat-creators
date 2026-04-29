# ADR-006: Notificações via WhatsApp Cloud API (oficial Meta)

## Status
Accepted — 2026-04-29

## Context
Avisos e informações operacionais (status de prospecção, tarefas, alertas) devem chegar por WhatsApp. Necessário cadastro de número de destino por usuário/contato. Decisão entre API oficial (Meta Cloud API) e bibliotecas não-oficiais (Baileys, whatsapp-web.js).

## Decision
- **WhatsApp Cloud API oficial (Meta)**.
- Número business verificado + WABA (WhatsApp Business Account).
- Integração via REST (Graph API `v21.0+`) — cliente HTTP nativo Spring (`RestClient`/`WebClient`). Sem SDK terceiro instável.
- Templates HSM (Highly Structured Messages) aprovados pela Meta para mensagens fora da janela de 24h (notificações proativas).
- Mensagens free-form somente dentro da janela de 24h após resposta do destinatário.
- Tabela `whatsapp_account` (número business, phone_number_id, waba_id, access_token cifrado).
- Tabela `whatsapp_contact` (número destino + opt-in timestamp + base legal LGPD).
- Webhook receiver para status (sent/delivered/read/failed) e mensagens recebidas.
- Fila assíncrona com idempotência (mesma mensagem não envia 2x).

## Alternatives considered
1. **Baileys / whatsapp-web.js (não-oficial)** — grátis, sem aprovação. Rejeitado: TOS Meta proíbe → ban de número é questão de tempo; QR code scan = sessão volátil; risco reputacional para SaaS comercial.
2. **Twilio WhatsApp** — abstrai Cloud API. Rejeitado: markup sobre preço Meta, vendor lock-in adicional.
3. **Cloud API oficial direto (escolhido)** — preço de tabela Meta, sem intermediário, suporte oficial.

## Consequences
- Positive: conformidade com TOS, escala oficial, suporte Meta, integração estável.
- Negative: custo por conversa (tabela Meta — utility/marketing/auth/service); aprovação de templates demora 24-48h; setup business verification.
- Risks: rejeição de template trava feature; rate limit por tier (1k/dia inicial → 100k/dia tier máximo); webhook precisa endpoint público HTTPS com verify token.

## Impact on specs
- **Security**: validação assinatura webhook (`X-Hub-Signature-256`); access_token cifrado; verify token em env.
- **Compliance** (LGPD): opt-in explícito antes do primeiro envio; texto de opt-out em toda mensagem; registro de consentimento (timestamp + IP + finalidade).
- **Scalability**: fila + rate limit por número; respeitar tier Meta.
- **Observability**: métricas por template (delivery rate, read rate, failure reasons); alerta em queda de delivery.
- **Data-architecture**: `whatsapp_account`, `whatsapp_contact`, `whatsapp_message`, `whatsapp_event`.
- Novo módulo: `docs/specs/whatsapp/` (a criar).

## References
- PRD: `docs/product/vision.md`
- Meta Cloud API: https://developers.facebook.com/docs/whatsapp/cloud-api
- Pricing: https://developers.facebook.com/docs/whatsapp/pricing
