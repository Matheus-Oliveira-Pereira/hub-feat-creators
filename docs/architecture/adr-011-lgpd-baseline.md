# ADR-011: LGPD baseline — base legal, retenção, opt-out, direitos do titular

## Status
Accepted — 2026-04-29

## Context

HUB Feat Creator trata **dados pessoais** desde o dia 1: nomes/handles/áudiência de influenciadores, e-mails/telefones de contatos de marcas, dados de usuárias da assessoria, conteúdo de e-mails enviados a terceiros. CLAUDE.md exige "LGPD desde dia 1, com base legal documentada".

Spec `compliance/` está ⏳ (ativar antes de produção), mas múltiplos PRDs já prescrevem regras (soft-delete, opt-out, notificação ao titular). Sem ADR consolidando decisões, detalhes operacionais viram folclore espalhado em PRDs.

Risco real:
- Multa LGPD: até 2% do faturamento (cap R$ 50M) por incidente
- Imagem de SaaS B2B: cliente final exige cláusula de processamento de dados; contrato exige DPO definido, retenção definida, fluxo de direito do titular
- Precisa estar pronto **antes** de prospectar marcas (não pós-fato)

## Decision

### 1. Papéis (Art. 5º LGPD)
- **Controlador**: a **assessoria** (cliente do HUB). Define finalidade do tratamento dos dados de influenciadores/marcas/contatos. HUB é processador desses dados.
- **Operador (processador)**: **HUB Feat Creator**. Trata dados conforme instrução do controlador.
- **Controlador dos dados das próprias usuárias** (cadastro de assessora, login): **HUB Feat Creator** (somos controladores).

Implicação: dois papéis simultâneos. Termos de Uso + DPA (Data Processing Agreement) refletem isso.

### 2. Base legal por entidade

| Entidade | Dado | Base legal | Justificativa |
|---|---|---|---|
| `assessorias` | Razão social, slug, plano | Execução de contrato (Art. 7º V) | Cadastro do próprio cliente |
| `usuarios` | Nome, e-mail, senha hash, login | Execução de contrato (Art. 7º V) | Acesso ao SaaS contratado |
| `convites` | E-mail de convidado | Legítimo interesse (Art. 7º IX) | Onboarding de equipe; opt-out via não-aceite |
| `influenciadores` | Nome, handles, áudiência, observações | **Legítimo interesse do controlador (assessoria)** | Atividade-fim da assessoria; HUB é operador |
| `marcas` | Razão social, site | Legítimo interesse | Dado público comercial |
| `contatos` | Nome, e-mail, telefone, cargo | **Legítimo interesse do controlador** | Comunicação comercial B2B |
| `email_envios` (destinatários) | E-mail de marca/contato | Legítimo interesse + opt-out registrado | Comunicação comercial; opt-out irreversível |
| `audit_log` | IDs + payload diff | Cumprimento de obrigação legal/regulatória | Auditoria (Art. 7º II + LGPD Art. 16) |
| `whatsapp_contact` | Telefone + opt-in | **Consentimento explícito** (Art. 7º I) | WhatsApp exige opt-in inequívoco |

### 3. Soft-delete + retenção

| Tabela | Política | Retenção pós soft-delete |
|---|---|---|
| `influenciadores`, `marcas`, `contatos`, `prospeccoes`, `tarefas` | Soft-delete (`deleted_at`) | Purga após **180 dias** (job LGPD) |
| `usuarios` | Soft-delete | **Anonimização** após 90 dias inatividade pós-delete (preserva audit_log) |
| `email_envios` | Sem delete; arquivamento | **5 anos** (auditoria comercial) — depois pseudonimização do destinatário sob demanda |
| `audit_log` | Imutável | **5 anos** (mínimo regulatório); depois pseudonimização |
| `email_optouts` | **Imutável e perpétuo** | Nunca deletar (precisa para honrar opt-out futuro) |
| `whatsapp_contact` opt-in | Mantido enquanto válido | Revogação → opt_out=TRUE permanente |
| `job` (fila) | Purgar `OK`/`MORTO` após **7 dias** | Sem dado pessoal sensível em payload (regra) |

Job recorrente `lgpd-purga-diaria`: roda 02:00 BRT, processa cada tabela conforme política; logs de purga em `audit_log`.

### 4. Opt-out — granularidade
- **E-mail**: por `(assessoria_id, email)` — opt-out de uma assessoria não afeta outra (cada uma é controladora distinta)
- **WhatsApp**: por `(assessoria_id, telefone)` — mesmo princípio
- **Auto-honra global**: HUB mantém **opcional** lista interna de domínios que pediram block geral (ex: spam complaints sistemáticos); admin global revisa

### 5. Direitos do titular (LGPD Art. 18)

Endpoints / processo (MVP manual; UI em fase futura):

| Direito | Implementação MVP | Fase 2 |
|---|---|---|
| Confirmação de tratamento | E-mail manual ao DPO em 48h | Self-service |
| Acesso aos dados | Export JSON da entidade pelo DPO | UI export |
| Correção | Edição via UI ou solicitação ao controlador | Mesmo |
| Anonimização/eliminação | Job manual `lgpd-eliminar-titular` | Self-service via portal |
| Portabilidade | Export CSV (já no PRD-001) | Mesmo |
| Revogação consentimento | Opt-out automático + remoção | Mesmo |
| Informação sobre compartilhamento | Página estática (privacy policy) | Mesmo |

SLA de resposta: **15 dias** (LGPD Art. 19 §3º). Encarregado registra solicitações em planilha + tabela `lgpd_solicitacao` (Fase 2).

### 6. Encarregado (DPO)

- **Quem**: definir antes do go-live (sócio fundador no MVP).
- **Onde divulgar**: rodapé do site + página `/privacidade`.
- **Contato**: e-mail dedicado `dpo@hub-feat-creators.com`.

### 7. Logs e PII

- **Nunca logar**: senha (qualquer forma), token JWT/refresh, conteúdo de e-mail enviado, payload completo de webhook
- **Pseudonimizar em log**: e-mail (`m***@dominio.com`), telefone (`+5511*****1234`), CPF (se vier — não previsto MVP)
- **MDC obrigatório**: `assessoria_id`, `usuario_id`, `request_id`. Audit-friendly sem PII raw.
- Pre-commit hook (ADR-004) já flaga `log.info("...email...")` patterns

### 8. Notificação proativa ao titular

- **Inserção de influenciador**: PRD-001 prescreve e-mail automático "Você está sendo gerenciado por <assessoria> no HUB Feat Creator. Direitos: ...". Disparado via PRD-004 (e-mail outbound) com idempotência por `(assessoria_id, influenciador_id, tipo=LGPD_NOTIFY)`.
- **Inserção de contato de marca**: idem; obrigatório dentro de 30 dias da inserção.
- **Conteúdo do e-mail**: padrão revisado pelo DPO; link para política de privacidade da assessoria + do HUB; link para opt-out direto.

### 9. Incidente de segurança (Art. 48)

- Detecção → notificar ANPD e titulares afetados em **prazo razoável** (ANPD recomenda ≤ 72h)
- Runbook em `docs/runbooks/incident-lgpd.md` (criar; placeholder agora)
- Tabela `lgpd_incidente` (Fase 2)

### 10. Transferência internacional

- MVP roda em **Railway (US-east)** + Vercel (US/edge) → transferência internacional ocorre
- Cláusulas contratuais padrão (CCPs ANPD) ou demonstrar adequação no DPA com cliente
- Avaliar migrar para Railway região São Paulo se houver demanda enterprise (custo +)

## Alternatives considered

1. **Hard-delete em vez de soft-delete + retenção**
   - + simples, menor footprint
   - − cliente exige histórico; auditoria comercial perdida
   - − rollback de delete acidental impossível
   - Descartado

2. **Consentimento como base legal universal**
   - + "mais seguro" superficialmente
   - − consentimento exige opt-in explícito + revogabilidade fácil; inviável para "assessoria cadastra contato de marca"
   - − legítimo interesse com balanceamento documentado é juridicamente sólido para B2B
   - Descartado para entidades B2B; consentimento mantido só onde a lei exige (WhatsApp)

3. **Adiar LGPD para "antes de produção"**
   - + foco em features
   - − retrofit de LGPD = redesign de schema + flows
   - − não-conformidade desde dia 1 é defeito legal; vazamento beta → multa real
   - Descartado: implementar baseline AGORA é mais barato

## Consequences

**Positivas**:
- Conformidade real desde MVP — pode prospectar enterprise sem retrabalho
- Decisões registradas — DPA com cliente é cópia anotada deste ADR
- Soft-delete + audit_log = safety net para incidentes operacionais (rollback)

**Negativas**:
- Job de purga é mais um recurso a operar (mas ADR-010 já cobre infra)
- E-mail de notificação ao titular adiciona volume de envio (custo SMTP da assessoria; comunicar como obrigação no contrato)
- Retenção de 5 anos em `email_envios` aumenta storage Postgres — particionamento por mês quando passar de 10M registros

**Riscos**:
- DPO definido como sócio: insuficiente em compliance enterprise; contratar/terceirizar antes do primeiro cliente que exigir
- Cliente assessoria que cadastra contato sem base legal própria → HUB pode ser corresponsável. Mitigar com termo de uso explícito ("você declara ter base legal para os dados que cadastra") + cláusula de indenização

## Impact on specs

- **compliance**: spec inteira é a aplicação operacional deste ADR — **ativar agora** (sair de ⏳)
- **security**: pseudonimização em log, pre-commit hook (já existente), criptografia em repouso de credenciais
- **data-architecture**: tabelas `email_optouts`, `lgpd_solicitacao` (Fase 2), particionamento de `email_envios`
- **api**: endpoints de export (portabilidade) por entidade; endpoint público de unsubscribe
- **observability**: log com MDC; alerta de `lgpd_purga_falhou_total > 0`
- **versioning**: política de retenção é regra de negócio; mudança = comunicação aos clientes

## References

- LGPD — Lei 13.709/2018: https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm
- ANPD guia incidentes: https://www.gov.br/anpd/
- PRDs 001/002/004 (referenciam regras aqui consolidadas)
- ADR-010 (job de purga roda nesta infra)
- ADR-009 (multi-tenant strict — pré-requisito para isolamento de dado entre controladores)
- DPA template: TBD em `docs/legal/dpa-template.md` (criar Fase 2)
