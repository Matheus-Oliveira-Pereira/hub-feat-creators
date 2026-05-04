# Runbook — Incidente de Dados (LGPD Art. 48)

## Contexto
LGPD Art. 48: comunicar ANPD e titulares em até 72h após ciência do incidente que possa acarretar risco ou dano relevante.

## Classificação de Severidade

| Nível | Descrição | Prazo ANPD |
|-------|-----------|------------|
| P1 — Crítico | Dados sensíveis expostos, acesso não autorizado em massa, ransomware | 72h |
| P2 — Alto | Vazamento de e-mails/telefones de titulares | 72h |
| P3 — Médio | Acesso indevido interno sem exfiltração confirmada | 5 dias úteis |
| P4 — Baixo | Logging acidental de PII sem acesso externo | Registro interno |

## Passos

### 1. Contenção (0–1h)
- Revogar tokens JWT afetados via `DELETE /api/v1/admin/sessions` (ou rotacionar `JWT_SECRET` em último caso)
- Suspender conta SMTP afetada: `PATCH /api/v1/admin/email-accounts/{id}` com `{ "ativo": false }`
- Criar snapshot do `audit_log` antes de qualquer alteração:
  ```sql
  COPY (SELECT * FROM audit_log WHERE created_at > NOW() - INTERVAL '7 days') TO '/tmp/audit_snapshot.csv' CSV HEADER;
  ```
- Isolar assessoria afetada se necessário (Railway: env var `TENANT_BLOCKED_IDS=<id>`)

### 2. Avaliação (1–4h)
- Quantificar titulares afetados:
  ```sql
  SELECT COUNT(DISTINCT titular_id) FROM pii_access_log
  WHERE accessed_at > '<timestamp_incidente>'
  AND accessed_by NOT IN (SELECT id FROM usuarios WHERE role = 'SISTEMA');
  ```
- Verificar integridade do audit_log (hash chain):
  ```sql
  SELECT id, prev_hash, chain_hash FROM audit_log ORDER BY created_at DESC LIMIT 100;
  ```
- Classificar tipo de dado: pseudonimizado / anonimizado / dado bruto

### 3. Notificação ANPD (P1/P2 — até 72h)
Formulário: https://www.gov.br/anpd/pt-br/canais_atendimento/comunicacao-de-incidente-de-seguranca

Informações obrigatórias:
- Data e hora da ciência do incidente
- Natureza dos dados afetados
- Número estimado de titulares
- Medidas tomadas / a tomar
- DPO responsável

### 4. Notificação a Titulares
- Usar `DsrController` para expor dados afetados: `GET /api/v1/dsr/dados/{titularTipo}/{titularId}`
- Notificar por e-mail via template `LGPD_INCIDENTE` (criar se não existir)
- Registrar notificação em `consentimentos` com `finalidade = 'NOTIFICACAO_INCIDENTE'`

### 5. Pós-incidente (até 15 dias)
- Post-mortem em `docs/runbooks/post-mortems/YYYY-MM-DD-incidente.md`
- Atualizar `data_processing_records` com medidas corretivas
- Revisar `PiiMaskingFilter` se PII apareceu em logs
- Acionar `RetentionJob` manualmente se necessário:
  ```bash
  curl -X POST http://localhost:8080/api/v1/admin/compliance/retention/run \
    -H "Authorization: Bearer $TOKEN"
  ```

## Contatos
- DPO: registrar em `app.compliance.dpo-email` em `application.yml`
- ANPD: anpd.gov.br | (61) 3411-7500
