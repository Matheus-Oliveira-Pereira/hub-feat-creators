'use client';

import * as React from 'react';
import { useSearchParams } from 'next/navigation';
import { Plus, Calendar, List, CheckSquare, Clock, AlertTriangle } from 'lucide-react';
import { toast } from 'sonner';
import { type Tarefa, type TarefaStatus, type TarefaPrioridade, type TarefaFiltros } from '@/lib/api';
import {
  useTarefas,
  useMudarStatusTarefa,
  useDeleteTarefa,
} from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Checkbox } from '@/components/ui/checkbox';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { PageHeader } from '@/components/app/page-header';
import { EmptyState } from '@/components/app/empty-state';
import { TarefaFormModal } from '@/components/forms/tarefa-form-modal';
import { TarefaDetailSheet } from '@/components/app/tarefa-detail-sheet';
import { TarefaAgenda } from '@/components/app/tarefa-agenda';
import { Can } from '@/components/auth/can';
import { cn } from '@/lib/utils';

const PRIORIDADE_CONFIG: Record<TarefaPrioridade, { label: string; className: string }> = {
  BAIXA:   { label: 'Baixa',   className: 'bg-slate-100 text-slate-700' },
  MEDIA:   { label: 'Média',   className: 'bg-blue-100 text-blue-700' },
  ALTA:    { label: 'Alta',    className: 'bg-amber-100 text-amber-700' },
  URGENTE: { label: 'Urgente', className: 'bg-red-100 text-red-700' },
};

const STATUS_LABEL: Record<TarefaStatus, string> = {
  TODO: 'A fazer',
  EM_ANDAMENTO: 'Em andamento',
  FEITA: 'Feita',
  CANCELADA: 'Cancelada',
};

function PrioridadeBadge({ prioridade }: { prioridade: TarefaPrioridade }) {
  const cfg = PRIORIDADE_CONFIG[prioridade];
  return (
    <span className={cn('inline-flex items-center rounded px-1.5 py-0.5 text-xs font-medium', cfg.className)}>
      {cfg.label}
    </span>
  );
}

function isVencida(tarefa: Tarefa): boolean {
  return (
    tarefa.status !== 'FEITA' &&
    tarefa.status !== 'CANCELADA' &&
    new Date(tarefa.prazo) < new Date()
  );
}

function TarefaRow({
  tarefa,
  onCheck,
  onSelect,
}: {
  tarefa: Tarefa;
  onCheck: (id: string, feita: boolean) => void;
  onSelect: (id: string) => void;
}) {
  const vencida = isVencida(tarefa);
  const feita = tarefa.status === 'FEITA';

  return (
    <tr
      className={cn(
        'group border-b border-border/50 transition-colors hover:bg-muted/30 cursor-pointer',
        feita && 'opacity-50',
      )}
      onClick={() => onSelect(tarefa.id)}
    >
      <td className="w-10 py-3 pl-4" onClick={e => e.stopPropagation()}>
        <Checkbox
          checked={feita}
          aria-label={feita ? 'Marcar como pendente' : 'Marcar como feita'}
          onCheckedChange={checked => onCheck(tarefa.id, !!checked)}
          className="h-5 w-5"
        />
      </td>
      <td className="py-3 pr-4">
        <div className="flex items-center gap-2">
          <span className={cn('text-sm font-medium', feita && 'line-through text-muted-foreground')}>
            {tarefa.titulo}
          </span>
        </div>
      </td>
      <td className="hidden py-3 pr-4 md:table-cell">
        <PrioridadeBadge prioridade={tarefa.prioridade} />
      </td>
      <td className="hidden py-3 pr-4 sm:table-cell">
        <span
          className={cn(
            'flex items-center gap-1 text-xs',
            vencida ? 'font-semibold text-red-600' : 'text-muted-foreground',
          )}
        >
          {vencida && <AlertTriangle className="h-3 w-3" />}
          {vencida && 'Atrasada · '}
          {new Date(tarefa.prazo).toLocaleDateString('pt-BR')}
        </span>
      </td>
      <td className="py-3 pr-4">
        <Badge variant="outline" className="text-xs">
          {STATUS_LABEL[tarefa.status]}
        </Badge>
      </td>
    </tr>
  );
}

function TarefaListSkeleton() {
  return (
    <div className="space-y-2">
      {[...Array(5)].map((_, i) => (
        <Skeleton key={i} className="h-12 w-full rounded" />
      ))}
    </div>
  );
}

type ViewMode = 'lista' | 'agenda';

function TarefasInner() {
  const searchParams = useSearchParams();
  const [view, setView] = React.useState<ViewMode>('lista');
  const [selectedId, setSelectedId] = React.useState<string | null>(null);
  const [createOpen, setCreateOpen] = React.useState(searchParams.get('new') === '1');

  const [filtros, setFiltros] = React.useState<TarefaFiltros>({
    prazoFiltro: undefined,
    status: undefined,
    prioridade: undefined,
  });

  const { data, isLoading } = useTarefas(filtros);
  const mudarStatus = useMudarStatusTarefa();

  const tarefasList = data?.data ?? [];

  function handleCheck(id: string, feita: boolean) {
    mudarStatus.mutate(
      { id, status: feita ? 'FEITA' : 'TODO' },
      {
        onError: () => toast.error('Erro ao atualizar tarefa'),
      },
    );
  }

  return (
    <div className="flex flex-col gap-4">
      <PageHeader
        title="Tarefas"
        description="Gerencie suas pendências e acompanhe prazos"
        actions={
          <div className="flex items-center gap-2">
            {/* View toggle */}
            <div className="flex rounded-md border border-border overflow-hidden">
              <button
                onClick={() => setView('lista')}
                className={cn(
                  'px-3 py-1.5 text-sm flex items-center gap-1.5 transition-colors',
                  view === 'lista' ? 'bg-primary text-primary-foreground' : 'hover:bg-muted',
                )}
              >
                <List className="h-3.5 w-3.5" />
                Lista
              </button>
              <button
                onClick={() => setView('agenda')}
                className={cn(
                  'px-3 py-1.5 text-sm flex items-center gap-1.5 transition-colors',
                  view === 'agenda' ? 'bg-primary text-primary-foreground' : 'hover:bg-muted',
                )}
              >
                <Calendar className="h-3.5 w-3.5" />
                Agenda
              </button>
            </div>
            <Can role="CTAR">
              <Button size="sm" onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4 mr-1" />
                Nova tarefa
              </Button>
            </Can>
          </div>
        }
      />

      {/* Filtros */}
      <div className="flex flex-wrap gap-2">
        <Select
          value={filtros.prazoFiltro ?? ''}
          onValueChange={v =>
            setFiltros(f => ({ ...f, prazoFiltro: v as TarefaFiltros['prazoFiltro'] || undefined }))
          }
        >
          <SelectTrigger className="h-8 w-36 text-xs">
            <SelectValue placeholder="Prazo" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Todos</SelectItem>
            <SelectItem value="VENCIDAS">Vencidas</SelectItem>
            <SelectItem value="HOJE">Hoje</SelectItem>
            <SelectItem value="SEMANA">Esta semana</SelectItem>
            <SelectItem value="FUTURAS">Futuras</SelectItem>
          </SelectContent>
        </Select>

        <Select
          value={filtros.status ?? ''}
          onValueChange={v =>
            setFiltros(f => ({ ...f, status: (v as TarefaStatus) || undefined }))
          }
        >
          <SelectTrigger className="h-8 w-36 text-xs">
            <SelectValue placeholder="Status" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Todos</SelectItem>
            {(Object.keys(STATUS_LABEL) as TarefaStatus[]).map(s => (
              <SelectItem key={s} value={s}>{STATUS_LABEL[s]}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <Select
          value={filtros.prioridade ?? ''}
          onValueChange={v =>
            setFiltros(f => ({ ...f, prioridade: (v as TarefaPrioridade) || undefined }))
          }
        >
          <SelectTrigger className="h-8 w-32 text-xs">
            <SelectValue placeholder="Prioridade" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="">Todas</SelectItem>
            {(Object.keys(PRIORIDADE_CONFIG) as TarefaPrioridade[]).map(p => (
              <SelectItem key={p} value={p}>{PRIORIDADE_CONFIG[p].label}</SelectItem>
            ))}
          </SelectContent>
        </Select>

        <button
          className={cn(
            'flex items-center gap-1.5 rounded-md border px-3 py-1 text-xs transition-colors',
            filtros.minhas ? 'border-primary bg-primary/10 text-primary' : 'border-border hover:bg-muted',
          )}
          onClick={() => setFiltros(f => ({ ...f, minhas: !f.minhas }))}
        >
          <CheckSquare className="h-3.5 w-3.5" />
          Minhas tarefas
        </button>
      </div>

      {/* Content */}
      {view === 'agenda' ? (
        <TarefaAgenda tarefas={tarefasList} onSelect={setSelectedId} />
      ) : isLoading ? (
        <TarefaListSkeleton />
      ) : tarefasList.length === 0 ? (
        <EmptyState
          title="Nenhuma tarefa encontrada"
          description="Crie uma tarefa para começar"
          action={
            <Can role="CTAR">
              <Button size="sm" onClick={() => setCreateOpen(true)}>
                <Plus className="h-4 w-4 mr-1" /> Nova tarefa
              </Button>
            </Can>
          }
        />
      ) : (
        <div className="rounded-lg border border-border bg-card overflow-hidden">
          <table className="w-full">
            <thead>
              <tr className="border-b border-border bg-muted/40">
                <th className="w-10 py-2 pl-4" />
                <th className="py-2 pr-4 text-left text-xs font-medium text-muted-foreground">Tarefa</th>
                <th className="hidden py-2 pr-4 text-left text-xs font-medium text-muted-foreground md:table-cell">Prioridade</th>
                <th className="hidden py-2 pr-4 text-left text-xs font-medium text-muted-foreground sm:table-cell">Prazo</th>
                <th className="py-2 pr-4 text-left text-xs font-medium text-muted-foreground">Status</th>
              </tr>
            </thead>
            <tbody>
              {tarefasList.map(t => (
                <TarefaRow
                  key={t.id}
                  tarefa={t}
                  onCheck={handleCheck}
                  onSelect={setSelectedId}
                />
              ))}
            </tbody>
          </table>
        </div>
      )}

      <TarefaFormModal open={createOpen} onOpenChange={setCreateOpen} />
      <TarefaDetailSheet id={selectedId} onClose={() => setSelectedId(null)} />
    </div>
  );
}

export default function TarefasPage() {
  return (
    <React.Suspense fallback={<TarefaListSkeleton />}>
      <TarefasInner />
    </React.Suspense>
  );
}
