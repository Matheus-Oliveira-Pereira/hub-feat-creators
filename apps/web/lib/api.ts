const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function getToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem('accessToken');
}

function notifyTokenChange(key: 'accessToken' | 'refreshToken', value: string | null) {
  if (typeof window === 'undefined') return;
  // dispatch manual: eventos `storage` só disparam entre abas; precisamos disparar
  // sintético na própria aba pra AuthProvider re-decodar o JWT após login/logout.
  window.dispatchEvent(new StorageEvent('storage', { key, newValue: value }));
}

export function setTokens(accessToken: string, refreshToken: string) {
  localStorage.setItem('accessToken', accessToken);
  localStorage.setItem('refreshToken', refreshToken);
  notifyTokenChange('accessToken', accessToken);
}

export function clearTokens() {
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  notifyTokenChange('accessToken', null);
}

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  put: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PUT', body: JSON.stringify(body) }),
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
};

// Auth
export const auth = {
  signup: (data: { assessoriaNome: string; slug: string; email: string; senha: string }) =>
    api.post<{ email: string; emailVerificado: boolean }>('/api/v1/auth/signup', data),
  login: (data: { email: string; senha: string; mfaCode?: string }) =>
    api.post<{ accessToken: string; refreshToken: string }>('/api/v1/auth/login', data),
  logout: (refreshToken: string) => api.post('/api/v1/auth/logout', { refreshToken }),
  verifyEmail: (token: string) => api.post('/api/v1/auth/verify-email', { token }),
  forgotPassword: (email: string) => api.post('/api/v1/auth/forgot-password', { email }),
  resetPassword: (token: string, novaSenha: string) =>
    api.post('/api/v1/auth/reset-password', { token, novaSenha }),
  aceitarConvite: (token: string, senha: string) =>
    api.post<{ id: string; email: string; role: string }>('/api/v1/auth/aceitar-convite', { token, senha }),
  mfaSetup: () =>
    api.post<{ secret: string; qrCodeUri: string; recoveryCodes: string[] }>('/api/v1/auth/mfa/setup', {}),
  mfaActivate: (code: string) => api.post('/api/v1/auth/mfa/activate', { code }),
  mfaDisable: (code: string) => api.post('/api/v1/auth/mfa/disable', { code }),
};

// Membros
export interface Membro {
  id: string;
  email: string;
  role: string;
  status: string;
  mfaAtivo: boolean;
  emailVerificado: boolean;
  profileId: string | null;
  ultimoLoginEm: string | null;
  createdAt: string;
}

export const membros = {
  list: () => api.get<Membro[]>('/api/v1/membros'),
  setStatus: (id: string, status: string) =>
    api.patch<Membro>(`/api/v1/membros/${id}/status`, { status }),
  remove: (id: string) => api.delete(`/api/v1/membros/${id}`),
};

// Convites
export interface ConviteItem {
  id: string;
  email: string;
  role: string;
  perfilId: string | null;
  expiresAt: string;
}

export const convites = {
  list: () => api.get<ConviteItem[]>('/api/v1/convites'),
  criar: (data: { email: string; role?: string; perfilId?: string }) =>
    api.post<ConviteItem>('/api/v1/convites', data),
};

export type BaseLegal = 'CONSENTIMENTO' | 'EXECUCAO_CONTRATO' | 'LEGITIMO_INTERESSE' | 'OBRIGACAO_LEGAL';

// Influenciadores
export interface Influenciador {
  id: string;
  nome: string;
  handles: Record<string, string>;
  nicho: string | null;
  audienciaTotal: number | null;
  observacoes: string | null;
  tags: string[];
  baseLegal: BaseLegal;
  createdAt: string;
  updatedAt: string;
}

export interface PageResponse<T> {
  data: T[];
  pagination: { cursor: string | null; hasMore: boolean; limit: number };
}

export const influenciadores = {
  list: (params?: { nome?: string; nicho?: string; cursor?: string; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.nome) q.set('nome', params.nome);
    if (params?.nicho) q.set('nicho', params.nicho);
    if (params?.cursor) q.set('page', params.cursor);
    if (params?.size) q.set('size', String(params.size));
    return api.get<PageResponse<Influenciador>>(`/api/v1/influenciadores?${q}`);
  },
  get: (id: string) => api.get<Influenciador>(`/api/v1/influenciadores/${id}`),
  create: (data: Partial<Influenciador>) =>
    api.post<Influenciador>('/api/v1/influenciadores', data),
  update: (id: string, data: Partial<Influenciador>) =>
    api.put<Influenciador>(`/api/v1/influenciadores/${id}`, data),
  delete: (id: string) => api.delete(`/api/v1/influenciadores/${id}`),
};

// Marcas
export interface Marca {
  id: string;
  nome: string;
  segmento: string | null;
  site: string | null;
  observacoes: string | null;
  tags: string[];
  baseLegal: BaseLegal;
  createdAt: string;
  updatedAt: string;
}

export const marcas = {
  list: (params?: { nome?: string; segmento?: string; cursor?: string; size?: number }) => {
    const q = new URLSearchParams();
    if (params?.nome) q.set('nome', params.nome);
    if (params?.segmento) q.set('segmento', params.segmento);
    if (params?.cursor) q.set('page', params.cursor);
    if (params?.size) q.set('size', String(params.size));
    return api.get<PageResponse<Marca>>(`/api/v1/marcas?${q}`);
  },
  get: (id: string) => api.get<Marca>(`/api/v1/marcas/${id}`),
  create: (data: Partial<Marca>) => api.post<Marca>('/api/v1/marcas', data),
  update: (id: string, data: Partial<Marca>) => api.put<Marca>(`/api/v1/marcas/${id}`, data),
  delete: (id: string) => api.delete(`/api/v1/marcas/${id}`),
};

// Prospecções
export type ProspeccaoStatus =
  | 'NOVA'
  | 'CONTATADA'
  | 'NEGOCIANDO'
  | 'FECHADA_GANHA'
  | 'FECHADA_PERDIDA';

export type MotivoPerda =
  | 'SEM_FIT'
  | 'ORCAMENTO'
  | 'TIMING'
  | 'CONCORRENTE'
  | 'SEM_RESPOSTA'
  | 'OUTRO';

export type EventoTipo = 'STATUS_CHANGE' | 'COMMENT' | 'EMAIL_SENT' | 'TASK_LINKED';

export interface Prospeccao {
  id: string;
  marcaId: string;
  influenciadorId: string | null;
  assessorResponsavelId: string;
  titulo: string;
  status: ProspeccaoStatus;
  valorEstimadoCentavos: number | null;
  proximaAcao: string | null;
  proximaAcaoEm: string | null;
  observacoes: string | null;
  tags: string[];
  motivoPerda: MotivoPerda | null;
  motivoPerdaDetalhe: string | null;
  fechadaEm: string | null;
  createdAt: string;
  updatedAt: string;
  createdBy: string | null;
}

export interface ProspeccaoPayload {
  marcaId: string;
  influenciadorId?: string | null;
  assessorResponsavelId?: string | null;
  titulo: string;
  valorEstimadoCentavos?: number | null;
  proximaAcao?: string | null;
  proximaAcaoEm?: string | null;
  observacoes?: string | null;
  tags?: string[];
}

export interface ProspeccaoEventoResponse {
  id: string;
  tipo: EventoTipo;
  payload: Record<string, unknown>;
  autorId: string | null;
  createdAt: string;
}

export interface ProspeccaoFiltros {
  status?: ProspeccaoStatus;
  assessorId?: string;
  marcaId?: string;
  nome?: string;
}

export interface ProspeccaoDashboard {
  porStatus: Partial<Record<ProspeccaoStatus, number>>;
  fechadasMes: number;
  taxaConversao: number;
  timeToCloseDiasMedio: number;
}

export const prospeccoes = {
  dashboard: () => api.get<ProspeccaoDashboard>('/api/v1/prospeccoes/dashboard'),
  list: (params: ProspeccaoFiltros & { cursor?: string; size?: number } = {}) => {
    const q = new URLSearchParams();
    if (params.status) q.set('status', params.status);
    if (params.assessorId) q.set('assessorId', params.assessorId);
    if (params.marcaId) q.set('marcaId', params.marcaId);
    if (params.nome) q.set('nome', params.nome);
    if (params.cursor) q.set('page', params.cursor);
    if (params.size) q.set('size', String(params.size));
    return api.get<PageResponse<Prospeccao>>(`/api/v1/prospeccoes?${q}`);
  },
  get: (id: string) => api.get<Prospeccao>(`/api/v1/prospeccoes/${id}`),
  eventos: (id: string) =>
    api.get<ProspeccaoEventoResponse[]>(`/api/v1/prospeccoes/${id}/eventos`),
  create: (data: ProspeccaoPayload) => api.post<Prospeccao>('/api/v1/prospeccoes', data),
  update: (id: string, data: ProspeccaoPayload) =>
    api.put<Prospeccao>(`/api/v1/prospeccoes/${id}`, data),
  mudarStatus: (
    id: string,
    payload: { status: ProspeccaoStatus; motivoPerda?: MotivoPerda; motivoPerdaDetalhe?: string }
  ) => api.patch<Prospeccao>(`/api/v1/prospeccoes/${id}/status`, payload),
  comentar: (id: string, texto: string) =>
    api.post<ProspeccaoEventoResponse>(`/api/v1/prospeccoes/${id}/comentarios`, { texto }),
  delete: (id: string) => api.delete(`/api/v1/prospeccoes/${id}`),
};

// Perfis (RBAC)
export interface Perfil {
  id: string;
  nome: string;
  descricao: string | null;
  roles: string[];
  isSystem: boolean;
  usuariosCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface PerfilPayload {
  nome: string;
  descricao?: string | null;
  roles: string[];
}

export const perfis = {
  list: () => api.get<Perfil[]>('/api/v1/perfis'),
  get: (id: string) => api.get<Perfil>(`/api/v1/perfis/${id}`),
  create: (data: PerfilPayload) => api.post<Perfil>('/api/v1/perfis', data),
  update: (id: string, data: PerfilPayload) => api.put<Perfil>(`/api/v1/perfis/${id}`, data),
  delete: (id: string) => api.delete(`/api/v1/perfis/${id}`),
  atribuir: (usuarioId: string, profileId: string) =>
    api.patch<void>(`/api/v1/usuarios/${usuarioId}/profile`, { profileId }),
};

// Contatos (de marca)
export interface Contato {
  id: string;
  marcaId: string;
  nome: string;
  email: string | null;
  telefone: string | null;
  cargo: string | null;
  emailInvalido: boolean;
  baseLegal: BaseLegal;
  createdAt: string;
  updatedAt: string;
}

export interface ContatoPayload {
  marcaId: string;
  nome: string;
  email?: string | null;
  telefone?: string | null;
  cargo?: string | null;
}

export const contatos = {
  listByMarca: (marcaId: string) =>
    api.get<Contato[]>(`/api/v1/contatos?marcaId=${marcaId}`),
  create: (data: ContatoPayload) => api.post<Contato>('/api/v1/contatos', data),
  update: (id: string, data: ContatoPayload) =>
    api.put<Contato>(`/api/v1/contatos/${id}`, data),
  delete: (id: string) => api.delete(`/api/v1/contatos/${id}`),
};

// Tarefas (PRD-003)
export type TarefaStatus = 'TODO' | 'EM_ANDAMENTO' | 'FEITA' | 'CANCELADA';
export type TarefaPrioridade = 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE';
export type EntidadeTipo = 'PROSPECCAO' | 'INFLUENCIADOR' | 'MARCA' | 'CONTATO';

export interface Tarefa {
  id: string;
  titulo: string;
  descricao: string | null;
  prazo: string;
  prioridade: TarefaPrioridade;
  status: TarefaStatus;
  responsavelId: string;
  criadorId: string;
  entidadeTipo: EntidadeTipo | null;
  entidadeId: string | null;
  concluidaEm: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TarefaPayload {
  titulo: string;
  descricao?: string | null;
  prazo: string;
  prioridade?: TarefaPrioridade;
  responsavelId?: string | null;
  entidadeTipo?: EntidadeTipo | null;
  entidadeId?: string | null;
}

export interface TarefaUpdatePayload {
  titulo?: string;
  descricao?: string | null;
  prazo?: string;
  prioridade?: TarefaPrioridade;
  responsavelId?: string | null;
  entidadeTipo?: EntidadeTipo | null;
  entidadeId?: string | null;
}

export interface TarefaFiltros {
  status?: TarefaStatus;
  prioridade?: TarefaPrioridade;
  responsavelId?: string;
  prazoFiltro?: 'VENCIDAS' | 'HOJE' | 'SEMANA' | 'FUTURAS';
  minhas?: boolean;
  page?: number;
  size?: number;
}

export interface TarefaComentario {
  id: string;
  tarefaId: string;
  autorId: string;
  texto: string;
  createdAt: string;
}

export interface AlertaResponse {
  count: number;
}

export interface UsuarioPreferencia {
  usuarioId: string;
  digestDiarioEnabled: boolean;
}

function buildTarefaQuery(filtros: TarefaFiltros = {}): string {
  const params = new URLSearchParams();
  if (filtros.status) params.set('status', filtros.status);
  if (filtros.prioridade) params.set('prioridade', filtros.prioridade);
  if (filtros.responsavelId) params.set('responsavelId', filtros.responsavelId);
  if (filtros.prazoFiltro) params.set('prazoFiltro', filtros.prazoFiltro);
  if (filtros.minhas) params.set('minhas', 'true');
  if (filtros.page !== undefined) params.set('page', String(filtros.page));
  if (filtros.size !== undefined) params.set('size', String(filtros.size));
  const qs = params.toString();
  return qs ? `?${qs}` : '';
}

export const tarefas = {
  list: (filtros?: TarefaFiltros) =>
    api.get<PageResponse<Tarefa>>(`/api/v1/tarefas${buildTarefaQuery(filtros)}`),
  me: (filtros?: Pick<TarefaFiltros, 'status' | 'prazoFiltro' | 'page' | 'size'>) =>
    api.get<PageResponse<Tarefa>>(`/api/v1/tarefas/me${buildTarefaQuery(filtros)}`),
  get: (id: string) => api.get<Tarefa>(`/api/v1/tarefas/${id}`),
  create: (data: TarefaPayload) => api.post<Tarefa>('/api/v1/tarefas', data),
  update: (id: string, data: TarefaUpdatePayload) =>
    api.patch<Tarefa>(`/api/v1/tarefas/${id}`, data),
  status: (id: string, status: TarefaStatus) =>
    api.patch<Tarefa>(`/api/v1/tarefas/${id}/status`, { status }),
  delete: (id: string) => api.delete(`/api/v1/tarefas/${id}`),
  alerta: () => api.get<AlertaResponse>('/api/v1/tarefas/alerta'),
  comentarios: (id: string) =>
    api.get<TarefaComentario[]>(`/api/v1/tarefas/${id}/comentarios`),
  addComentario: (id: string, texto: string) =>
    api.post<TarefaComentario>(`/api/v1/tarefas/${id}/comentarios`, { texto }),
  preferencias: () => api.get<UsuarioPreferencia>('/api/v1/tarefas/preferencias'),
  updatePreferencias: (digestDiarioEnabled: boolean) =>
    api.patch<UsuarioPreferencia>('/api/v1/tarefas/preferencias', { digestDiarioEnabled }),
};

// ─── Email ────────────────────────────────────────────────────────────────────

export type EmailAccountStatus = 'ATIVA' | 'PAUSADA' | 'FALHA_AUTH';
export type TlsMode = 'STARTTLS' | 'SSL';
export type EmailEnvioStatus = 'ENFILEIRADO' | 'ENVIADO' | 'FALHOU' | 'BOUNCED';
export type EmailEventoTipo = 'ABERTO' | 'CLICADO' | 'BOUNCE' | 'COMPLAINT' | 'UNSUBSCRIBE';

export interface EmailAccount {
  id: string;
  nome: string;
  host: string;
  port: number;
  username: string;
  fromAddress: string;
  fromName: string;
  tlsMode: TlsMode;
  dailyQuota: number;
  status: EmailAccountStatus;
  falhasAuthCount: number;
  updatedAt: string;
}

export interface EmailAccountPayload {
  nome: string;
  host: string;
  port: number;
  username: string;
  password: string;
  fromAddress: string;
  fromName: string;
  tlsMode?: TlsMode;
  dailyQuota?: number;
}

export interface EmailAccountUpdatePayload {
  nome?: string;
  host?: string;
  port?: number;
  username?: string;
  password?: string;
  fromAddress?: string;
  fromName?: string;
  tlsMode?: TlsMode;
  dailyQuota?: number;
}

export interface EmailTemplate {
  id: string;
  nome: string;
  assunto: string;
  corpoHtml: string;
  corpoTexto?: string | null;
  variaveis: string[];
  createdAt: string;
  updatedAt: string;
}

export interface EmailTemplatePayload {
  nome: string;
  assunto: string;
  corpoHtml: string;
  corpoTexto?: string | null;
  variaveis?: string[];
}

export interface EmailLayout {
  id: string;
  headerHtml: string;
  footerHtml: string;
  updatedAt: string;
}

export interface EmailEnvio {
  id: string;
  accountId: string;
  templateId: string;
  destinatarioEmail: string;
  destinatarioNome?: string | null;
  assunto: string;
  status: EmailEnvioStatus;
  tentativas: number;
  smtpMessageId?: string | null;
  enviadoEm?: string | null;
  createdAt: string;
}

export interface EmailEnvioPayload {
  accountId: string;
  templateId: string;
  destinatarioEmail: string;
  destinatarioNome?: string;
  vars?: Record<string, unknown>;
  contexto?: Record<string, unknown>;
  idempotencyKey?: string;
  trackingEnabled?: boolean;
}

export interface EmailEvento {
  id: string;
  tipo: EmailEventoTipo;
  payload: Record<string, unknown>;
  createdAt: string;
}

// ─── WhatsApp ────────────────────────────────────────────────────────────────

export type WaAccountStatus = 'ATIVO' | 'INATIVO' | 'ERRO';
export type WaTemplateStatus = 'PENDING' | 'APPROVED' | 'REJECTED' | 'PAUSED';
export type WaTemplateCategoria = 'MARKETING' | 'UTILITY' | 'AUTHENTICATION';
export type WaEnvioStatus = 'ENFILEIRADO' | 'ENVIADO' | 'ENTREGUE' | 'LIDO' | 'FALHOU';
export type WaEnvioTipo = 'TEMPLATE' | 'FREEFORM' | 'MIDIA';

export interface WaAccount {
  id: string;
  wabaId: string;
  phoneNumberId: string;
  phoneE164: string;
  displayName: string;
  status: WaAccountStatus;
  dailyLimit: number;
  dailySent: number;
}

export interface WaAccountPayload {
  wabaId: string;
  phoneNumberId: string;
  phoneE164: string;
  displayName: string;
  accessToken: string;
  appSecret: string;
}

export interface WaTemplate {
  id: string;
  accountId: string;
  nome: string;
  idioma: string;
  categoria: WaTemplateCategoria;
  corpo: string;
  variaveis: string[];
  status: WaTemplateStatus;
  metaTemplateId: string | null;
  motivoRejeicao: string | null;
}

export interface WaTemplatePayload {
  accountId: string;
  nome: string;
  idioma?: string;
  categoria: WaTemplateCategoria;
  corpo: string;
  variaveis?: string[];
}

export interface WaEnvio {
  id: string;
  tipo: WaEnvioTipo;
  destinatarioE164: string;
  status: WaEnvioStatus;
  wamid: string | null;
}

export const whatsapp = {
  accounts: {
    list: () => api.get<WaAccount[]>('/api/v1/whatsapp/accounts'),
    create: (data: WaAccountPayload) => api.post<WaAccount>('/api/v1/whatsapp/accounts', data),
    update: (id: string, data: Partial<Pick<WaAccountPayload, 'displayName' | 'accessToken' | 'appSecret'>>) =>
      api.patch<WaAccount>(`/api/v1/whatsapp/accounts/${id}`, data),
    delete: (id: string) => api.delete(`/api/v1/whatsapp/accounts/${id}`),
  },
  templates: {
    list: () => api.get<WaTemplate[]>('/api/v1/whatsapp/templates'),
    create: (data: WaTemplatePayload) => api.post<WaTemplate>('/api/v1/whatsapp/templates', data),
    submit: (id: string) => api.post<WaTemplate>(`/api/v1/whatsapp/templates/${id}/submit`, {}),
  },
  envios: {
    sendTemplate: (data: {
      accountId: string;
      templateId: string;
      destinatarioE164: string;
      components?: Record<string, unknown>[];
      idempotencyKey?: string;
    }) => api.post<WaEnvio>('/api/v1/whatsapp/envios/template', data),
    sendFreeform: (data: {
      accountId: string;
      destinatarioE164: string;
      text: string;
      idempotencyKey?: string;
    }) => api.post<WaEnvio>('/api/v1/whatsapp/envios/freeform', data),
  },
};

export const email = {
  accounts: {
    list: () => api.get<EmailAccount[]>('/api/v1/email/accounts'),
    get: (id: string) => api.get<EmailAccount>(`/api/v1/email/accounts/${id}`),
    create: (data: EmailAccountPayload) => api.post<EmailAccount>('/api/v1/email/accounts', data),
    update: (id: string, data: EmailAccountUpdatePayload) =>
      api.patch<EmailAccount>(`/api/v1/email/accounts/${id}`, data),
    delete: (id: string) => api.delete(`/api/v1/email/accounts/${id}`),
    test: (id: string) => api.post<void>(`/api/v1/email/accounts/${id}/test`, {}),
  },
  templates: {
    list: () => api.get<EmailTemplate[]>('/api/v1/email/templates'),
    get: (id: string) => api.get<EmailTemplate>(`/api/v1/email/templates/${id}`),
    create: (data: EmailTemplatePayload) => api.post<EmailTemplate>('/api/v1/email/templates', data),
    update: (id: string, data: Partial<EmailTemplatePayload>) =>
      api.patch<EmailTemplate>(`/api/v1/email/templates/${id}`, data),
    delete: (id: string) => api.delete(`/api/v1/email/templates/${id}`),
    preview: (id: string, vars: Record<string, unknown>) =>
      api.post<{ html: string }>(`/api/v1/email/templates/${id}/preview`, { vars }),
  },
  layout: {
    get: () => api.get<EmailLayout>('/api/v1/email/layout'),
    save: (data: Partial<Pick<EmailLayout, 'headerHtml' | 'footerHtml'>>) =>
      api.put<EmailLayout>('/api/v1/email/layout', data),
  },
  envios: {
    list: (params?: { contexto?: string; page?: number; size?: number }) => {
      const qs = new URLSearchParams();
      if (params?.contexto) qs.set('contexto', params.contexto);
      if (params?.page !== undefined) qs.set('page', String(params.page));
      if (params?.size !== undefined) qs.set('size', String(params.size));
      const q = qs.toString();
      return api.get<PageResponse<EmailEnvio>>(`/api/v1/email/envios${q ? `?${q}` : ''}`);
    },
    get: (id: string) => api.get<EmailEnvio>(`/api/v1/email/envios/${id}`),
    send: (data: EmailEnvioPayload) => api.post<EmailEnvio>('/api/v1/email/envios', data),
    eventos: (id: string) => api.get<EmailEvento[]>(`/api/v1/email/envios/${id}/eventos`),
  },
};

// ─── Notificações ────────────────────────────────────────────────────────────

export interface Notificacao {
  id: string;
  tipo: string;
  prioridade: 'LOW' | 'NORMAL' | 'HIGH';
  titulo: string;
  mensagem: string;
  payload: Record<string, unknown>;
  alvoTipo?: string;
  alvoId?: string;
  agrupadas: number;
  lidaEm?: string;
  createdAt: string;
}

export interface NotificacaoPreferencia {
  tipo: string;
  canal: 'INAPP' | 'PUSH' | 'EMAIL';
  habilitado: boolean;
}

export const notificacoes = {
  list: (params?: { apenasNaoLidas?: boolean; tipo?: string; page?: number; size?: number }) => {
    const qs = new URLSearchParams();
    if (params?.apenasNaoLidas) qs.set('apenasNaoLidas', 'true');
    if (params?.tipo) qs.set('tipo', params.tipo);
    if (params?.page !== undefined) qs.set('page', String(params.page));
    if (params?.size !== undefined) qs.set('size', String(params.size));
    const q = qs.toString();
    return api.get<PageResponse<Notificacao>>(`/api/v1/notificacoes${q ? `?${q}` : ''}`);
  },
  contagem: () => api.get<{ naoLidas: number }>('/api/v1/notificacoes/contagem'),
  marcarLida: (id: string) => api.post<void>(`/api/v1/notificacoes/${id}/lida`, {}),
  marcarTodasLidas: () => api.post<void>('/api/v1/notificacoes/lidas', {}),
  prefs: () => api.get<NotificacaoPreferencia[]>('/api/v1/notificacoes/prefs'),
  updatePref: (tipo: string, canal: string, habilitado: boolean) =>
    api.put<NotificacaoPreferencia>(`/api/v1/notificacoes/prefs/${tipo}/${canal}`, { habilitado }),
};

export const webpush = {
  publicKey: () => api.get<{ publicKey: string }>('/api/v1/webpush/public-key'),
  subscribe: (subscription: PushSubscriptionJSON) =>
    api.post<void>('/api/v1/webpush/subscribe', subscription),
  unsubscribe: (endpoint: string) =>
    api.post<void>('/api/v1/webpush/unsubscribe', { endpoint }),
};

// ─── Histórico Unificado ─────────────────────────────────────────────────────

export interface Evento {
  id: string;
  tipo: string;
  entidadesRelacionadas: Array<{ tipo: string; id: string }>;
  payload: Record<string, unknown>;
  autorId?: string;
  ts: string;
}

export const historico = {
  list: (params: {
    entidade_tipo: string;
    entidade_id: string;
    tipos?: string;
    cursor?: string;
    size?: number;
  }) => {
    const q = new URLSearchParams();
    q.set('entidade_tipo', params.entidade_tipo);
    q.set('entidade_id', params.entidade_id);
    if (params.tipos) q.set('tipos', params.tipos);
    if (params.cursor) q.set('cursor', params.cursor);
    if (params.size !== undefined) q.set('size', String(params.size));
    return api.get<PageResponse<Evento>>(`/api/v1/historico?${q}`);
  },
};

// ─── Importação CSV (PRD-011) ─────────────────────────────────────────────────

export type ImportEntidade = 'INFLUENCIADOR' | 'MARCA' | 'CONTATO';
export type ImportStatus =
  | 'UPLOADADO'
  | 'VALIDANDO'
  | 'PRONTO_DRY_RUN'
  | 'EXECUTANDO'
  | 'CONCLUIDO'
  | 'CANCELADO'
  | 'FALHOU';
export type DedupStrategy = 'SKIP' | 'UPDATE' | 'DUPLICATE';

export interface ImportJob {
  id: string;
  entidade: ImportEntidade;
  arquivoNome: string;
  status: ImportStatus;
  totalLinhas: number | null;
  processadas: number;
  sucesso: number;
  falha: number;
  iniciadoEm: string | null;
  concluidoEm: string | null;
  createdAt: string;
}

export interface ImportLinha {
  linha: number;
  status: 'OK' | 'ERRO' | 'SKIP' | 'UPDATE';
  entidadeId: string | null;
  erros: string[];
}

export interface ColumnSuggestion {
  csvHeader: string;
  suggestedField: string | null;
  distance: number;
}

export interface ProbeResponse {
  headers: string[];
  encoding: string;
  separator: string;
  suggestions: ColumnSuggestion[];
}

export interface ImportTemplate {
  id: string;
  nome: string;
  entidade: ImportEntidade;
  mapeamento: Record<string, string>;
  createdAt: string;
}

async function uploadFile(file: File, entidade: ImportEntidade): Promise<ImportJob> {
  const token = getToken();
  const fd = new FormData();
  fd.append('file', file);
  fd.append('entidade', entidade);
  const res = await fetch(`${API_URL}/api/v1/imports/upload`, {
    method: 'POST',
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    body: fd,
  });
  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }
  return res.json();
}

export const importacoes = {
  upload: uploadFile,
  probe: (id: string) => api.get<ProbeResponse>(`/api/v1/imports/${id}/probe`),
  setMapeamento: (id: string, mapeamento: Record<string, string>, baseLegal: string, dedupStrategy: DedupStrategy) =>
    api.put<ImportJob>(`/api/v1/imports/${id}/mapeamento`, { mapeamento, baseLegal, dedupStrategy }),
  dryRun: (id: string) => api.post<ImportJob>(`/api/v1/imports/${id}/dry-run`, {}),
  listDryRun: (id: string, page = 0, size = 200) =>
    api.get<{ linhas: ImportLinha[]; total: number; hasMore: boolean }>(
      `/api/v1/imports/${id}/dry-run?page=${page}&size=${size}`
    ),
  execute: (id: string) => api.post<ImportJob>(`/api/v1/imports/${id}/execute`, {}),
  cancel: (id: string) => api.delete(`/api/v1/imports/${id}`),
  get: (id: string) => api.get<ImportJob>(`/api/v1/imports/${id}`),
  list: (page = 0, size = 20) =>
    api.get<{ jobs: ImportJob[]; total: number; hasMore: boolean }>(
      `/api/v1/imports?page=${page}&size=${size}`
    ),
  relatorioUrl: (id: string) => {
    const token = getToken();
    return `${API_URL}/api/v1/imports/${id}/relatorio${token ? `?token=${token}` : ''}`;
  },
  listTemplates: (entidade?: ImportEntidade) =>
    api.get<ImportTemplate[]>(`/api/v1/imports/templates${entidade ? `?entidade=${entidade}` : ''}`),
  createTemplate: (nome: string, entidade: ImportEntidade, mapeamento: Record<string, string>) =>
    api.post<ImportTemplate>('/api/v1/imports/templates', { nome, entidade, mapeamento }),
  deleteTemplate: (id: string) => api.delete(`/api/v1/imports/templates/${id}`),
};

// ─── Relatórios (PRD-012) ─────────────────────────────────────────────────────

export type RelatorioTipo = 'FUNIL' | 'PERFORMANCE_ASSESSOR' | 'TAREFAS_SLA';

export interface FunilItem {
  status: string;
  qtd: number;
  qtdAnterior: number;
  deltaPercent: number;
  valorTotalCentavos: number;
  tempoMedioDias: number;
}

export interface FunilResult {
  itens: FunilItem[];
  periodoAtual: string;
  periodoAnterior: string;
}

export interface AssessorItem {
  assessorId: string;
  assessorEmail: string;
  abertas: number;
  ganhas: number;
  perdidas: number;
  ticketMedioCentavos: number;
  tempoMedioFechamentoDias: number;
  abertasAnterior: number;
  ganhasAnterior: number;
  perdidasAnterior: number;
}

export interface PerformanceResult {
  assessores: AssessorItem[];
  periodoAtual: string;
  periodoAnterior: string;
}

export interface TarefaSlaItem {
  responsavelId: string;
  responsavelEmail: string;
  total: number;
  noPrazo: number;
  atrasadas: number;
  atrasoMedioHoras: number;
  totalAnterior: number;
}

export interface TarefaSlaResult {
  assessores: TarefaSlaItem[];
  periodoAtual: string;
  periodoAnterior: string;
}

export interface RelatorioSalvo {
  id: string;
  nome: string;
  relatorioTipo: RelatorioTipo;
  filtros: Record<string, unknown>;
  createdAt: string;
}

function buildPeriodParams(from: string, to: string, extra?: Record<string, string>) {
  const p = new URLSearchParams({ from, to });
  if (extra) Object.entries(extra).forEach(([k, v]) => p.set(k, v));
  return p.toString();
}

export const relatorios = {
  funil: (from: string, to: string, assessorId?: string) =>
    api.get<FunilResult>(`/api/v1/relatorios/funil?${buildPeriodParams(from, to, assessorId ? { assessorId } : undefined)}`),

  performanceAssessor: (from: string, to: string) =>
    api.get<PerformanceResult>(`/api/v1/relatorios/performance-assessor?${buildPeriodParams(from, to)}`),

  tarefasSla: (from: string, to: string, responsavelId?: string) =>
    api.get<TarefaSlaResult>(`/api/v1/relatorios/tarefas-sla?${buildPeriodParams(from, to, responsavelId ? { responsavelId } : undefined)}`),

  exportUrl: (tipo: 'funil' | 'performance-assessor' | 'tarefas-sla', from: string, to: string, extra?: Record<string, string>) => {
    const token = getToken();
    const params = buildPeriodParams(from, to, { ...(extra ?? {}), ...(token ? { token } : {}) });
    return `${API_URL}/api/v1/relatorios/${tipo}/export.csv?${params}`;
  },

  listarSalvos: () => api.get<RelatorioSalvo[]>('/api/v1/relatorios-salvos'),
  salvar: (nome: string, tipo: RelatorioTipo, filtros: Record<string, unknown>) =>
    api.post<RelatorioSalvo>('/api/v1/relatorios-salvos', { nome, tipo, filtros }),
  deletarSalvo: (id: string) => api.delete(`/api/v1/relatorios-salvos/${id}`),
};

// ─── Portal Creator ───────────────────────────────────────────────────────────

export interface CreatorTokenResponse {
  token: string;
  creatorUserId: string;
  email: string;
  influenciadorId: string;
}

export interface InviteInfo {
  email: string;
  nomeInfluenciador: string;
}

export interface AssessoriaBranding {
  logoUrl: string | null;
  corPrimaria: string | null;
}

export interface PortalTarefa {
  id: string;
  titulo: string;
  descricao: string | null;
  prazo: string;
  prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' | 'URGENTE';
  status: string;
  visivelParaCreator: boolean;
}

export interface PortalComentario {
  id: string;
  texto: string;
  autorTipo: 'USUARIO' | 'CREATOR';
  createdAt: string;
}

export interface CreatorEntregavel {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  status: 'ENVIADO' | 'EM_REVISAO' | 'APROVADO' | 'SOLICITADA_REVISAO';
  feedback: string | null;
  criadoEm: string;
}

let creatorToken: string | null = null;

export function setCreatorToken(token: string | null) {
  creatorToken = token;
  if (typeof window !== 'undefined') {
    if (token) localStorage.setItem('creatorToken', token);
    else localStorage.removeItem('creatorToken');
  }
}

export function getCreatorToken(): string | null {
  if (creatorToken) return creatorToken;
  if (typeof window !== 'undefined') {
    return localStorage.getItem('creatorToken');
  }
  return null;
}

async function creatorRequest<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getCreatorToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (!res.ok) {
    const body = await res.json().catch(() => ({}));
    throw { status: res.status, ...body };
  }

  if (res.status === 204) return undefined as T;
  return res.json();
}

export const portalAuth = {
  login: (email: string, senha: string) =>
    fetch(`${API_URL}/api/v1/portal/auth/login`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, senha }),
    }).then(async (r) => {
      if (!r.ok) throw await r.json().catch(() => ({}));
      return r.json() as Promise<CreatorTokenResponse>;
    }),

  aceitarConvite: (token: string, senha: string) =>
    fetch(`${API_URL}/api/v1/portal/auth/convite/aceitar`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ token, senha }),
    }).then(async (r) => {
      if (!r.ok) throw await r.json().catch(() => ({}));
      return r.json() as Promise<CreatorTokenResponse>;
    }),

  infoConvite: (token: string) =>
    fetch(`${API_URL}/api/v1/portal/auth/convite/info?token=${encodeURIComponent(token)}`)
      .then(async (r) => {
        if (!r.ok) throw await r.json().catch(() => ({}));
        return r.json() as Promise<InviteInfo>;
      }),

  branding: () =>
    fetch(`${API_URL}/api/v1/portal/branding`)
      .then(async (r) => {
        if (!r.ok) throw await r.json().catch(() => ({}));
        return r.json() as Promise<AssessoriaBranding>;
      }),
};

export const portalMe = {
  tarefas: () => creatorRequest<PortalTarefa[]>('/api/v1/portal/me/tarefas'),
  tarefa: (id: string) => creatorRequest<PortalTarefa>(`/api/v1/portal/me/tarefas/${id}`),

  comentarios: (tarefaId: string) =>
    creatorRequest<PortalComentario[]>(`/api/v1/portal/me/tarefas/${tarefaId}/comentarios`),
  comentar: (tarefaId: string, texto: string) =>
    creatorRequest<PortalComentario>(`/api/v1/portal/me/tarefas/${tarefaId}/comentarios`, {
      method: 'POST',
      body: JSON.stringify({ texto }),
    }),

  entregaveis: (tarefaId: string) =>
    creatorRequest<CreatorEntregavel[]>(`/api/v1/portal/me/tarefas/${tarefaId}/entregaveis`),
  enviarEntregavel: (tarefaId: string, file: File) => {
    const token = getCreatorToken();
    const form = new FormData();
    form.append('file', file);
    return fetch(`${API_URL}/api/v1/portal/me/tarefas/${tarefaId}/entregaveis`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: form,
    }).then(async (r) => {
      if (!r.ok) throw await r.json().catch(() => ({}));
      return r.json() as Promise<CreatorEntregavel>;
    });
  },
  downloadUrl: (entregavelId: string) => {
    const token = getCreatorToken();
    return `${API_URL}/api/v1/portal/me/entregaveis/${entregavelId}/download${token ? `?token=${token}` : ''}`;
  },
};

// ─── Assessora: invite + branding ─────────────────────────────────────────────

export const portalAdmin = {
  convidar: (influenciadorId: string, email: string) =>
    api.post<{ id: string }>('/api/v1/portal/convites', { influenciadorId, email }),
  setBranding: (logoUrl: string | null, corPrimaria: string | null) =>
    api.put<AssessoriaBranding>('/api/v1/portal/branding', { logoUrl, corPrimaria }),
  revisarEntregavel: (entregavelId: string, status: string, feedback?: string) =>
    api.patch<CreatorEntregavel>(`/api/v1/portal/entregaveis/${entregavelId}/revisao`, { status, feedback }),
};

// ─── Social accounts ──────────────────────────────────────────────────────────

export interface SocialAccount {
  id: string;
  plataforma: 'INSTAGRAM' | 'YOUTUBE' | 'TIKTOK';
  handle: string | null;
  status: 'ATIVA' | 'TOKEN_EXPIRADO' | 'REVOGADA' | 'ERRO';
  conectadoEm: string;
  ultimoSyncEm: string | null;
}

export interface SocialSnapshot {
  dia: string;
  followers: number | null;
  following: number | null;
  postsTotal: number | null;
  engagementRate: number | null;
}

export const social = {
  listAccounts: (influenciadorId: string) =>
    api.get<SocialAccount[]>(`/api/v1/social/accounts?influenciadorId=${influenciadorId}`),
  listSnapshots: (accountId: string, limit = 30) =>
    api.get<SocialSnapshot[]>(`/api/v1/social/snapshots?accountId=${accountId}&limit=${limit}`),
  disconnect: (accountId: string) =>
    api.delete(`/api/v1/social/accounts/${accountId}`),
  startOAuth: (influenciadorId: string, plataforma: string) =>
    `${API_URL}/api/v1/social/auth/${plataforma}/start?influenciadorId=${influenciadorId}`,
};

// ─── IA Match ─────────────────────────────────────────────────────────────────

export interface MatchSugestao {
  id: string;
  prospeccaoId: string;
  influenciadorId: string;
  score: number;
  razoes: Array<{ tipo: string; descricao: string; peso: number }>;
  modeloVersao: string;
  geradoEm: string;
}

export interface Briefing {
  id: string;
  prospeccaoId: string;
  vertical: string | null;
  objetivo: string | null;
  audiencia: Record<string, unknown> | null;
  formato: string | null;
  budgetMin: number | null;
  budgetMax: number | null;
  texto: string | null;
  atualizadoEm: string;
}

export interface BriefingInput {
  prospeccaoId: string;
  vertical?: string;
  objetivo?: string;
  audiencia?: Record<string, unknown>;
  formato?: string;
  budgetMin?: number;
  budgetMax?: number;
  texto?: string;
}

export const match = {
  getSugestoes: (prospeccaoId: string) =>
    api.get<MatchSugestao[]>(`/api/v1/match?prospeccaoId=${prospeccaoId}`),
  runMatch: (prospeccaoId: string) =>
    api.post<MatchSugestao[]>(`/api/v1/match/run?prospeccaoId=${prospeccaoId}`, {}),
  addFeedback: (sugestaoId: string, sinal: string, comentario?: string) =>
    api.post(`/api/v1/match/feedback`, { sugestaoId, sinal, comentario }),
  getBriefing: (prospeccaoId: string) =>
    api.get<Briefing>(`/api/v1/briefings/by-prospeccao/${prospeccaoId}`),
  upsertBriefing: (input: BriefingInput) =>
    api.post<Briefing>(`/api/v1/briefings`, input),
};
