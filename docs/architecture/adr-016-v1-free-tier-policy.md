# ADR-016: Política de Serviços de Terceiros — v1 Free-Only com Exceções

## Status
Accepted — 2026-05-13

## Context
Decisão estratégica para v1 (lançamento): HUB Feat Creators **não cobra usuários** e **só consome serviços gratuitos**, com lista fechada de exceções pagas necessárias para a operação.

Motivação:
- Time-to-market e custo operacional mínimo
- Validar produto antes de monetizar
- Reduzir complexidade legal/fiscal (sem marketplace, sem take rate, sem NF emitida pelo HUB para usuários)
- Foco em adoção orgânica vs receita

## Decision

### Regra geral
Toda integração em v1 deve usar **tier gratuito** ou software **open-source self-hosted**. Adicionar dependência paga = requer ADR explícito + atualização desta lista.

### Exceções aprovadas (serviços pagos permitidos em v1)

| Serviço | Tipo | Custo | Justificativa |
|---|---|---|---|
| **WhatsApp Cloud API** (Meta) | Pago por conversa | Tabela Meta (utility/marketing/auth/service) | ADR-006: bibliotecas não-oficiais violam ToS Meta; canal de notificação crítico |
| **Anthropic API** (Claude) | Pago por token | Pay-as-you-go | Uso interno de dev (Claude Code + agents); zero impacto para usuário final |
| **Hosting paid tier** (Railway/Vercel) | Pago se exceder free | Pay-as-you-go | Free tier inicial; upgrade quando produção exigir uptime/recursos |
| **Domínio + e-mail transacional HUB** | Pago | ~R$ 50/ano domínio + free tier SMTP transacional (Resend/SendGrid) | Identidade própria do HUB + e-mails do sistema (verify, reset senha) |

### Implicações imediatas

- **Marketplace (ex-PRD-017): cancelado em v1.** Sem take rate, sem split de pagamento, sem NF emitida pelo HUB, sem assinatura digital paga, sem KYC pago. Roadmap futuro só após validação do produto base.
- **E-mail outbound de usuários** (PRD-004): segue SMTP relay externo cadastrado pelo próprio usuário (Gmail/Outlook/M365) — credenciais cifradas — sem custo para HUB.
- **WhatsApp** (PRD-008): usuário traz própria conta Cloud API + número business; HUB não custodia conta Meta.
- **Social OAuth** (PRD-015): Instagram/YouTube/TikTok APIs gratuitas até cota; respeitar rate limits.
- **ai-worker** (PRD-016): self-hosted (sentence-transformers no Docker próprio); zero custo de inferência externa.
- **pgvector**: extensão PostgreSQL OSS; zero custo.
- **Web Push** (PRD-009): VAPID padrão aberto; FCM/APNs free tier (Expo); zero custo.
- **Compliance/LGPD**: HUB não custodia dados de pagamento, não emite NF para usuário final, não retém fundos → reduz drasticamente exposição regulatória.

### O que muda no produto
- Sem tela de billing, sem checkout, sem plan upgrade
- Sem cobrança recorrente, sem trial expirando, sem paywall
- Sem cadastro de cartão de crédito de usuário
- "Plano" do usuário = único, ilimitado, grátis em v1

## Alternatives considered

1. **Free-only com exceções fechadas (escolhido)** — clareza de escopo; gate explícito para novas dependências pagas; usuário não paga.

2. **Free-only puro (sem exceções)** — máxima pureza. Rejeitado: WhatsApp via Baileys = banimento garantido (ver ADR-006); Anthropic API é inviável trocar por modelo local sem perda de qualidade material; hosting 100% free tier não aguenta produção.

3. **Freemium em v1** (free + plano pago opcional) — captura early-adopter willingness-to-pay. Rejeitado: introduz complexidade de billing antes da hora; distrai foco de produto; risco de canibalizar adoção orgânica.

4. **Pago desde dia 1** — clarity de monetização. Rejeitado: barreira alta de adoção; HUB ainda não validou PMF; competidores oferecem grátis para entrar.

## Consequences

- Positive: zero pressão de monetização, zero compliance financeiro/fiscal complexo, custo operacional baixíssimo, adoção mais fácil, escopo de v1 enxuto (sem marketplace/billing).
- Negative: sustentabilidade depende de runway/investimento até v2; risco de criar expectativa "sempre grátis"; sem sinal de willingness-to-pay até monetizar.
- Risks: usuário acostuma com grátis e churn ao monetizar v2; uso abusivo (sem fricção paga = sem freio); custos próprios (WhatsApp, hosting) podem escalar mais rápido que captação.

## Impact on specs

- **Compliance**: ToS de uso "grátis em fase beta"; reservar direito de monetizar v2 com aviso prévio (30d); LGPD não muda (já tratada).
- **Security**: sem dado de cartão = redução de PCI scope a zero; foco mantém em LGPD + AES-GCM tokens externos.
- **Scalability**: tier gratuito de hosting tem limite; alertar quando aproximar (CPU/memória/builds).
- **Observability**: monitorar custo WhatsApp/Anthropic/hosting mensal; budget alert por serviço.
- **Roadmap**: monetização (marketplace, billing) volta como PRD novo quando v1 validar PMF — não copiar PRD-017 antigo direto; rever do zero.

### Adição/remoção de dependência paga
Qualquer mudança nesta lista requer:
1. Novo ADR justificando inclusão
2. Atualização desta tabela
3. Update CLAUDE.md
4. Update `.env.example` se aplicável

## References
- ADR-006 (WhatsApp Cloud API — exceção #1)
- Visão de produto: `docs/product/vision.md`
- Decisão deletou: PRD-017 (Marketplace) e ADRs 017-021 (gateway/assinatura/NF/regulatório/take rate/KYC) — voltam como PRD/ADRs novos quando v2 entrar no roadmap
