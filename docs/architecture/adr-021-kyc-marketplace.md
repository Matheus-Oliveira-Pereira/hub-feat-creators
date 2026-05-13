# ADR-021: KYC e Validação Cadastral de Marca/Assessoria (PRD-017)

## Status
Proposed — 2026-05-13

## Context
PRD-017 §68: "KYC marca (CNPJ + sócios) via integração Serpro/CNA". Necessidades:
- Validação CNPJ ativo + situação cadastral (Receita Federal)
- Quadro societário (sócios + participação)
- PEP (Pessoa Politicamente Exposta) — exigência PLD-FT
- Sanções OFAC/ONU/COAF — exigência PLD-FT internacional
- Listas restritivas (CCF, Serasa) — risco de crédito
- KYC para assessoria já existe no signup interno (apenas e-mail + senha) → precisa upgrade para marketplace

ADR-019 estabeleceu que gateway (Pagar.me) faz **KYC regulatório** para recebimento; este ADR cobre **KYC complementar do HUB** (qualidade de cadastro, antifraude, listas restritivas).

## Decision

**Idwall** como provedor KYC complementar.

- API REST com endpoints: `/cnpj`, `/cpf`, `/processos`, `/pep`, `/sancoes`, `/score`
- Combo "KYC PJ" cobre CNPJ + QSA + PEP + sanções + score em uma chamada (~R$ 3,00-5,00/consulta)
- Onboarding flow com OCR + face match para sócio assinante (~R$ 8,00/onboarding)
- Webhook de re-validação periódica (CNPJ que vira "BAIXADA" depois de cadastrado)
- Tabelas: `kyc_consultas` (tipo, documento_hash, score, resultado JSONB, ts), `kyc_status_entidade` (assessoria_id|marca_id, status, ultima_validacao, proxima_validacao)

Fluxo:
1. Cadastro marca/assessoria pede CNPJ
2. HUB chama Idwall → score + status
3. Score >= 700 + situação "ATIVA" + sem sanção = aprovado automático
4. Score < 700 ou PEP detectado = "EM ANÁLISE" (revisão manual via admin)
5. Re-validação anual (cron) + on-demand antes de contrato > R$ 50k
6. Falha de chamada Idwall = cadastro fica "PENDENTE_KYC" mas não bloqueia signup → bloqueia primeiro contrato

## Alternatives considered

1. **Idwall (escolhido)** — líder BR em KYC, APIs maduras, OCR + facematch nativo, doc PT-BR, casos de uso marketplace conhecidos (iFood, Loft). Custo previsível.

2. **BigData Corp** — base massiva BR, preço competitivo. Rejeitado: API menos polida; OCR + facematch via parceiro; doc PT-BR irregular.

3. **Serpro (Receita Federal direta)** — fonte oficial, barato (R$ 0,30/consulta). Rejeitado: requer convênio direto com Serpro (3-6 meses, exige CNPJ próprio + caso de uso aprovado); só CNPJ — sem PEP/sanção/score. Reavaliar como **complemento** futuro (consulta primária + Idwall para enriquecimento) quando volume justificar.

4. **CNA (Casa de Negócios)** — citado no PRD. Rejeitado: API menor cobertura, descontinuação anunciada de alguns endpoints em 2025.

5. **Unico Check / Datafácil** — concorrentes diretos. Rejeitado paridade técnica sem diferencial.

6. **KYC manual (admin valida CNPJ via Receita)** — zero custo de SaaS. Rejeitado MVP: gargalo humano para escala marketplace; risco de fraude (sócio laranja, CNPJ baixado); inviável > 50 cadastros/dia.

7. **Não fazer KYC complementar** — confiar 100% no gateway. Rejeitado: gateway só valida para recebimento; HUB ainda responde por fraude de catálogo (assessoria falsa publicada); listas restritivas (OFAC) são responsabilidade do HUB também.

## Consequences

- Positive: redução de fraude no marketplace; conformidade PLD-FT/COAF; trilha de auditoria de cadastro; UX rápida (consulta ~2s); upgrade simples no signup quando marketplace ativar.
- Negative: custo por consulta (~R$ 3-5 PJ + R$ 8 onboarding); lock-in Idwall; CNPJ pequeno/MEI pode falhar score sem ser fraude.
- Risks: Idwall fora do ar → fila + retry, falha 24h+ = aprovar manualmente; PEP falso-positivo (homônimos) → fluxo de revisão manual; LGPD: tratamento de dado de sócio exige base legal "execução de contrato" + ROPA atualizado; armazenar `resultado JSONB` cifrado se contiver PII sensível (CPF mascarado em logs).

## Impact on specs

- **Compliance LGPD**: documento (CPF/CNPJ) hasheado para chave de busca; resposta Idwall cifrada em repouso; base legal "execução de contrato + obrigação legal PLD"; ROPA registra finalidade KYC; retenção 5 anos pós-encerramento.
- **Security**: chave API Idwall em secret; webhook signature; PII em `kyc_consultas.resultado` cifrada AES-GCM com `KYC_KEY`.
- **Observability**: métricas taxa de aprovação automática, tempo médio de análise manual, custo médio KYC/cadastro, alertas de mudança em listas restritivas (sancionado depois de cadastrado).
- **Data-architecture**: `kyc_consultas` (append-only), `kyc_status_entidade` (status atual).
- **Operacional**: runbook "PEP detectado" + "sócio sancionado" + "CNPJ baixou depois de cadastrado".
- Novo módulo: `docs/specs/kyc/` (a criar).

## References
- PRD: `docs/product/17-marketplace.md`
- Idwall API: https://docs.idwall.co
- Resolução BCB 4.753/2019 (KYC): https://www.bcb.gov.br/pre/normativos/busca/downloadNormativo.asp?arquivo=/Lists/Normativos/Attachments/50890/Res_4753_v3_P.pdf
- ADR relacionado: ADR-016 (gateway faz KYC regulatório), ADR-019 (enquadramento)
