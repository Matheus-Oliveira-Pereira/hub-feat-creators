'use client';

import {
  useQuery,
  useMutation,
  useInfiniteQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import {
  auth,
  influenciadores,
  marcas,
  contatos,
  perfis,
  prospeccoes,
  tarefas,
  Influenciador,
  Marca,
  Contato,
  Perfil,
  Prospeccao,
  Tarefa,
  TarefaComentario,
  TarefaFiltros,
  TarefaStatus,
  TarefaPrioridade,
  EntidadeTipo,
  AlertaResponse,
  UsuarioPreferencia,
  ProspeccaoEventoResponse,
  ProspeccaoFiltros,
  ProspeccaoStatus,
  MotivoPerda,
  PageResponse,
} from '@/lib/api';
import type {
  InfluenciadorInput,
  MarcaInput,
  ContatoInput,
  PerfilInput,
  ProspeccaoInput,
  TarefaInput,
  LoginInput,
  SignupInput,
} from '@/lib/schemas';

// ────────────────────────────────────────────────────────────────────────────
// Query keys
// ────────────────────────────────────────────────────────────────────────────

export const qk = {
  influenciadores: {
    all: ['influenciadores'] as const,
    list: (filter: { nome?: string }) => ['influenciadores', 'list', filter] as const,
    detail: (id: string) => ['influenciadores', 'detail', id] as const,
  },
  marcas: {
    all: ['marcas'] as const,
    list: (filter: { nome?: string }) => ['marcas', 'list', filter] as const,
    detail: (id: string) => ['marcas', 'detail', id] as const,
  },
  contatos: {
    byMarca: (marcaId: string) => ['contatos', 'marca', marcaId] as const,
  },
  perfis: {
    all: ['perfis'] as const,
  },
  prospeccoes: {
    all: ['prospeccoes'] as const,
    list: (filtros: ProspeccaoFiltros) => ['prospeccoes', 'list', filtros] as const,
    detail: (id: string) => ['prospeccoes', 'detail', id] as const,
    eventos: (id: string) => ['prospeccoes', 'eventos', id] as const,
    dashboard: ['prospeccoes', 'dashboard'] as const,
  },
  tarefas: {
    all: ['tarefas'] as const,
    list: (filtros: TarefaFiltros) => ['tarefas', 'list', filtros] as const,
    me: (filtros?: object) => ['tarefas', 'me', filtros] as const,
    detail: (id: string) => ['tarefas', 'detail', id] as const,
    comentarios: (id: string) => ['tarefas', 'comentarios', id] as const,
    alerta: ['tarefas', 'alerta'] as const,
    preferencias: ['tarefas', 'preferencias'] as const,
  },
};

// ────────────────────────────────────────────────────────────────────────────
// Auth
// ────────────────────────────────────────────────────────────────────────────

export function useLoginMutation() {
  return useMutation({
    mutationFn: (input: LoginInput) => auth.login(input),
  });
}

export function useSignupMutation() {
  return useMutation({
    mutationFn: (input: SignupInput) => auth.signup(input),
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Influenciadores
// ────────────────────────────────────────────────────────────────────────────

const PAGE_SIZE = 30;

export function useInfluenciadores(filter: { nome?: string } = {}) {
  return useInfiniteQuery<PageResponse<Influenciador>>({
    queryKey: qk.influenciadores.list(filter),
    queryFn: ({ pageParam }) =>
      influenciadores.list({
        nome: filter.nome,
        size: PAGE_SIZE,
        cursor: pageParam as string | undefined,
      }),
    initialPageParam: undefined,
    getNextPageParam: last => (last.pagination.hasMore ? last.pagination.cursor : undefined),
    placeholderData: keepPreviousData,
  });
}

function inputToInfluenciadorPayload(input: InfluenciadorInput): Partial<Influenciador> {
  const ig = input.instagram.replace(/^@/, '').trim();
  const audiencia = input.audienciaTotal.trim();
  const observ = input.observacoes.trim();
  const nicho = input.nicho.trim();
  return {
    nome: input.nome.trim(),
    nicho: nicho ? nicho : null,
    handles: ig ? { instagram: ig } : {},
    audienciaTotal: audiencia ? Number(audiencia) : null,
    observacoes: observ ? observ : null,
    tags: input.tags,
  };
}

export function useCreateInfluenciador() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: InfluenciadorInput) =>
      influenciadores.create(inputToInfluenciadorPayload(input)),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.influenciadores.all });
    },
  });
}

export function useUpdateInfluenciador() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: InfluenciadorInput }) =>
      influenciadores.update(id, inputToInfluenciadorPayload(input)),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.influenciadores.all });
      qc.invalidateQueries({ queryKey: qk.influenciadores.detail(id) });
    },
  });
}

export function useDeleteInfluenciador() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => influenciadores.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.influenciadores.all }),
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Marcas
// ────────────────────────────────────────────────────────────────────────────

export function useMarcas(filter: { nome?: string } = {}) {
  return useInfiniteQuery<PageResponse<Marca>>({
    queryKey: qk.marcas.list(filter),
    queryFn: ({ pageParam }) =>
      marcas.list({
        nome: filter.nome,
        size: PAGE_SIZE,
        cursor: pageParam as string | undefined,
      }),
    initialPageParam: undefined,
    getNextPageParam: last => (last.pagination.hasMore ? last.pagination.cursor : undefined),
    placeholderData: keepPreviousData,
  });
}

function inputToMarcaPayload(input: MarcaInput): Partial<Marca> {
  const segmento = input.segmento.trim();
  const observ = input.observacoes.trim();
  const siteRaw = input.site.trim();
  const site = siteRaw && !/^https?:\/\//i.test(siteRaw) ? `https://${siteRaw}` : siteRaw;
  return {
    nome: input.nome.trim(),
    segmento: segmento ? segmento : null,
    site: site ? site : null,
    observacoes: observ ? observ : null,
    tags: input.tags,
  };
}

export function useCreateMarca() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: MarcaInput) => marcas.create(inputToMarcaPayload(input)),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.marcas.all }),
  });
}

export function useUpdateMarca() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: MarcaInput }) =>
      marcas.update(id, inputToMarcaPayload(input)),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.marcas.all });
      qc.invalidateQueries({ queryKey: qk.marcas.detail(id) });
    },
  });
}

export function useDeleteMarca() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => marcas.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.marcas.all }),
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Contatos
// ────────────────────────────────────────────────────────────────────────────

export function useContatosByMarca(marcaId: string | null | undefined) {
  return useQuery<Contato[]>({
    queryKey: marcaId ? qk.contatos.byMarca(marcaId) : ['contatos', 'none'],
    queryFn: () => contatos.listByMarca(marcaId!),
    enabled: !!marcaId,
  });
}

function inputToContatoPayload(marcaId: string, input: ContatoInput) {
  const email = input.email.trim();
  const telefone = input.telefone.trim();
  const cargo = input.cargo.trim();
  return {
    marcaId,
    nome: input.nome.trim(),
    email: email ? email : null,
    telefone: telefone ? telefone : null,
    cargo: cargo ? cargo : null,
  };
}

export function useCreateContato() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ marcaId, input }: { marcaId: string; input: ContatoInput }) =>
      contatos.create(inputToContatoPayload(marcaId, input)),
    onSuccess: (_data, { marcaId }) => {
      qc.invalidateQueries({ queryKey: qk.contatos.byMarca(marcaId) });
    },
  });
}

export function useUpdateContato() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({
      id,
      marcaId,
      input,
    }: {
      id: string;
      marcaId: string;
      input: ContatoInput;
    }) => contatos.update(id, inputToContatoPayload(marcaId, input)),
    onSuccess: (_data, { marcaId }) => {
      qc.invalidateQueries({ queryKey: qk.contatos.byMarca(marcaId) });
    },
  });
}

export function useDeleteContato() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id }: { id: string; marcaId: string }) => contatos.delete(id),
    onSuccess: (_data, { marcaId }) => {
      qc.invalidateQueries({ queryKey: qk.contatos.byMarca(marcaId) });
    },
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Perfis (RBAC)
// ────────────────────────────────────────────────────────────────────────────

export function usePerfis() {
  return useQuery<Perfil[]>({
    queryKey: qk.perfis.all,
    queryFn: () => perfis.list(),
  });
}

function inputToPerfilPayload(input: PerfilInput) {
  return {
    nome: input.nome.trim(),
    descricao: input.descricao.trim() || null,
    roles: input.roles,
  };
}

export function useCreatePerfil() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: PerfilInput) => perfis.create(inputToPerfilPayload(input)),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.perfis.all }),
  });
}

export function useUpdatePerfil() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: PerfilInput }) =>
      perfis.update(id, inputToPerfilPayload(input)),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.perfis.all }),
  });
}

export function useDeletePerfil() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => perfis.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.perfis.all }),
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Prospecções
// ────────────────────────────────────────────────────────────────────────────

export function useProspeccoes(filtros: ProspeccaoFiltros = {}) {
  return useInfiniteQuery<PageResponse<Prospeccao>>({
    queryKey: qk.prospeccoes.list(filtros),
    queryFn: ({ pageParam }) =>
      prospeccoes.list({
        ...filtros,
        size: PAGE_SIZE,
        cursor: pageParam as string | undefined,
      }),
    initialPageParam: undefined,
    getNextPageParam: last => (last.pagination.hasMore ? last.pagination.cursor : undefined),
    placeholderData: keepPreviousData,
  });
}

export function useProspeccaoEventos(id: string | null | undefined) {
  return useQuery<ProspeccaoEventoResponse[]>({
    queryKey: id ? qk.prospeccoes.eventos(id) : ['prospeccoes', 'eventos', 'none'],
    queryFn: () => prospeccoes.eventos(id!),
    enabled: !!id,
  });
}

function inputToProspeccaoPayload(input: ProspeccaoInput) {
  const valor = input.valorEstimado.replace(/\./g, '').replace(',', '.').trim();
  const valorCentavos = valor ? Math.round(Number(valor) * 100) : null;
  const proxima = input.proximaAcao.trim();
  const observ = input.observacoes.trim();
  return {
    marcaId: input.marcaId,
    influenciadorId: input.influenciadorId || null,
    assessorResponsavelId: input.assessorResponsavelId || null,
    titulo: input.titulo.trim(),
    valorEstimadoCentavos: valorCentavos,
    proximaAcao: proxima || null,
    proximaAcaoEm: input.proximaAcaoEm || null,
    observacoes: observ || null,
    tags: input.tags,
  };
}

export function useCreateProspeccao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: ProspeccaoInput) =>
      prospeccoes.create(inputToProspeccaoPayload(input)),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.prospeccoes.all }),
  });
}

export function useUpdateProspeccao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: ProspeccaoInput }) =>
      prospeccoes.update(id, inputToProspeccaoPayload(input)),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.prospeccoes.all });
      qc.invalidateQueries({ queryKey: qk.prospeccoes.detail(id) });
    },
  });
}

export function useMudarStatusProspeccao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (vars: {
      id: string;
      status: ProspeccaoStatus;
      motivoPerda?: MotivoPerda;
      motivoPerdaDetalhe?: string;
    }) =>
      prospeccoes.mudarStatus(vars.id, {
        status: vars.status,
        motivoPerda: vars.motivoPerda,
        motivoPerdaDetalhe: vars.motivoPerdaDetalhe,
      }),
    onSuccess: (_data, vars) => {
      qc.invalidateQueries({ queryKey: qk.prospeccoes.all });
      qc.invalidateQueries({ queryKey: qk.prospeccoes.eventos(vars.id) });
    },
  });
}

export function useComentarProspeccao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, texto }: { id: string; texto: string }) =>
      prospeccoes.comentar(id, texto),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.prospeccoes.eventos(id) });
    },
  });
}

export function useDeleteProspeccao() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => prospeccoes.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.prospeccoes.all }),
  });
}

export function useProspeccaoDashboard() {
  return useQuery({
    queryKey: qk.prospeccoes.dashboard,
    queryFn: () => prospeccoes.dashboard(),
    staleTime: 60_000,
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Tarefas (PRD-003)
// ────────────────────────────────────────────────────────────────────────────

export function useTarefas(filtros: TarefaFiltros = {}) {
  return useQuery<PageResponse<Tarefa>>({
    queryKey: qk.tarefas.list(filtros),
    queryFn: () => tarefas.list(filtros),
    placeholderData: keepPreviousData,
  });
}

export function useMinhasTarefas(filtros?: Pick<TarefaFiltros, 'status' | 'prazoFiltro'>) {
  return useQuery<PageResponse<Tarefa>>({
    queryKey: qk.tarefas.me(filtros),
    queryFn: () => tarefas.me(filtros),
    placeholderData: keepPreviousData,
  });
}

export function useTarefa(id: string | null | undefined) {
  return useQuery<Tarefa>({
    queryKey: id ? qk.tarefas.detail(id) : ['tarefas', 'detail', 'none'],
    queryFn: () => tarefas.get(id!),
    enabled: !!id,
  });
}

export function useTarefaComentarios(id: string | null | undefined) {
  return useQuery<TarefaComentario[]>({
    queryKey: id ? qk.tarefas.comentarios(id) : ['tarefas', 'comentarios', 'none'],
    queryFn: () => tarefas.comentarios(id!),
    enabled: !!id,
  });
}

export function useTarefaAlerta() {
  return useQuery<AlertaResponse>({
    queryKey: qk.tarefas.alerta,
    queryFn: () => tarefas.alerta(),
    refetchInterval: 60_000,
    staleTime: 30_000,
  });
}

export function useUsuarioPreferencias() {
  return useQuery<UsuarioPreferencia>({
    queryKey: qk.tarefas.preferencias,
    queryFn: () => tarefas.preferencias(),
  });
}

function inputToTarefaPayload(input: TarefaInput) {
  return {
    titulo: input.titulo.trim(),
    descricao: input.descricao?.trim() || null,
    prazo: input.prazo,
    prioridade: input.prioridade as TarefaPrioridade,
    responsavelId: input.responsavelId || null,
    entidadeTipo: (input.entidadeTipo as EntidadeTipo) || null,
    entidadeId: input.entidadeId || null,
  };
}

export function useCreateTarefa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (input: TarefaInput) => tarefas.create(inputToTarefaPayload(input) as Parameters<typeof tarefas.create>[0]),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.tarefas.all });
      qc.invalidateQueries({ queryKey: qk.tarefas.alerta });
    },
  });
}

export function useUpdateTarefa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, input }: { id: string; input: Partial<TarefaInput> }) =>
      tarefas.update(id, inputToTarefaPayload(input as TarefaInput)),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.tarefas.all });
      qc.invalidateQueries({ queryKey: qk.tarefas.detail(id) });
      qc.invalidateQueries({ queryKey: qk.tarefas.alerta });
    },
  });
}

export function useMudarStatusTarefa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, status }: { id: string; status: TarefaStatus }) =>
      tarefas.status(id, status),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.tarefas.all });
      qc.invalidateQueries({ queryKey: qk.tarefas.detail(id) });
      qc.invalidateQueries({ queryKey: qk.tarefas.alerta });
    },
  });
}

export function useDeleteTarefa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => tarefas.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.tarefas.all });
      qc.invalidateQueries({ queryKey: qk.tarefas.alerta });
    },
  });
}

export function useAddComentarioTarefa() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, texto }: { id: string; texto: string }) =>
      tarefas.addComentario(id, texto),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.tarefas.comentarios(id) });
    },
  });
}

export function useUpdatePreferencias() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (digestDiarioEnabled: boolean) =>
      tarefas.updatePreferencias(digestDiarioEnabled),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.tarefas.preferencias }),
  });
}
