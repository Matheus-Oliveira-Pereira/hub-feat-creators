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

// Influenciadores
export interface Influenciador {
  id: string;
  nome: string;
  handles: Record<string, string>;
  nicho: string | null;
  audienciaTotal: number | null;
  observacoes: string | null;
  tags: string[];
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
