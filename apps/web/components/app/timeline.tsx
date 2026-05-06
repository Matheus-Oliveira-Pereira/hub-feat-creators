'use client';

import * as React from 'react';
import {
  ArrowRight,
  MessageSquare,
  Mail,
  Link as LinkIcon,
  Trophy,
  XCircle,
  User,
  CheckSquare,
  Building2,
  AtSign,
  Loader2,
} from 'lucide-react';
import { Skeleton } from '@/components/ui/skeleton';
import { Button } from '@/components/ui/button';
import { useHistorico } from '@/lib/queries';
import type { Evento } from '@/lib/api';

// ─── Label map ────────────────────────────────────────────────────────────────

const TIPO_LABEL: Record<string, string> = {
  PROSPECCAO_CRIADA: 'Prospecção criada',
  PROSPECCAO_STATUS_MUDOU: 'Status alterado',
  PROSPECCAO_COMENTARIO: 'Comentário',
  PROSPECCAO_FECHADA_GANHA: 'Fechada (ganha)',
  PROSPECCAO_FECHADA_PERDIDA: 'Fechada (perdida)',
  TAREFA_CRIADA: 'Tarefa criada',
  TAREFA_CONCLUIDA: 'Tarefa concluída',
  TAREFA_REATRIBUIDA: 'Tarefa reatribuída',
  EMAIL_ENVIADO: 'E-mail enviado',
  EMAIL_BOUNCED: 'E-mail devolvido',
  WA_ENVIADO: 'WhatsApp enviado',
  INFLUENCIADOR_CRIADO: 'Influenciador cadastrado',
  MARCA_CRIADA: 'Marca cadastrada',
  CONTATO_CRIADO: 'Contato cadastrado',
};

const TIPO_ICON: Record<string, React.ElementType> = {
  PROSPECCAO_CRIADA: Building2,
  PROSPECCAO_STATUS_MUDOU: ArrowRight,
  PROSPECCAO_COMENTARIO: MessageSquare,
  PROSPECCAO_FECHADA_GANHA: Trophy,
  PROSPECCAO_FECHADA_PERDIDA: XCircle,
  TAREFA_CRIADA: CheckSquare,
  TAREFA_CONCLUIDA: CheckSquare,
  TAREFA_REATRIBUIDA: User,
  EMAIL_ENVIADO: Mail,
  EMAIL_BOUNCED: Mail,
  WA_ENVIADO: AtSign,
  INFLUENCIADOR_CRIADO: User,
  MARCA_CRIADA: Building2,
  CONTATO_CRIADO: User,
};

// ─── Time util ────────────────────────────────────────────────────────────────

function timeAgo(isoTs: string): string {
  const diff = Date.now() - new Date(isoTs).getTime();
  const mins = Math.floor(diff / 60_000);
  if (mins < 1) return 'agora';
  if (mins < 60) return `${mins}min atrás`;
  const hrs = Math.floor(mins / 60);
  if (hrs < 24) return `${hrs}h atrás`;
  const days = Math.floor(hrs / 24);
  if (days < 7) return `${days}d atrás`;
  return new Date(isoTs).toLocaleDateString('pt-BR');
}

function dayLabel(isoTs: string): string {
  return new Date(isoTs).toLocaleDateString('pt-BR', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
  });
}

// ─── All event types for filter chips ────────────────────────────────────────

const ALL_TIPOS = Object.keys(TIPO_LABEL);

// ─── Components ──────────────────────────────────────────────────────────────

function TimelineItem({ evento }: { evento: Evento }) {
  const Icon = TIPO_ICON[evento.tipo] ?? ArrowRight;
  const label = TIPO_LABEL[evento.tipo] ?? evento.tipo;

  const detail = (() => {
    const p = evento.payload;
    if (p.texto) return String(p.texto);
    if (p.de && p.para) return `${String(p.de)} → ${String(p.para)}`;
    if (p.titulo) return String(p.titulo);
    if (p.nome) return String(p.nome);
    if (p.assunto) return String(p.assunto);
    return null;
  })();

  return (
    <li className="relative pl-8">
      <span className="absolute left-0 top-1 flex h-6 w-6 items-center justify-center rounded-full border border-border bg-card">
        <Icon className="h-3 w-3 text-muted-foreground" />
      </span>
      <div className="rounded-md border border-border bg-muted/30 p-3 text-sm">
        <div className="flex items-center justify-between gap-2 mb-1">
          <span className="font-medium text-xs uppercase tracking-wider text-muted-foreground">
            {label}
          </span>
          <span className="text-xs text-muted-foreground tabular-nums">{timeAgo(evento.ts)}</span>
        </div>
        {detail && <p className="text-sm text-foreground/80 line-clamp-3">{detail}</p>}
      </div>
    </li>
  );
}

// ─── Main component ───────────────────────────────────────────────────────────

interface TimelineProps {
  entidadeTipo: string;
  entidadeId: string;
}

export function Timeline({ entidadeTipo, entidadeId }: TimelineProps) {
  const [tiposFiltro, setTiposFiltro] = React.useState<string[]>([]);

  const tiposParam = tiposFiltro.length > 0 ? tiposFiltro.join(',') : undefined;
  const { data, fetchNextPage, hasNextPage, isFetchingNextPage, isLoading } = useHistorico(
    entidadeTipo,
    entidadeId,
    tiposParam
  );

  const allEventos = data?.pages.flatMap(p => p.data) ?? [];

  // Group by day
  const byDay: { label: string; eventos: Evento[] }[] = [];
  for (const ev of allEventos) {
    const dl = dayLabel(ev.ts);
    const last = byDay[byDay.length - 1];
    if (last?.label === dl) {
      last.eventos.push(ev);
    } else {
      byDay.push({ label: dl, eventos: [ev] });
    }
  }

  const toggleFiltro = (tipo: string) => {
    setTiposFiltro(prev =>
      prev.includes(tipo) ? prev.filter(t => t !== tipo) : [...prev, tipo]
    );
  };

  if (isLoading) {
    return (
      <div className="space-y-3 py-3">
        {Array.from({ length: 4 }).map((_, i) => (
          <Skeleton key={i} className="h-16 w-full rounded-md" />
        ))}
      </div>
    );
  }

  return (
    <div className="space-y-4 py-3">
      {/* Filter chips */}
      <div className="flex flex-wrap gap-1.5">
        {ALL_TIPOS.map(tipo => {
          const active = tiposFiltro.includes(tipo);
          return (
            <button
              key={tipo}
              onClick={() => toggleFiltro(tipo)}
              className={[
                'rounded-full px-2.5 py-0.5 text-xs font-medium border transition-colors',
                active
                  ? 'bg-primary text-primary-foreground border-primary'
                  : 'bg-muted/40 text-muted-foreground border-border hover:bg-muted',
              ].join(' ')}
            >
              {TIPO_LABEL[tipo]}
            </button>
          );
        })}
        {tiposFiltro.length > 0 && (
          <button
            onClick={() => setTiposFiltro([])}
            className="rounded-full px-2.5 py-0.5 text-xs font-medium border border-border text-muted-foreground hover:bg-muted transition-colors"
          >
            Limpar filtros
          </button>
        )}
      </div>

      {/* Timeline */}
      {byDay.length === 0 ? (
        <p className="text-sm text-muted-foreground py-6 text-center">Sem eventos.</p>
      ) : (
        byDay.map(group => (
          <div key={group.label}>
            <p className="text-xs font-semibold uppercase tracking-wider text-muted-foreground mb-2">
              {group.label}
            </p>
            <ol className="space-y-3">
              {group.eventos.map(ev => (
                <TimelineItem key={ev.id} evento={ev} />
              ))}
            </ol>
          </div>
        ))
      )}

      {/* Load more */}
      {hasNextPage && (
        <div className="pt-2 flex justify-center">
          <Button
            variant="outline"
            size="sm"
            onClick={() => fetchNextPage()}
            disabled={isFetchingNextPage}
          >
            {isFetchingNextPage ? (
              <Loader2 className="h-3.5 w-3.5 animate-spin mr-1.5" />
            ) : null}
            Carregar mais
          </Button>
        </div>
      )}
    </div>
  );
}
