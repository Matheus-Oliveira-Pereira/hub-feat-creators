# Runbook — DSR (Data Subject Request / Direitos do Titular)

## Contexto
LGPD Art. 18 garante ao titular: acesso, correção, exclusão, portabilidade e oposição ao tratamento.
Prazo legal: 15 dias para resposta (Art. 19).

## Fluxo Técnico

```
1. Assessoria cria DSR via POST /api/v1/admin/compliance/dsr
2. Sistema gera token SHA-256 (single-use, expira em 48h) e envia por e-mail
3. Titular executa via POST /api/v1/dsr/execute/{token}
4. Sistema executa ação e marca token como usado
```

## Tipos de DSR

| Tipo | Ação | Endpoint |
|------|------|----------|
| ACESSO | Exporta todos os dados do titular | `/dsr/execute/{token}` → retorna Map |
| EXCLUSAO | Anonimiza nome, nula e-mail/telefone, sets `deleted_at` | Idem |
| CORRECAO | Redireciona para form de edição | A implementar |
| PORTABILIDADE | Exporta dados em JSON estruturado | A implementar |
| OPOSICAO | Registra opt-out do tratamento | A implementar |

## Operação Manual

### Criar DSR
```bash
curl -X POST http://localhost:8080/api/v1/admin/compliance/dsr \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "titularTipo": "INFLUENCIADOR",
    "titularId": "<uuid>",
    "tipoDsr": "EXCLUSAO",
    "solicitanteEmail": "titular@email.com",
    "descricao": "Solicitação recebida por e-mail em 2026-05-04"
  }'
```

### Verificar DSRs Vencendo
```bash
curl http://localhost:8080/api/v1/admin/compliance/dsr/alertas?diasRestantes=3 \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Consultar dados do titular (ACESSO)
```bash
curl http://localhost:8080/api/v1/dsr/dados/INFLUENCIADOR/<uuid> \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

### Executar DSR com token
```bash
curl -X POST http://localhost:8080/api/v1/dsr/execute/<token>
```

## Verificar SLA

```sql
-- DSRs vencendo em 3 dias
SELECT id, tipo, titular_tipo, titular_id, prazo_legal, status
FROM dsr_solicitacoes
WHERE status IN ('PENDENTE','EM_ANDAMENTO')
  AND prazo_legal <= NOW() + INTERVAL '3 days'
ORDER BY prazo_legal;
```

## Anonimização Manual (emergência)

Se o token expirou e o titular insiste:

```sql
-- APENAS com autorização registrada no ticket
UPDATE influenciadores
SET nome = 'Titular #' || LEFT(id::text, 8),
    deleted_at = NOW()
WHERE id = '<uuid>';

UPDATE handles SET instagram = NULL, youtube = NULL, tiktok = NULL
WHERE influenciador_id = '<uuid>';
```

Registrar no `audit_log`:
```sql
INSERT INTO audit_log (entidade, entidade_id, acao, payload, user_id, assessoria_id, created_at)
VALUES ('influenciador', '<uuid>', 'ANONIMIZACAO_MANUAL', '{"motivo":"DSR_EXCLUSAO_TOKEN_EXPIRADO","ticket":"#123"}',
        '<admin_user_id>', '<assessoria_id>', NOW());
```

## Retenção

`RetentionJob` roda diariamente às 03:00 UTC:
- Influenciadores/marcas/contatos deletados há >180d → anonimização automática
- Jobs com status OK/MORTO há >7d → purga
- Audit_log >5 anos → pseudonimização

Forçar execução manual (dev/staging):
```bash
# endpoint não exposto em produção — executar via Spring Actuator
curl -X POST http://localhost:8080/actuator/scheduledtasks
```

## Alertas e Monitoramento

- `GET /api/v1/admin/compliance/dsr/alertas?diasRestantes=3` → DSRs prestes a vencer
- Configurar cron externo para chamar endpoint diariamente e alertar equipe

## Documentação de conformidade

- ROPA: `GET /api/v1/admin/compliance/ropa`
- Cada tratamento documentado em `data_processing_records`
