import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api, Tarefa, CreatorEntregavel, Comentario } from '@/lib/api';

export const qk = {
  tarefas: () => ['tarefas'] as const,
  tarefa: (id: string) => ['tarefas', id] as const,
  entregaveis: (tarefaId: string) => ['entregaveis', tarefaId] as const,
  comentarios: (tarefaId: string) => ['comentarios', tarefaId] as const,
};

export function useTarefas() {
  return useQuery({
    queryKey: qk.tarefas(),
    queryFn: () => api.get<Tarefa[]>('/api/v1/portal/me/tarefas'),
    staleTime: 5 * 60 * 1000,
  });
}

export function useTarefa(id: string) {
  return useQuery({
    queryKey: qk.tarefa(id),
    queryFn: () => api.get<Tarefa>(`/api/v1/portal/me/tarefas/${id}`),
    staleTime: 5 * 60 * 1000,
  });
}

export function useEntregaveis(tarefaId: string) {
  return useQuery({
    queryKey: qk.entregaveis(tarefaId),
    queryFn: () => api.get<CreatorEntregavel[]>(`/api/v1/portal/me/tarefas/${tarefaId}/entregaveis`),
    staleTime: 30 * 1000,
  });
}

export function useComentarios(tarefaId: string) {
  return useQuery({
    queryKey: qk.comentarios(tarefaId),
    queryFn: () => api.get<Comentario[]>(`/api/v1/portal/me/tarefas/${tarefaId}/comentarios`),
    staleTime: 30 * 1000,
  });
}

export function useAdicionarComentario(tarefaId: string) {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (texto: string) =>
      api.post<Comentario>(`/api/v1/portal/me/tarefas/${tarefaId}/comentarios`, { texto }),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.comentarios(tarefaId) }),
  });
}
