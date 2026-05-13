# ADR-016: Payment Gateway para Marketplace (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 (Marketplace) requer gateway de pagamento com suporte a:
- Split payment (marca paga → take rate HUB + valor para assessoria)
- Escrow (retenção até entrega aprovada)
- Sub-contas (KYC por assessoria; cada assessoria = recebedor)
- Webhooks com signature (PCI-DSS scope mínimo)
- Mercado BR-first: público-alvo são assessorias e marcas brasileiras → meios de pagamento locais (PIX, boleto, cartão BRL)

Decisão entre 2 candidatos principais e variantes.

## Decision

**Pagar.me Marketplace (Stone)** como gateway primário.

- Split nativo via API Recebedores + Transferências automáticas
- Suporte PIX, boleto, cartão (parcelamento até 12x), wallets
- Anti-fraude integrado (Clearsale opcional)
- Webhook com `X-Hub-Signature` (HMAC-SHA256)
- API REST estável; SDK Java existe mas usaremos HTTP direto (`HttpClient` nativo, padrão do projeto)
- Tabelas: `payment_recebedores` (sub-conta assessoria), `payment_transactions` (charge_id, status), `payment_webhooks_log` (idempotência)

**Stripe Connect = Fase 5** (expansão internacional). Schema do projeto fica vendor-agnostic via interface `PaymentGateway` para permitir adapter futuro.

## Alternatives considered

1. **Stripe Connect Standard/Express** — robusto, melhor DX globalmente, sub-contas com onboarding hosted. Rejeitado MVP: PIX limitado (só via Stripe Brasil beta), assessorias BR estranham fluxo gringo, boleto via 3rd party, IOF + spread cambial elevam custo, suporte BR fraco. Reavaliar Fase 5.

2. **Pagar.me Marketplace (escolhido)** — split nativo BRL, PIX/boleto first-class, MED automatizada, dashboard PT-BR para recebedores. Limitação: lock-in BR; expansão intl precisa adapter.

3. **Mercado Pago Marketplace** — preço competitivo, base ML grande. Rejeitado: API marketplace menos madura que Pagar.me, split mais frágil, casos de retenção arbitrária reportados.

4. **Asaas Subcontas / Iugu Marketplace** — opções locais. Rejeitado: volume/escala menor → menor maturidade técnica para volumetria projetada.

5. **PIX direto via Banco Central (sem gateway)** — barato, mas HUB precisaria ser PSP regulado pelo BCB (ver ADR-019). Inviável MVP.

## Consequences

- Positive: time-to-market curto; meios de pagamento locais cobertos; split + escrow nativos; menor custo BRL vs Stripe; suporte PT-BR.
- Negative: lock-in BR; abstração `PaymentGateway` precisa ser desenhada cuidadosamente para troca futura; custo de MDR + taxa de marketplace (~4-5% cartão, R$ 0,99 PIX).
- Risks: Pagar.me/Stone podem mudar política de marketplace; janela D+30 padrão de cartão; chargeback fica com HUB se split já ocorreu (mitigar com escrow de 30d antes do release).

## Impact on specs

- **Security**: webhook signature obrigatório; chaves API em secrets; sem PCI scope (tokenização via Pagar.me Checkout/Elements).
- **Compliance**: KYC delegado ao gateway (Pagar.me faz onboarding do recebedor com CNPJ + sócios); HUB não armazena PAN.
- **Scalability**: idempotência por `transaction_id`; webhook deduplication via tabela append-only.
- **Observability**: métricas GMV, MDR efetivo, taxa de chargeback, tempo médio até release escrow.
- **Data-architecture**: tabelas novas conforme V18 (a criar com PRD-017).
- Novo módulo: `docs/specs/payments/` (a criar).

## References
- PRD: `docs/product/17-marketplace.md`
- Pagar.me Marketplace: https://docs.pagar.me/docs/marketplace
- Stripe Connect (comparação futura): https://stripe.com/docs/connect
- ADR relacionado: ADR-017 (assinatura), ADR-018 (NF), ADR-019 (regulatório), ADR-020 (take rate)
