# ADR-020: Modelo de Take Rate do Marketplace (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 Open Question §122: "Take rate fixo ou variável por plano?". Decisão impacta:
- Schema (`contratos.take_rate` precisa suportar variabilidade?)
- Lógica de split (cálculo dinâmico no momento da geração da charge)
- Monetização (LTV assessoria, churn, GMV)
- Concorrência (Hotmart 9.9%, Eduzz 9.9%+R$1, Mercado Livre Ads 6-13%, marketplaces de influência intl 15-20%)

Restrição: HUB capta receita de duas fontes simultâneas no MVP — **assinatura SaaS** (plano da assessoria) + **take rate** (do marketplace). Modelo precisa não-canibalizar SaaS.

## Decision

**Take rate variável por plano da assessoria** — escala inversa: paga mais SaaS, leva take rate menor.

| Plano assessoria | Assinatura mensal | Take rate marketplace |
|---|---|---|
| **Free** | R$ 0 | 15% |
| **Starter** | R$ 199/mês | 12% |
| **Pro** | R$ 499/mês | 8% |
| **Enterprise** | R$ 1.499/mês | 5% (negociável) |

Implementação:
- Schema: `assessoria_plano` (assessoria_id, plano, take_rate_override) — campo `take_rate_override` permite contrato comercial customizado para Enterprise
- Cálculo: `MarketplaceFeeService.computeTakeRate(assessoriaId)` consulta plano ativo no momento da geração do `contrato` → grava `take_rate` no contrato (snapshot, **nunca** recalcula)
- Snapshot é crítico: troca de plano não retroage em contratos abertos
- Take rate aplica sobre `valor_total` do contrato (NÃO sobre line items)
- Floor: R$ 9,90 mínimo (evita contratos R$ 50 com take de R$ 4 viáveis)

## Alternatives considered

1. **Take rate fixo único (ex: 10%)** — simples, previsível. Rejeitado: não diferencia clientes que pagam SaaS premium; assessora grande migra de plataforma por economia de 5pp.

2. **Take rate variável por plano (escolhido)** — alinha incentivos (assessora grande paga assinatura, paga menos take); marketplace puxa upgrade de SaaS.

3. **Take rate variável por volume (degraus de GMV)** — "primeiros R$ 10k = 15%, próximos R$ 40k = 10%, acima = 5%". Rejeitado MVP: complexidade de cálculo e UX; cria comportamento estranho (assessor segura faturamento p/ entrar em degrau melhor); reavaliar Fase 2 do marketplace.

4. **Take rate variável por vertical** — moda 8%, tech 12%, etc. Rejeitado: complexidade sem dado para calibrar; arbitrário; vira marketing.

5. **Só assinatura SaaS, take rate zero** — modelo SaaS puro. Rejeitado: deixa GMV imensa na mesa; marketplace só faz sentido com take; assinatura sozinha sub-monetiza alto-volume.

6. **Só take rate, sem assinatura SaaS** — modelo marketplace puro. Rejeitado: receita instável (depende de fechamento de contratos); destrói a recorrência atual; assessoras pequenas sem GMV ficam grátis para sempre.

## Consequences

- Positive: monetização dupla (SaaS + take rate); incentivo claro para upgrade; protege LTV de Enterprise; competitivo vs Hotmart/Eduzz (~10%) sem ser predatório.
- Negative: complexidade de billing (assinatura + transacional); UX precisa explicar bem; risco de mensagem "duas cobranças" confundir comercial.
- Risks: assessora Free com 1 contrato grande paga muito take e abandona; assessora Enterprise negocia take ainda menor → manter `take_rate_override` documentado com aprovação BLLG/OWNR; benchmark de mercado pode forçar baixa (intl marketplaces ainda 15-20%).

## Impact on specs

- **Data-architecture**: `assessoria_plano` (plano + assinatura ativa); `contratos.take_rate` snapshot; `take_rate_override` auditável (quem aprovou, quando).
- **Compliance**: take rate exposto no contrato assinado (transparência para marca e assessoria); cláusula no ToS.
- **Observability**: métricas take rate efetivo por plano, GMV por plano, conversão Free→Starter via Marketplace ("você economizaria R$ X/mês upgradeando").
- **API**: `MarketplaceFeeService` central; nunca calcular take rate inline em controllers.
- **Billing**: integrar com `assessoria_plano` quando módulo de billing/subscription existir (atualmente não há — PRD futuro de "Billing"); MVP do PRD-017 pode hardcoded para um único plano até billing existir, mas schema já preparado.

## References
- PRD: `docs/product/17-marketplace.md`
- Benchmark Hotmart/Eduzz (público): https://hotmart.com/pt-br/blog/comissoes-hotmart
- ADR relacionado: ADR-016 (gateway), ADR-019 (regulatório)
