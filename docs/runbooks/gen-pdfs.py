# -*- coding: utf-8 -*-
"""Gera 2 PDFs: tutorial-completo.pdf e guia-de-teste.pdf"""
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import cm
from reportlab.lib.colors import HexColor
from reportlab.platypus import (
    SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle,
    KeepTogether, ListFlowable, ListItem
)
from reportlab.lib.enums import TA_LEFT, TA_JUSTIFY
from pathlib import Path

LIME = HexColor("#C2E000")
INK = HexColor("#141414")
GRAY = HexColor("#6B7280")
LIGHT = HexColor("#F3F4F6")
RED = HexColor("#DC2626")
GREEN = HexColor("#16A34A")
BLUE = HexColor("#2563EB")

styles = getSampleStyleSheet()
H1 = ParagraphStyle('H1', parent=styles['Heading1'], textColor=INK, fontSize=22, spaceAfter=14, spaceBefore=8, fontName='Helvetica-Bold')
H2 = ParagraphStyle('H2', parent=styles['Heading2'], textColor=INK, fontSize=15, spaceAfter=8, spaceBefore=14, fontName='Helvetica-Bold')
H3 = ParagraphStyle('H3', parent=styles['Heading3'], textColor=INK, fontSize=12, spaceAfter=5, spaceBefore=10, fontName='Helvetica-Bold')
BODY = ParagraphStyle('Body', parent=styles['BodyText'], fontSize=10, leading=14, alignment=TA_JUSTIFY, spaceAfter=4)
CODE = ParagraphStyle('Code', parent=styles['Code'], fontSize=8.5, leading=11, backColor=LIGHT, leftIndent=6, rightIndent=6, borderPadding=4, fontName='Courier', spaceAfter=6, spaceBefore=4)
NOTE = ParagraphStyle('Note', parent=styles['BodyText'], fontSize=9, leading=12, textColor=GRAY, leftIndent=10, spaceAfter=4)
WARN = ParagraphStyle('Warn', parent=styles['BodyText'], fontSize=9.5, leading=12, textColor=RED, leftIndent=10, spaceAfter=4)
OK = ParagraphStyle('Ok', parent=styles['BodyText'], fontSize=9.5, leading=12, textColor=GREEN, leftIndent=10, spaceAfter=4)
COVER_TITLE = ParagraphStyle('CoverT', parent=H1, fontSize=34, alignment=TA_LEFT, spaceAfter=10)
COVER_SUB = ParagraphStyle('CoverS', parent=BODY, fontSize=14, textColor=GRAY, alignment=TA_LEFT, spaceAfter=6)


def step(n, text):
    return Paragraph(f'<font color="#C2E000"><b>{n}.</b></font> {text}', BODY)


def kbd(text):
    return f'<font face="Courier" backColor="#F3F4F6"> {text} </font>'


def cover(title, subtitle):
    return [
        Spacer(1, 4 * cm),
        Paragraph('<font color="#C2E000">feat.</font> creators', ParagraphStyle('Brand', parent=H1, fontSize=18)),
        Spacer(1, 2 * cm),
        Paragraph(title, COVER_TITLE),
        Paragraph(subtitle, COVER_SUB),
        Spacer(1, 8 * cm),
        Paragraph(f'Versão: 1.0 · Maio 2026', NOTE),
        PageBreak(),
    ]


def section(title, *content):
    out = [Paragraph(title, H2)]
    out.extend(content)
    return out


def subsection(title, *content):
    out = [Paragraph(title, H3)]
    out.extend(content)
    return out


def p(text):
    return Paragraph(text, BODY)


def code(text):
    text = text.replace('<', '&lt;').replace('>', '&gt;').replace('\n', '<br/>')
    return Paragraph(text, CODE)


def warn(text):
    return Paragraph(f'<b>!</b> {text}', WARN)


def ok(text):
    return Paragraph(f'<b>OK</b> {text}', OK)


def bullets(items):
    return ListFlowable(
        [ListItem(Paragraph(i, BODY), leftIndent=12) for i in items],
        bulletType='bullet', bulletColor=LIME, leftIndent=14, spaceAfter=6,
    )


def numbered(items):
    return ListFlowable(
        [ListItem(Paragraph(i, BODY), leftIndent=12) for i in items],
        bulletType='1', leftIndent=14, spaceAfter=6,
    )


def kv_table(rows):
    t = Table(rows, colWidths=[5 * cm, 11 * cm])
    t.setStyle(TableStyle([
        ('BACKGROUND', (0, 0), (0, -1), LIGHT),
        ('TEXTCOLOR', (0, 0), (-1, -1), INK),
        ('FONTNAME', (0, 0), (-1, -1), 'Helvetica'),
        ('FONTSIZE', (0, 0), (-1, -1), 9),
        ('VALIGN', (0, 0), (-1, -1), 'TOP'),
        ('LEFTPADDING', (0, 0), (-1, -1), 6),
        ('RIGHTPADDING', (0, 0), (-1, -1), 6),
        ('TOPPADDING', (0, 0), (-1, -1), 6),
        ('BOTTOMPADDING', (0, 0), (-1, -1), 6),
        ('GRID', (0, 0), (-1, -1), 0.3, GRAY),
    ]))
    return t


def header_footer(canvas, doc):
    canvas.saveState()
    canvas.setFillColor(GRAY)
    canvas.setFont('Helvetica', 8)
    canvas.drawString(2 * cm, 1 * cm, 'HUB feat. creators')
    canvas.drawRightString(A4[0] - 2 * cm, 1 * cm, f'Pág. {doc.page}')
    canvas.setStrokeColor(LIME)
    canvas.setLineWidth(2)
    canvas.line(2 * cm, A4[1] - 1.5 * cm, 6 * cm, A4[1] - 1.5 * cm)
    canvas.restoreState()


# ============================ PDF 1: TUTORIAL COMPLETO ============================

def build_tutorial():
    story = []
    story.extend(cover(
        'Tutorial Completo',
        'Guia funcional tela a tela do HUB feat. creators'
    ))

    # ============= SUMÁRIO =============
    story.append(Paragraph('Sumário', H1))
    story.append(bullets([
        '1. Visão geral do sistema',
        '2. Conceitos fundamentais (multi-tenant, ADM, perfis)',
        '3. Autenticação (signup, login, MFA, recuperação)',
        '4. Painel ADM (plataforma)',
        '5. App principal (assessoria)',
        '6. Portal do creator',
        '7. Mobile (Expo)',
        '8. Atalhos e produtividade',
    ]))
    story.append(PageBreak())

    # ============= 1. VISÃO GERAL =============
    story.extend(section('1. Visão Geral'))
    story.append(p(
        'HUB feat. creators é um SaaS multi-tenant para assessorias de influenciadores digitais. '
        'Centraliza prospecção de marcas, cadastros de influenciadores, tarefas, envio de e-mails, '
        'notificações via WhatsApp e portal externo para creators.'
    ))
    story.append(p('Três tipos de aplicação compõem o sistema:'))
    story.append(kv_table([
        ['Web (Next.js)', 'Painel operacional + admin de plataforma + portal externo de creators.'],
        ['API (Spring Boot)', 'Backend Java 21, PostgreSQL 16, pgvector, Flyway, JWT.'],
        ['Mobile (Expo)', 'App do usuário final (creator) para Android e iOS — opcional.'],
    ]))
    story.append(Spacer(1, 0.3 * cm))

    story.extend(subsection('Módulos principais'))
    story.append(bullets([
        '<b>Cadastros</b> — influenciadores, marcas, contatos.',
        '<b>Prospecção</b> — kanban marca↔influenciador com state machine.',
        '<b>Tarefas</b> — agenda, digest matinal, alertas in-app.',
        '<b>E-mail outbound</b> — múltiplas contas SMTP, templates Mustache, tracking.',
        '<b>WhatsApp</b> — Cloud API oficial Meta, templates HSM, janela 24h.',
        '<b>Notificações</b> — SSE in-app, Web Push, digest diário.',
        '<b>Histórico</b> — timeline append-only por entidade.',
        '<b>Importação</b> — CSV/XLSX em massa com validação.',
        '<b>Relatórios</b> — funil, performance, SLA, CSV export.',
        '<b>Portal Creator</b> — área externa para influenciadores aprovarem entregas.',
        '<b>Match IA</b> — sugestão marca↔creator via embeddings (pgvector).',
        '<b>Compliance LGPD</b> — base legal, DSR, retenção, PII masking.',
    ]))
    story.append(PageBreak())

    # ============= 2. CONCEITOS =============
    story.extend(section('2. Conceitos Fundamentais'))

    story.extend(subsection('Multi-tenancy por assessoria'))
    story.append(p(
        'Cada assessoria é um tenant isolado. Todos os registros carregam coluna <b>assessoria_id</b> '
        'e são filtrados automaticamente pelo <i>TenantAspect</i>. Usuários INTERNOS só veem dados '
        'da própria assessoria.'
    ))

    story.extend(subsection('Tipos de usuário'))
    story.append(kv_table([
        ['ADM', 'Super-administrador da plataforma. Sem assessoria. Bypass do filtro multi-tenant. Permissão ADMN implícita. Configura integrações globais (SMTP, WhatsApp, etc).'],
        ['INTERNO', 'Usuário de assessoria. Possui assessoria_id obrigatório. Permissões dadas via perfil RBAC. Exemplo de role: OWNR, MNGR, ASSR.'],
        ['CREATOR', 'Influenciador acessando o portal externo. Token tipo=CREATOR. Não acessa app principal.'],
    ]))

    story.extend(subsection('Perfis e permissões (RBAC 4-letter)'))
    story.append(p(
        'Permissões são códigos de 4 letras. Cada perfil agrega N permissões. Cada usuário tem 1 perfil. '
        'Mudança de perfil só propaga no próximo refresh JWT (até 60 min).'
    ))
    story.append(p('Exemplos de códigos:'))
    story.append(kv_table([
        ['INFR / INFW', 'Influenciadores: leitura / escrita.'],
        ['MRCR / MRCW', 'Marcas: leitura / escrita.'],
        ['PRSR / PRSW', 'Prospecção: leitura / escrita.'],
        ['TRFR / TRFW', 'Tarefas: leitura / escrita.'],
        ['EMLR / EMLW / EMLS', 'E-mail: leitura / escrita / envio.'],
        ['WPPR / WPPW / WPPS', 'WhatsApp: leitura / escrita / envio.'],
        ['MTCR / MTCW', 'Match IA: leitura / escrita.'],
        ['BHIS', 'Histórico (timeline) — leitura.'],
        ['ADMN', 'Admin de plataforma — implícito para tipo=ADM.'],
        ['EXPT', 'Exportação CSV de relatórios.'],
        ['OWNR', 'Dono da assessoria — superperfil interno.'],
    ]))
    story.append(PageBreak())

    # ============= 3. AUTENTICAÇÃO =============
    story.extend(section('3. Autenticação'))

    story.extend(subsection('3.1 Signup (/signup)'))
    story.append(p('Tela de cadastro inicial. Comportamentos:'))
    story.append(bullets([
        '<b>Sistema vazio</b> — primeiro signup vira ADM automaticamente. Não pede nome de assessoria.',
        '<b>Sistema com ADM</b> — signup público desativado; só convite por OWNER.',
        'Senha mínima 12 caracteres, hash Argon2id (t=3, m=64MB).',
        'E-mail precisa verificação por token enviado por SMTP (24h).',
        'Convite marca e-mail verificado automaticamente (convidado recebeu o e-mail).',
    ]))

    story.extend(subsection('3.2 Login (/login)'))
    story.append(p('Fluxo:'))
    story.append(numbered([
        'Usuário entra e-mail + senha.',
        'Backend valida hash Argon2id.',
        'Se MFA ativo → pede código TOTP (6 dígitos).',
        'Se ADM sem MFA → retorna tempToken + flag mfaSetupRequired (forçar setup).',
        'Sucesso → JWT access (60min INTERNO / 24h ADM) + refresh token.',
    ]))
    story.append(warn(
        '5 falhas em 15 min → bloqueio de 30 min (LoginLockout por e-mail). '
        'Reseta em login bem-sucedido.'
    ))

    story.extend(subsection('3.3 Recuperação de senha'))
    story.append(bullets([
        '<b>/forgot-password</b> — usuário entra e-mail → recebe link com token (1h).',
        '<b>/reset-password?token=...</b> — define nova senha.',
        'Token single-use. Reuso retorna 404 (não vaza motivo).',
    ]))

    story.extend(subsection('3.4 Verificação de e-mail'))
    story.append(p(
        '/verify-email?token=... — confirma o e-mail. '
        'Login bloqueado até <b>email_verificado_em</b> ser preenchido.'
    ))

    story.extend(subsection('3.5 Aceitar convite'))
    story.append(p(
        '/convite/[token] — convidado define senha e ativa a conta. '
        'Token válido por 7 dias. Convite marca e-mail como verificado.'
    ))
    story.append(PageBreak())

    # ============= 4. PAINEL ADM =============
    story.extend(section('4. Painel ADM — /admin'))
    story.append(p(
        'Acessível apenas para tipo=ADM. Layout próprio (sidebar vermelha com badge ADMIN). '
        'Bypass automático do filtro multi-tenant — ADM vê dados de todas assessorias.'
    ))
    story.append(warn(
        'Todo acesso de ADM a dados de outra assessoria é registrado em '
        '<b>admin_audit_log</b> (append-only via trigger BEFORE UPDATE/DELETE).'
    ))

    story.extend(subsection('4.1 Dashboard (/admin)'))
    story.append(p('4 cards de KPI da plataforma:'))
    story.append(bullets([
        'Total de assessorias ativas',
        'Total de usuários (ADM + INTERNO)',
        'Total de feature flags',
        'Entradas no audit log (últimas 24h)',
    ]))

    story.extend(subsection('4.2 Assessorias (/admin/assessorias)'))
    story.append(p('Lista paginada (20/pág) de todas assessorias. Ações:'))
    story.append(bullets([
        '<b>Ver</b> — drill-down em /admin/assessorias/[id]: edita nome, slug, plano.',
        '<b>Anonimizar</b> — POST /api/v1/admin/assessorias/[id]/anonymize: substitui PII por hashes. Não reverte.',
        'Plano atual: TRIAL, BASIC, PRO, ENTERPRISE (free-tier MVP).',
    ]))

    story.extend(subsection('4.3 Usuários (/admin/usuarios)'))
    story.append(p('Lista paginada (50/pág) de todos usuários da plataforma. Mostra:'))
    story.append(bullets([
        'E-mail, tipo (ADM/INTERNO), role, status (ATIVO/INATIVO), assessoria.',
        'Ação <b>PUT /admin/usuarios/[id]/status</b> — ativa ou desativa conta.',
    ]))

    story.extend(subsection('4.4 Feature Flags (/admin/feature-flags)'))
    story.append(p('Toggle de flags runtime sem deploy. 8 flags semeadas:'))
    story.append(kv_table([
        ['FEATURE_PORTAL_ENABLED', 'Liga portal externo de creators.'],
        ['FEATURE_MOBILE_ENABLED', 'Liga envio de push para app Expo.'],
        ['FEATURE_WHATSAPP_ENABLED', 'Liga módulo WhatsApp Cloud API.'],
        ['FEATURE_COMPLIANCE_STRICT', 'Exige base_legal em todo cadastro de PII.'],
        ['FEATURE_MATCH_IA', 'Liga tab de Match IA na sheet de prospecção.'],
        ['FEATURE_IMPORT_BULK', 'Liga importação CSV/XLSX.'],
        ['FEATURE_RELATORIOS', 'Liga módulo de relatórios.'],
        ['FEATURE_SOCIAL_OAUTH', 'Liga ingestão Instagram/YouTube/TikTok.'],
    ]))
    story.append(Paragraph(
        'Cache Caffeine 30s TTL. Fallback em env var se DB falhar.', NOTE,
    ))

    story.extend(subsection('4.5 Audit Log (/admin/audit)'))
    story.append(p('Trilha imutável de ações administrativas (paginada 50/pág, filtros admin/ação/assessoria):'))
    story.append(bullets([
        '<b>SIGNUP_ADM</b> — primeiro signup vira ADM',
        '<b>TENANT_BYPASS</b> — ADM acessou dados de outra assessoria',
        '<b>FLAG_TOGGLE</b> — mudança em feature flag',
        '<b>ASSESSORIA_UPDATE / ANONYMIZE</b>',
        '<b>USUARIO_STATUS</b> — ativação/inativação',
        '<b>INTEGRATION_UPDATE</b> — credenciais SMTP/WhatsApp atualizadas',
    ]))
    story.append(PageBreak())

    # ============= 5. APP PRINCIPAL =============
    story.extend(section('5. App Principal — assessoria'))
    story.append(p(
        'Layout AppShell: sidebar colapsável à esquerda + topbar com avatar/notificações + '
        'Cmd+K (paleta de comandos). Acessível a usuários tipo=INTERNO.'
    ))

    story.extend(subsection('5.1 Dashboard (/)'))
    story.append(p('Página inicial. Conteúdo:'))
    story.append(bullets([
        '4 KPI cards: prospecções ativas, fechadas no mês, tarefas pendentes, e-mails enviados (7d).',
        'Funil chart (Recharts): contagem por status de prospecção.',
        'Próximas tarefas (top 5 vencendo).',
        'Notificações recentes (drawer lateral).',
    ]))

    story.extend(subsection('5.2 Influenciadores (/influenciadores)'))
    story.append(p('Listagem em cards/tabela com filtros (nicho, tag, busca por nome). Ações:'))
    story.append(bullets([
        '<b>+ Novo</b> (Cmd+K ?new=1) — modal com campos: nome, handles (IG/TikTok/YT), nicho, tags, observações, base_legal, assessor_responsavel_id.',
        '<b>Clicar no card</b> — drawer lateral com tabs: Dados, Contatos, Histórico, Tarefas, Match IA.',
        '<b>Editar</b> — abre modal pré-preenchido.',
        '<b>Soft-delete</b> — preenche deleted_at. Job de retenção anonimiza após 180 dias (LGPD).',
        'Cada handle (instagram/tiktok/youtube) é único por assessoria.',
    ]))

    story.extend(subsection('5.3 Marcas (/marcas)'))
    story.append(p('Estrutura idêntica a Influenciadores. Campos: nome (único na assessoria), segmento, site, tags, observações, base_legal.'))

    story.extend(subsection('5.4 Prospecção (/prospeccao)'))
    story.append(p('Tab principal: <b>Kanban</b> com colunas por status. Drag-and-drop via @dnd-kit:'))
    story.append(kv_table([
        ['IDEIA', 'Início. Apenas marca e influenciador escolhidos.'],
        ['CONTATO', 'Primeiro contato feito.'],
        ['EM_NEGOCIACAO', 'Briefing recebido, valores em discussão.'],
        ['PROPOSTA_ENVIADA', 'Proposta formal enviada.'],
        ['FECHADA_GANHA', 'Cliente fechou. Terminal.'],
        ['FECHADA_PERDIDA', 'Cliente recusou. Terminal — exige motivo.'],
    ]))
    story.append(p('Transições obrigatórias seguem state machine (ProspeccaoStateMachine.java). Transição inválida → 422.'))
    story.append(p('Drawer lateral ao clicar em card:'))
    story.append(bullets([
        '<b>Dados</b> — título, marca, influenciador, valor estimado, próxima ação.',
        '<b>Histórico</b> — eventos automáticos (status change, comentário, criação).',
        '<b>Tarefas</b> — relacionadas a essa prospecção.',
        '<b>E-mails</b> — disparados no contexto.',
        '<b>Match IA</b> — sugestões de creator por marca via pgvector.',
    ]))
    story.append(p('Tab alternativa: <b>Lista</b> — tabela paginada com filtros (status, assessor, marca, busca).'))
    story.append(warn(
        'Visibilidade ASSESSOR vs OWNER: ASSESSOR só vê prospecções onde é created_by OU assessor_responsavel_id. OWNER vê tudo da assessoria.'
    ))

    story.extend(subsection('5.5 Tarefas (/tarefas)'))
    story.append(p('Duas views:'))
    story.append(bullets([
        '<b>Lista</b> — tabela com filtros (status, prioridade, assessor, prazo).',
        '<b>Agenda semanal</b> — calendário de 7 dias com cards arrastáveis.',
    ]))
    story.append(p('Status válidos: TODO, EM_ANDAMENTO, FEITA, CANCELADA.'))
    story.append(p('Drawer detalhe: descrição, prazo (default 23:59 do dia), checklist, vínculo a prospecção/influenciador/marca, comentários.'))
    story.append(p('Digest diário 07:00 BRT — envia e-mail com tarefas vencendo + atrasadas.'))

    story.extend(subsection('5.6 E-mail (/email)'))
    story.append(p('4 tabs:'))
    story.append(kv_table([
        ['Envios', 'Histórico de e-mails enviados. Status: PENDENTE, ENVIADO, FALHADO, ABERTO, CLICADO.'],
        ['Templates', 'CRUD de templates Mustache. Variáveis: {{influenciador.nome}}, {{marca.nome}}, etc. Preview HTML.'],
        ['Contas SMTP', 'Múltiplas contas por assessoria. Credenciais cifradas AES-GCM. Teste de conexão antes de salvar.'],
        ['Layout', 'Editor HTML do cabeçalho/rodapé padrão da assessoria.'],
    ]))
    story.append(p('Botão <b>Novo envio</b> abre modal: escolhe template, destinatário (influenciador/marca/contato), conta SMTP, agendamento.'))
    story.append(warn(
        'Circuit breaker: 3 falhas de auth em 10min → conta marcada FALHA_AUTH. Reseta ao testar conexão com sucesso.'
    ))

    story.extend(subsection('5.7 WhatsApp (/whatsapp)'))
    story.append(p('Tabs:'))
    story.append(bullets([
        '<b>Contas</b> — cadastro de conta Cloud API (phone_number_id, access_token, app_secret). Tokens cifrados AES-GCM.',
        '<b>Templates HSM</b> — submete templates para aprovação Meta. Polling a cada 15min.',
        '<b>Envios</b> — fila assíncrona. Idempotência via UNIQUE key.',
        '<b>Opt-outs</b> — números que enviaram "parar/sair/stop". Bloqueio perpétuo.',
    ]))
    story.append(warn('Mensagens FREEFORM só dentro da janela 24h após inbound do usuário. Fora dela → 422 JANELA_FECHADA. Template HSM sempre permitido se aprovado.'))

    story.extend(subsection('5.8 Importação (/importacao)'))
    story.append(p('Wizard de 4 passos:'))
    story.append(numbered([
        'Selecionar tipo (influenciadores, marcas, contatos).',
        'Upload do arquivo CSV/XLSX (templates disponíveis para download).',
        'Dry-run — preview de validação (CPF/CNPJ/phone). Mostra erros por linha.',
        'Confirmar — processa em background. SSE de progresso em tempo real.',
    ]))

    story.extend(subsection('5.9 Relatórios (/relatorios)'))
    story.append(p('Cards de relatórios disponíveis:'))
    story.append(bullets([
        '<b>Funil de prospecção</b> — contagem + % por status.',
        '<b>Performance por assessor</b> — fechadas/perdidas + ticket médio.',
        '<b>SLA de tarefas</b> — % no prazo, atrasadas, comparativo período anterior.',
    ]))
    story.append(p('Cada relatório aceita período (last7d / last30d / last90d / custom). Botão <b>Exportar CSV</b> (permissão EXPT).'))
    story.append(p('MVs (materialized views) atualizam diariamente 03:00 BRT.'))

    story.extend(subsection('5.10 Perfis (/perfis)'))
    story.append(p('CRUD de perfis RBAC. Lista perfis existentes + botão <b>+ Novo perfil</b>.'))
    story.append(p('Modal: nome do perfil + checkboxes agrupados por entidade (Influenciador / Marca / Prospecção / etc).'))

    story.extend(subsection('5.11 Membros (/membros)'))
    story.append(p('Gestão de usuários da assessoria. Ações:'))
    story.append(bullets([
        '<b>+ Convidar</b> — informa e-mail + perfil. Envia link válido por 7 dias.',
        'Lista de membros: status (ATIVO/INATIVO/PENDENTE), MFA, último login.',
        'Editar perfil de um membro. Trocar perfil propaga em até 60min (próximo refresh JWT).',
    ]))
    story.append(PageBreak())

    # ============= 6. PORTAL CREATOR =============
    story.extend(section('6. Portal do Creator — /[slug]/...'))
    story.append(p(
        'Portal externo de assessoria. URL: <b>http://app/[slug]/login</b> onde slug é o identificador da assessoria. '
        'Token JWT tipo=CREATOR (24h). Feature flag FEATURE_PORTAL_ENABLED deve estar true.'
    ))
    story.append(bullets([
        '<b>/[slug]/login</b> — creator entra com e-mail e senha. Branding por assessoria (logo, cores).',
        '<b>/[slug]/convite</b> — recebe convite por e-mail e define senha (1 conta por influenciador).',
        '<b>/[slug]/tarefas</b> — lista de tarefas/entregáveis atribuídos.',
        '<b>/[slug]/tarefas/[id]</b> — detalhe: descrição, prazo, anexar arquivo, comentar.',
    ]))
    story.append(warn(
        'Comentários externos do creator aparecem na timeline interna marcados como "via portal".'
    ))
    story.append(PageBreak())

    # ============= 7. MOBILE =============
    story.extend(section('7. Mobile (Expo)'))
    story.append(p(
        'App Expo SDK 51 (Android + iOS). Apenas para creators. '
        'Autenticação via SecureStore (Keychain/Keystore). Push via FCM/APNs (ExpoPushSender).'
    ))
    story.append(bullets([
        'Login com biometria opcional (toggle nas configurações).',
        'Home: tarefas pendentes + notificações.',
        'Detalhe da tarefa: upload de entregável com retry automático.',
        'Cache offline (Zustand + AsyncStorage).',
    ]))
    story.append(PageBreak())

    # ============= 8. ATALHOS =============
    story.extend(section('8. Atalhos e Produtividade'))
    story.append(kv_table([
        ['Cmd+K (Ctrl+K)', 'Abre paleta global de comandos.'],
        ['Cmd+K + "novo"', 'Cria entidade no contexto atual (?new=1 na URL).'],
        ['/', 'Foca o campo de busca da lista atual.'],
        ['Esc', 'Fecha modal/drawer.'],
        ['?', 'Mostra ajuda contextual (quando disponível).'],
    ]))

    story.extend(subsection('Padrões visuais'))
    story.append(bullets([
        'Cor primária: lime <font face="Courier">#C2E000</font>',
        'Cor de texto: ink <font face="Courier">#141414</font>',
        'Toggle dark/light via topbar (auto-detect inicial).',
        'Toasts no canto inferior direito (sucesso lime, erro vermelho).',
    ]))

    return story


# ============================ PDF 2: GUIA DE TESTE ============================

def build_guia_teste():
    story = []
    story.extend(cover(
        'Guia de Teste',
        'Roteiro passo a passo: do setup do ADM ao teste de cada tela'
    ))

    # ============= SUMÁRIO =============
    story.append(Paragraph('Sumário', H1))
    story.append(bullets([
        'Pré-requisitos',
        'Fase 1 — Setup do ADM (primeiro signup)',
        'Fase 2 — Configurar conta SMTP global',
        'Fase 3 — Configurar WhatsApp Cloud API (opcional)',
        'Fase 4 — Feature flags + integrações',
        'Fase 5 — Criar primeira assessoria + OWNER',
        'Fase 6 — Login do OWNER + setup inicial (perfis, membros)',
        'Fase 7 — Teste minucioso por tela',
        'Anexo — Critérios de aceitação por funcionalidade',
    ]))
    story.append(PageBreak())

    # ============= PRÉ-REQUISITOS =============
    story.extend(section('Pré-requisitos'))
    story.append(p('Antes de começar, confirme:'))
    story.append(bullets([
        'Docker Desktop rodando + container <font face="Courier">hub-postgres</font> ativo (porta 5432).',
        'Backend rodando em <font face="Courier">http://localhost:8080</font> (verificar com <font face="Courier">curl http://localhost:8080/actuator/health</font>).',
        'Frontend rodando em <font face="Courier">http://localhost:3000</font> (<font face="Courier">cd apps/web && pnpm dev</font>).',
        'Banco recém-resetado (apenas migrations Flyway aplicadas, tabelas vazias).',
    ]))
    story.append(p('Comandos úteis:'))
    story.append(code(
        '# Subir Postgres\n'
        'docker start hub-postgres\n\n'
        '# Backend (apps/api)\n'
        'SPRING_PROFILES_ACTIVE=dev DATABASE_URL=jdbc:postgresql://localhost:5432/hub_feat_creators \\\n'
        '  DATABASE_USERNAME=hub_app DATABASE_PASSWORD=changeme ./mvnw spring-boot:run\n\n'
        '# Frontend (apps/web)\n'
        'pnpm dev'
    ))
    story.append(warn('Se a porta 8080 já estiver em uso, mate o processo anterior antes de subir.'))
    story.append(PageBreak())

    # ============= FASE 1: SETUP ADM =============
    story.extend(section('Fase 1 — Setup do ADM'))
    story.append(p(
        'Em sistema vazio (tabela <font face="Courier">usuarios</font> sem nenhuma linha), o primeiro signup '
        'é automaticamente promovido a ADM. Isso permite onboarding antes de existir qualquer assessoria.'
    ))

    story.extend(subsection('1.1 Verificar que o sistema está vazio'))
    story.append(code(
        'docker exec hub-postgres psql -U hub_app -d hub_feat_creators \\\n'
        '  -c "SELECT count(*) FROM usuarios WHERE deleted_at IS NULL;"'
    ))
    story.append(ok('Resultado esperado: count = 0.'))

    story.extend(subsection('1.2 Cadastro do ADM'))
    story.append(p('Acessar <b>http://localhost:3000/signup</b>. Preencher:'))
    story.append(kv_table([
        ['E-mail', 'seu e-mail real (válido para receber verificação)'],
        ['Senha', 'mínimo 12 caracteres, mix de letras/números/símbolos'],
        ['Nome', 'Seu nome completo'],
    ]))
    story.append(p('Clicar em <b>Criar conta</b>.'))
    story.append(ok('Resposta esperada: redireciona para /login com toast "Conta criada. Verifique seu e-mail."'))
    story.append(p('Resposta da API contém <font face="Courier">"isAdm": true</font> — confirma criação como ADM.'))

    story.extend(subsection('1.3 Verificar criação no DB'))
    story.append(code(
        'docker exec hub-postgres psql -U hub_app -d hub_feat_creators \\\n'
        '  -c "SELECT email, tipo, assessoria_id, email_verificado_em FROM usuarios;"'
    ))
    story.append(ok('Esperado: 1 linha com tipo=ADM, assessoria_id=NULL, email_verificado_em=NULL.'))

    story.extend(subsection('1.4 Verificar e-mail'))
    story.append(p(
        'O sistema dispara e-mail com link <b>/verify-email?token=...</b>. '
        'Em dev (sem SMTP real configurado), o token aparece no log do backend.'
    ))
    story.append(p('Como capturar o token em dev:'))
    story.append(code(
        '# Procurar no log do backend\n'
        'grep "onboarding.verify.sent" backend.log\n\n'
        '# OU diretamente no DB\n'
        'docker exec hub-postgres psql -U hub_app -d hub_feat_creators \\\n'
        '  -c "SELECT token FROM email_verify_tokens ORDER BY created_at DESC LIMIT 1;"'
    ))
    story.append(p('Acessar <b>http://localhost:3000/verify-email?token=&lt;TOKEN&gt;</b>.'))
    story.append(ok('Esperado: toast "E-mail verificado" e redirect para /login.'))

    story.extend(subsection('1.5 Primeiro login do ADM'))
    story.append(p('Acessar <b>/login</b> e entrar com e-mail + senha.'))
    story.append(warn(
        'Como ADM ainda não configurou MFA, login retorna <b>mfaSetupRequired: true</b>. '
        'Frontend deve redirecionar para tela de configuração de MFA TOTP.'
    ))
    story.append(p('Na tela de MFA:'))
    story.append(numbered([
        'Sistema gera secret TOTP. Mostra QR code.',
        'Escanear com Google Authenticator / Authy / 1Password.',
        'Digitar código de 6 dígitos para confirmar.',
        'Backend cifra o secret (AES-GCM com MFA_KEY) e habilita MFA.',
        'Retorna token JWT real (24h, tipo=ADM).',
    ]))
    story.append(ok('Esperado: redirect para /admin (painel ADM).'))

    story.extend(subsection('1.6 Confirmar identidade ADM no browser'))
    story.append(p('No console do browser (F12):'))
    story.append(code(
        'JSON.parse(atob(localStorage.getItem("accessToken").split(".")[1]))'
    ))
    story.append(ok('Deve mostrar: <font face="Courier">{ tipo: "ADM", role: "ADM", usuarioId: "..." }</font> (sem <font face="Courier">ass</font>).'))
    story.append(PageBreak())

    # ============= FASE 2: SMTP =============
    story.extend(section('Fase 2 — Configurar Conta SMTP Global'))
    story.append(p(
        'Antes de outros usuários poderem se cadastrar/receber convites, o ADM precisa configurar '
        'uma conta SMTP global que o sistema usará para enviar e-mails transacionais (verify, reset, convite).'
    ))
    story.append(warn(
        'OBS: no estado atual do código, a configuração SMTP é por assessoria (PRD-004). Para o ADM '
        'configurar uma conta GLOBAL de plataforma, será necessário adicionar tela em /admin/integrations/email '
        '(planejada — não implementada no MVP).'
    ))

    story.extend(subsection('Alternativa MVP — usar SMTP via .env'))
    story.append(p('No arquivo <font face="Courier">apps/api/.env</font> (não commitar):'))
    story.append(code(
        'SPRING_MAIL_HOST=smtp.gmail.com\n'
        'SPRING_MAIL_PORT=587\n'
        'SPRING_MAIL_USERNAME=seu-email@gmail.com\n'
        'SPRING_MAIL_PASSWORD=app-password-16-chars\n'
        'SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true\n'
        'SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true'
    ))
    story.append(warn(
        'Gmail: use App Password (não a senha normal). Habilite em '
        'https://myaccount.google.com/apppasswords. Conta precisa ter 2FA.'
    ))
    story.append(p('Reiniciar backend para aplicar.'))

    story.extend(subsection('Validar envio'))
    story.append(p('Disparar um e-mail de teste — pode ser via convite (fase 5). Verificar caixa de entrada do destinatário.'))
    story.append(p('Se falhar, log do backend mostra: <font face="Courier">email.send.fail motivo=...</font>'))
    story.append(PageBreak())

    # ============= FASE 3: WHATSAPP =============
    story.extend(section('Fase 3 — Configurar WhatsApp (Opcional)'))
    story.append(p('Pré-requisitos do lado Meta:'))
    story.append(numbered([
        'Conta business em business.facebook.com.',
        'App configurado no Meta for Developers (https://developers.facebook.com).',
        'Produto WhatsApp adicionado. Número verificado.',
        'Token de acesso permanente (não expira) ou de longa duração.',
        'App Secret (Settings → Basic).',
        'Phone Number ID (WhatsApp → API Setup).',
        'Verify Token customizado para webhook.',
    ]))

    story.extend(subsection('Configuração no HUB (futura, por assessoria)'))
    story.append(p('Tela <b>/whatsapp → Contas → + Nova conta</b>:'))
    story.append(kv_table([
        ['Display name', 'Nome amigável (ex: "WhatsApp Marketing")'],
        ['Phone number', '+55 11 9XXXX-XXXX'],
        ['Phone number ID', 'do Meta API Setup'],
        ['WABA ID', 'WhatsApp Business Account ID'],
        ['Access token', 'token permanente'],
        ['App secret', 'da app no Meta'],
        ['Verify token', 'string aleatória que você escolhe'],
    ]))
    story.append(p('Webhook URL para configurar no Meta:'))
    story.append(code('https://&lt;seu-domínio&gt;/api/v1/whatsapp/webhook'))
    story.append(p('Eventos: <b>messages</b>, <b>message_template_status_update</b>.'))
    story.append(warn(
        'Em dev local, exponha com ngrok: <font face="Courier">ngrok http 8080</font> → use a URL HTTPS gerada.'
    ))
    story.append(PageBreak())

    # ============= FASE 4: FEATURE FLAGS =============
    story.extend(section('Fase 4 — Feature Flags e Integrações'))
    story.append(p('Como ADM, acessar <b>/admin/feature-flags</b>. Para o teste completo, ativar:'))
    story.append(kv_table([
        ['FEATURE_PORTAL_ENABLED', 'true — habilita portal de creators'],
        ['FEATURE_WHATSAPP_ENABLED', 'true — se SMTP/WhatsApp configurados'],
        ['FEATURE_COMPLIANCE_STRICT', 'true (default) — força base_legal'],
        ['FEATURE_MATCH_IA', 'true se ai-worker estiver de pé'],
        ['FEATURE_IMPORT_BULK', 'true'],
        ['FEATURE_RELATORIOS', 'true'],
        ['FEATURE_MOBILE_ENABLED', 'false (a menos que esteja testando mobile)'],
        ['FEATURE_SOCIAL_OAUTH', 'false (requer apps OAuth configuradas)'],
    ]))
    story.append(p('Cada toggle gera entrada em <font face="Courier">admin_audit_log</font>.'))
    story.append(PageBreak())

    # ============= FASE 5: CRIAR ASSESSORIA + OWNER =============
    story.extend(section('Fase 5 — Criar Primeira Assessoria + OWNER'))
    story.append(p('Como ADM no painel:'))

    story.extend(subsection('5.1 Criar assessoria'))
    story.append(p('<b>/admin/assessorias → + Nova</b> (ou via API):'))
    story.append(code(
        'POST /api/v1/admin/assessorias\n'
        'Authorization: Bearer &lt;ADM_TOKEN&gt;\n'
        'Content-Type: application/json\n\n'
        '{ "nome": "Assessoria Teste", "slug": "teste", "plano": "TRIAL" }'
    ))
    story.append(ok('Resposta: 201 com { id, nome, slug, plano, createdAt }.'))

    story.extend(subsection('5.2 Criar OWNER da assessoria'))
    story.append(p('Via API (admin endpoint):'))
    story.append(code(
        'POST /api/v1/admin/usuarios\n'
        'Authorization: Bearer &lt;ADM_TOKEN&gt;\n\n'
        '{\n'
        '  "email": "owner@teste.com",\n'
        '  "nome": "Owner Teste",\n'
        '  "tipo": "INTERNO",\n'
        '  "assessoriaId": "&lt;ASSESSORIA_ID&gt;",\n'
        '  "role": "OWNR"\n'
        '}'
    ))
    story.append(p('Sistema envia convite com link <b>/convite/[token]</b>. Validade: 7 dias.'))

    story.extend(subsection('5.3 OWNER aceita convite'))
    story.append(p('Acessar link recebido por e-mail. Definir senha (12+ chars). E-mail é marcado verificado automaticamente.'))
    story.append(ok('Após criar senha → redirect para /login. Login funciona normalmente.'))
    story.append(PageBreak())

    # ============= FASE 6: SETUP OWNER =============
    story.extend(section('Fase 6 — Setup Inicial do OWNER'))
    story.append(p('OWNER faz login e acessa o app principal (não o painel admin).'))

    story.extend(subsection('6.1 Criar perfis RBAC'))
    story.append(p('Em <b>/perfis</b>:'))
    story.append(numbered([
        'Clicar em <b>+ Novo perfil</b>.',
        'Nome: "Assessor Pleno".',
        'Permissões marcadas: INFR, INFW, MRCR, MRCW, PRSR, PRSW, TRFR, TRFW, EMLR, EMLW, BHIS.',
        'Salvar.',
    ]))
    story.append(p('Criar segundo perfil "Assessor Junior" só com READ (INFR, MRCR, PRSR, TRFR).'))

    story.extend(subsection('6.2 Convidar membros'))
    story.append(p('Em <b>/membros → + Convidar</b>:'))
    story.append(bullets([
        'E-mail do novo membro',
        'Perfil escolhido (dropdown com os criados acima)',
    ]))
    story.append(p('Repetir para criar pelo menos 2 assessores de perfis diferentes para teste de visibilidade.'))
    story.append(PageBreak())

    # ============= FASE 7: TESTE POR TELA =============
    story.extend(section('Fase 7 — Teste Minucioso por Tela'))
    story.append(p('Para cada tela abaixo, percorrer todos os critérios listados. Marcar PASS/FAIL.'))

    story.extend(subsection('7.1 Dashboard (/)'))
    story.append(bullets([
        'Carrega sem erro 500.',
        'KPIs mostram 0/zero em sistema novo.',
        'Funil chart renderiza (mesmo vazio).',
        'Notificações drawer abre e fecha (sino na topbar).',
    ]))

    story.extend(subsection('7.2 Influenciadores (/influenciadores)'))
    story.append(bullets([
        'Lista vazia mostra <b>EmptyState</b> com CTA "+ Novo".',
        'Modal de criação abre via Cmd+K ou botão.',
        'Validação client-side: nome obrigatório, handle único, base_legal obrigatório (se FEATURE_COMPLIANCE_STRICT).',
        'Salvar → entidade aparece na lista. Toast de sucesso.',
        'Clicar card abre drawer. Tab Histórico mostra evento INFLUENCIADOR_CRIADO.',
        'Editar nome → salvar → drawer atualiza. Timeline ganha evento INFLUENCIADOR_ATUALIZADO.',
        'Soft-delete (menu kebab → Excluir): some da lista. Timeline ganha evento INFLUENCIADOR_REMOVIDO.',
        'Filtros de nicho/tag funcionam.',
        'Busca por nome retorna match parcial (case-insensitive).',
        'Paginação funciona com 20+ registros.',
    ]))

    story.extend(subsection('7.3 Marcas (/marcas)'))
    story.append(p('Mesmos critérios de Influenciadores. Adicional:'))
    story.append(bullets([
        'Nome de marca é único na assessoria (tenta duplicar → erro 409).',
    ]))

    story.extend(subsection('7.4 Prospecção (/prospeccao)'))
    story.append(bullets([
        'Kanban renderiza 6 colunas (IDEIA → FECHADA_PERDIDA).',
        'Criar prospecção: modal com seleção marca + influenciador + título.',
        'Card aparece em IDEIA.',
        'Drag card de IDEIA → CONTATO funciona. Toast "Status alterado".',
        'Tentar drag CONTATO → FECHADA_GANHA pulando estados → erro 422 (transição inválida).',
        'Drag para FECHADA_PERDIDA abre modal obrigando motivo.',
        'Drawer detalhe: todas tabs carregam (Dados, Histórico, Tarefas, E-mails, Match IA se flag ativa).',
        'Histórico mostra evento PROSPECCAO_STATUS_CHANGE com de/para.',
        'Tab Match IA: se Influenciador NÃO selecionado, mostra sugestões. Score visível.',
        'Visibilidade ASSESSOR: logar como assessor1 → vê só prospecções dele. Logar como assessor2 → vê só dele.',
        'OWNER vê tudo.',
        'Lista (tab) tem filtros por status/assessor/marca/busca.',
    ]))

    story.extend(subsection('7.5 Tarefas (/tarefas)'))
    story.append(bullets([
        'Criar tarefa: modal com título, descrição, prazo, prioridade, responsável.',
        'Default de prazo só com data → 23:59 automaticamente.',
        'Card aparece na agenda no dia certo.',
        'Marcar como FEITA → status muda. Concluida_em registrado.',
        'Voltar para TODO → concluida_em limpa.',
        'Editar tarefa CANCELADA → 422 (terminal).',
        'Vincular tarefa a prospecção → drawer da prospecção mostra na tab Tarefas.',
        'Digest 07:00 BRT: aguardar ou forçar via JobRunner endpoint admin.',
    ]))

    story.extend(subsection('7.6 E-mail (/email)'))
    story.append(bullets([
        '<b>Contas SMTP</b>: criar conta. Teste de conexão antes de salvar (botão "Testar"). Sucesso = toast verde. Falha = erro detalhado.',
        '<b>Templates</b>: criar template Mustache. Preview render. Variáveis {{influenciador.nome}}, {{marca.nome}}.',
        '<b>Layout</b>: editor HTML salva cabeçalho/rodapé por assessoria.',
        '<b>Envios → Novo</b>: escolhe template + destinatário + conta. Confirma. Envio entra na fila.',
        'Status muda PENDENTE → ENVIADO. Tracking pixel registra ABERTO quando destinatário abrir.',
        'Falha 3x auth → conta vai para FALHA_AUTH. Testar conexão reseta.',
        'List-Unsubscribe header presente.',
    ]))

    story.extend(subsection('7.7 WhatsApp (/whatsapp)'))
    story.append(bullets([
        '<b>Contas → + Nova</b>: cadastrar com tokens reais (Meta).',
        '<b>Templates</b>: criar template HSM, submeter para aprovação. Status PENDENTE → APPROVED (até 48h).',
        '<b>Envios → Novo</b>: escolher número de destino + template aprovado. Envio entra na fila.',
        'Webhook Meta envia status: sent → delivered → read.',
        'Enviar mensagem FREEFORM fora da janela 24h → 422 JANELA_FECHADA.',
        'Enviar template aprovado funciona sempre.',
        'Reply do usuário com "parar" → adiciona em opt-out. Próximo envio para esse número → bloqueado.',
        'Idempotência: enviar 2x com mesma idempotency_key → segundo retorna envio existente.',
    ]))

    story.extend(subsection('7.8 Importação (/importacao)'))
    story.append(bullets([
        'Wizard passo 1: escolher tipo. Botão "Baixar template" funciona.',
        'Passo 2: upload arquivo. Validação de extensão (.csv ou .xlsx).',
        'Passo 3 (dry-run): mostra preview com erros por linha (CPF inválido, e-mail duplicado, etc).',
        'Passo 4: confirmar. SSE de progresso mostra X / Y processados em tempo real.',
        'Finaliza com summary: criados, atualizados, ignorados, erros.',
        'Erros são exportáveis em CSV para correção.',
    ]))

    story.extend(subsection('7.9 Relatórios (/relatorios)'))
    story.append(bullets([
        'Card "Funil de prospecção" carrega gráfico com contagem por status.',
        'Card "Performance por assessor" mostra ranking.',
        'Card "SLA de tarefas" mostra %.',
        'Filtro de período altera dados.',
        'Botão "Exportar CSV" baixa arquivo (apenas para perfis com EXPT).',
        'Para perfil sem EXPT, botão fica disabled ou esconde.',
    ]))

    story.extend(subsection('7.10 Perfis (/perfis)'))
    story.append(bullets([
        'Lista mostra perfis criados.',
        'Criar perfil: checkboxes agrupados por entidade.',
        'Editar perfil: muda permissões.',
        'Membros com esse perfil só veem mudança no próximo refresh JWT (até 60min).',
    ]))

    story.extend(subsection('7.11 Membros (/membros)'))
    story.append(bullets([
        'Lista mostra todos membros da assessoria + status.',
        'Convidar: e-mail + perfil. Link enviado por e-mail.',
        'Aceitar convite ativa conta.',
        'Editar perfil de membro existente.',
        'Inativar membro: status muda. Login bloqueado.',
    ]))

    story.extend(subsection('7.12 Portal do Creator'))
    story.append(p('Pré-requisito: FEATURE_PORTAL_ENABLED=true.'))
    story.append(bullets([
        'OWNER cria CreatorUser via influenciador (botão "Convidar creator" no drawer).',
        'Convite enviado por e-mail com link <b>/[slug]/convite?token=...</b>.',
        'Creator acessa, define senha.',
        'Login em <b>/[slug]/login</b> funciona com branding da assessoria.',
        'Tela de tarefas mostra apenas as marcadas visivel_para_creator=true.',
        'Upload de entregável funciona. Arquivo salva em volume local (ou S3 prod).',
        'Comentário do creator aparece na timeline interna marcado "via portal".',
    ]))

    story.extend(subsection('7.13 Painel ADM (volta como ADM)'))
    story.append(bullets([
        'Logout do OWNER → login com conta ADM → entrar em <b>/admin</b>.',
        'Dashboard mostra KPIs atualizados.',
        'Audit log lista todas as ações feitas até aqui.',
        'Filtrar audit log por adminId ou ação funciona.',
        'Feature flag toggle gera entrada em audit log.',
        'Acessar /admin/assessorias → ver assessoria criada → editar plano.',
        'Audit log registra ASSESSORIA_UPDATE.',
    ]))
    story.append(PageBreak())

    # ============= ANEXO =============
    story.extend(section('Anexo — Critérios de Aceitação Globais'))
    story.append(bullets([
        '<b>Auth</b> — Token expira em 60min (INTERNO) / 24h (ADM). Refresh funciona. Logout invalida refresh.',
        '<b>Multi-tenant</b> — Usuário INTERNO de Assessoria A não consegue ver/editar dado de Assessoria B (mesmo via API).',
        '<b>RBAC</b> — Endpoint protegido por @RequirePermission retorna 403 quando perfil não tem a permissão.',
        '<b>LGPD</b> — Cadastros sem base_legal falham 422 quando FEATURE_COMPLIANCE_STRICT=true.',
        '<b>Soft-delete</b> — Listagens nunca mostram registros com deleted_at preenchido.',
        '<b>Histórico</b> — Toda mutação relevante gera evento em <font face="Courier">eventos</font> table.',
        '<b>Idempotência</b> — Reenvio de e-mail/WhatsApp com mesma key não duplica.',
        '<b>Audit ADM</b> — Toda ação de ADM em tenant alheio é registrada em admin_audit_log (imutável).',
        '<b>Performance</b> — Listas paginadas (sem retorno de >100 registros sem paginação).',
        '<b>SSE</b> — Notificações in-app chegam em <2s de evento gerado no backend.',
    ]))

    story.append(Spacer(1, 0.5 * cm))
    story.append(p('<i>Encerramento do teste:</i> com todos os critérios PASS, sistema está pronto para staging.'))

    return story


# ============================ MAIN ============================

def gen_pdf(filename, story):
    out = Path(__file__).parent / filename
    doc = SimpleDocTemplate(
        str(out), pagesize=A4,
        leftMargin=2 * cm, rightMargin=2 * cm,
        topMargin=2 * cm, bottomMargin=2 * cm,
        title=filename.replace('.pdf', '').replace('-', ' ').title(),
        author='HUB feat. creators',
    )
    doc.build(story, onFirstPage=header_footer, onLaterPages=header_footer)
    print(f'OK {out}')


if __name__ == '__main__':
    gen_pdf('tutorial-completo.pdf', build_tutorial())
    gen_pdf('guia-de-teste.pdf', build_guia_teste())
