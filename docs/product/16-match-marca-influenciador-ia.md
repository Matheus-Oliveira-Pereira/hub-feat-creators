# PRD-016: Match Marca ↔ Influenciador (IA)

## Context

Decisão de match marca/creator é manual e enviesada por memória da assessora. Com `social_snapshots` (PRD-015) + histórico de prospecções (PRD-002, PRD-010), há sinal estatístico suficiente para sugerir creators relevantes para uma marca/briefing — reduzindo ciclo de prospecção e melhorando hit rate.
Vision: Fase 3 — núcleo IA.
Depende de: PRDs 01, 02, 10, 15.

## Objective

Calcular score de match entre marca/briefing e creator usando features de nicho, audiência, performance histórica e similaridade vetorial; expor sugestões ranqueadas com explicação ("por que esse?") e feedback loop para melhorar o modelo.

## Scope

### Includes
- [ ] **Briefing estruturado**: campo na marca/prospecção com `vertical`, `objetivo` (awareness, conversão), `audiencia_alvo` (idade, gênero, regiões), `formato` (reels, vídeo, post), `budget_range`, `texto_livre`
- [ ] **Feature engineering**: extrair de `social_snapshots` + dados cadastrais — vertical inferido, audience proxy (engagement, geographic mix se disponível), histórico ganho/perdido com marcas similares
- [ ] **Embeddings**: caption posts + briefings → vector via modelo open (sentence-transformers PT-BR) ou API gerenciada
- [ ] **Vector store**: pgvector no Postgres
- [ ] **Score híbrido**: `score = w1*similaridade_vetor + w2*aderencia_categorica + w3*historico_assessoria + w4*saude_canal`
- [ ] **Explicação**: para cada sugestão, lista top-3 razões textuais ("Vertical fitness alinhado", "Audiência 25-34 majoritária", "Engajamento 4.2% acima da média do nicho")
- [ ] **Feedback loop**: assessora marca sugestão como "boa", "ok", "ruim"; alimenta peso histórico
- [ ] **UI**: tela "Sugestões para esta prospecção" com lista ranqueada, filtros, drill-down
- [ ] **Bulk match**: dado um briefing, retorna top N creators do banco (ASSESSORIA escope) — `/api/v1/match?prospeccao_id=`
- [ ] **Reverse match**: dado um creator, retorna marcas em prospecção que combinam — útil em pipeline

### Excludes
- [ ] Modelo proprietário fine-tuned — Fase 4 (precisa volume de dados)
- [ ] Predição de ROI / preço — Fase 4
- [ ] Geração de copy de pitch (LLM) — PRD futuro
- [ ] Match cross-tenant ("creators de outra assessoria também combinam") — proibido por LGPD/contrato
- [ ] Auto-criação de prospecção — humano sempre no loop

## Not Doing

- **Black-box ranking** — explicação obrigatória (auditoria + confiança).
- **Modelo treinado in-house** — começar com heurística + embeddings prontos; avançar quando tiver dados.

## User Stories

- Como assessora, quero ver top 10 creators que combinam com briefing da marca X
- Como assessora, quero saber por que cada creator foi sugerido
- Como assessora, quero ensinar o sistema dizendo o que serviu ou não
- Como OWNER, quero auditar se sugestões respeitam meus filtros (orçamento, vertical)

## Acceptance Criteria

- [ ] **AC-1**: Endpoint `/api/v1/match?prospeccao_id=X&top=10` retorna lista ranqueada com score + razões
- [ ] **AC-2**: Score reproduzível: mesmo input → mesmo output (sem aleatoriedade na inferência)
- [ ] **AC-3**: Vector index (pgvector ivfflat ou hnsw) responde em < 200ms p95 em 100k creators
- [ ] **AC-4**: Embeddings recalculados quando `briefing` ou `caption` muda (job assíncrono)
- [ ] **AC-5**: Feedback registrado: tabela `match_feedback`; usado em recomputação semanal de pesos
- [ ] **AC-6**: Filtros respeitados: budget, regiões, opt-out de creator (creator pode opt-out de aparecer em sugestões)
- [ ] **AC-7**: Cross-tenant: busca só dentro do escope da assessoria
- [ ] **AC-8**: Explicação verossímil: cada razão referencia feature concreta (não alucinação)
- [ ] **AC-9**: Modelo versionado: `match_model_versions(versao, pesos JSONB, ativada_em)`; rollback para versão anterior em 1 comando
- [ ] **AC-NF-1**: Sugestão não usa dado fora do escopo da assessoria (verificável via teste)
- [ ] **AC-NF-2**: Cobertura ≥ 80% em `MatchService`, scorers
- [ ] **AC-NF-3**: Explicabilidade: 100% das sugestões têm ≥ 1 razão

## Technical Decisions

- **Modelo embeddings**: `paraphrase-multilingual-MiniLM-L12-v2` (open, suporta PT-BR) hospedado em worker Python ou via `Ollama`/HuggingFace API; **alternativa**: `voyage-3-lite` ou `text-embedding-3-small` (OpenAI) se acessível
- **Vector store**: pgvector extension; índice HNSW
- **Heurística inicial**: pesos hardcoded (`w1=0.4, w2=0.25, w3=0.2, w4=0.15`); ajuste por feedback
- **Worker IA**: serviço auxiliar `apps/ai-worker` (Python FastAPI) ou job em Java + ONNX runtime
- **Decisão final**: ADR pendente (`adr-014-match-arquitetura`)

### Schema

```sql
CREATE EXTENSION IF NOT EXISTS vector;

briefings (id, assessoria_id, prospeccao_id FK UNIQUE, vertical TEXT, objetivo TEXT, audiencia JSONB,
  formato TEXT, budget_min, budget_max, texto TEXT, embedding vector(384), atualizado_em)

creator_profile_features (id, influenciador_id FK UNIQUE, vertical_inferido TEXT, top_temas TEXT[],
  audience_geo JSONB, audience_demo JSONB, engagement_30d NUMERIC, freq_post_30d NUMERIC,
  embedding vector(384), atualizado_em)

match_sugestoes (id, prospeccao_id FK, influenciador_id FK, score NUMERIC, razoes JSONB,
  modelo_versao TEXT, gerado_em, UNIQUE (prospeccao_id, influenciador_id, modelo_versao))

match_feedback (id, sugestao_id FK, autor_id FK, sinal CHECK ('BOA','OK','RUIM'),
  comentario TEXT, created_at)

match_model_versions (versao PK, pesos JSONB, descricao, ativada_em, desativada_em)

creator_match_optout (influenciador_id PK, motivo, created_at)

CREATE INDEX briefings_emb_hnsw ON briefings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX creator_features_emb_hnsw ON creator_profile_features USING hnsw (embedding vector_cosine_ops);
```

## Impact on Specs

- **Compliance**: feedback do humano é PII se contiver nome; armazenar agregado; LGPD direito explicação (art. 20)
- **Security**: validação input briefing (XSS no texto livre), rate limit em `/match` (custo CPU)
- **Scalability**: vector index requer tuning; reembedding em massa via job
- **Observability**: `match_sugestoes_geradas`, `match_feedback_total{sinal}`, latência inferência
- **API**: `/api/v1/match`, `/api/v1/match/feedback`, `/api/v1/briefings`
- **AI/ML spec**: este PRD ativa o módulo

## Rollout

- **Feature flag**: `feature.match.enabled`
- **Migrations**: V0016_*
- **Modelo seed**: versão `v1.0` com pesos default
- **Backfill**: gerar embeddings para creators existentes via job (≤ 1h em 10k creators)
- **Rollback**: flag off; sugestões previamente geradas ficam mas tela some

## Métricas

- ≥ 30% das prospecções novas usam match em 90d
- Hit rate (sugestão "BOA" ou "OK") ≥ 60% em 30d (baseline para meta)
- Latência p95 < 500ms na lista top 10
- Zero violação de escopo cross-tenant

## Open questions

- [ ] Worker IA em Java (ONNX) ou Python (sentence-transformers)? — Python lib ecosystem maduro; preferir Python isolado
- [ ] Pesos por assessoria (personalização) ou globais? — globais MVP; personalização Fase 4
- [ ] Custo embeddings: self-host vs API gerenciada? — self-host pgvector + sentence-transformers MVP (custo previsível)
