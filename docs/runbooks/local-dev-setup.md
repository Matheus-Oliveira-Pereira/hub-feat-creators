# Rodando o projeto localmente

Guia completo para subir **API + Web + (opcional) Mobile + (opcional) AI Worker** em ambiente de desenvolvimento.

---

## Pré-requisitos

| Ferramenta | Versão mínima | Verificar |
|---|---|---|
| Java (JDK) | 21 LTS | `java -version` |
| Maven | wrapper incluso | `./mvnw -v` |
| Node.js | 20 LTS | `node -v` |
| pnpm | 9+ | `pnpm -v` |
| PostgreSQL | 16 | `psql --version` |
| Docker | qualquer recente | `docker -v` *(só p/ testes de integração e AI Worker)* |
| Python | 3.12 | `python --version` *(só p/ AI Worker)* |

> **Windows**: use PowerShell ou Git Bash. `./mvnw` funciona nos dois.

---

## 1. Clone e estrutura

```bash
git clone https://github.com/Matheus-Oliveira-Pereira/hub-feat-creators.git
cd hub-feat-creators
```

Estrutura relevante:

```
apps/
  api/        → Spring Boot (porta 8080)
  web/        → Next.js  (porta 3000)
  mobile/     → Expo     (porta 8081 / Metro)
  ai-worker/  → FastAPI  (porta 8000) — opcional
```

---

## 2. Banco de dados (PostgreSQL)

### 2.1 Criar banco e usuários

```sql
-- conectar como superuser (ex: psql -U postgres)
CREATE USER hub_app      WITH PASSWORD 'changeme';
CREATE USER hub_migrator WITH PASSWORD 'changeme';
CREATE DATABASE hub_feat_creators
    OWNER hub_app
    ENCODING 'UTF8'
    LC_COLLATE 'pt_BR.UTF-8'
    LC_CTYPE   'pt_BR.UTF-8'
    TEMPLATE template0;

\c hub_feat_creators
GRANT ALL PRIVILEGES ON DATABASE hub_feat_creators TO hub_migrator;
```

> **Windows / sem locale pt_BR**: se `pt_BR.UTF-8` falhar, use `en_US.UTF-8`.
> A collation afeta ordenação de nomes acentuados, mas não quebra o sistema.

### 2.2 Extensões (pgvector)

```sql
-- ainda conectado ao hub_feat_creators como superuser
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS citext;
```

> **pgvector não instalado?**
> - Linux/Mac: `brew install pgvector` ou pacote do distro
> - Windows: baixe o binário em https://github.com/pgvector/pgvector e siga o README
> - Alternativa: suba o Postgres via Docker (veja seção 2.3)

### 2.3 Alternativa: Postgres via Docker (mais fácil)

```bash
docker run -d \
  --name hub-postgres \
  -e POSTGRES_USER=hub_app \
  -e POSTGRES_PASSWORD=changeme \
  -e POSTGRES_DB=hub_feat_creators \
  -p 5432:5432 \
  pgvector/pgvector:pg16
```

Depois conecte e rode as extensões:

```bash
docker exec -it hub-postgres psql -U hub_app -d hub_feat_creators \
  -c "CREATE EXTENSION IF NOT EXISTS vector; CREATE EXTENSION IF NOT EXISTS \"uuid-ossp\"; CREATE EXTENSION IF NOT EXISTS citext;"
```

---

## 3. Variáveis de ambiente

### 3.1 API (`apps/api`)

Copie o exemplo na raiz do monorepo:

```bash
cp .env.example .env
```

**Valores mínimos para rodar localmente** (o resto pode ficar como default):

```dotenv
DATABASE_URL=jdbc:postgresql://localhost:5432/hub_feat_creators
DATABASE_USERNAME=hub_app
DATABASE_PASSWORD=changeme

JWT_SECRET=dev-local-jwt-secret-minimo-32-chars!!

# Deixe os dev-only defaults — OK para dev local
EMAIL_KEY=dev-only-not-for-prod-32-byte-aes-key
WHATSAPP_KEY=dev-only-not-for-prod-whatsapp-32b!
SOCIAL_KEY=dev-only-not-for-prod-social-32b!!!
MFA_KEY=dev-only-not-for-prod-mfa-key-32bytes
```

> Os valores `dev-only-not-for-prod-*` são aceitos pela validação de startup
> **apenas fora de produção**. Em prod o `StartupSecretValidator` bloqueia o boot.

O Spring Boot lê as variáveis via `application.yml` com fallback `${VAR:default}`.
Em dev você pode exportá-las no shell ou usar um arquivo `.env` na pasta `apps/api/`
e um plugin como [dotenv-maven-plugin](https://github.com/nicoulaj/dotenv-maven-plugin)
— ou simplesmente exportar no terminal antes de rodar:

```powershell
# PowerShell
$env:DATABASE_URL = "jdbc:postgresql://localhost:5432/hub_feat_creators"
$env:DATABASE_USERNAME = "hub_app"
$env:DATABASE_PASSWORD = "changeme"
$env:JWT_SECRET = "dev-local-jwt-secret-minimo-32-chars!!"
```

```bash
# Bash / Git Bash
export DATABASE_URL="jdbc:postgresql://localhost:5432/hub_feat_creators"
export DATABASE_USERNAME="hub_app"
export DATABASE_PASSWORD="changeme"
export JWT_SECRET="dev-local-jwt-secret-minimo-32-chars!!"
```

### 3.2 Web (`apps/web`)

```bash
cp apps/web/.env.example apps/web/.env.local
```

Conteúdo padrão já funciona para dev:

```dotenv
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### 3.3 Mobile (`apps/mobile`)

```bash
cp apps/mobile/.env.example apps/mobile/.env.local
```

```dotenv
EXPO_PUBLIC_API_URL=http://localhost:8080
```

---

## 4. Subir a API

```bash
cd apps/api
./../../mvnw spring-boot:run
```

Na primeira execução o **Flyway** roda as 18 migrations automaticamente
(`V1__init.sql` → `V18__hardening_leva2_indexes.sql`). Deve aparecer:

```
Flyway Community Edition … successfully applied 18 migrations
…
Started ApiApplication in X.XXX seconds
```

Verificar: `curl http://localhost:8080/actuator/health` → `{"status":"UP"}`

### Flags de feature (dev)

Por padrão os módulos opcionais estão **desativados**:

| Flag | Default | Ativar |
|---|---|---|
| `FEATURE_PORTAL_ENABLED` | `false` | `true` |
| `FEATURE_WHATSAPP_ENABLED` | `false` | `true` (+ credenciais Meta) |
| `FEATURE_SOCIAL_ENABLED` | `false` | `true` (+ credenciais OAuth) |
| `FEATURE_AI_MATCH_ENABLED` | `false` | `true` (+ AI Worker rodando) |
| `FEATURE_MOBILE_ENABLED` | `false` | `true` |
| `FEATURE_COMPLIANCE_STRICT` | `true` | — |
| `FEATURE_SIGNUP_ENABLED` | `true` | — |

---

## 5. Criar primeiro usuário (owner)

O signup está habilitado por default. Acesse `http://localhost:3000/signup`
após subir o web, ou via API diretamente:

```bash
curl -X POST http://localhost:8080/api/v1/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "assessoriaNome": "Minha Assessoria",
    "email": "admin@local.dev",
    "senha": "Senha@1234",
    "nome": "Admin Local"
  }'
```

O e-mail de verificação é logado no console (sem SMTP configurado em dev):

```
INFO  EmailService - verify email=admin@local.dev token=<TOKEN>
```

Copie o token e verifique:

```bash
curl -X POST http://localhost:8080/api/v1/auth/verify-email \
  -H "Content-Type: application/json" \
  -d '{"token": "<TOKEN>"}'
```

Depois faça login:

```bash
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@local.dev", "senha": "Senha@1234"}'
```

Guarde o `accessToken` retornado para chamadas autenticadas.

---

## 6. Subir o Web

```bash
cd apps/web
pnpm install       # primeira vez
pnpm dev
```

Acesse: `http://localhost:3000`

---

## 7. Subir o Mobile (opcional)

Requer Expo Go no celular ou simulador iOS/Android instalado.

```bash
cd apps/mobile
pnpm install       # primeira vez
pnpm start         # Metro bundler
```

- **Android emulator**: pressione `a` no terminal do Metro
- **iOS simulator** (Mac): pressione `i`
- **Dispositivo físico**: escaneie o QR Code com o app Expo Go

> O mobile aponta para `EXPO_PUBLIC_API_URL=http://localhost:8080`.
> Se o dispositivo físico estiver em outra rede, substitua `localhost` pelo IP da máquina.

---

## 8. AI Worker — embeddings para Match IA (opcional)

Necessário apenas se `FEATURE_AI_MATCH_ENABLED=true`. Sem o worker o match
funciona em modo degradado (score sem componente vetorial).

### Via Docker (recomendado — download do modelo ~200 MB):

```bash
cd apps/ai-worker
docker build -t hub-ai-worker .
docker run -d --name hub-ai-worker -p 8000:8000 hub-ai-worker
```

### Via Python direto:

```bash
cd apps/ai-worker
pip install -r requirements.txt
uvicorn main:app --host 0.0.0.0 --port 8000
```

Verificar: `curl http://localhost:8000/health` → `{"status":"ok"}`

Depois ative na API:

```bash
export FEATURE_AI_MATCH_ENABLED=true
export AI_WORKER_URL=http://localhost:8000
# reiniciar a API
```

---

## 9. Rodar os testes

### 9.1 Testes unitários da API (sem Docker)

```bash
cd apps/api
./../../mvnw test -Dexclude="**/*IT.java,ApiApplicationTests.java"
# ou simplesmente (Docker ausente → só o ApiApplicationTests falha, o resto passa)
./../../mvnw test
```

Resultado esperado: **217 testes**, 0 falhas (1 erro `ApiApplicationTests` se Docker ausente — ignorar).

### 9.2 Testes de integração da API (requer Docker)

```bash
cd apps/api
./../../mvnw verify
```

Sobe PostgreSQL via Testcontainers (`pgvector/pgvector:pg16`) automaticamente.

### 9.3 Testes unitários Web (Vitest)

```bash
cd apps/web
pnpm test
# 29 testes, 0 falhas
```

### 9.4 Testes e2e Web (Playwright)

Requer o web rodando (`pnpm dev` em outro terminal):

```bash
cd apps/web
pnpm test:e2e
```

O Playwright sobe o Next.js automaticamente se não estiver rodando (`webServer` no config).
As chamadas de API são interceptadas (mock via `page.route()`), sem necessidade de backend real.

### 9.5 Lint + typecheck

```bash
cd apps/api  && ./../../mvnw spotless:check      # Java
cd apps/web  && pnpm lint && pnpm typecheck      # TypeScript / ESLint
cd apps/mobile && pnpm lint                      # Expo
```

---

## 10. Fluxo mínimo para validar o produto

1. **Signup** → verificar e-mail (token no log) → login
2. **Criar influenciador**: `POST /api/v1/influenciadores` ou pelo web em `/influenciadores?new=1`
3. **Criar marca**: `/marcas?new=1`
4. **Criar prospecção**: kanban em `/prospeccao` → mover status NOVA → CONTATADA → NEGOCIANDO → FECHADA_GANHA
5. **Criar tarefa**: `/tarefas?new=1` → marcar como FEITA
6. **Importação bulk**: `/importacao` → fazer upload de CSV (template em `/importacao?template=influenciadores`)
7. **Relatório**: `/relatorios` → funil de prospecção

---

## 11. Portas em uso

| Serviço | Porta |
|---|---|
| API Spring Boot | 8080 |
| Web Next.js | 3000 |
| Mobile Expo Metro | 8081 |
| AI Worker FastAPI | 8000 |
| PostgreSQL | 5432 |

---

## 12. Problemas comuns

### `Flyway migration failed: relation already exists`
O banco tem resquícios de uma migration anterior. Opção mais segura:
```sql
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
-- depois reiniciar a API
```

### `StartupSecretValidator: key too short`
Variável `EMAIL_KEY` / `WHATSAPP_KEY` / `SOCIAL_KEY` / `MFA_KEY` com menos de 32 bytes.
Use os valores `dev-only-not-for-prod-*` do `.env.example` em dev.

### `pgvector extension not found`
A migration `V17__ia_match_vectors.sql` usa `CREATE EXTENSION vector`.
Instale pgvector (seção 2.2) ou use a imagem Docker `pgvector/pgvector:pg16`.

### `Port 8080 already in use`
```bash
# Linux/Mac
lsof -ti:8080 | xargs kill
# Windows PowerShell
Stop-Process -Id (Get-NetTCPConnection -LocalPort 8080).OwningProcess -Force
```

### `CORS error no navegador`
Confirme que `CORS_ALLOWED_ORIGINS=http://localhost:3000` está definido na API.

### `Email de verificação não chega`
Em dev sem SMTP configurado, o token é logado no console da API:
```
grep "verify email" <log>
```
