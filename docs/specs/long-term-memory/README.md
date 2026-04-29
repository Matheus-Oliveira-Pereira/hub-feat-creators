# Module: Long-Term Memory — HUB Feat Creator

Memória vetorial persistente para agentes Claude Code (L4). Permite ao sistema "lembrar" decisões, soluções e contexto de meses atrás.

## Por quê

`CLAUDE.md` dura 1 sessão. Obsidian/`docs/` dura para sempre, mas precisa ser lido. Vector DB resolve o meio: busca semântica instantânea sobre todo histórico do projeto.

```
"Como resolvemos o rate limit antes?"
→ Vector DB encontra: ADR-007, PR #42, post-mortem de março
→ Agent recebe contexto preciso sem ler 200 arquivos
```

## Arquitetura — 4 camadas

| Camada | Storage | Duração | O que armazena | Busca |
|--------|---------|---------|---------------|-------|
| Curto prazo | `CLAUDE.md` | sessão | stack, convenções, gotchas | leitura completa |
| Médio prazo | `docs/` (Obsidian + Git) | permanente | PRDs, ADRs, specs, runbooks | grep, wiki-links |
| Longo prazo | Vector DB (projeto) | permanente | embeddings de docs/código | semântica |
| Cross-projeto | Vector DB global | permanente | ADRs, post-mortems, learner reports de **todos** projetos do dev | semântica (`--global`) |

## Stack escolhido

### MVP — Chroma local
- **Razão**: zero infra, setup imediato, suficiente para 1 dev
- **Embedding**: `all-MiniLM-L6-v2` (sentence-transformers, local, 384 dim, free)
- **Storage**: `memory/.chroma/` (gitignored — cada dev indexa local)
- **Quando trocar**: time ≥ 2 devs OU índice > 100k chunks

### Quando crescer — pgvector compartilhado
- **Onde**: extensão do mesmo Postgres da app (schema separado `memory`) ou DB dedicado em Railway
- **Embedding**: mesmo local OU upgrade para cloud (Voyage `voyage-3` ou OpenAI `text-embedding-3-small`)
- **Vantagem**: 1 indexação, todos buscam — não duplica trabalho

Setup pgvector documentado no template original (incluído abaixo na seção **Setup pgvector** quando ativarmos).

## O que indexar

| Source | Tipo | Chunking | Frequência |
|--------|------|----------|------------|
| `docs/architecture/` | ADRs | 1 ADR = 1 chunk | post-commit |
| `docs/product/` | PRDs | seções (`##`) | post-commit |
| `docs/runbooks/post-mortems/` | post-mortems | 1 = 1 chunk | post-commit |
| `docs/specs/` | specs modulares | seções (`##`) | post-commit |
| `apps/api/src/` | código Java | 1 arquivo = 1 chunk | post-commit |
| `apps/web/` | código TS/TSX | 1 arquivo = 1 chunk | post-commit |
| Git log | commits | 1 mensagem = 1 chunk | post-commit |

Configurado em `memory/config.yaml` — sources já apontados para `apps/api/src/` (Java) e `apps/web/` (TS/TSX).

### Não indexar
- `node_modules/`, `target/`, `.next/`, `dist/`
- `.env*`, segredos
- Dados de teste com PII real
- Migrations (são SQL — pouco valor semântico vs ADR que explica o porquê)

## Cross-project memory (global)

Decisões em um projeto frequentemente se aplicam a outros. Camada global mora em `~/.claude/memory/global/` e armazena:
- ADRs (decisões arquiteturais transferem entre projetos)
- Post-mortems (lições aprendidas universais)
- Learner reports (padrões extraídos)

**Habilitado** neste projeto (`memory/config.yaml`: `global_memory.enabled: true`).

## Metadata por chunk

```json
{
  "source": "docs/architecture/adr-007-rate-limiting.md",
  "type": "adr",
  "title": "ADR-007: Rate Limiting via Redis",
  "last_modified": "2026-04-15",
  "tags": ["rate-limiting", "redis", "api"],
  "chunk_index": 0
}
```

## Uso

### Index inicial
```bash
pip install -r memory/requirements.txt
python memory/index.py
```

### Incremental (pós-commit)
```bash
python memory/index.py --incremental
```
Configurado como hook `post-commit` em `scripts/post-commit-index.sh`.

### Search
```bash
# Projeto
python memory/query.py "como tratamos rate limit"

# Global (todos projetos)
python memory/query.py "rate limit" --global

# Merged
python memory/query.py "rate limit" --both --agent-format

# Filtrado por tipo
python memory/query.py "deploy falha" --type post_mortem

# Top-N
python memory/query.py "auth" --top 10
```

### Stats
```bash
python memory/query.py --stats
```

### Via Claude
- Skill `memory` é auto-invocada quando agent precisa contexto histórico
- `/memory search "..."` slash command
- Lead agent consulta antes de planning em features grandes

## Quando o sistema deve consultar memória
- Antes de propor decisão arquitetural (existe ADR similar?)
- Antes de implementar feature (existe PR/decisão relacionada?)
- Quando vê erro recorrente (existe post-mortem?)
- Em pergunta histórica do usuário ("como fizemos X?")
- Antes de design de API (convenções já estabelecidas?)

## Limitações conhecidas
- Embedding local é mais fraco que cloud (Voyage/OpenAI) em queries técnicas longas — aceitar trade-off no MVP
- Indexação de código Java não distingue inner classes de top-level — busca pode trazer arquivo grande quando só uma classe importa
- Git log truncado em 200 commits (`memory/config.yaml`) — suficiente; full log demora indexar

## Manutenção
- Reindex full quando trocar embedding model
- Reindex full quando estrutura de chunks mudar
- `memory/.chroma/` regenerável — pode deletar e reindexar sem perda real

## Referências
- `memory/config.yaml` — toda configuração
- `memory/index.py`, `memory/query.py` — código
- README do template original com setup detalhado de pgvector
