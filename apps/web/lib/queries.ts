'use client';

import { useEffect, useRef } from 'react';
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
  email,
  notificacoes,
  webpush,
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
  EmailAccount,
  EmailAccountPayload,
  EmailAccountUpdatePayload,
  EmailTemplate,
  EmailTemplatePayload,
  EmailLayout,
  EmailEnvio,
  EmailEnvioPayload,
  EmailEvento,
  Notificacao,
  NotificacaoPreferencia,
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
  email: {
    accounts: ['email', 'accounts'] as const,
    account: (id: string) => ['email', 'accounts', id] as const,
    templates: ['email', 'templates'] as const,
    template: (id: string) => ['email', 'templates', id] as const,
    layout: ['email', 'layout'] as const,
    envios: (params?: object) => ['email', 'envios', params] as const,
    envio: (id: string) => ['email', 'envios', id] as const,
    eventos: (id: string) => ['email', 'envios', id, 'eventos'] as const,
  },
  notificacoes: {
    all: ['notificacoes'] as const,
    list: (params?: object) => ['notificacoes', 'list', params] as const,
    contagem: ['notificacoes', 'contagem'] as const,
    prefs: ['notificacoes', 'prefs'] as const,
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

// ─── Email ─────────────────────────────────────────────────────────────────

export function useEmailAccounts() {
  return useQuery({ queryKey: qk.email.accounts, queryFn: () => email.accounts.list() });
}

export function useEmailAccount(id: string) {
  return useQuery({ queryKey: qk.email.account(id), queryFn: () => email.accounts.get(id) });
}

export function useCreateEmailAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EmailAccountPayload) => email.accounts.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.accounts }),
  });
}

export function useUpdateEmailAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: EmailAccountUpdatePayload }) =>
      email.accounts.update(id, data),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.email.accounts });
      qc.invalidateQueries({ queryKey: qk.email.account(id) });
    },
  });
}

export function useDeleteEmailAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => email.accounts.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.accounts }),
  });
}

export function useTestEmailAccount() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => email.accounts.test(id),
    onSuccess: (_data, id) => qc.invalidateQueries({ queryKey: qk.email.account(id) }),
  });
}

export function useEmailTemplates() {
  return useQuery({ queryKey: qk.email.templates, queryFn: () => email.templates.list() });
}

export function useEmailTemplate(id: string) {
  return useQuery({ queryKey: qk.email.template(id), queryFn: () => email.templates.get(id), enabled: !!id });
}

export function useCreateEmailTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EmailTemplatePayload) => email.templates.create(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.templates }),
  });
}

export function useUpdateEmailTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: Partial<EmailTemplatePayload> }) =>
      email.templates.update(id, data),
    onSuccess: (_data, { id }) => {
      qc.invalidateQueries({ queryKey: qk.email.templates });
      qc.invalidateQueries({ queryKey: qk.email.template(id) });
    },
  });
}

export function useDeleteEmailTemplate() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => email.templates.delete(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.templates }),
  });
}

export function usePreviewEmailTemplate() {
  return useMutation({
    mutationFn: ({ id, vars }: { id: string; vars: Record<string, unknown> }) =>
      email.templates.preview(id, vars),
  });
}

export function useEmailLayout() {
  return useQuery({ queryKey: qk.email.layout, queryFn: () => email.layout.get() });
}

export function useSaveEmailLayout() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: Partial<Pick<EmailLayout, 'headerHtml' | 'footerHtml'>>) =>
      email.layout.save(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.layout }),
  });
}

export function useEmailEnvios(params?: { contexto?: string; page?: number; size?: number }) {
  return useQuery({
    queryKey: qk.email.envios(params),
    queryFn: () => email.envios.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useEmailEnvio(id: string) {
  return useQuery({ queryKey: qk.email.envio(id), queryFn: () => email.envios.get(id), enabled: !!id });
}

export function useSendEmail() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (data: EmailEnvioPayload) => email.envios.send(data),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.email.envios() }),
  });
}

export function useEmailEventos(envioId: string) {
  return useQuery({
    queryKey: qk.email.eventos(envioId),
    queryFn: () => email.envios.eventos(envioId),
    enabled: !!envioId,
  });
}

// ────────────────────────────────────────────────────────────────────────────
// Notificações
// ────────────────────────────────────────────────────────────────────────────

export function useNotificacoes(params?: { apenasNaoLidas?: boolean; tipo?: string; page?: number; size?: number }) {
  return useQuery<PageResponse<Notificacao>>({
    queryKey: qk.notificacoes.list(params),
    queryFn: () => notificacoes.list(params),
    placeholderData: keepPreviousData,
  });
}

export function useNotificacaoContagem() {
  return useQuery<{ naoLidas: number }>({
    queryKey: qk.notificacoes.contagem,
    queryFn: () => notificacoes.contagem(),
    staleTime: 30_000,
    refetchInterval: 60_000,
  });
}

export function useMarcarLida() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => notificacoes.marcarLida(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.notificacoes.contagem });
      qc.invalidateQueries({ queryKey: qk.notificacoes.all });
    },
  });
}

export function useMarcarTodasLidas() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: () => notificacoes.marcarTodasLidas(),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: qk.notificacoes.contagem });
      qc.invalidateQueries({ queryKey: qk.notificacoes.all });
    },
  });
}

export function useNotificacaoPrefs() {
  return useQuery<NotificacaoPreferencia[]>({
    queryKey: qk.notificacoes.prefs,
    queryFn: () => notificacoes.prefs(),
  });
}

export function useUpdateNotificacaoPref() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: ({ tipo, canal, habilitado }: { tipo: string; canal: string; habilitado: boolean }) =>
      notificacoes.updatePref(tipo, canal, habilitado),
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.notificacoes.prefs }),
  });
}

export function useWebPushSubscribe() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: async () => {
      const { publicKey } = await webpush.publicKey();
      if (!publicKey) throw new Error('VAPID public key not configured');
      const reg = await navigator.serviceWorker.ready;
      const sub = await reg.pushManager.subscribe({
        userVisibleOnly: true,
        applicationServerKey: urlBase64ToUint8Array(publicKey),
      });
      await webpush.subscribe(sub.toJSON() as PushSubscriptionJSON);
      return sub;
    },
    onSuccess: () => qc.invalidateQueries({ queryKey: qk.notificacoes.prefs }),
  });
}

function urlBase64ToUint8Array(base64String: string): Uint8Array {
  const padding = '='.repeat((4 - (base64String.length % 4)) % 4);
  const base64 = (base64String + padding).replace(/-/g, '+').replace(/_/g, '/');
  const rawData = window.atob(base64);
  return Uint8Array.from([...rawData].map(c => c.charCodeAt(0)));
}

export function useNotificacaoSSE(enabled = true) {
  const qc = useQueryClient();
  const reconnectTimeout = useRef<ReturnType<typeof setTimeout>>();

  useEffect(() => {
    if (!enabled) return;

    let es: EventSource | null = null;
    let closed = false;

    const connect = () => {
      if (closed) return;
      const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
      if (!token) return;

      const url = `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/api/v1/notificacoes/stream`;
      es = new EventSource(url, { withCredentials: true });

      es.onmessage = () => {
        qc.invalidateQueries({ queryKey: qk.notificacoes.contagem });
      };

      es.addEventListener('notificacao', () => {
        qc.invalidateQueries({ queryKey: qk.notificacoes.contagem });
      });

      es.onerror = () => {
        es?.close();
        if (!closed) {
          reconnectTimeout.current = setTimeout(connect, 3000);
        }
      };
    };

    connect();

    return () => {
      closed = true;
      clearTimeout(reconnectTimeout.current);
      es?.close();
    };
  }, [enabled, qc]);
}
