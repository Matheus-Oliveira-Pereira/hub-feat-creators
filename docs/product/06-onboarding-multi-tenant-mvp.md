# PRD-006: Onboarding & Multi-tenant — MVP

## Context

PRDs 01-05 presumem assessoria já existe + usuário já autenticado. Hoje criação de tenant exige seed manual (insert SQL). Sem onboarding self-service, produto não escala além de clientes piloto.
Vision: Fase 1 — "Autenticação multi-tenant (uma assessoria = um workspace)".
Depende de: [[05-rbac-mvp]] (perfis), ADRs [[adr-008-auth-jwt]], [[adr-009-multi-tenant-strategy]].

## Objective

Permitir signup self-service de assessoria (workspace), convite/aceite de membros, recuperação de senha e login MFA opcional — com isolamento estrito multi-tenant garantido por aspect + filtros de query.

## Scope

### Includes
- [ ] **Signup assessoria**: form (nome assessoria, slug, e-mail OWNER, senha, aceite ToS/LGPD) → cria `assessoria` + usuário OWNER + perfis padrão (OWNER, ASSESSOR, FINANCEIRO)
- [ ] **Verificação de e-mail** OWNER via token (validade 24h); login bloqueado até verificar
- [ ] **Login** com bloqueio após 5 falhas em 15min (lockout temporário 30min)
- [ ] **Recuperação senha**: e-mail com token (validade 1h, single-use)
- [ ] **MFA TOTP opcional** (RFC 6238) — QR code no setup; recovery codes (10x single-use)
- [ ] **Convite de membro**: OWNER envia convite por e-mail com perfil pré-atribuído; aceite cria usuário no tenant
- [ ] **Listagem de membros**: ativar/desativar, trocar perfil, remover (soft-delete)
- [ ] **Sessão**: JWT 60min + refresh token 30d (rotativo); logout invalida refresh
- [ ] **Switch de tenant** (Fase 1.5): usuário em múltiplos tenants escolhe ao logar
- [ ] **Tenant resolver**: header `X-Tenant-Id` + claim JWT — aspect rejeita mismatch
- [ ] **Auditoria**: `audit_log` com login, logout, troca senha, MFA on/off, convite, remoção

### Excludes
- [ ] SSO SAML/OIDC corporativo — Fase 2
- [ ] Self-serve billing — fora MVP
- [ ] Domínio customizado por tenant (vanity URL) — Fase 2
- [ ] Soft-delete de tenant inteiro (close workspace) — manual via runbook

## Not Doing

- **SSO** — assessorias-piloto pequenas, login/senha basta. Reavaliar em B2B enterprise.
- **Billing self-service** — cobrança manual no MVP via contrato.

## User Stories

- Como dona de assessoria, quero criar workspace sem suporte para iniciar trial
- Como OWNER, quero convidar minha equipe com perfil definido
- Como OWNER, quero ativar MFA para proteger acesso
- Como ASSESSOR, quero recuperar senha sem esperar admin

## Acceptance Criteria

- [ ] **AC-1**: Signup cria registros em transação atômica (assessoria + owner + perfis seed); falha em qualquer passo → rollback total
- [ ] **AC-2**: Senha hash com Argon2id (params: t=3, m=64MB, p=4); nunca armazenada em claro
- [ ] **AC-3**: Token verificação e-mail é UUID v7 cifrado; expirado retorna 410
- [ ] **AC-4**: Lockout login: 5 falhas em 15min → 30min lock; chave `(assessoria_slug + email)`
- [ ] **AC-5**: MFA TOTP valida janela ±1 step; recovery code é single-use (UNIQUE + soft-delete em uso)
- [ ] **AC-6**: Convite expira em 7d; aceite exige senha + verificação e-mail combinados
- [ ] **AC-7**: Cross-tenant: requisição com JWT do tenant A + `X-Tenant-Id` do B retorna 403
- [ ] **AC-8**: Refresh token rotativo: usar refresh antigo após emitir novo invalida ambos (token reuse detection)
- [ ] **AC-9**: Auditoria: todo evento sensível grava `(assessoria_id, user_id, action, ip, user_agent, created_at)`
- [ ] **AC-NF-1**: Latência login p95 < 300ms (Argon2 dominante)
- [ ] **AC-NF-2**: Cobertura ≥ 85% em `AuthService`, `TenantAspect`, `MfaService`

## Technical Decisions

- **Hash**: Argon2id (Spring Security `Argon2PasswordEncoder`)
- **JWT**: HS256 com chave rotacionável (kid no header); claims `sub`, `tenant_id`, `perfil_id`, `permissoes[]`, `exp`
- **MFA**: lib `totp-java`; secret cifrado AES-GCM
- **Refresh tokens**: tabela `refresh_tokens(id, user_id, hash, expires_at, revogado_em, substituido_por_id)` — rotação chain detection
- **E-mails transacionais**: usa infra PRD-004 (conta SMTP do sistema cadastrada como `system@` ou provedor dedicado interno) — separar de SMTP de assessoria

### Schema

```sql
assessorias (id, nome, slug UNIQUE, status, created_at, deleted_at)
usuarios (id, assessoria_id FK, nome, email CITEXT, senha_hash, email_verificado_em, mfa_secret_enc, mfa_ativo, status, created_at, deleted_at,
  UNIQUE (assessoria_id, email))
convites (id, assessoria_id, email, perfil_id, token_hash, expires_at, aceito_em, criado_por_id, created_at)
refresh_tokens (id, user_id, token_hash, expires_at, revogado_em, substituido_por_id)
mfa_recovery_codes (id, user_id, code_hash, used_at)
audit_log (id, assessoria_id, user_id, action, target_id, ip, user_agent, payload JSONB, created_at)
login_attempts (key TEXT PK, count INT, locked_until)
```

## Impact on Specs

- **Compliance**: LGPD — consentimento ToS no signup; base legal "execução de contrato"; auditoria 5y; direito de exclusão exige soft-delete tenant + purga após retenção
- **Security**: Argon2, lockout, MFA, token rotation, audit log; OWASP ASVS L2
- **Scalability**: lookup `(assessoria_slug, email)` indexado; cache de permissões 60min (já em RBAC)
- **Observability**: métricas `auth_login_total{status}`, `auth_lockout_total`, `mfa_setup_total`, `convite_aceito_total`
- **Accessibility**: forms WCAG 2.1 AA; QR code MFA tem fallback secret manual
- **API**: `/api/v1/auth/{signup,login,logout,refresh,verify-email,reset-password,mfa/*}`, `/api/v1/membros`, `/api/v1/convites`

## Rollout

- **Feature flag**: `feature.signup.enabled` (pode desligar signup mantendo login)
- **Migrations**: V0006_*; backfill de `assessoria` e `usuarios` existentes (seed manual) → setar `email_verificado_em = now()` para legados
- **Rollback**: flag off; tabelas auxiliares ficam
- **Monitoramento**: alerta lockouts > 50/h (ataque); alerta token reuse > 0 (vazamento)

## Métricas

- Signup → primeiro login < 5min mediana
- ≥ 90% verificam e-mail em 24h
- Zero incidente cross-tenant
- Adoção MFA OWNER ≥ 40% em 90d

## Open questions

- [ ] Slug colisão: gerar sufixo numérico ou rejeitar? — rejeitar com sugestão
- [ ] Permitir mesmo e-mail em múltiplos tenants? — sim, identidade global por (tenant, email)
- [ ] MFA obrigatório para OWNER? — opcional MVP, obrigatório em planos enterprise futuro
