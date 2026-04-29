#!/bin/bash
# Hook: lint check after file write — HUB Feat Creator
# Exit 0 = allow, Exit 2 = block

FILE="$1"

case "$FILE" in
  apps/api/**/*.java)
    # Spotless quick check (apenas formatação; estilo completo via mvnw spotless:check)
    if command -v ./apps/api/mvnw >/dev/null 2>&1; then
      (cd apps/api && ./mvnw -q spotless:check -DspotlessFiles="$(realpath --relative-to=apps/api "../../$FILE")" 2>/dev/null)
    fi
    ;;
  apps/web/**/*.ts|apps/web/**/*.tsx|apps/web/**/*.js|apps/web/**/*.jsx)
    if [ -d "apps/web/node_modules" ]; then
      (cd apps/web && pnpm exec eslint --quiet "$(realpath --relative-to=apps/web "../../$FILE")" 2>/dev/null)
    fi
    ;;
  memory/*.py|scripts/*.py)
    if command -v ruff >/dev/null 2>&1; then
      ruff check "$FILE" --quiet 2>/dev/null
    fi
    ;;
  *)
    exit 0
    ;;
esac

if [ $? -ne 0 ]; then
  echo "⚠️  Lint issues em $FILE"
  exit 0  # warn but don't block (mudar para exit 2 quando lint estiver estável)
fi

exit 0
