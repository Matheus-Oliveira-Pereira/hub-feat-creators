'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Trash2, MessageSquare, Check, RotateCcw, Link as LinkIcon, AlertTriangle } from 'lucide-react';
import { type Tarefa, type TarefaStatus, type TarefaPrioridade } from '@/lib/api';
import {
  useTarefa,
  useTarefaComentarios,
  useMudarStatusTarefa,
  useDeleteTarefa,
  useAddComentarioTarefa,
} from '@/lib/queries';
import { comentarioSchema, type ComentarioInput } from '@/lib/schemas';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import { Skeleton } from '@/components/ui/skeleton';
import { Badge } from '@/components/ui/badge';
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

const ENTIDADE_LABEL: Record<string, string> = {
  PROSPECCAO: 'Prospecção',
  INFLUENCIADOR: 'Influenciador',
  MARCA: 'Marca',
  CONTATO: 'Contato',
};

function isVencida(tarefa: Tarefa): boolean {
  return tarefa.status !== 'FEITA' && tarefa.status !== 'CANCELADA' && new Date(tarefa.prazo) < new Date();
}

function ComentariosSection({ tarefaId }: { tarefaId: string }) {
  const { data: comentarios = [] } = useTarefaComentarios(tarefaId);
  const addComentario = useAddComentarioTarefa();

  const form = useForm<ComentarioInput>({
    resolver: zodResolver(comentarioSchema),
    defaultValues: { texto: '' },
  });

  function onSubmit(data: ComentarioInput) {
    addComentario.mutate(
      { id: tarefaId, texto: data.texto },
      {
        onSuccess: () => form.reset(),
        onError: () => toast.error('Erro ao adicionar comentário'),
      },
    );
  }

  return (
    <div className="space-y-4">
      <h3 className="flex items-center gap-2 text-sm font-semibold">
        <MessageSquare className="h-4 w-4" />
        Comentários
      </h3>

      <div className="space-y-3">
        {comentarios.length === 0 && (
          <p className="text-xs text-muted-foreground">Nenhum comentário ainda.</p>
        )}
        {comentarios.map(c => (
          <div key={c.id} className="rounded-md border border-border bg-muted/30 p-3">
            <p className="text-sm">{c.texto}</p>
            <p className="mt-1 text-xs text-muted-foreground">
              {new Date(c.createdAt).toLocaleString('pt-BR')}
            </p>
          </div>
        ))}
      </div>

      <Can role="CTAR">
        <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-2">
          <Textarea
            {...form.register('texto')}
            placeholder="Adicionar comentário..."
            rows={2}
            className="resize-none text-sm"
          />
          <Button type="submit" size="sm" disabled={addComentario.isPending}>
            Comentar
          </Button>
        </form>
      </Can>
    </div>
  );
}

export function TarefaDetailSheet({
  id,
  onClose,
}: {
  id: string | null;
  onClose: () => void;
}) {
  const { data: tarefa, isLoading } = useTarefa(id);
  const mudarStatus = useMudarStatusTarefa();
  const deletar = useDeleteTarefa();

  function handleToggleFeita() {
    if (!tarefa) return;
    const novo: TarefaStatus = tarefa.status === 'FEITA' ? 'TODO' : 'FEITA';
    mudarStatus.mutate({ id: tarefa.id, status: novo }, {
      onError: () => toast.error('Erro ao atualizar status'),
    });
  }

  function handleDelete() {
    if (!tarefa) return;
    deletar.mutate(tarefa.id, {
      onSuccess: () => { toast.success('Tarefa excluída'); onClose(); },
      onError: () => toast.error('Erro ao excluir tarefa'),
    });
  }

  return (
    <Sheet open={!!id} onOpenChange={open => !open && onClose()}>
      <SheetContent className="flex w-full flex-col gap-0 overflow-hidden p-0 sm:max-w-lg">
        {isLoading || !tarefa ? (
          <div className="space-y-4 p-6">
            <Skeleton className="h-6 w-2/3" />
            <Skeleton className="h-4 w-full" />
            <Skeleton className="h-4 w-1/2" />
          </div>
        ) : (
          <>
            <SheetHeader className="border-b border-border px-6 py-4">
              <div className="flex items-start justify-between gap-2">
                <div className="space-y-1">
                  <SheetTitle className={cn(tarefa.status === 'FEITA' && 'line-through text-muted-foreground')}>
                    {tarefa.titulo}
                  </SheetTitle>
                  <SheetDescription>
                    Criada em {new Date(tarefa.createdAt).toLocaleDateString('pt-BR')}
                  </SheetDescription>
                </div>
                <Can role="DTAR">
                  <Button
                    variant="ghost"
                    size="icon"
                    className="text-destructive hover:text-destructive"
                    onClick={handleDelete}
                    disabled={deletar.isPending}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </Can>
              </div>
            </SheetHeader>

            <div className="flex-1 overflow-y-auto px-6 py-4 space-y-6">
              {/* Meta */}
              <div className="grid grid-cols-2 gap-4 rounded-lg border border-border bg-muted/30 p-4 text-sm">
                <div>
                  <span className="text-xs text-muted-foreground">Status</span>
                  <p className="mt-0.5 font-medium">{STATUS_LABEL[tarefa.status]}</p>
                </div>
                <div>
                  <span className="text-xs text-muted-foreground">Prioridade</span>
                  <div className="mt-0.5">
                    <span className={cn('inline-flex items-center rounded px-1.5 py-0.5 text-xs font-medium', PRIORIDADE_CONFIG[tarefa.prioridade].className)}>
                      {PRIORIDADE_CONFIG[tarefa.prioridade].label}
                    </span>
                  </div>
                </div>
                <div>
                  <span className="text-xs text-muted-foreground">Prazo</span>
                  <p className={cn('mt-0.5 font-medium', isVencida(tarefa) && 'flex items-center gap-1 text-red-600')}>
                    {isVencida(tarefa) && <AlertTriangle className="h-3 w-3" />}
                    {new Date(tarefa.prazo).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
                  </p>
                </div>
                {tarefa.concluidaEm && (
                  <div>
                    <span className="text-xs text-muted-foreground">Concluída em</span>
                    <p className="mt-0.5 font-medium">
                      {new Date(tarefa.concluidaEm).toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
                    </p>
                  </div>
                )}
              </div>

              {/* Descrição */}
              {tarefa.descricao && (
                <div>
                  <h3 className="mb-1 text-sm font-semibold">Descrição</h3>
                  <p className="whitespace-pre-wrap text-sm text-muted-foreground">{tarefa.descricao}</p>
                </div>
              )}

              {/* Entidade vinculada */}
              {tarefa.entidadeTipo && tarefa.entidadeId && (
                <div>
                  <h3 className="mb-1 flex items-center gap-1 text-sm font-semibold">
                    <LinkIcon className="h-3.5 w-3.5" />
                    Vinculada a
                  </h3>
                  <Badge variant="outline" className="text-xs">
                    {ENTIDADE_LABEL[tarefa.entidadeTipo]}
                  </Badge>
                </div>
              )}

              <ComentariosSection tarefaId={tarefa.id} />
            </div>

            {/* Footer actions */}
            <div className="border-t border-border px-6 py-4 flex gap-2">
              <Can role="ETAR">
                <Button
                  variant={tarefa.status === 'FEITA' ? 'outline' : 'default'}
                  size="sm"
                  onClick={handleToggleFeita}
                  disabled={mudarStatus.isPending}
                  className="flex-1"
                >
                  {tarefa.status === 'FEITA' ? (
                    <><RotateCcw className="h-4 w-4 mr-1" /> Reabrir</>
                  ) : (
                    <><Check className="h-4 w-4 mr-1" /> Marcar como feita</>
                  )}
                </Button>
              </Can>
            </div>
          </>
        )}
      </SheetContent>
    </Sheet>
  );
}
