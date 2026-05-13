# ADR-018: Emissão de Nota Fiscal (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 AC-10: emissão de NFS-e (Nota Fiscal de Serviço eletrônica) por contrato concluído. Complexidades:
- NFS-e é **municipal** no Brasil: cada prefeitura tem layout, webservice e regras próprias (ABRASF v2.x é parcial)
- Retenção PIS/COFINS/ISS configurável por município + atividade
- Emissor é a **assessoria** (prestadora do serviço), não o HUB → integração precisa multi-tenant com certificado A1 por assessoria
- Take rate do HUB requer NF própria (HUB emite NFS-e contra a assessoria pelo serviço de intermediação)

## Decision

**NFE.io** como provedor primário.

- Cobertura ~5.000 municípios BR (maior do mercado)
- API REST unificada (esconde particularidades municipais)
- Armazenamento de certificado A1 cifrado por empresa (assessoria carrega certificado via UI; HUB armazena cifrado AES-GCM com `NF_KEY`)
- Webhook de status (autorizada, rejeitada, cancelada)
- Suporte NFS-e + NF-e (produto, futuro) + NFC-e
- Tabelas: `nf_empresa` (1:1 com assessoria, certificado_a1_enc), `nf_emissoes` (provider_id, contrato_id, status, xml_url, pdf_url, rps_numero)
- Abstração `NfProvider` interface (escapar do provedor é caro mas possível)

**HUB emite NFS-e do take rate** via NFE.io também — empresa "HUB Feat Creators Ltda." com certificado próprio em vault separado.

## Alternatives considered

1. **NFE.io (escolhido)** — maior cobertura municipal, API consolidada, doc PT-BR, preço por NF emitida (~R$ 0,30-0,50). Risco: vendor crítico (NF travada = bloqueia contrato).

2. **Plugfy** — bom para SaaS, foco multi-tenant. Rejeitado MVP: cobertura municipal menor (~3.500), API menos madura para casos exóticos (Recife, Salvador), suporte mais lento.

3. **Eduzz NF** — preço agressivo. Rejeitado: foco em infoprodutos, marketplace generalista mal documentado, casos de instabilidade reportados em 2025.

4. **eNotas** — concorrente direto NFE.io. Rejeitado: API mais verbosa, doc inferior, preço similar sem diferencial.

5. **Integração direta município-a-município (sem provider)** — controle total, custo zero por NF. Rejeitado: equipe pequena, cada município é 1-2 semanas de integração + manutenção; 5.000 municípios = inviável.

6. **NF manual fora plataforma** — assessoria emite por conta própria, faz upload. Rejeitado: viola AC-10 (emissão integrada); experiência ruim; risco de não-emissão = sonegação.

## Consequences

- Positive: time-to-market mínimo, cobertura ampla, abstração municipal, multi-tenant via certificado por assessoria.
- Negative: custo por NF emitida; lock-in NFE.io; certificado A1 carrega complexidade (validade 1 ano, renovação manual hoje).
- Risks: SLA NFE.io ~99% — indisponibilidade trava emissão; mitigar com fila + retry; rejeição por município (dados incompletos) = UX feia se não tratada; certificado vencido = falha de emissão silenciosa (alerta com 30/15/7d antes do vencimento).

## Impact on specs

- **Security**: certificado A1 cifrado em repouso (AES-GCM com chave dedicada `NF_KEY`); senha do certificado nunca em log; webhook signature.
- **Compliance**: retenções fiscais (PIS/COFINS/ISS) calculadas via NFE.io conforme município e atividade da assessoria (CNAE); HUB não dá conselho fiscal — assessoria responde pela validade dos dados; XML armazenado 5 anos (exigência fiscal).
- **Observability**: métricas taxa de autorização, tempo médio até autorização, rejeições por município/motivo, certificados vencendo.
- **Data-architecture**: `nf_empresa`, `nf_emissoes`, `nf_eventos` (append-only).
- **Operacional**: runbook para fluxo "certificado vencido" + "NF rejeitada manualmente" + reenvio.
- Novo módulo: `docs/specs/nf/` (a criar).

## References
- PRD: `docs/product/17-marketplace.md`
- NFE.io API: https://nfe.io/docs/api
- ABRASF v2.x: https://abrasf.org.br/biblioteca
- ADR relacionado: ADR-016 (gateway — split casa com emissão), ADR-019 (regulatório)
