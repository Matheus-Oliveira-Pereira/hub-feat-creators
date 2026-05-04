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
    api.post<{ accessToken: string; refreshToken: string }>('/api/v1/auth/signup', data),
  login: (data: { email: string; senha: string }) =>
    api.post<{ accessToken: string; refreshToken: string }>('/api/v1/auth/login', data),
  logout: (refreshToken: string) => api.post('/api/v1/auth/logout', { refreshToken }),
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
  assessoriaId: string;
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
