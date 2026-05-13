# ADR-019: Enquadramento Regulatório do Fluxo de Pagamentos (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 Open Question §121: "HUB intermedia ou só facilita?" — questão central que determina se HUB é **Instituição de Pagamento (IP)** regulada pelo Banco Central via Resolução BCB 80/2021. Implicações:

- Se HUB **retém dinheiro** (escrow em conta própria) → custódia de fundos de terceiros = atividade regulada, exige autorização BCB
- Se HUB **não toca o dinheiro** (split direto via gateway) → não é IP, mas perde controle do escrow
- Marketplace típico no Brasil (iFood, Mercado Livre, Hotmart) opera via parceria com IP licenciada (Stone, Pagar.me, Adyen) → NÃO se enquadra como IP

LGPD/PLD-FT também impactam: KYC obrigatório acima de R$ 50k/mês por recebedor (Circular BCB 3.978/2020).

## Decision

**HUB NÃO é Instituição de Pagamento.** Marketplace usa Pagar.me (ADR-016) como IP autorizada pelo BCB. Modelo:

1. **Recebedor = assessoria** (cada uma cria sub-conta Pagar.me com KYC próprio)
2. **Pagador = marca** (paga via gateway; dinheiro **nunca** entra em conta HUB)
3. **Escrow = recurso do gateway** (Pagar.me "split com retenção" — dinheiro fica em conta de garantia do gateway, não do HUB)
4. **Take rate HUB** = sub-conta própria Pagar.me; recebe split automático junto com assessoria
5. **Release escrow** = HUB chama API Pagar.me para liberar; gateway executa a transferência
6. **Disputa** = HUB suspende release via API; chargeback é responsabilidade do gateway

**Implicação chave**: HUB é "subadquirente" no sentido comercial mas **facilitador** no sentido regulatório (RPS — Subadquirente). Não precisa autorização BCB enquanto:
- Não custodia fundos
- Não emite moeda eletrônica
- Não realiza arranjo de pagamento próprio
- Volume < R$ 500MM/ano (acima disso, BCB pode exigir registro)

KYC complementar do HUB (validação adicional além do gateway) usa **Idwall** (ADR a criar futuramente se PRD-017 evoluir).

## Alternatives considered

1. **HUB como IP licenciada (Sociedade de Crédito Direto / Instituição de Pagamento)** — autonomia total, captura todo MDR. Rejeitado: 18-24 meses para autorização BCB, capital mínimo R$ 1MM, compliance contínuo (PLD-FT, comunicação COAF mensal, auditoria PwC/KPMG), inviável para startup. Reavaliar > 5 anos.

2. **HUB facilitador via gateway IP (escolhido)** — modelo dominante no marketplace BR; sem fricção regulatória; gateway absorve compliance.

3. **HUB conta-corrente própria com escrow manual** — totalmente ilegal sem licença BCB; HUB respondendo solidariamente a fraude/PLD. Rejeitado: risco jurídico inaceitável.

4. **Pagamento P2P fora da plataforma + cobrança de assinatura** — modelo "anúncio classificado". Rejeitado: viola visão de produto (PRD-017 §11 take rate on-platform); reduz monetização drasticamente; sem controle de disputa.

## Consequences

- Positive: zero compliance regulatório direto; time-to-market rápido; pode operar imediatamente assim que contrato com Pagar.me estiver assinado; sem capital mínimo BCB.
- Negative: take rate efetivo do HUB é "take rate − MDR Pagar.me − taxa marketplace gateway" ≈ HUB fica com 5-10pp em vez de 15-20pp se fosse IP própria; dependência crítica do gateway.
- Risks: regulação muda (BCB tem revisto enquadramento de subadquirentes em 2024-2025) → monitorar; se volume > R$ 500MM/ano, BCB pode reclassificar; gateway pode bloquear conta HUB → contingência crítica (manter ADR aberto p/ Stripe/Adyen como backup futuro).

## Impact on specs

- **Compliance**: HUB documenta no ToS marketplace que é "facilitador" — fluxo de fundos é "Marca → Pagar.me → Recebedor (Assessoria + HUB)"; HUB nunca recebe valor de terceiro em conta própria.
- **Security**: HUB não armazena dado bancário de assessoria/marca (KYC + conta = no gateway); sub-conta gateway é referência opaca (`recebedor_id`).
- **Legal**: ToS marketplace + Política de Pagamento devem deixar explícito o modelo facilitador; cláusula de "HUB não responde por inadimplência entre partes" (mas responde pela facilitação correta).
- **Observability**: monitorar volume mensal; alerta se aproximar R$ 500MM/ano → trigger revisão regulatória.
- **Operacional**: runbook "gateway suspendeu conta HUB" (cenário extremo: contingência via 2º gateway).

## Open subquestions (não bloqueantes para a decisão)
- COAF: ainda precisamos comunicação de operações suspeitas? Hoje, gateway cuida — mas se HUB tiver "visão consolidada" do marketplace, pode haver dever próprio. Verificar com escritório jurídico antes do beta.
- ISS sobre take rate: HUB emite NFS-e para a assessoria (ADR-018); município HUB define alíquota; estrutura possível em SP (2-5%).

## References
- PRD: `docs/product/17-marketplace.md`
- Resolução BCB 80/2021: https://www.bcb.gov.br/estabilidadefinanceira/exibenormativo?tipo=Resolu%C3%A7%C3%A3o%20BCB&numero=80
- Circular BCB 3.978/2020 (PLD-FT): https://www.bcb.gov.br/pre/normativos/busca/downloadNormativo.asp?arquivo=/Lists/Normativos/Attachments/50905/Circ_3978_v3_P.pdf
- Lei 12.865/2013 (arranjos de pagamento): https://www.planalto.gov.br/ccivil_03/_ato2011-2014/2013/lei/l12865.htm
- ADR relacionado: ADR-016 (gateway), ADR-018 (NF)
