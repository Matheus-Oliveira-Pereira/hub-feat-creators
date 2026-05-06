'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import {
  Pencil,
  Trash2,
  Building2,
  Calendar,
  AtSign,
  ArrowRight,
  Clock,
  MessageSquare,
  Mail,
  Link as LinkIcon,
  XCircle,
  Trophy,
  RotateCcw,
} from 'lucide-react';
import {
  type Prospeccao,
  type ProspeccaoEventoResponse,
  type ProspeccaoStatus,
  type EventoTipo,
} from '@/lib/api';
import {
  useProspeccaoEventos,
  useComentarProspeccao,
  useMudarStatusProspeccao,
} from '@/lib/queries';
import { comentarioSchema, type ComentarioInput } from '@/lib/schemas';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Can } from '@/components/auth/can';
import { Timeline } from '@/components/app/timeline';
import {
  STATUS_LABEL,
  STATUS_TONE,
  STATUS_ORDER,
  formatBRL,
  isValidTransition,
} from '@/lib/prospeccao';
import { cn } from '@/lib/utils';

interface Props {
  prospeccao: Prospeccao | null;
  onOpenChange: (open: boolean) => void;
  marcaNomeById?: Map<string, string>;
  influNomeById?: Map<string, string>;
  onEdit: (p: Prospeccao) => void;
  onDelete: (p: Prospeccao) => void;
  onMove: (p: Prospeccao, novo: ProspeccaoStatus) => void;
}

export function ProspeccaoDetailSheet({
  prospeccao,
  onOpenChange,
  marcaNomeById,
  influNomeById,
  onEdit,
  onDelete,
  onMove,
}: Props) {
  return (
    <Sheet open={!!prospeccao} onOpenChange={open => !open && onOpenChange(false)}>
      <SheetContent className="sm:max-w-xl flex flex-col">
        {prospeccao && (
          <Inner
            p={prospeccao}
            marcaNomeById={marcaNomeById}
            influNomeById={influNomeById}
            onEdit={onEdit}
            onDelete={onDelete}
            onMove={onMove}
          />
        )}
      </SheetContent>
    </Sheet>
  );
}

function Inner({
  p,
  marcaNomeById,
  influNomeById,
  onEdit,
  onDelete,
  onMove,
}: {
  p: Prospeccao;
  marcaNomeById?: Map<string, string>;
  influNomeById?: Map<string, string>;
  onEdit: (p: Prospeccao) => void;
  onDelete: (p: Prospeccao) => void;
  onMove: (p: Prospeccao, novo: ProspeccaoStatus) => void;
}) {
  const tone = STATUS_TONE[p.status];
  const proximas = STATUS_ORDER.filter(s => s !== p.status && isValidTransition(p.status, s));
  const marcaNome = marcaNomeById?.get(p.marcaId);
  const influNome = p.influenciadorId ? influNomeById?.get(p.influenciadorId) : null;

  return (
    <>
      <SheetHeader>
        <div className="flex items-start gap-3 mb-2">
          <div className="min-w-0 flex-1">
            <Badge
              className={cn(tone.bg, tone.text, 'border-transparent ring-1', tone.ring, 'mb-2')}
            >
              <span className={cn('h-1.5 w-1.5 rounded-full mr-1', tone.dot)} />
              {STATUS_LABEL[p.status]}
            </Badge>
            <SheetTitle className="leading-tight">{p.titulo}</SheetTitle>
            {marcaNome && (
              <SheetDescription className="flex items-center gap-1.5 mt-1">
                <Building2 className="h-3.5 w-3.5" />
                {marcaNome}
                {influNome && (
                  <>
                    <ArrowRight className="h-3 w-3 text-subtle" />
                    <AtSign className="h-3.5 w-3.5" />
                    {influNome}
                  </>
                )}
              </SheetDescription>
            )}
          </div>
        </div>
      </SheetHeader>

      <Tabs defaultValue="dados" className="flex-1 flex flex-col mt-4 min-h-0">
        <TabsList className="self-start">
          <TabsTrigger value="dados">Dados</TabsTrigger>
          <TabsTrigger value="timeline">
            <Clock className="h-3.5 w-3.5" /> Timeline
          </TabsTrigger>
          <TabsTrigger value="comentarios">
            <MessageSquare className="h-3.5 w-3.5" /> Comentários
          </TabsTrigger>
        </TabsList>

        <TabsContent value="dados" className="flex-1 overflow-y-auto scrollbar-thin pr-1">
          <DadosTab p={p} />
        </TabsContent>
        <TabsContent value="timeline" className="flex-1 overflow-y-auto scrollbar-thin pr-1">
          <Timeline entidadeTipo="PROSPECCAO" entidadeId={p.id} />
        </TabsContent>
        <TabsContent value="comentarios" className="flex-1 overflow-y-auto scrollbar-thin pr-1">
          <ComentariosTab prospeccaoId={p.id} />
        </TabsContent>
      </Tabs>

      {/* Footer com ações */}
      <div className="border-t border-border pt-3 mt-3 flex flex-wrap items-center gap-2">
        <Can role="EPRO">
          <Button variant="outline" size="sm" onClick={() => onEdit(p)}>
            <Pencil className="h-4 w-4" /> Editar
          </Button>
        </Can>
        {proximas.length > 0 && (
          <Can role="EPRO">
            <div className="flex flex-wrap gap-1.5">
              {proximas.map(s => {
                const t = STATUS_TONE[s];
                const Icon =
                  s === 'FECHADA_GANHA'
                    ? Trophy
                    : s === 'FECHADA_PERDIDA'
                      ? XCircle
                      : s === 'NOVA' && p.status === 'FECHADA_PERDIDA'
                        ? RotateCcw
                        : ArrowRight;
                return (
                  <Button
                    key={s}
                    variant="subtle"
                    size="sm"
                    onClick={() => onMove(p, s)}
                    className={cn('gap-1.5', t.text)}
                  >
                    <Icon className="h-3.5 w-3.5" />
                    {STATUS_LABEL[s]}
                  </Button>
                );
              })}
            </div>
          </Can>
        )}
        <div className="ml-auto">
          <Can role="DPRO">
            <Button
              variant="ghost"
              size="sm"
              onClick={() => onDelete(p)}
              className="text-destructive hover:text-destructive"
            >
              <Trash2 className="h-4 w-4" /> Remover
            </Button>
          </Can>
        </div>
      </div>
    </>
  );
}

function DadosTab({ p }: { p: Prospeccao }) {
  return (
    <div className="space-y-5 py-3">
      <Section title="Valor estimado">
        <p className="font-display text-2xl font-semibold tabular-nums">
          {formatBRL(p.valorEstimadoCentavos)}
        </p>
      </Section>

      {(p.proximaAcao || p.proximaAcaoEm) && (
        <Section title="Próxima ação">
          {p.proximaAcaoEm && (
            <p className="inline-flex items-center gap-1.5 text-sm text-muted-foreground mb-1">
              <Calendar className="h-3.5 w-3.5" />
              {new Date(p.proximaAcaoEm).toLocaleDateString('pt-BR', {
                weekday: 'short',
                day: '2-digit',
                month: 'short',
              })}
            </p>
          )}
          {p.proximaAcao && <p className="text-sm">{p.proximaAcao}</p>}
        </Section>
      )}

      <Section title="Tags">
        {p.tags.length === 0 ? (
          <p className="text-sm text-muted-foreground">Sem tags.</p>
        ) : (
          <div className="flex flex-wrap gap-1.5">
            {p.tags.map(t => (
              <Badge key={t} variant="secondary">
                {t}
              </Badge>
            ))}
          </div>
        )}
      </Section>

      {p.observacoes && (
        <Section title="Observações">
          <p className="text-sm whitespace-pre-wrap">{p.observacoes}</p>
        </Section>
      )}

      {p.motivoPerda && (
        <Section title="Motivo da perda">
          <Badge variant="destructive">{p.motivoPerda}</Badge>
          {p.motivoPerdaDetalhe && (
            <p className="text-sm mt-2 whitespace-pre-wrap">{p.motivoPerdaDetalhe}</p>
          )}
        </Section>
      )}

      <div className="pt-3 border-t border-border text-xs text-muted-foreground">
        Criada em {new Date(p.createdAt).toLocaleDateString('pt-BR')}
        {p.fechadaEm && (
          <> · Fechada em {new Date(p.fechadaEm).toLocaleDateString('pt-BR')}</>
        )}
      </div>
    </div>
  );
}

function TimelineTab({ prospeccaoId }: { prospeccaoId: string }) {
  const query = useProspeccaoEventos(prospeccaoId);
  if (query.isLoading) {
    return (
      <div className="space-y-3 py-3">
        {Array.from({ length: 3 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full rounded-md" />
        ))}
      </div>
    );
  }
  const eventos = query.data ?? [];
  if (eventos.length === 0) {
    return <p className="text-sm text-muted-foreground py-6 text-center">Sem eventos.</p>;
  }
  return (
    <ol className="space-y-3 py-3">
      {eventos.map(e => (
        <EventoItem key={e.id} evento={e} />
      ))}
    </ol>
  );
}

function EventoItem({ evento }: { evento: ProspeccaoEventoResponse }) {
  const Icon = ICON_BY_TIPO[evento.tipo];
  const ts = new Date(evento.createdAt);
  return (
    <li className="relative pl-8">
      <span className="absolute left-0 top-1 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-card">
        <Icon className="h-3 w-3 text-muted-foreground" />
      </span>
      <div className="rounded-md border border-border bg-muted/30 p-3 text-sm">
        <div className="flex items-center justify-between gap-2 mb-1">
          <span className="font-medium text-xs uppercase tracking-wider text-muted-foreground">
            {evento.tipo.replace('_', ' ')}
          </span>
          <span className="text-xs text-muted-foreground tabular-nums">
            {ts.toLocaleString('pt-BR', { dateStyle: 'short', timeStyle: 'short' })}
          </span>
        </div>
        <EventoPayload tipo={evento.tipo} payload={evento.payload} />
      </div>
    </li>
  );
}

const ICON_BY_TIPO: Record<EventoTipo, React.ElementType> = {
  STATUS_CHANGE: ArrowRight,
  COMMENT: MessageSquare,
  EMAIL_SENT: Mail,
  TASK_LINKED: LinkIcon,
};

function EventoPayload({
  tipo,
  payload,
}: {
  tipo: EventoTipo;
  payload: Record<string, unknown>;
}) {
  if (tipo === 'STATUS_CHANGE') {
    const de = payload.de as string | undefined;
    const para = payload.para as string | undefined;
    return (
      <p className="text-sm">
        {de ? (
          <>
            <Badge variant="muted" className="font-mono text-[10px]">{de}</Badge>{' '}
            <ArrowRight className="inline h-3 w-3 mx-1" />{' '}
          </>
        ) : (
          'criada como '
        )}
        <Badge variant="muted" className="font-mono text-[10px]">{para}</Badge>
        {payload.motivo_perda ? (
          <span className="ml-2 text-xs text-muted-foreground">
            ({String(payload.motivo_perda)})
          </span>
        ) : null}
      </p>
    );
  }
  if (tipo === 'COMMENT') {
    return <p className="text-sm whitespace-pre-wrap">{String(payload.texto ?? '')}</p>;
  }
  return <pre className="text-xs text-muted-foreground">{JSON.stringify(payload)}</pre>;
}

function ComentariosTab({ prospeccaoId }: { prospeccaoId: string }) {
  const eventosQ = useProspeccaoEventos(prospeccaoId);
  const comentar = useComentarProspeccao();
  const form = useForm<ComentarioInput>({
    resolver: zodResolver(comentarioSchema),
    defaultValues: { texto: '' },
  });

  async function onSubmit(values: ComentarioInput) {
    try {
      await comentar.mutateAsync({ id: prospeccaoId, texto: values.texto.trim() });
      form.reset({ texto: '' });
      toast.success('Comentário adicionado.');
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro.');
    }
  }

  const comentarios = (eventosQ.data ?? []).filter(e => e.tipo === 'COMMENT');

  return (
    <div className="space-y-4 py-3">
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-2">
        <textarea
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
          placeholder="Adicionar comentário interno…"
          aria-invalid={!!form.formState.errors.texto}
          {...form.register('texto')}
        />
        {form.formState.errors.texto && (
          <p className="text-xs text-destructive" role="alert">
            {form.formState.errors.texto.message}
          </p>
        )}
        <div className="flex justify-end">
          <Button type="submit" size="sm" disabled={comentar.isPending}>
            {comentar.isPending ? 'Enviando…' : 'Comentar'}
          </Button>
        </div>
      </form>

      {eventosQ.isLoading ? (
        <Skeleton className="h-16 w-full rounded-md" />
      ) : comentarios.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-4">
          Nenhum comentário ainda.
        </p>
      ) : (
        <ul className="space-y-2">
          {comentarios.map(c => (
            <li
              key={c.id}
              className="rounded-md border border-border bg-muted/30 p-3 text-sm"
            >
              <p className="whitespace-pre-wrap">{String(c.payload.texto ?? '')}</p>
              <p className="text-xs text-muted-foreground mt-1.5 tabular-nums">
                {new Date(c.createdAt).toLocaleString('pt-BR', {
                  dateStyle: 'short',
                  timeStyle: 'short',
                })}
              </p>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}

function Section({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
        {title}
      </h4>
      {children}
    </section>
  );
}
