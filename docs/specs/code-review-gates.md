# Code Review Gates — HUB Feat Creator

## Filosofia

> **Agents e skills são para tarefas on-demand.
> Hooks são para garantias que não podem falhar.**

Um `@security-auditor` é ótimo para review profundo, mas precisa ser invocado.
Um pre-commit hook roda **sempre**, sem ninguém precisar lembrar.

## Como funciona

Todo `git commit` dispara `scripts/pre-commit-review.sh` via Claude Code hooks.
O script roda checks automatizados nos arquivos staged e:

- **Bloqueia** o commit (exit code 2) — para issues **MUST FIX**
- **Avisa** mas permite (exit code 0) — para **SHOULD FIX**
- **Passa limpo** — todos checks verdes

## Review levels

Configurado via `bootstrap.sh --review <level>` ou env `REVIEW_LEVEL`. **HUB Feat Creator usa `hybrid`**.

| Level | O que roda | Tempo | Custo | Quando usar |
|-------|-----------|-------|-------|-------------|
| **simple** | Só bash (grep, compile, test parcial) | ~10-20s | Free | Equipes que pagam por API |
| **hybrid** | Bash + Sonnet AI review | ~25-40s | ~$0.01/commit | **Default — escolha do projeto** |
| **deep** | Bash + Opus AI review | ~50-90s | ~$0.05/commit | Antes de release de produção |

### Como AI review funciona (hybrid/deep)

1. Bash checks rodam primeiro (deterministas, rápidos)
2. Se nenhum MUST FIX bloqueia, AI review roda no diff staged
3. AI analisa: bugs de lógica, edge cases, riscos de segurança, business logic
4. AI adiciona **warnings only** — nunca bloqueia
5. Bash bloqueia; AI dá inteligência

### Pré-requisitos
- `claude` CLI instalado (usa plano Max/Pro — sem API key) **OU**
- `ANTHROPIC_API_KEY` em `.env` ou `~/.env` (fallback API)
- `scripts/ai-review.sh` presente (já está no projeto)

## Categorias de check

### Universais (built-in)

| # | Check | Severidade | Captura |
|---|-------|-----------|---------|
| 1 | Compilação / type check | MUST FIX | Build quebrado |
| 2 | Tests passing (unit dos arquivos modificados) | MUST FIX | Regressão |
| 3 | Hardcoded secrets (regex AWS, JWT, etc) | MUST FIX | Vazamento credencial |
| 4 | Quality (`console.log`, `any`, `@SuppressWarnings`) | SHOULD FIX | Debug artifacts, weak typing |
| 5 | Error handling (`fetch` sem `.catch`, `try/catch`) | SHOULD FIX | Exceções não tratadas |
| 6 | Test coverage gaps | CONSIDER | Arquivo sem teste correspondente |

### Específicos do HUB Feat Creator

> Estes capturam **bugs do nosso domínio**. Adicionar quando aprender com bug em produção.

| # | Check | Severidade | Captura |
|---|-------|-----------|---------|
| P1 | Query Java sem filtro `assessoria_id` (multi-tenant leak) | MUST FIX | Vazamento entre assessorias = P0 |
| P2 | Query sem filtro `deleted_at IS NULL` em soft-deletables | SHOULD FIX | Mostra registros deletados |
| P3 | Logger com PII raw (`email`, `telefone`, `cpf`, `senha` em string de log) | MUST FIX | Incidente LGPD |
| P4 | `@Entity` modificada sem nova `V*.sql` em `db/migration/` | MUST FIX | Schema desync prod ↔ código |
| P5 | Endpoint criado sem teste de cross-tenant | SHOULD FIX | Multi-tenant não validado |
| P6 | `String.format` em SQL nativo (SQL injection) | MUST FIX | OWASP A03 |
| P7 | Idempotency-Key ausente em endpoint de envio (e-mail) | SHOULD FIX | Retry duplica |
| P8 | `@Transactional` em método cross-aggregate sem rollback explícito | CONSIDER | Estado parcial em falha |
| P9 | `console.log` / `System.out.println` em código novo | SHOULD FIX | Logger estruturado é regra |
| P10 | Senha em string de teste (`"123456"` etc) com `Argon2` na função | CONSIDER | Senha fraca em fixture é OK; senha real em fixture é bug |

> Cada check específico nasce de incidente real ou risco identificado em ADR/PRD. **Não adicionar speculative checks** — gera fadiga.

## Configuração

### Stack — topo do `scripts/pre-commit-review.sh`

```bash
# Java (apps/api)
JAVA_FILES="apps/api/src/main/java"
JAVA_TEST_FILES="apps/api/src/test/java"
JAVA_COMPILE_CMD="./mvnw -B compile -pl apps/api -q"
JAVA_TEST_CMD="./mvnw -B test -pl apps/api -q -Dtest='*Test'"

# TypeScript (apps/web)
TS_FILES="apps/web"
TS_LINT_CMD="pnpm -C apps/web lint"
TS_COMPILE_CMD="pnpm -C apps/web tsc --noEmit"
TS_TEST_CMD="pnpm -C apps/web test --run"
```

## Integração com outras camadas

| Camada | Tool | Propósito |
|--------|------|-----------|
| L2 | `code-review` skill | Review profundo on-demand (manual) |
| L3 | `pre-commit-review.sh` hook | Gate automático em todo commit |
| L3 | `lint-check.sh` hook | Lint em todo write de arquivo |
| L3 | `security-check.sh` hook | Bloqueio de bash perigoso |
| L3 | `docs-check.sh` hook | Aviso se `apps/*` mudou sem `docs/*` |
| L4 | `quality-guardian` agent | Review de qualidade profundo (sonnet) em mudanças grandes |
| L4 | `security-auditor` agent | Audit OWASP em `/spec-review` (opus) |

Pre-commit dá **cobertura rápida e automática**.
Agents dão **análise profunda em mudanças significativas**.

## Adicionando check novo

Quando bug aparecer em produção, perguntar: "**Pre-commit teria pego?**"

Se sim:

```bash
# [YYYY-MM-DD] Bug: <descricao curta>
# Causa raiz: <o que aconteceu>
# Check: <padrão a procurar>
for f in $STAGED_JAVA_FILES; do
  MATCH=$(grep -nE '<padrão>' "$f" || true)
  if [ -n "$MATCH" ]; then
    echo "❌ MUST FIX [$f]: <descricao>"
    MUST_FIX=$((MUST_FIX + 1))
  fi
done
```

Incidentes viram guardrails permanentes.

## Origem

Spec original do template `claude-proj-blueprint`, validada em `amaia-agent`.
Adaptada para HUB Feat Creator com checks específicos de multi-tenancy, LGPD e schema/migration sync.
ADR: `docs/architecture/adr-004-pre-commit-review.md`.
