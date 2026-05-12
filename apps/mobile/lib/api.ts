import { useAuthStore } from '@/store/auth';
import { logout } from '@/lib/auth';

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';

async function request<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const token = useAuthStore.getState().token;
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...(options.headers as Record<string, string>),
  };
  if (token) headers['Authorization'] = `Bearer ${token}`;

  const res = await fetch(`${API_URL}${path}`, { ...options, headers });

  if (res.status === 401) {
    await logout();
    throw new Error('UNAUTHORIZED');
  }
  if (res.status === 204) return undefined as T;
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message ?? `HTTP ${res.status}`);
  }
  return res.json();
}

export const api = {
  get: <T>(path: string) => request<T>(path),
  post: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'POST', body: JSON.stringify(body) }),
  patch: <T>(path: string, body: unknown) =>
    request<T>(path, { method: 'PATCH', body: JSON.stringify(body) }),
  delete: (path: string) => request<void>(path, { method: 'DELETE' }),
};

// Tipos das respostas do portal
export interface Tarefa {
  id: string;
  titulo: string;
  descricao: string | null;
  prazo: string | null;
  prioridade: 'BAIXA' | 'MEDIA' | 'ALTA' | 'CRITICA';
  status: string;
  visivelParaCreator: boolean;
}

export interface CreatorEntregavel {
  id: string;
  filename: string;
  contentType: string;
  sizeBytes: number;
  status: 'ENVIADO' | 'EM_REVISAO' | 'APROVADO' | 'SOLICITADA_REVISAO';
  feedback: string | null;
  criadoEm: string;
  downloadUrl?: string;
}

export interface Comentario {
  id: string;
  texto: string;
  autorTipo: 'USUARIO' | 'CREATOR';
  interno: boolean;
  criadoEm: string;
}
