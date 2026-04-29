# Deploy Runbook — HUB Feat Creator

Roteiro completo de deploy para staging e produção.

## Ambientes

| Ambiente | Backend | Frontend | Trigger |
|----------|---------|----------|---------|
| Staging | Railway `api-staging` | Vercel preview promovido | Auto ao merge em `main` |
| Produção | Railway `api-prod` | Vercel produção | Manual via tag `v*.*.*` |

---

## Pré-requisitos (todos os deploys)

- [ ] Branch `main` com CI 100% verde
- [ ] Tests passando (`./mvnw verify` + `pnpm test`)
- [ ] Lint clean (`./mvnw spotless:check` + `pnpm lint`)
- [ ] Sem segredos commitados (verificar `git log -p` ou rodar `gitleaks` localmente)
- [ ] PR aprovado e mergeado (squash + merge)
- [ ] Migrations Flyway revisadas — destrutivas têm ADR
- [ ] Spec review limpa (se houver flag `requires-spec-review`)

---

## Deploy Staging (automático)

Acontece sozinho ao merge em `main`. Sequência:

1. GitHub Actions verifica build
2. Railway detecta push em `main` → build api → deploy `api-staging`
3. Vercel detecta push em `main` → build web → promote preview para `staging.hub-feat-creators.com`
4. Workflow `smoke-tests-staging` roda automaticamente após deploy:
   - `curl https://api-staging.hub-feat-creators.com/api/v1/health` → 200
   - `curl https://api-staging.hub-feat-creators.com/api/v1/health/ready` → 200
   - Login com user de teste → token recebido
   - Criar prospecção mock → 201
5. Notificação Slack `#releases` com resultado

Se smoke test falha → rollback automático Railway (versão anterior) + alerta crítico.

---

## Deploy Produção (manual)

### 1. Preparação (dev local)

```bash
# Garantir main atualizado
git checkout main
git pull origin main

# Verificar que staging passou
# - Acessar https://staging.hub-feat-creators.com
# - Validar fluxo crítico manualmente (login → criar prospecção → enviar e-mail)

# Atualizar CHANGELOG.md (mover [Unreleased] → [vX.Y.Z] - YYYY-MM-DD)
# Atualizar versão no pom.xml e package.json se aplicável
git add CHANGELOG.md apps/api/pom.xml apps/web/package.json
git commit -m "chore(release): vX.Y.Z

Não alterou: apps/api/src, apps/web/app, docs/"
git push origin main
```

### 2. Tag e push

```bash
# Tag assinada (configurar gpg.signingKey no git config se ainda não)
git tag -s vX.Y.Z -m "Release vX.Y.Z"
git push origin vX.Y.Z
```

### 3. Workflow `release-prod` é triggado pela tag

Sequência automatizada após tag:
1. Verifica que tag aponta para commit em `main`
2. Aguarda smoke-tests staging do mesmo commit estarem verdes (até 30 min)
3. Build container api → push para GHCR com tags `vX.Y.Z` e `latest`
4. Deploy Railway `api-prod` (rolling — instâncias substituídas uma a uma)
5. Migrations Flyway rodam no boot da nova versão (< 30s; > 30s requer pattern expand-contract)
6. Vercel promove última prod build para `app.hub-feat-creators.com`
7. Smoke tests em produção (mesmo set do staging, contra URLs prod)
8. Cria GitHub Release com notas extraídas do CHANGELOG
9. Notifica Slack `#releases` + e-mail aos owners (apenas mudanças visíveis ao usuário)

### 4. Validação humana pós-deploy

Em até **15 min após deploy**:

- [ ] Healthcheck OK (`/api/v1/health/ready` → 200)
- [ ] Login com conta real funciona
- [ ] Criar prospecção real funciona
- [ ] Enviar e-mail real funciona (verificar recebimento real)
- [ ] Dashboard Grafana **System Health** sem alertas
- [ ] Error rate em `Errors & Exceptions` dashboard estável (não > baseline + 50%)
- [ ] Latência p95 estável
- [ ] Sem novas exceções não vistas em staging (`Errors & Exceptions` filtrado por traceId pós-deploy)

---

## Rollback

### Quando rollback
- Healthcheck falhando após deploy
- Error rate > 2% sustentado por 5+ min
- Bug crítico afetando login, multi-tenant ou pagamentos (futuro)
- Vazamento de dados detectado → P0 (ver runbook `cross-tenant-leak.md`)

### Como (Railway api)

```bash
# Via CLI
railway rollback --service api-prod

# OU via UI
# Railway Dashboard → api-prod → Deployments → versão anterior → "Redeploy"
```

### Como (Vercel web)

```bash
# UI: Vercel Dashboard → hub-web → Deployments → versão estável anterior → "Promote to Production"
```

### Considerações importantes

- **Migrations Flyway são forward-only**. Se a release nova aplicou migration destrutiva, rollback do app **não reverte schema**:
  - Se nova versão da app **continua compatível** com schema antigo → rollback puro do app funciona
  - Se schema mudou de forma incompatível → criar **migration corretiva nova** (`V<timestamp>__rollback_xyz.sql`) e fazer hotfix com a versão antiga + corretiva
  - **Nunca editar V já aplicada**
- Comunicar usuários afetados se houver perda de dados (LGPD: 72h ANPD se grave)
- Abrir post-mortem em `docs/runbooks/post-mortems/`

---

## Hotfix (urgência em produção)

Quando bug crítico em prod e `main` tem features não-deployáveis ainda:

```bash
# Branch a partir da tag prod atual
git checkout -b hotfix/<descricao> vX.Y.Z

# Implementar fix
# ./mvnw verify e pnpm test
git commit -m "fix: <descricao>"

# Tag patch
git tag -s vX.Y.(Z+1) -m "Hotfix: <descricao>"
git push origin hotfix/<descricao> vX.Y.(Z+1)

# Workflow release-prod dispara
# Após deploy verificado, cherry-pick para main
git checkout main
git cherry-pick <commit-hash-do-fix>
git push origin main
```

---

## Janela de manutenção

- **Não há** no MVP (deploys são zero-downtime via rolling)
- **Migrations destrutivas** que exigem downtime: comunicar 48h antes; janela 03:00-05:00 BRT (BR uso mínimo)
- **Postmortem** obrigatório para qualquer downtime > 5 min

---

## Contatos durante incidentes

| Função | Pessoa | Canal |
|--------|--------|-------|
| Tech Lead / DRI atual | Matheus Oliveira Pereira | ma.pe.oli.1998@gmail.com / Slack `#oncall` |
| LGPD / DPO | Matheus (acumula) | mesmo |
| Provedor api (Railway support) | Railway support | https://railway.app/help |
| Provedor web (Vercel support) | Vercel support | https://vercel.com/support |
| Provedor e-mail (Resend) | Resend support | https://resend.com/help |

---

## Verificações pós-deploy

- [ ] Smoke tests automatizados verdes (workflow GitHub Actions)
- [ ] Healthchecks 200 (`/health` + `/health/ready`)
- [ ] Login funcional (manual — uma conta de teste)
- [ ] Criar prospecção funcional (manual)
- [ ] Envio de e-mail funcional (verificar caixa de entrada de uma conta de teste real)
- [ ] Dashboards Grafana sem alertas vermelhos
- [ ] Error rate < baseline × 1.5 nos primeiros 30 min
- [ ] Latência p95 dentro do SLO
- [ ] Sem error log novo recorrente (filtrar por release tag em `Errors & Exceptions`)
- [ ] Frontend Web Vitals sem degradação (LCP, INP)
- [ ] CHANGELOG.md atualizado com a release
- [ ] GitHub Release criada com link para CHANGELOG
- [ ] Slack `#releases` notificado
- [ ] (Se features visíveis ao usuário) E-mail aos owners de assessoria enviado

---

## Pós-mortem se algo deu errado

Sempre que:
- Rollback foi necessário
- Downtime > 5 min
- Bug com impacto a usuários reais
- Vazamento de dados (qualquer)
- Falha em compliance check (LGPD)

→ abrir post-mortem usando `docs/runbooks/post-mortems/_template-post-mortem.md` em até 48h.
