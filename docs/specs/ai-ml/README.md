# Module: AI/ML — HUB Feat Creator

> **Status**: módulo **placeholder** — feature de match marca↔influencer planejada para **Fase 3** do roadmap. Especificação completa será preenchida quando feature entrar em design.

## Visão

Sugerir matches entre marcas e influenciadores cadastrados na plataforma com base em:
- **Nicho** (moda, fitness, gaming, lifestyle...)
- **Audiência** (porte, faixa etária dominante, geografia, engajamento)
- **Histórico** (campanhas passadas, performance medida, status do relacionamento)
- **Compatibilidade de marca** (valores, restrições, segmento)

Output: ranking top-N influenciadores para uma marca (e vice-versa) com **explicação** ("audience match 85%, nicho fitness, engajamento 4.2% acima da média do nicho").

## Princípios

- **Explicabilidade > caixa preta** — cada sugestão precisa razão humana-legível; modelo não-explicável não entra em produção
- **Humano no loop** — sugestões são ranking, nunca decisão automática. Assessora confirma antes de qualquer ação (e-mail, contato)
- **Transparência ao influenciador** — quando feature ativa, influenciador pode ver/contestar o que a plataforma "sabe" sobre ele (LGPD direito de acesso)
- **Bias awareness** — auditar regularmente se modelo discrimina por gênero, raça, geografia, porte indevidamente

## Modelos planejados (rascunho — sujeito a mudanças)

| Modelo | Provider | Uso | Custo estimado |
|--------|----------|-----|----------------|
| TBD — embeddings de perfil | OpenAI `text-embedding-3-small` ou Voyage `voyage-3` | Vetorizar bio, nicho, conteúdo público | TBD |
| TBD — ranking | LightGBM ou similar (modelo próprio) | Score de match | $0 (self-hosted) |
| Claude Sonnet 4.x | Anthropic | Geração de razão explicativa do match em pt-BR | TBD |

> Decisões de modelo serão registradas em ADR específico (futuro: ADR-XXX-modelo-match) com benchmarks contra baseline.

## Prompt engineering (preview)

- **Versionamento**: Git — prompts em `apps/api/src/main/resources/prompts/<feature>/<version>.txt`
- **Format**: system + user com placeholders nomeados (`{{influenciador.nicho}}`, etc)
- **Review**: prompt change exige PR review como código
- **A/B**: comparar versões via promptfoo ou eval custom

## Evals

- **Framework**: a definir (candidatos: **promptfoo** + métricas custom; **Braintrust** quando time crescer)
- **Dataset**: amostra anonimizada de matches históricos validados pela assessora (com consentimento)
- **Métricas**:
  - Precision@10 (assessora valida 5+ dos top 10?)
  - NDCG@10 (ranking faz sentido?)
  - Latência: p95 < 5s para top-10
  - Custo por sugestão: < $0.05
  - Bias score (paridade demográfica entre nichos)
- **Frequência**: a cada mudança de prompt; semanal em produção via amostragem

## Guardrails

### Input validation
- Max tokens por request: limites por endpoint
- Filtro de PII em prompts enviados a LLMs externos — pseudonimizar nomes/contatos antes
- Rate limit: 100 sugestões/dia/assessoria no MVP da feature

### Output validation
- Output do LLM passa por:
  - JSON Schema validation
  - Hallucination check: IDs sugeridos **devem existir** no DB da assessoria
  - Safety filter: bloquear comparações depreciativas, juízos de valor sobre pessoas

### Cost ceiling
- Por request: $0.10 hard limit
- Por dia / assessoria: $5 default (configurável por plano)
- Mensal global: alerta se passar 1.5x do orçamento

### Fallback
- LLM externo fora → fallback para ranking puramente baseado em embeddings (sem geração de razão)
- Embedding service fora → retornar erro 503 com mensagem clara, **não** ranking heurístico ruim

### Human-in-the-loop (obrigatório)
- Sugestão **nunca** dispara contato automático
- Assessora vê ranking + razão + opção "marcar como ruim" (alimenta retrain)
- Marca/influenciador pode optar-out de aparecer em sugestões (LGPD)

## Governança

- [ ] Model card por modelo em produção (em `docs/specs/ai-ml/model-cards/`)
- [ ] Audit trail de cada sugestão (input, output, modelo, versão de prompt) — 1 ano retenção
- [ ] Documentação de bias e fairness checks (revisão trimestral)
- [ ] LGPD: base legal explícita para tratamento (legítimo interesse + opt-out)
- [ ] EU AI Act / ISO 42001: avaliar adoção quando expandir a clientes UE

## AI observability

- Prompt logging: **sim, sem PII** (pseudonimizada antes de prompt)
- Tracing: integrado ao OpenTelemetry stack do projeto (spans para chamada LLM + embedding)
- Métricas: latência, custo, taxa de fallback, hit rate de cache
- Feedback loop: feedback explícito ("essa sugestão é boa/ruim") + implícito (assessora abriu, contatou, fechou) → dataset de retraining

## RAG (knowledge base interna — futuro)

Caso pertinente para a feature:
- **Vector store**: pgvector (mesmo Postgres da app — schema `ai_ml`)
- **Embedding model**: `text-embedding-3-small` (1536 dim) ou `voyage-3` (1024 dim) — escolha em ADR
- **Chunking**: por seção lógica (perfil de influenciador como 1 doc; bio + handles + nicho + tags)
- **Reindexing**: incremental on update de cadastro; full refresh semanal

## Próximos passos quando ativar
1. PRD detalhado em `docs/product/<n>-match-marca-influencer.md`
2. ADR de escolha de modelo (com benchmarks)
3. ADR de arquitetura (online vs batch)
4. Eval suite inicial com 50+ exemplos validados
5. Feature flag `FEATURE_AI_MATCH_ENABLED` (já no `.env.example`)
6. Rollout: 5 assessoras pilotos → 25% → 100%
