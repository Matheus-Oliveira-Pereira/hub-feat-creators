# API Conventions — HUB Feat Creator

REST API do backend (`apps/api`). Pareia com skill `api-designer`.

## Style
**REST** (JSON sobre HTTPS).

## Base URL
- Produção: `https://api.hub-feat-creators.com/api/v1`
- Staging: `https://api-staging.hub-feat-creators.com/api/v1`
- Local: `http://localhost:8080/api/v1`

## Autenticação
- **JWT Bearer** no header `Authorization: Bearer <access_token>`
- Refresh via `POST /api/v1/auth/refresh` com refresh token (rotacionado a cada uso)
- Login: `POST /api/v1/auth/login` → `{ accessToken, refreshToken, user }`
- Logout: `POST /api/v1/auth/logout` (revoga família de refresh tokens)
- Detalhes em `docs/specs/security/`

## Versionamento
- **URL prefix** (`/api/v1`, `/api/v2`)
- Suporte simultâneo: máx 2 versões
- Notice de deprecação: 6 meses antes de remoção
- Headers em endpoint deprecado: `Deprecation: true`, `Sunset: <RFC 1123 date>`
- Detalhes em `docs/specs/versioning/`

## Content type
- Request: `application/json; charset=utf-8`
- Response: `application/json; charset=utf-8`
- Upload: `multipart/form-data` (avatar, anexos)

## Paginação — **cursor-based** (default)
Cursor é mais robusto que offset para listas que mudam (prospecções, tarefas).

```http
GET /api/v1/prospeccoes?cursor=eyJpZCI6IjEyMyJ9&limit=20

200 OK
{
  "data": [...],
  "pagination": {
    "cursor": "eyJpZCI6IjE0MyJ9",
    "hasMore": true,
    "limit": 20
  }
}
```

- Cursor é opaco (base64 de `{ id, createdAt }`)
- `limit` default 20, máximo 100
- Listas pequenas e estáveis (ex: roles) podem usar offset — documentar exceção no endpoint

## Filtros e ordenação
```http
GET /api/v1/prospeccoes?status=ATIVA&sort=createdAt&order=desc
```
- Múltiplos filtros: query params separados (`?status=ATIVA&assessor=123`)
- Operadores: por padrão equality; ranges com sufixo (`createdAt_gte`, `createdAt_lte`)
- Sort default: `createdAt desc`

## Formato de erro

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Dados inválidos.",
    "traceId": "01HJ7K2P9X...",
    "details": [
      { "field": "email", "message": "E-mail inválido" }
    ]
  }
}
```

- `code`: enum estável (não muda entre versões)
- `message`: pt-BR, voltado ao usuário final
- `traceId`: correlation ID para debug (mesmo do header `X-Trace-Id`)
- `details`: array opcional para erros de validação

### Códigos padrão

| Code | HTTP | Significado |
|------|------|-------------|
| `VALIDATION_ERROR` | 400 | Body/params falharam validação |
| `UNAUTHORIZED` | 401 | Auth ausente ou inválida |
| `FORBIDDEN` | 403 | Auth OK, sem permissão |
| `NOT_FOUND` | 404 | Recurso inexistente |
| `CONFLICT` | 409 | Conflito de estado (duplicata, versão) |
| `PRECONDITION_FAILED` | 412 | If-Match / ETag falhou |
| `UNPROCESSABLE_ENTITY` | 422 | Validação semântica falhou |
| `RATE_LIMITED` | 429 | Rate limit excedido |
| `INTERNAL_ERROR` | 500 | Erro inesperado (ver traceId em logs) |
| `SERVICE_UNAVAILABLE` | 503 | Dependência fora do ar |

## Convenções de naming
- **Recursos**: substantivos plurais (`/influenciadores`, `/marcas`, `/prospeccoes`)
- **Ações HTTP**: verbos via método, não no path (`POST /prospeccoes`, **não** `/criarProspeccao`)
- **Sub-recursos**: máx 2 níveis (`/prospeccoes/{id}/tarefas` OK; `/.../tarefas/{id}/comentarios` separar em `/tarefas/{id}/comentarios`)
- **IDs**: **UUID v7** (Postgres `uuid` + lib que gere v7 — preserva ordenação por tempo, melhor cache de índice)
- **Datas**: ISO 8601 UTC (`2026-04-29T14:32:00Z`); cliente converte para `America/Sao_Paulo`
- **Enums**: `UPPER_SNAKE_CASE` em JSON (`status: "ATIVA"`)
- **Booleans**: prefixo semântico (`isAtivo`, `hasContrato`)
- **Casing**: `camelCase` em JSON (frontend TS espera assim); Java mapeia via Jackson

## Idempotência
Endpoints `POST` que criam recursos aceitam header `Idempotency-Key`:
```http
POST /api/v1/emails/enviar
Idempotency-Key: 01HJ7K2P9X-evento-prospeccao-456
```
- Servidor armazena resposta por 24h
- Retry com mesma key retorna resposta cacheada
- Obrigatório em: envio de e-mail, criação de prospecção via webhook, pagamento (futuro)

## Concorrência otimista
- Recursos editáveis retornam `ETag` no GET
- `PATCH`/`PUT` exige `If-Match: <etag>` — falha com `412 PRECONDITION_FAILED` se mudou

## Rate limiting

| Tipo | Limite | Janela |
|------|--------|--------|
| Endpoints públicos (`/auth/login`, `/auth/recuperar-senha`) | 10 req | 1 min |
| Autenticados (geral) | 600 req | 1 min |
| Envio de e-mail (`/emails/*`) | 100 req | 1 min por assessoria |
| Webhooks recebidos | 1000 req | 1 min por origem |

Headers de resposta:
- `X-RateLimit-Limit`
- `X-RateLimit-Remaining`
- `X-RateLimit-Reset` (epoch)

Implementação: bucket por (assessoria_id, endpoint_class) em Redis.

## CORS
```
Allowed origins: https://app.hub-feat-creators.com, https://*-hub.vercel.app (preview), http://localhost:3000 (dev)
Allowed methods: GET, POST, PUT, PATCH, DELETE, OPTIONS
Allowed headers: Authorization, Content-Type, Idempotency-Key, If-Match, X-Trace-Id
Exposed headers: X-Trace-Id, ETag, X-RateLimit-Remaining
Max age: 86400
Credentials: true (cookies só para refresh em ambiente same-site)
```

Configurado via env `CORS_ALLOWED_ORIGINS` (comma-separated).

## Segurança
- Sempre HTTPS (TLS 1.3); HSTS preload
- Dados sensíveis nunca em URL (use body ou header)
- Validação Bean Validation (`@Valid`) em todo DTO
- Output encoding automático via Jackson (XSS em response improvável; mas headers `Content-Type` corretos previnem MIME sniffing)
- Detalhes: `docs/specs/security/`

## Documentação
- **OpenAPI 3.1** — auto-gerada via `springdoc-openapi`
- Endpoint: `GET /api/v1/openapi.json` e UI Swagger em `/api/v1/docs`
- Em produção: UI desabilitada; JSON disponível via auth admin
- Versionado: snapshot do JSON commitado em `docs/api/openapi-v1.json` a cada release

## Convenções de endpoint (exemplos do domínio)

```
POST   /api/v1/auth/login
POST   /api/v1/auth/refresh
POST   /api/v1/auth/logout

GET    /api/v1/influenciadores
POST   /api/v1/influenciadores
GET    /api/v1/influenciadores/{id}
PATCH  /api/v1/influenciadores/{id}
DELETE /api/v1/influenciadores/{id}        # soft-delete

GET    /api/v1/marcas
POST   /api/v1/marcas
...

GET    /api/v1/prospeccoes
POST   /api/v1/prospeccoes
GET    /api/v1/prospeccoes/{id}
POST   /api/v1/prospeccoes/{id}/tarefas
POST   /api/v1/prospeccoes/{id}/emails

GET    /api/v1/tarefas?dueDate_lte=2026-05-01&status=PENDENTE
POST   /api/v1/tarefas/{id}/concluir
```

## Webhooks (futuro)
- Assinatura HMAC-SHA256 no header `X-Hub-Signature-256`
- Retry: 5 tentativas com backoff exponencial (1m, 5m, 30m, 2h, 12h)
- Dead letter queue após 5 falhas
