# Product Vision — HUB Feat Creator

## Purpose
Centralizar e automatizar o trabalho operacional de assessorias de influenciadores digitais — prospecção, gestão de tarefas, comunicação e cadastros — em um único hub. No futuro, usar IA para sugerir matches entre marcas e influenciadores com base em fit de audiência, nicho e histórico.

## Problem
Assessorias de influenciadores hoje operam com ferramentas fragmentadas: planilhas para cadastros, e-mail manual para prospecção, WhatsApp para tarefas, Notion para acompanhamento. Resultado: dados duplicados, prospecções perdidas, follow-ups esquecidos, dificuldade em encontrar o influenciador certo para cada marca. Não existe um sistema feito para o fluxo específico de uma assessoria — CRMs genéricos não modelam influencer/marca/campanha como entidades de primeira classe.

## Target audience

### Persona primária — Assessora
- Profissional que gerencia carteira de 10–100 influenciadores
- Faz prospecção ativa de marcas, negocia campanhas, acompanha entregáveis
- Dor: tempo gasto em tarefas operacionais (e-mail, follow-up, organização) em vez de relacionamento
- Sucesso: fechar mais campanhas com menos tempo administrativo

### Persona secundária — Influenciador
- Creator de qualquer plataforma (Instagram, YouTube, TikTok, Twitch, etc) e qualquer porte (nano a top-tier)
- Interage com a plataforma para visualizar oportunidades, agenda e tarefas atribuídas pela assessoria
- Dor: falta de visibilidade do que a assessoria está prospectando em seu nome
- Sucesso: clareza sobre pipeline de campanhas e prazos

### Persona futura — Marca
- Empresa que contrata influenciadores via assessoria
- (Fora do escopo do MVP — entrará no L2 do roadmap)

## Value proposition
- **Único hub**: cadastros, prospecções, tarefas e comunicação em um lugar — fim das planilhas
- **Automação operacional**: lembretes de follow-up, envio de e-mails templated, alertas de tarefas em atraso
- **Match inteligente (futuro)**: IA sugere influenciadores para uma marca (ou vice-versa) com base em nicho, audiência, histórico de performance
- **Especializado no fluxo de assessoria**: ao contrário de CRMs genéricos, modela influenciador, marca, campanha e prospecção como entidades nativas

## Success metrics

### MVP (3 core)
- [ ] **WAU de assessoras** — Weekly Active Users; saúde do produto
- [ ] **Prospecções concluídas/mês** — atividade-fim que prova valor entregue
- [ ] **Time-to-close de prospecção** — dias entre criação e contrato fechado; eficiência ganha pela ferramenta

### Secundárias (acompanhar, não ainda como meta)
- [ ] Cadastros novos/mês (influenciadores + marcas)
- [ ] Taxa de resposta de prospecções enviadas
- [ ] NPS de assessoras
- [ ] Churn mensal de assessorias

## Roadmap (alto nível)

### Fase 1 — MVP operacional
- Cadastros (influenciador, marca, contato)
- Prospecção (criação, status, histórico)
- Tarefas + alertas
- Envio de e-mails templated (provedor: Resend ou similar)
- Autenticação multi-tenant (uma assessoria = um workspace)

### Fase 2 — Comunicação ampliada
- Portal do influenciador (visualizar tarefas, pipeline)
- Notificações (e-mail + push)
- Histórico unificado por influenciador/marca

### Fase 3 — Match inteligente (IA)
- Ingestão de dados públicos de redes sociais (com consentimento)
- Modelo de match marca↔influenciador (nicho, audiência, performance)
- Sugestões proativas de prospecção

### Fase 4 — Marketplace (visão longa)
- Marcas se cadastram diretamente e contratam via plataforma
- Pagamentos, contratos, entregáveis tudo na plataforma

## Restrições e premissas
- **LGPD obrigatória**: trata dado pessoal de influenciadores e contatos de marcas — base legal documentada desde dia 1, mesmo antes da auditoria formal de compliance entrar
- **Multi-tenant strict**: assessorias não podem ver dados umas das outras em hipótese alguma — isolamento testado
- **pt-BR only no MVP** — internacionalização só após PMF
- **Mobile-friendly, não mobile-first**: assessoras trabalham principalmente em desktop, mas portal do influenciador precisa funcionar bem em celular

## Links
- Repo: https://github.com/Matheus-Oliveira-Pereira/hub-feat-creators
- Design: TBD (Claude Design — gerar a partir do código + brand assets quando definidos)
- Production: TBD (Vercel + Railway — provisionar após primeira release)
