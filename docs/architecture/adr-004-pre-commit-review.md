# ADR-004: Pre-commit Code Review Hook

## Status
Accepted — herdado do template `claude-proj-blueprint` e adaptado ao HUB Feat Creator.

## Context

Confiar somente em review on-demand (skill, agent, request manual) deixa lacunas. Histórico documentado no template original mostra bugs reais que checks automatizados teriam capturado:

1. **Bug de cálculo de preço**: cálculo de cupom errado porque cupons têm cap oculto. Usuários viram preços enganosos.
2. **Bug de transparência Pix**: bot postou preço Pix como preço principal, fazendo desconto parecer maior do que era.
3. **Bug de redirect de afiliado**: `fetch()` seguiu redirect para login, quebrando links para usuários finais.

Os 3 seriam capturados por grep checks pre-commit simples.

No HUB Feat Creator, a probabilidade equivalente é alta:
- **Multi-tenant strict**: query sem filtro de `assessoria_id` é catastrófica — vazamento entre assessorias = P0
- **Soft-delete**: query sem filtro `deleted_at IS NULL` mostra dados deletados
- **PII em log**: e-mail/telefone em log raw → incidente LGPD
- **JWT_SECRET em código**: vazamento credencial de auth
- **`@Transactional` ausente** em método que faz múltiplas writes em estado inconsistente
- **Commit sem migration** quando entidade `@Entity` mudou

## Decision

Adotar `scripts/pre-commit-review.sh` herdado do blueprint, registrado em `.claude/hooks.json` como `PreToolUse` em `Bash(git commit*)`.

### Configuração para HUB Feat Creator

- **Stack**: monorepo com Java + TypeScript — script roda checks contextuais por extensão
- **Compile/test commands**:
  - Java: `./mvnw -B verify -pl apps/api -am -q` (apenas se `apps/api/**` foi alterado)
  - TS: `pnpm -C apps/web lint && pnpm -C apps/web test --run` (apenas se `apps/web/**` foi alterado)
- **Review level**: **hybrid** (bash + Claude Sonnet review do diff via `claude --print`)

### Checks específicos do projeto a adicionar

Em `[SPEC]` sections do `pre-commit-review.sh`:

```bash
# 1. Multi-tenant filter ausente (Java)
TENANT_LEAK=$(grep -nE 'JdbcTemplate|EntityManager|@Query' "$f" | grep -v 'assessoria_id' || true)

# 2. Logger com PII raw
PII_LOG=$(grep -nE 'log\.(info|debug|warn).*\b(email|telefone|cpf|senha)\b' "$f" || true)

# 3. JWT_SECRET ou similar em código
SECRET_HARDCODE=$(grep -nE '(JWT_SECRET|API_KEY|PASSWORD)\s*=\s*"[A-Za-z0-9]{8,}"' "$f" || true)

# 4. Entity modificado sem migration
if [ "$ENTITY_CHANGED" = "1" ] && [ "$MIGRATION_ADDED" = "0" ]; then
  echo "❌ MUST FIX: @Entity modificada sem nova V*.sql em apps/api/src/main/resources/db/migration"
fi

# 5. console.log / print esquecido
DEBUG_LOG=$(grep -nE 'console\.(log|debug|info)|System\.out\.println' "$f" || true)

# 6. any TS / @SuppressWarnings Java em código novo
ANY_TS=$(grep -nE ': any\b|as any\b' "$f" || true)
SUPPRESS=$(grep -nE '@SuppressWarnings' "$f" || true)
```

Severidade: 1-4 = **MUST FIX** (bloqueia); 5-6 = **SHOULD FIX** (warning).

## Alternatives considered

1. **Git native pre-commit** (`.git/hooks/pre-commit`)
   - **+** funciona fora do Claude Code
   - **−** não integra com hooks do Claude Code, não versionado no repo
   - Manter ambos seria útil em futuro com mais devs (CI complementar) — registrar em ADR separado se for adotado

2. **Só CI (GitHub Actions)**
   - **+** nunca esquecido
   - **−** feedback lento (push → wait → fail → fix → push)
   - **−** commits ruins entram no histórico antes de serem barrados
   - Decisão: CI mantém os mesmos checks (defesa em profundidade)

3. **Só agent review** (invocar antes de commit)
   - **+** análise profunda
   - **−** requer invocação explícita — esquecível
   - **−** lento para mudanças pequenas
   - Mantemos `@quality-guardian` para mudanças grandes em `/spec-review`

4. **Husky + lint-staged**
   - **+** ecosistema maduro
   - **−** só Node — não cobre Java do api
   - Decisão: não — usar pre-commit-review.sh que cobre ambos

## Consequences

**Positivas**:
- Toda commit revisado automaticamente — sem exceção
- Checks específicos do projeto encodam lições antes de produção
- Feedback rápido (segundos)
- Stack-agnostic (cobre api Java + web TS no mesmo script)
- Hybrid review (Sonnet) pega bugs de lógica que regex não pega — barato (~$0.01/commit)

**Negativas**:
- Adiciona ~10-20s ao commit (compile + test parcial + AI review)
- Grep checks têm falsos positivos — refinar conforme uso
- Custo AI review acumula em equipes maiores; reavaliar `--review-level` se passar de $5/mês

**Riscos**:
- Checks excessivos podem gerar fadiga e cultura de "comentar para passar" — mitigar mantendo só checks que pegaram bug real
- Solo dev ignorando próprio warning sob pressão — mitigar registrando ignorados em log para revisão semanal

## Impact on specs
- **security**: pega secrets hardcoded antes de entrar no histórico Git; pega PII em log
- **testing**: força existência de teste para arquivo modificado (hint, não bloqueio)
- **observability**: pega `console.log` / `System.out.println` que deveriam ser logger estruturado
- **data-architecture**: pega entity modificada sem migration
- **multi-tenancy** (security): pega query sem filtro de tenant

## References
- Spec: `docs/specs/code-review-gates.md`
- Hook: `.claude/hooks.json` → `PreToolUse` → `Bash(git commit*)`
- Script: `scripts/pre-commit-review.sh`
- Origem: `claude-proj-blueprint` + bugs do projeto `amaia-agent` (template original)
- Adaptação para HUB Feat Creator: 2026-04-29
