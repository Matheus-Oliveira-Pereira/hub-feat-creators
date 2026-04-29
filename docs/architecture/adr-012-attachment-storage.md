# ADR-012: Storage de anexos — abstração + Railway volume → S3 (futuro)

## Status
Accepted — 2026-04-29

## Context

PRD-004 (e-mail outbound) prevê anexos (≤ 3 arquivos, ≤ 10MB total). Futuros PRDs podem precisar (foto de perfil influenciador, contrato anexo na prospecção). Decisão necessária: onde armazenar binários e como expor.

Restrições MVP:
- Postgres `BYTEA` é tentador (zero infra) — viável até ~10MB/registro mas explode tamanho de DB, backup pesado, query lenta
- Railway oferece **persistent volume** atado a um serviço — barato, simples, mas não distribuído (uma réplica = um volume)
- S3-compatible (AWS S3, Cloudflare R2, Backblaze B2, Railway-managed) é padrão da indústria mas adiciona dependência

## Decision

### Abstração `AttachmentStorage` em código

Interface em `apps/api`:

```java
public interface AttachmentStorage {
  StorageRef put(StoragePut req);                // upload, retorna ref opaca
  byte[] get(StorageRef ref);
  URI presign(StorageRef ref, Duration ttl);     // URL assinada (S3) ou link interno autenticado (volume)
  void delete(StorageRef ref);
}
```

Duas implementações:
- `LocalVolumeAttachmentStorage` (default MVP, Railway volume em `/data/attachments/{assessoria_id}/{uuid}`)
- `S3AttachmentStorage` (config-driven, ativada em prod quando volume não basta)

Selecionada via `app.storage.driver=local|s3` em `application.yml`.

### Convenções de path/key
- Layout: `attachments/{assessoria_id}/{ano}/{mes}/{uuid}-{slug-do-filename}`
- Multi-tenant: prefixo `assessoria_id` permite policy IAM por tenant em S3 futuro
- `slug-do-filename` ajuda debugging mas não é confiável (rename arbitrário)

### Servir arquivos
- **Nunca** servir direto pelo Spring Boot em prod (consome thread)
- MVP volume: endpoint `GET /api/v1/attachments/{id}` que valida tenant + retorna `X-Sendfile`/`X-Accel-Redirect` (Railway proxy não suporta nativamente — fallback streaming controlado com `StreamingResponseBody`)
- Prod S3: `presign(ref, 7d)` retorna URL S3 assinada; cliente baixa direto

### Schema (referência genérica)

```sql
attachment (
  id UUID PK,
  assessoria_id UUID FK NOT NULL,
  contexto_tipo TEXT NOT NULL,                  -- 'EMAIL_ENVIO', 'PROSPECCAO_CONTRATO', ...
  contexto_id UUID NOT NULL,
  filename TEXT NOT NULL,
  content_type TEXT NOT NULL,
  size_bytes BIGINT NOT NULL,
  storage_driver TEXT NOT NULL,                 -- 'LOCAL'|'S3'
  storage_key TEXT NOT NULL,                    -- path ou key
  checksum_sha256 TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  created_by UUID FK,
  deleted_at TIMESTAMPTZ NULL
)
```

PRD-004 (`email_attachments`) é uma view/extensão dessa tabela ou tabela própria com FK lógica para `attachment`. Decisão de modelagem na implementação; abstração de driver é a parte fixa.

### Limites operacionais
- Tamanho máx por arquivo: **10 MB** (MVP). Acima: rejeitar com 413
- Tamanho máx por contexto: 30 MB (3 × 10 MB para e-mail)
- Tipos permitidos (e-mail): allowlist `pdf, docx, png, jpg, jpeg, xlsx, pptx, txt` — bloquear `exe, bat, sh, js, html, svg`
- Validação: por **magic bytes** (Apache Tika), não só extensão
- Antivírus: **adiar** (aceitar como risco documentado em ADR-011); reavaliar com primeiro incidente

### Cifragem
- MVP: **não cifrar em repouso** (volume Railway é privado; S3 prod usa SSE-S3/KMS)
- Senhas/tokens **nunca** vão para anexo (regra coberta em ADR-011)
- Documentos jurídicos sensíveis (contratos): considerar cifragem cliente-side em PRD futuro (não previsto MVP)

### Limpeza
- Soft-delete via `attachment.deleted_at`
- Job `attachment-purga` (roda diário via ADR-010): apaga binário do storage onde `deleted_at < now() - 30 days`; mantém row para audit
- Sincronização: ao deletar contexto pai (e-mail enviado, prospecção), soft-delete cascade

## Alternatives considered

1. **Postgres `BYTEA`**
   - + zero infra, atômico com DB transaction
   - − backup do DB cresce desproporcional
   - − query mesmo sem `bytea_col` carrega tuple grande (toast ajuda mas não cura)
   - Descartado para anexos genéricos; OK para imagens pequenas (<100KB) raramente acessadas

2. **Direto S3 desde MVP**
   - + escala, durável, padrão indústria
   - − vendor lock-in cedo (mitigado por interface)
   - − conta S3 + IAM + custos mínimos ($0.023/GB/mês ainda assim adiciona)
   - − latência de network vs volume local
   - Adiar até volume justificar

3. **Cloudflare R2 / Backblaze B2 (S3-compatible barato)**
   - + custo egress zero (R2)
   - + S3 API
   - − ainda exige conta + setup
   - Quando migrar de volume, R2 é forte candidato

4. **Object storage do Railway (managed)**
   - + sem ops
   - − feature relativamente nova/limitada; reavaliar quando estável

5. **CDN para servir** (Cloudflare em frente)
   - + descarrega api
   - − complexidade desnecessária no MVP (anexos são privados, raramente acessados)
   - Reavaliar se virar produto público (Fase 4 marketplace)

## Consequences

**Positivas**:
- Interface protege de vendor lock-in
- MVP simples (volume) sem custo extra
- Migração a S3-like é troca de bean, sem refactor de domínio
- Multi-tenant na key permite policy/quota por assessoria depois

**Negativas**:
- Volume Railway é single-replica → escala vertical até migrar
- Backup do volume é responsabilidade ops manual (snapshots Railway); S3 traz versionamento nativo
- Streaming via Spring consome thread; mitigar com tamanho limitado no MVP

**Riscos**:
- Volume cheio sem alerta → uploads falham. Mitigar com métrica `disk_free_bytes` e alerta < 1GB
- Driver mismatch: row diz `LOCAL` mas binário sumiu (volume reset). Mitigar com checksum + healthcheck periódico
- Tipo malicioso passando magic bytes (PDF com payload): aceitar como risco; recomendação de antivírus em fase futura

## Impact on specs

- **security**: validação magic bytes; allowlist; sem AV (risco aceito); link presign com TTL curto
- **data-architecture**: tabela `attachment` + abstração de driver
- **scalability**: alerta de volume; plano de migração a S3
- **observability**: métricas `attachment_upload_total{driver}`, `attachment_size_bytes_histogram`, `attachment_storage_used_bytes`
- **api**: endpoints `POST /attachments`, `GET /attachments/{id}` com presign

## References

- PRD-004 (anexos em e-mail)
- ADR-009 (multi-tenant — prefixo `assessoria_id`)
- ADR-011 (LGPD — não cifrar credencial em anexo; retenção)
- Apache Tika (magic bytes): https://tika.apache.org/
- AWS S3 SDK Java v2: https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/
- Cloudflare R2: https://developers.cloudflare.com/r2/
