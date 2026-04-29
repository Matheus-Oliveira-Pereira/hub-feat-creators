Run the HUB Feat Creator deploy checklist.

Workflow:

1. **Backend tests**: `./mvnw -B verify -pl apps/api`
2. **Frontend tests**: `pnpm -C apps/web lint && pnpm -C apps/web tsc --noEmit && pnpm -C apps/web test --run && pnpm -C apps/web build`
3. **Secret scan**:
   ```
   grep -rE "(API_KEY|SECRET|PASSWORD|TOKEN)\s*=\s*['\"][A-Za-z0-9_-]{8,}" \
     --include="*.java" --include="*.ts" --include="*.tsx" --include="*.yml" --include="*.yaml" \
     apps/ scripts/ .github/
   ```
4. **Pending migrations**: `ls apps/api/src/main/resources/db/migration/V*.sql | tail -10` — verificar se as últimas migrations estão presentes na branch e foram revisadas
5. **Multi-tenant filter check** (críticos): `grep -rnE 'JdbcTemplate|@Query' apps/api/src/main/java | grep -v assessoria_id` — toda query tem filtro de tenant?
6. **Changes since last tag**: `git log $(git describe --tags --abbrev=0)..HEAD --oneline`
7. **CHANGELOG**: ler `CHANGELOG.md` — entry da release atual está completa?
8. **Generate release notes**: agregar mudanças por tipo (Added, Changed, Fixed, Security, Deprecated)
9. **Smoke test plan**: listar 3-5 fluxos a validar após deploy:
   - Login com conta de teste real
   - Criar prospecção
   - Enviar e-mail de prospecção
   - Listar influenciadores (cross-tenant validation)
   - Healthchecks `/health` e `/health/ready`

Se qualquer passo falhar, parar e reportar.
Detalhes do procedimento de deploy, rollback e contatos: `docs/runbooks/deploy.md`.
