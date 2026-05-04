# PRD-017: Marketplace

## Context

Visão longa do produto: marca chega ao HUB, monta briefing, recebe propostas de assessorias com creators sugeridos, contrata na plataforma. Inverte fluxo: hoje assessora caça marca; futuro = marca também encontra assessora/creator.
Vision: Fase 4.
Depende de: PRDs 01-16; em especial 06, 07, 13, 16. **Maturidade de produto + base de creators robusta**.

## Objective

Cadastro self-service de marcas no HUB, listagem pública de assessorias/creators (com opt-in), publicação de briefings abertos, recebimento de propostas, contratação e gestão financeira on-platform — com take rate ou assinatura.

## Scope

### Includes
- [ ] **Cadastro marca self-service**: signup paralelo a assessoria; `marca_user` separado
- [ ] **Catálogo público**: assessorias e creators que opt-in aparecem em busca; perfil com métricas, casos, verticais
- [ ] **Briefing aberto**: marca publica brief; assessorias inscritas recebem notificação
- [ ] **Propostas**: assessoria envia proposta (creators sugeridos via PRD-016, valor, prazo, condições)
- [ ] **Negociação**: chat estruturado marca↔assessoria
- [ ] **Contratação on-platform**: contrato gerado, assinatura digital (DocuSign/Clicksign integrações)
- [ ] **Pagamento**: split via gateway (Stripe Connect, Pagar.me Marketplace) — take rate HUB
- [ ] **Escrow**: valor retido até entrega aprovada; release automático/manual
- [ ] **Disputas**: fluxo de mediação manual MVP
- [ ] **Avaliações bidirecionais**: marca avalia assessoria; assessoria avalia marca
- [ ] **Search & discovery**: filtros vertical, orçamento, região; full-text + match (PRD-016)
- [ ] **Compliance financeiro**: emissão de NF (integração com sistema fiscal), retenção de impostos

### Excludes
- [ ] Marca contrata creator direto (sem assessoria) — fora MVP marketplace; preserva valor da assessoria
- [ ] Pagamento internacional / multimoeda — Fase 5
- [ ] White-label do marketplace — fora escopo

## Not Doing

- **Disrupção da assessoria**: produto NÃO compete com assessora; marketplace usa creators **através** da assessora. Discovery direto de creator é roadmap muito posterior, com decisão de produto explícita.
- **Pagamento manual fora plataforma**: contrato fechado fora = sem take rate. Termos de uso desincentivam.

## User Stories

- Como marca, quero publicar campanha e receber 5 propostas de assessorias
- Como assessora, quero responder briefings públicos para captar marcas novas
- Como marca, quero pagar dentro da plataforma e ter NF
- Como HUB, quero arrecadar take rate sustentável

## Acceptance Criteria

- [ ] **AC-1**: Marca self-service cria workspace com identidade separada (`marca_users`); plano free vs pago
- [ ] **AC-2**: Briefing público marca tem flag `marketplace_visible=true`; default false (não vaza interno)
- [ ] **AC-3**: Notificação assessoria filtra por matching (vertical, budget, região) — não spammar
- [ ] **AC-4**: Proposta gerada com PDF anexo (resumo + anexos); revisão antes envio
- [ ] **AC-5**: Aceite proposta cria `contrato` com cláusulas mínimas; assinaturas via integração externa
- [ ] **AC-6**: Pagamento split: marca paga 100%, gateway retém take rate HUB, libera ao assessor após entrega
- [ ] **AC-7**: Escrow release: assessora confirma entrega + marca aprova; ou janela 14d sem disputa
- [ ] **AC-8**: Disputa abre ticket interno; nenhuma transação se desbloqueia até resolver
- [ ] **AC-9**: Avaliação só após contrato finalizado; bidirecional cego (revelar simultâneo)
- [ ] **AC-10**: Compliance: emissão NF integrada (NFS-e municipal); retenção PIS/COFINS/ISS configurável
- [ ] **AC-NF-1**: Auditoria financeira inviolável: ledger append-only com hash chain
- [ ] **AC-NF-2**: SLA pagamento: liberação em D+2 após release escrow
- [ ] **AC-NF-3**: Cobertura ≥ 90% em `PaymentService`, `EscrowService`, `LedgerService` (criticidade financeira)

## Technical Decisions

- **Gateway pagamento**: Stripe Connect (US/intl) ou Pagar.me Marketplace (BR) — ADR pendente; preferência BR pelo público
- **Assinatura digital**: Clicksign (BR) ou DocuSign (intl)
- **Ledger**: tabela `ledger_entries` append-only (DEBIT/CREDIT) com hash chain; nunca UPDATE/DELETE
- **NF**: integração com Plugfy/Eduzz/NFE.io (BR municipal varia)
- **Compliance**: KYC marca (CNPJ + sócios) via integração serpro/cna; KYC assessoria já no signup interno

### Schema

```sql
marca_users (id, marca_id FK UNIQUE, email, senha_hash, mfa_*, status, created_at)

briefings_publicos (id, marca_id, titulo, descricao, vertical, audiencia JSONB, formato, budget_min, budget_max,
  prazo_dt, marketplace_visible BOOL, status, created_at, deleted_at)

propostas (id, briefing_id FK, assessoria_id FK, valor NUMERIC, prazo_dt, conteudo TEXT,
  creators_sugeridos UUID[], status CHECK ('RASCUNHO','ENVIADA','VISUALIZADA','ACEITA','REJEITADA','EXPIRADA'),
  enviada_em, atualizada_em)

contratos (id, proposta_id FK UNIQUE, valor_total NUMERIC, take_rate NUMERIC, status,
  assinado_marca_em, assinado_assessoria_em, gateway_charge_id, escrow_status, created_at)

ledger_entries (id, contrato_id, tipo CHECK ('DEBIT','CREDIT'), valor NUMERIC, conta TEXT,
  prev_hash, hash, ts)

avaliacoes (id, contrato_id, autor_tipo CHECK ('MARCA','ASSESSORIA'), nota INT, comentario, revelada_em)

disputas (id, contrato_id, abridor_tipo, motivo, status CHECK ('ABERTA','EM_ANALISE','RESOLVIDA'), resolvida_em)
```

## Impact on Specs

- **Compliance**: KYC obrigatório, retenção fiscal, ROPA atualizado, contratos assinados arquivados 10y
- **Security**: gateway = PCI-DSS (não tocar dado de cartão direto), webhooks de pagamento com signature, ledger imutável
- **Scalability**: marketplace traz tráfego público (catálogo); CDN, cache agressivo
- **Observability**: GMV, take rate, contratos assinados, escrow disputado, NPS bidirecional
- **API**: `/api/v1/marketplace/*` (público + autenticado); webhooks gateway
- **Testing**: sandbox gateway em ITs; mock cliente NF; teste de hash chain ledger

## Rollout

- **Feature flag**: `feature.marketplace.enabled` por tenant + global
- **Beta fechado**: 5-10 assessorias + 10-20 marcas selecionadas → 6 meses iteração antes de abrir
- **Migrations**: V0017_*
- **Rollback**: flag off bloqueia rotas marketplace; tenants existentes inalterados
- **Legal**: ToS específico marketplace, Política de Pagamento, Política de Disputa — review jurídico obrigatório

## Métricas

- GMV mensal cresce N M/M (definir baseline pós-beta)
- Take rate efetivo dentro de target (5-15%)
- Disputas < 5% dos contratos
- Tempo briefing → proposta aceita mediana < 7d
- NPS marca ≥ 40, NPS assessoria ≥ 40

## Open questions

- [ ] Gateway: Stripe (intl-friendly) ou Pagar.me (Brasil-first)? — ADR-pendente
- [ ] HUB intermedia ou só facilita? — intermedia (escrow) para confiança; impacta enquadramento legal (instituição de pagamento? PIX intermediário?)
- [ ] Take rate fixo ou variável por plano? — variável; plano premium reduz take em troca de assinatura
- [ ] Discovery direto de creator (sem assessoria) algum dia? — roadmap aberto; decisão de produto separada
