'use client';

import * as React from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Plus,
  Download,
  MoreHorizontal,
  Trash2,
  Eye,
  Pencil,
  XCircle,
  Trophy,
  ChevronRight,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { type Prospeccao, type ProspeccaoStatus } from '@/lib/api';
import {
  useProspeccoes,
  useDeleteProspeccao,
  useMudarStatusProspeccao,
  useMarcas,
  useInfluenciadores,
} from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/app/page-header';
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';
import { ProspeccaoKanban } from '@/components/app/prospeccao-kanban';
import { ProspeccaoDetailSheet } from '@/components/app/prospeccao-detail-sheet';
import { ProspeccaoFormModal } from '@/components/forms/prospeccao-form-modal';
import { FecharPerdidaModal } from '@/components/forms/fechar-perdida-modal';
import { Can } from '@/components/auth/can';
import { Input } from '@/components/ui/input';
import { Search, X } from 'lucide-react';
import { cn } from '@/lib/utils';
import {
  STATUS_LABEL,
  STATUS_TONE,
  STATUS_ORDER,
  formatBRL,
  isValidTransition,
} from '@/lib/prospeccao';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

export default function ProspeccaoPage() {
  return (
    <React.Suspense fallback={null}>
      <ProspeccaoInner />
    </React.Suspense>
  );
}

function ProspeccaoInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [search, setSearch] = React.useState('');
  const [debounced, setDebounced] = React.useState('');
  const [view, setView] = React.useState<'kanban' | 'list'>('kanban');
  const [statusFilter, setStatusFilter] = React.useState<ProspeccaoStatus | undefined>();
  const [formState, setFormState] = React.useState<{ open: boolean; item: Prospeccao | null }>({
    open: false,
    item: null,
  });
  const [perdidaTarget, setPerdidaTarget] = React.useState<string | null>(null);
  const [detail, setDetail] = React.useState<Prospeccao | null>(null);

  React.useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 250);
    return () => clearTimeout(t);
  }, [search]);

  const query = useProspeccoes({
    nome: debounced || undefined,
    status: statusFilter,
  });
  const data = React.useMemo(
    () => query.data?.pages.flatMap(p => p.data) ?? [],
    [query.data]
  );

  const marcasQ = useMarcas();
  const marcaNomeById = React.useMemo(() => {
    const map = new Map<string, string>();
    (marcasQ.data?.pages.flatMap(p => p.data) ?? []).forEach(m => map.set(m.id, m.nome));
    return map;
  }, [marcasQ.data]);

  const influQ = useInfluenciadores();
  const influNomeById = React.useMemo(() => {
    const map = new Map<string, string>();
    (influQ.data?.pages.flatMap(p => p.data) ?? []).forEach(i => map.set(i.id, i.nome));
    return map;
  }, [influQ.data]);

  // mantém detail sincronizado com lista (após edit/move)
  React.useEffect(() => {
    if (!detail) return;
    const fresh = data.find(p => p.id === detail.id);
    if (fresh && fresh !== detail) setDetail(fresh);
  }, [data, detail]);

  React.useEffect(() => {
    if (query.error) router.push('/login');
  }, [query.error, router]);

  React.useEffect(() => {
    if (searchParams.get('new') === '1') {
      setFormState({ open: true, item: null });
      router.replace('/prospeccao');
    }
  }, [searchParams, router]);

  const del = useDeleteProspeccao();
  const mudarStatus = useMudarStatusProspeccao();

  async function handleDelete(p: Prospeccao) {
    if (!confirm(`Remover "${p.titulo}"?`)) return;
    try {
      await del.mutateAsync(p.id);
      setDetail(null);
      toast.success('Removida.');
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao remover.');
    }
  }

  function openEdit(p: Prospeccao) {
    setDetail(null);
    setFormState({ open: true, item: p });
  }

  async function handleMove(p: Prospeccao, novo: ProspeccaoStatus) {
    if (novo === 'FECHADA_PERDIDA') {
      setPerdidaTarget(p.id);
      return;
    }
    try {
      await mudarStatus.mutateAsync({ id: p.id, status: novo });
      toast.success(`Movida para ${STATUS_LABEL[novo]}`);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Transição inválida.');
    }
  }

  const loading = query.isLoading;

  return (
    <div className="mx-auto w-full max-w-[1400px] px-4 py-8 md:px-8 md:py-12">
      <PageHeader
        eyebrow="Pipeline"
        title="Prospecção"
        description="Pipeline marca↔influenciador. Arraste cards entre colunas para mudar o status."
        actions={
          <>
            <Can role="EXPT">
              <Button asChild variant="outline">
                <a href={`${API_URL}/api/v1/prospeccoes/export.csv`}>
                  <Download className="h-4 w-4" /> Exportar CSV
                </a>
              </Button>
            </Can>
            <Can role="CPRO">
              <Button onClick={() => setFormState({ open: true, item: null })}>
                <Plus className="h-4 w-4" /> Nova
              </Button>
            </Can>
          </>
        }
      />

      {/* Filtros */}
      <div className="flex flex-col gap-3 mb-6 lg:flex-row lg:items-center lg:justify-between">
        <div className="flex flex-1 items-center gap-2 flex-wrap">
          <div className="relative flex-1 max-w-sm">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              value={search}
              onChange={e => setSearch(e.target.value)}
              placeholder="Buscar por título…"
              className="pl-9 pr-9"
            />
            {search && (
              <button
                onClick={() => setSearch('')}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground"
                aria-label="Limpar"
              >
                <X className="h-3.5 w-3.5" />
              </button>
            )}
          </div>
          <div className="flex flex-wrap gap-1">
            <button
              onClick={() => setStatusFilter(undefined)}
              className={cn(
                'inline-flex items-center gap-1 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors',
                !statusFilter
                  ? 'border-foreground bg-foreground text-background'
                  : 'border-border text-muted-foreground hover:bg-accent'
              )}
            >
              Todas
            </button>
            {STATUS_ORDER.map(s => {
              const tone = STATUS_TONE[s];
              const active = statusFilter === s;
              return (
                <button
                  key={s}
                  onClick={() => setStatusFilter(active ? undefined : s)}
                  className={cn(
                    'inline-flex items-center gap-1.5 rounded-full border px-2.5 py-1 text-xs font-medium transition-colors',
                    active
                      ? cn(tone.bg, tone.text, 'ring-1', tone.ring, 'border-transparent')
                      : 'border-border text-muted-foreground hover:bg-accent'
                  )}
                >
                  <span className={cn('h-1.5 w-1.5 rounded-full', tone.dot)} aria-hidden="true" />
                  {STATUS_LABEL[s]}
                </button>
              );
            })}
          </div>
          <span className="text-sm text-muted-foreground tabular-nums whitespace-nowrap">
            {data.length} {data.length === 1 ? 'oportunidade' : 'oportunidades'}
          </span>
        </div>
        <Tabs value={view} onValueChange={v => setView(v as 'kanban' | 'list')}>
          <TabsList>
            <TabsTrigger value="kanban">Kanban</TabsTrigger>
            <TabsTrigger value="list">Lista</TabsTrigger>
          </TabsList>
        </Tabs>
      </div>

      {loading ? (
        view === 'kanban' ? (
          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-3">
            {Array.from({ length: 5 }).map((_, i) => (
              <Skeleton key={i} className="h-72 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <Skeleton className="h-96 w-full rounded-xl" />
        )
      ) : data.length === 0 ? (
        <EmptyState
          illustration={<EmptyIllustration variant="chart" />}
          title={debounced || statusFilter ? 'Nada encontrado' : 'Nenhuma oportunidade ainda'}
          description={
            debounced || statusFilter
              ? 'Tente outro filtro.'
              : 'Crie sua primeira prospecção. Ela aparece como NOVA e segue pelo pipeline.'
          }
          action={
            !debounced &&
            !statusFilter && (
              <Can role="CPRO">
                <Button onClick={() => setFormState({ open: true, item: null })}>
                  <Plus className="h-4 w-4" /> Nova prospecção
                </Button>
              </Can>
            )
          }
        />
      ) : view === 'kanban' ? (
        <ProspeccaoKanban
          items={data}
          marcaNomeById={marcaNomeById}
          onCardClick={p => setDetail(p)}
          onMove={handleMove}
        />
      ) : (
        <Card className="overflow-hidden">
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 border-b border-border">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Título</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Status</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Marca</th>
                  <th className="text-right px-5 py-3 font-medium text-muted-foreground">Valor</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Próxima</th>
                  <th className="px-5 py-3 w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                <AnimatePresence mode="popLayout">
                  {data.map(p => {
                    const tone = STATUS_TONE[p.status];
                    return (
                      <motion.tr
                        key={p.id}
                        layout
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        className="cursor-pointer transition-colors hover:bg-accent/50"
                        onClick={() => setDetail(p)}
                      >
                        <td className="px-5 py-3 font-medium">{p.titulo}</td>
                        <td className="px-5 py-3">
                          <Badge
                            className={cn(
                              tone.bg,
                              tone.text,
                              'border-transparent ring-1',
                              tone.ring
                            )}
                          >
                            <span className={cn('h-1.5 w-1.5 rounded-full mr-1', tone.dot)} />
                            {STATUS_LABEL[p.status]}
                          </Badge>
                        </td>
                        <td className="px-5 py-3 text-muted-foreground">
                          {marcaNomeById.get(p.marcaId) ?? '—'}
                        </td>
                        <td className="px-5 py-3 text-right tabular-nums">
                          {formatBRL(p.valorEstimadoCentavos)}
                        </td>
                        <td className="px-5 py-3 text-muted-foreground">
                          {p.proximaAcaoEm
                            ? new Date(p.proximaAcaoEm).toLocaleDateString('pt-BR')
                            : '—'}
                        </td>
                        <td className="px-5 py-3 text-right">
                          <RowMenu
                            p={p}
                            onView={() => setDetail(p)}
                            onEdit={() => openEdit(p)}
                            onMove={handleMove}
                            onDelete={() => handleDelete(p)}
                          />
                        </td>
                      </motion.tr>
                    );
                  })}
                </AnimatePresence>
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      {query.hasNextPage && (
        <div className="mt-6 flex justify-center">
          <Button
            variant="outline"
            onClick={() => query.fetchNextPage()}
            disabled={query.isFetchingNextPage}
          >
            {query.isFetchingNextPage ? 'Carregando…' : 'Carregar mais'}
          </Button>
        </div>
      )}

      <ProspeccaoFormModal
        open={formState.open}
        onOpenChange={open => setFormState(prev => ({ ...prev, open }))}
        prospeccao={formState.item}
      />

      {perdidaTarget && (
        <FecharPerdidaModal
          open={!!perdidaTarget}
          onOpenChange={open => !open && setPerdidaTarget(null)}
          prospeccaoId={perdidaTarget}
        />
      )}

      <ProspeccaoDetailSheet
        prospeccao={detail}
        onOpenChange={open => !open && setDetail(null)}
        marcaNomeById={marcaNomeById}
        influNomeById={influNomeById}
        onEdit={openEdit}
        onDelete={handleDelete}
        onMove={handleMove}
      />
    </div>
  );
}

function RowMenu({
  p,
  onView,
  onEdit,
  onMove,
  onDelete,
}: {
  p: Prospeccao;
  onView: () => void;
  onEdit: () => void;
  onMove: (p: Prospeccao, novo: ProspeccaoStatus) => void;
  onDelete: () => void;
}) {
  const proximas = STATUS_ORDER.filter(s => s !== p.status && isValidTransition(p.status, s));
  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild onClick={e => e.stopPropagation()}>
        <Button variant="ghost" size="icon-sm">
          <MoreHorizontal className="h-4 w-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
        <DropdownMenuItem onClick={onView}>
          <Eye className="h-4 w-4" /> Detalhes
        </DropdownMenuItem>
        <DropdownMenuItem onClick={onEdit}>
          <Pencil className="h-4 w-4" /> Editar
        </DropdownMenuItem>
        {proximas.length > 0 && (
          <>
            <DropdownMenuSeparator />
            <DropdownMenuLabel>Mover para</DropdownMenuLabel>
            {proximas.map(s => (
              <DropdownMenuItem key={s} onClick={() => onMove(p, s)}>
                {s === 'FECHADA_GANHA' && <Trophy className="h-4 w-4" />}
                {s === 'FECHADA_PERDIDA' && <XCircle className="h-4 w-4" />}
                {!['FECHADA_GANHA', 'FECHADA_PERDIDA'].includes(s) && (
                  <ChevronRight className="h-4 w-4" />
                )}
                {STATUS_LABEL[s]}
              </DropdownMenuItem>
            ))}
          </>
        )}
        <DropdownMenuSeparator />
        <Can role="DPRO">
          <DropdownMenuItem
            onClick={onDelete}
            className="text-destructive focus:text-destructive"
          >
            <Trash2 className="h-4 w-4" /> Remover
          </DropdownMenuItem>
        </Can>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
