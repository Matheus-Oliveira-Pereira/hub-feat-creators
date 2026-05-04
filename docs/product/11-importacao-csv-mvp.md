# PRD-011: Importação CSV — MVP

## Context

Assessoria nova chega com planilha de creators/marcas/contatos. Cadastro manual em massa = barreira de adoção. Sem importação, fricção de onboarding mata trial. Quick win.
Vision: enabler de onboarding (sub-Fase 1).
Depende de: PRDs 01, 06, 07.

## Objective

Importar CSV/XLSX de influenciadores, marcas e contatos com mapeamento de colunas, dry-run com pré-visualização de erros, dedup por chave natural e relatório pós-import.

## Scope

### Includes
- [ ] **Upload CSV/XLSX** ≤ 5MB, ≤ 10k linhas
- [ ] **Detecção de encoding** (UTF-8 / Latin-1 / CP1252) e separador (`,;\t`)
- [ ] **Mapeamento de colunas**: UI mostra primeiras linhas; usuária mapeia cada coluna do CSV → campo da entidade (auto-sugestão por nome similar)
- [ ] **Templates de importação**: salvar mapeamento por entidade para reuso (`import_template`)
- [ ] **Dry-run**: validação completa sem persistir; retorna por linha `{ linha, status, erros[] , entidade_preview }`
- [ ] **Dedup**:
  - Influenciador: por `instagram_handle` ou `email`
  - Marca: por `cnpj` ou `nome` (case-insensitive)
  - Contato: por `email` (CITEXT) escopo da marca
- [ ] **Estratégia dedup**: `SKIP|UPDATE|DUPLICATE` — usuária escolhe
- [ ] **Validações**: campos obrigatórios, formato (e-mail, CPF/CNPJ via algoritmo, URL, telefone E.164), enums
- [ ] **Base legal LGPD**: usuária declara base legal aplicável a todo lote (PRD-007)
- [ ] **Execução assíncrona**: import roda em job; progress bar via SSE
- [ ] **Relatório**: download CSV com `linha, status, id_criado, erros`
- [ ] **Rollback**: cancelar job em andamento + marcar entidades criadas para soft-delete (estratégia `cancelable_batch_id`)

### Excludes
- [ ] Importar prospecções/tarefas/e-mails — fora MVP (relacionamentos complexos)
- [ ] Sync contínuo com Google Sheets — Fase 2
- [ ] Mapeamento de campos custom — fora MVP (sem custom fields ainda)
- [ ] OCR/PDF — fora escopo

## Not Doing

- **Import síncrono** — arquivos > 1k linhas timeout HTTP; async desde sempre.
- **Custom fields** — produto não tem ainda; PRD futuro.

## User Stories

- Como nova assessora, quero subir planilha de 500 creators e ter cadastros prontos
- Como OWNER, quero ver o que vai entrar antes de confirmar
- Como ASSESSOR, quero reusar mapeamento da última vez
- Como auditor, quero saber quem importou o quê e quando

## Acceptance Criteria

- [ ] **AC-1**: Upload aceita CSV (UTF-8 BOM/sem BOM, Latin-1) e XLSX; detecção retorna encoding inferido + separador
- [ ] **AC-2**: Auto-sugestão de mapeamento usa Levenshtein < 3 entre header e nome de campo
- [ ] **AC-3**: Dry-run nunca grava; relatório de validação retorna em ≤ 30s para 10k linhas
- [ ] **AC-4**: Validação CPF/CNPJ: algoritmo (DV) + formato; aceita com/sem máscara
- [ ] **AC-5**: Telefone normalizado para E.164 BR (`+55...`); inválido = erro
- [ ] **AC-6**: Dedup `UPDATE` atualiza só campos mapeados não-null; preserva resto
- [ ] **AC-7**: `base_legal` obrigatória por lote; gravada em cada entidade criada
- [ ] **AC-8**: Job progress via SSE: `{processadas, total, sucesso, falha, eta_ms}`
- [ ] **AC-9**: Cancel em job ativo: marca registros criados como `deleted_at`; relatório indica "cancelado em linha N"
- [ ] **AC-10**: Cross-tenant: arquivo upload em tenant A não vaza para B
- [ ] **AC-11**: Rate limit: 1 import simultâneo por usuário; 5 por tenant
- [ ] **AC-NF-1**: Memória: parse stream linha-a-linha; não carrega arquivo todo
- [ ] **AC-NF-2**: Performance: 10k linhas em < 60s
- [ ] **AC-NF-3**: Cobertura ≥ 80% em `ImportService`, parsers, validators

## Technical Decisions

- **Parser CSV**: `univocity-parsers` (alta perf, streaming)
- **XLSX**: Apache POI streaming (`SXSSF`)
- **Storage upload**: temp em volume + S3-compat; deletado após 7d ou conclusão
- **Job**: `tipo=IMPORT_BULK` (ADR-010)
- **Batch insert**: `JdbcTemplate.batchUpdate` em chunks de 500
- **Lock dedup**: SELECT FOR UPDATE por chave natural durante chunk

### Schema

```sql
import_jobs (id, assessoria_id, usuario_id, entidade CHECK ('INFLUENCIADOR','MARCA','CONTATO'),
  arquivo_path, mapeamento JSONB, base_legal TEXT, dedup_strategy TEXT,
  status CHECK ('UPLOADADO','VALIDANDO','PRONTO_DRY_RUN','EXECUTANDO','CONCLUIDO','CANCELADO','FALHOU'),
  total_linhas INT, processadas INT, sucesso INT, falha INT,
  cancelable_batch_id UUID,
  iniciado_em, concluido_em, relatorio_path)

import_job_linhas (job_id FK, linha INT, status TEXT, entidade_id UUID NULL, erros JSONB,
  PRIMARY KEY (job_id, linha))

import_templates (id, assessoria_id, nome, entidade, mapeamento JSONB, created_at, deleted_at)
```

## Impact on Specs

- **Compliance**: base legal por lote; auditoria do upload (quem, quando, hash do arquivo)
- **Security**: scan tamanho/extensão; ZIP bomb protection (limite linhas pré-parse); MIME sniff
- **Scalability**: streaming + chunks; rate limit
- **Observability**: `import_iniciado_total`, `import_falha_total{motivo}`, histograma duração
- **API**: `/api/v1/imports/{,upload,dry-run,execute,cancel,relatorio,templates}`
- **Testing**: fixtures com encoding zoo (UTF-8 BOM, Latin-1, sep `;`); fuzz validators

## Rollout

- **Feature flag**: `feature.import.enabled`
- **Migrations**: V0011_*
- **Rollback**: flag off; jobs ativos finalizam

## Métricas

- ≥ 60% novas assessorias usam import em 7d
- Taxa de erro mediana < 5% por job
- Tempo upload → execução completa p95 < 5min

## Open questions

- [ ] Permitir Google Sheets URL direto? — adiar Fase 2
- [ ] Importar avatar/foto via URL no CSV? — sim, async download com timeout 5s
