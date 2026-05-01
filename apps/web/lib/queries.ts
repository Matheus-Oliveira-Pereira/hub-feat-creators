'use client';

import {
  useQuery,
  useMutation,
  useInfiniteQuery,
  useQueryClient,
  keepPreviousData,
} from '@tanstack/react-query';
import { auth, influenciadores, marcas, Influenciador, Marca, PageResponse } from '@/lib/api';
import type { InfluenciadorInput, MarcaInput, LoginInput, SignupInput } from '@/lib/schemas';

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

export function useInfluenciadores(filter: { nome?: string } = {}) {
  return useInfiniteQuery<PageResponse<Influenciador>>({
    queryKey: qk.influenciadores.list(filter),
    queryFn: ({ pageParam }) =>
      influenciadores.list({
        nome: filter.nome,
        cursor: pageParam as string | undefined,
      }),
    initialPageParam: undefined,
    getNextPageParam: last => (last.pagination.hasMore ? last.pagination.cursor : undefined),
    placeholderData: keepPreviousData,
  });
}

function inputToInfluenciadorPayload(input: InfluenciadorInput): Partial<Influenciador> {
  return {
    nome: input.nome,
    nicho: input.nicho ?? null,
    handles: input.instagram ? { instagram: input.instagram } : {},
    audienciaTotal: input.audienciaTotal ?? null,
    observacoes: input.observacoes ?? null,
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
      marcas.list({ nome: filter.nome, cursor: pageParam as string | undefined }),
    initialPageParam: undefined,
    getNextPageParam: last => (last.pagination.hasMore ? last.pagination.cursor : undefined),
    placeholderData: keepPreviousData,
  });
}

function inputToMarcaPayload(input: MarcaInput): Partial<Marca> {
  return {
    nome: input.nome,
    segmento: input.segmento ?? null,
    site: input.site ?? null,
    observacoes: input.observacoes ?? null,
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
