# Module: Compliance — HUB Feat Creator

> **Status**: ⏳ **pendente** — ativar antes da primeira release em produção.
>
> Esta página é **baseline funcional**: regulamentações aplicáveis identificadas e princípios já incorporados ao projeto desde o dia 1, mesmo sem auditoria formal. Conteúdo será expandido (DPIA, ROPA, checklists detalhados) quando o módulo for promovido a "ativo".

## Regulamentações aplicáveis

### Proteção de dados

| Regulamentação | Escopo | Aplica? | Skill criada? |
|----------------|--------|---------|---------------|
| **LGPD** (Lei 13.709/2018) | Dados pessoais — Brasil | **Sim, principal** | ⏳ a criar |
| GDPR (EU 2016/679) | Dados pessoais — UE | Não no MVP (público pt-BR) — reavaliar se expandir | — |
| CCPA/CPRA | Dados pessoais — Califórnia | Não no MVP | — |
| HIPAA | Dados de saúde — EUA | Não aplicável | — |

### Setoriais

| Regulamentação | Escopo | Aplica? |
|----------------|--------|---------|
| Marco Civil da Internet (Lei 12.965/2014) | Internet — Brasil | **Sim** — guarda de logs por 6 meses (Art. 15) |
| CDC (Código de Defesa do Consumidor) | Relação fornecedor↔consumidor | **Sim** — termos de uso e política clara |
| PCI-DSS | Pagamentos com cartão | Aplicará quando entrar pagamentos (Fase 4) |

### ISOs (futuro)

| Standard | Escopo | Quando |
|----------|--------|--------|
| ISO 27001 | SGSI | Após PMF + 50+ assessorias clientes |
| ISO 27701 | SGPI (privacidade) | Junto com 27001 |
| ISO 42001 | AI governance | Quando feature de match IA entrar em produção |

## LGPD — princípios já incorporados

> Estes não esperam o módulo ser "ativado" — são **inegociáveis desde o primeiro commit**.

### Bases legais por finalidade

| Dado | Finalidade | Base legal LGPD (Art. 7º) |
|------|-----------|---------------------------|
| Cadastro do usuário (assessor) | Prestação do serviço | Execução de contrato (V) |
| Cadastro de influenciador | Operação de assessoria | Legítimo interesse (IX) — com notificação + opt-out |
| Cadastro de marca/contato | Prospecção comercial | Legítimo interesse (IX) — com notificação + opt-out |
| Logs de aplicação | Segurança e prevenção a fraudes | Legítimo interesse (IX) |
| E-mails enviados | Execução de contrato (assessor) + Legítimo interesse (destinatário) | V e IX |
| Audit log | Cumprimento de obrigação legal + segurança | II, IX |
| Analytics de produto (PostHog) | Melhoria do serviço | **Consentimento** (I) — opt-in obrigatório |
| Match marca↔influencer (Fase 3) | Execução de contrato + Legítimo interesse | V, IX — com transparência adicional |

### Direitos dos titulares (Art. 18) — endpoints já planejados

| Direito | Endpoint / fluxo | Prazo (LGPD) |
|---------|------------------|--------------|
| Confirmação de tratamento + Acesso | `GET /api/v1/me/dados-pessoais` (próprios dados) | 15 dias |
| Correção | `PATCH /api/v1/...` ou contato | 15 dias |
| Anonimização / eliminação | `DELETE /api/v1/me` (soft + purge) | 15 dias |
| Portabilidade | Export CSV/JSON | 15 dias |
| Informação sobre compartilhamento | Doc pública + dashboard | 15 dias |
| Revogação de consentimento | Toggle em config + `POST /api/v1/me/consentimento/revogar` | imediato |
| Oposição (legítimo interesse) | Solicitação por canal direto + opt-out auto | 15 dias |

Influenciadores e contatos cadastrados em assessorias terão canal próprio (público) para exercer direitos sem precisar contato com a assessoria — `GET https://hub-feat-creators.com/lgpd/<token>` (token enviado no e-mail de notificação inicial).

### Princípios operacionais (Art. 6º)

- ✅ **Finalidade**: cada coleta tem propósito documentado em PRD
- ✅ **Adequação**: dados coletados são compatíveis com finalidade
- ✅ **Necessidade**: minimização — não coletar campo que não é usado
- ✅ **Livre acesso**: titular pode consultar/exportar seus dados
- ✅ **Qualidade**: dados precisos; correção como direito
- ✅ **Transparência**: política pública + e-mail de notificação no primeiro tratamento de cada influenciador/marca/contato
- ✅ **Segurança**: ver `docs/specs/security/`
- ✅ **Prevenção**: monitoramento + DPIA quando feature trouxer risco novo
- ✅ **Não discriminação**: bias auditado em IA (ver `ai-ml/`)
- ✅ **Responsabilização**: registros, audit log, ROPA, DPA com vendors

## Documentos a criar (quando ativar)

- [ ] `data-mapping.md` — mapeamento completo de dados pessoais (categoria, fonte, fluxo, retenção)
- [ ] `dpia.md` — DPIA (Data Protection Impact Assessment), começando pela feature de match IA
- [ ] `ropa.md` — RoPA (Record of Processing Activities) — exigência ANPD
- [ ] `data-retention.md` — política consolidada de retenção (já rascunhada em `data-architecture/`)
- [ ] `checklists/lgpd-checklist.md` — checklist por feature antes de release
- [ ] `dpa-vendors/` — DPAs assinados (Railway, Vercel, Resend, PostHog, Anthropic etc)
- [ ] `politica-privacidade.md` — texto público
- [ ] `termos-de-uso.md` — texto público
- [ ] `incident-response.md` — plano de resposta a incidente de dados (ANPD: 72h se afetar direitos)

## Encarregado de Dados (DPO)
- **Atual**: Matheus Oliveira Pereira (ma.pe.oli.1998@gmail.com) — solo dev acumula função
- **Quando contratar externo**: > 50 assessorias clientes ou primeiro incidente reportado à ANPD

## Fluxo integrado

1. **PRD escrito** → identifica dados pessoais tratados e base legal
2. **Implementação** → controles técnicos (ver `security/`) + audit log
3. **Review** → `@compliance-auditor` verifica em `/spec-review`
4. **Pre-launch** → checklist LGPD + DPIA se necessário
5. **Pós-incidente** → post-mortem + atualização de controles + comunicação ANPD se aplicável

## Quando promover para "ativo"

Triggers:
- [ ] Antes do **primeiro deploy de produção** com dados reais (mesmo dados beta com consentimento)
- [ ] Antes de habilitar **feature de match IA** (Fase 3 — DPIA obrigatório)
- [ ] Antes de habilitar **pagamentos** (Fase 4 — PCI-DSS adicional)
- [ ] Se contratar **assessoria fora do Brasil** (avaliar GDPR/CCPA)

Ao ativar:
1. Atualizar `docs/specs/README.md` movendo de "pendente" para "ativo"
2. Criar todos documentos da seção "Documentos a criar"
3. Atualizar `CLAUDE.md` Modular Specifications
4. Skill `compliance` ganhando triggers em PRDs

## Referências
- LGPD — http://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm
- ANPD (autoridade) — https://www.gov.br/anpd/
- Marco Civil — http://www.planalto.gov.br/ccivil_03/_ato2011-2014/2014/lei/l12965.htm
- CDC — http://www.planalto.gov.br/ccivil_03/leis/l8078compilado.htm
