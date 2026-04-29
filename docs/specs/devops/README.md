# Module: DevOps — HUB Feat Creator

CI/CD, IaC, ambientes, operação.

## CI/CD

- **Plataforma**: **GitHub Actions** (workflows em `.github/workflows/`)
- **CD backend**: Railway (auto-deploy via GitHub integration)
- **CD frontend**: Vercel (auto-deploy via GitHub integration)

### Pipeline

```
Push (qualquer branch)
  → Lint (api + web)
  → Unit tests (api + web)
  → Build (api: jar; web: next build)

Pull Request → main
  + Integration tests (Testcontainers)
  + Security scan (OWASP DC + Dependabot + secret scan)
  + Coverage check (≥ 70% delta)
  + E2E (Playwright contra build da PR)

Merge → main
  → Deploy automático staging (Railway api-staging, Vercel preview promoted)
  → Smoke tests (curl healthcheck + um fluxo crítico)

Tag v*.*.* (manual)
  → Deploy produção (Railway prod, Vercel prod)
  → Post-deploy: notificar Slack, criar release no GitHub
```

### Stages

| Stage | Trigger | Obrigatório | Timeout |
|-------|---------|-------------|---------|
| Lint api (Spotless+Checkstyle) | Push | Sim | 5 min |
| Lint web (ESLint+Prettier+tsc --noEmit) | Push | Sim | 5 min |
| Unit api (JUnit) | Push | Sim | 10 min |
| Unit web (Vitest) | Push | Sim | 5 min |
| Build api (`mvn package`) | PR | Sim | 10 min |
| Build web (`pnpm build`) | PR | Sim | 8 min |
| Integration api (Testcontainers + Postgres) | PR | Sim | 15 min |
| Security scan (OWASP DC + Trivy) | PR | Sim | 10 min |
| E2E (Playwright) | PR | Sim | 20 min |
| Deploy staging | Merge main | Auto | 10 min |
| Smoke tests staging | Pós-deploy | Sim | 5 min |
| Deploy prod | Tag manual | Manual | 10 min |

### Caching CI
- Maven: cache `~/.m2` por hash de `pom.xml`
- pnpm: cache `~/.pnpm-store` por hash de `pnpm-lock.yaml`
- Docker layers: GHA cache `gha`

## Ambientes

| Ambiente | URL frontend | URL backend | Deploy | Dados |
|----------|-------------|-------------|--------|-------|
| Local | `http://localhost:3000` | `http://localhost:8080` | manual | fixtures + Testcontainers |
| Preview (PR) | `<pr>-hub.vercel.app` | staging api | auto (Vercel preview) | staging anonimizado |
| Staging | `https://staging.hub-feat-creators.com` | `https://api-staging.hub-feat-creators.com` | auto (merge main) | anonimizado de prod |
| Produção | `https://app.hub-feat-creators.com` | `https://api.hub-feat-creators.com` | manual (tag git) | reais |

Anonimização: dump diário de prod → script de mascaramento (e-mails → `usuario+<hash>@example.com`, telefones zerados, hashes de senha trocados por hash conhecido) → restore em staging.

## Infraestrutura

- **Tool IaC**: nenhum no MVP (Railway+Vercel são PaaS, config via dashboard + `railway.toml` / `vercel.json` no repo)
- **Quando migrar para Terraform**: se sair de Railway/Vercel para AWS/GCP, ou time crescer ≥ 3 devs
- **Local IaC futuro**: `/infra/terraform`
- **State backend**: S3 + DynamoDB lock (quando aplicável)

### Configuração no repo
- `apps/api/railway.toml` → build, start command, healthcheck path
- `apps/web/vercel.json` → build, framework, headers, redirects
- `.github/workflows/*.yml` → CI

## Containers

- **Backend**: Dockerfile multi-stage (build com `eclipse-temurin:21-jdk` → runtime com `eclipse-temurin:21-jre`)
- **Imagem base**: `eclipse-temurin:21-jre` (oficial, mantida)
- **Frontend**: Vercel cuida da build; Docker só se sair de Vercel
- **Registry**: GHCR (GitHub Container Registry) — gratuito e integrado
- **Vulnerability scan**: **Trivy** na CI a cada PR; falha em CVE crítica não corrigida em base image

## Gestão de segredos
- **Vault**: Railway Secrets (api), Vercel Environment Variables (web)
- **Local dev**: `.env` (gitignored), commit apenas `.env.example`
- **Injeção**: env vars (não mounted secrets — Railway/Vercel não suportam nativamente)
- **Rotação**: trimestral para JWT_SECRET; sob demanda para chaves de terceiros
- **Detecção em commit**: `scripts/security-check.sh` (regex de chaves AWS/JWT/etc) + GitHub Push Protection

## Backup e disaster recovery

| Métrica | Target |
|---------|--------|
| **RPO** (perda máxima) | 24h |
| **RTO** (tempo restore) | 4h |
| Estratégia | Railway PITR (7d) + snapshot diário S3 retido 90d |
| Teste de restore | Trimestral em staging |

DR plan documentado em `docs/runbooks/disaster-recovery.md` (criar quando primeiro incidente real ou pré-launch).

## DORA metrics — targets

| Métrica | MVP | 6 meses | 1 ano |
|---------|-----|---------|-------|
| Deployment frequency | 1/semana | 5/semana | diário+ |
| Lead time for changes | < 1 semana | < 2 dias | < 1 dia |
| MTTR | < 4h | < 1h | < 30 min |
| Change failure rate | < 30% | < 15% | < 10% |

Coletadas via:
- GitHub Actions deploy events
- Tag de release
- Incidentes em `docs/runbooks/post-mortems/`

## Healthchecks
- `GET /api/v1/health` → liveness (sempre 200 se app subiu)
- `GET /api/v1/health/ready` → readiness (200 só se DB + Redis acessíveis)
- Frontend: Vercel monitora root automaticamente

## Janela de manutenção
- Não aplicável no MVP (deploys são zero-downtime)
- Migrations destrutivas: comunicar 48h antes em staging; janela noturna BR (3h-5h) se houver downtime real
