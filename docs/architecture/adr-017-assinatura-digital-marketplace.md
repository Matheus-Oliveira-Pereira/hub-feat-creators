# ADR-017: Assinatura Digital de Contratos (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 AC-5: aceite de proposta gera contrato assinado por marca e assessoria. Necessário:
- Assinatura digital com validade jurídica MP 2.200-2/2001 (BR) ou eIDAS (intl futuro)
- Trilha de auditoria (IP, timestamp, hash do documento)
- API para envio programático + webhook de status
- Armazenamento de evidências por 10 anos (PRD-017 §95 Compliance)
- Custo por documento controlado (margem do take rate)

## Decision

**Clicksign API** como provedor primário.

- Assinatura eletrônica avançada com hash + timestamp ICP-Brasil opcional
- API REST + webhooks (signature HMAC)
- Pacotes a partir de R$ 0,99/documento em volume
- Armazenamento legal de 10 anos incluso no plano
- Templates de contrato com placeholders → preenchemos via Mustache (já temos lib do PRD-004)
- Tabela `contrato_assinatura` (clicksign_document_key, status, signed_marca_em, signed_assessoria_em, evidence_url)
- Abstração `SignatureProvider` interface para troca futura (DocuSign intl)

## Alternatives considered

1. **Clicksign (escolhido)** — BR-first, valor jurídico via MP 2.200-2, preço competitivo, API simples, suporte PT-BR. Limitação: lock-in BR; ICP-Brasil é addon caro se exigido.

2. **DocuSign** — padrão global, eIDAS qualificada, base massiva. Rejeitado MVP: preço 3-5x maior em BRL, fluxo de assinatura menos otimizado para celular BR, suporte BR fraco. Reavaliar Fase 5 intl.

3. **D4Sign** — concorrente BR direto da Clicksign. Rejeitado: paridade técnica mas reputação de suporte inferior nos últimos 12 meses; ecossistema de integrações menor.

4. **Autentique** — barato, simples. Rejeitado: foco em B2C, API marketplace-friendly limitada, sem suporte robusto a múltiplos signatários sequenciais com ordem custom.

5. **Self-hosted (assinatura via PDF + hash chain próprio)** — barato. Rejeitado: valor jurídico depende de aceitação do juiz; HUB vira contraparte técnica em litígio; manutenção da trilha de evidências = custo escondido.

## Consequences

- Positive: time-to-market curto, validade jurídica reconhecida, custo previsível, BR-friendly mobile.
- Negative: lock-in Clicksign; sem qualificação eIDAS (impede mercado europeu sem migrar p/ DocuSign).
- Risks: SLA Clicksign 99.5% — indisponibilidade trava fechamento de contratos; mitigar com retry exponencial + fallback de "guarda offline" (gera PDF assinado quando voltar).

## Impact on specs

- **Security**: webhook signature obrigatório; document_key tratado como referência opaca; PDFs originais armazenados em `AttachmentStorage` (ADR-012) + hash SHA-256 antes do envio.
- **Compliance**: evidências armazenadas 10 anos (Clicksign cobre); cópia local do PDF assinado em `attachment_storage`; LGPD: signatário consente via clique → registrar IP + timestamp + base legal "execução de contrato".
- **Observability**: métricas tempo médio assinatura, taxa de abandono, contratos pendentes > 7d (alerta).
- **API**: `/api/v1/marketplace/contratos/{id}/assinar` cria envelope; webhook `/api/v1/webhooks/clicksign` atualiza status.
- Novo módulo: `docs/specs/signature/` (a criar).

## References
- PRD: `docs/product/17-marketplace.md`
- Clicksign API: https://developers.clicksign.com/docs
- MP 2.200-2/2001: https://www.planalto.gov.br/ccivil_03/mpv/antigas_2001/2200-2.htm
- ADR relacionado: ADR-016 (gateway), ADR-018 (NF)
