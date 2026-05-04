'use client';

import * as React from 'react';
import { AlertTriangle } from 'lucide-react';
import { type Tarefa, type TarefaPrioridade } from '@/lib/api';
import { cn } from '@/lib/utils';

const PRIORIDADE_DOT: Record<TarefaPrioridade, string> = {
  BAIXA:   'bg-slate-400',
  MEDIA:   'bg-blue-500',
  ALTA:    'bg-amber-500',
  URGENTE: 'bg-red-600',
};

function startOfWeek(date: Date): Date {
  const d = new Date(date);
  const day = d.getDay(); // 0=Dom
  const diff = day === 0 ? -6 : 1 - day; // normaliza segunda
  d.setDate(d.getDate() + diff);
  d.setHours(0, 0, 0, 0);
  return d;
}

function addDays(date: Date, n: number): Date {
  const d = new Date(date);
  d.setDate(d.getDate() + n);
  return d;
}

const DIAS = ['Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb', 'Dom'];

export function TarefaAgenda({
  tarefas,
  onSelect,
}: {
  tarefas: Tarefa[];
  onSelect: (id: string) => void;
}) {
  const [semana, setSemana] = React.useState(() => startOfWeek(new Date()));

  const dias = Array.from({ length: 7 }, (_, i) => addDays(semana, i));
  const hoje = new Date();
  hoje.setHours(0, 0, 0, 0);

  function tarefasDoDia(dia: Date): Tarefa[] {
    const diaStr = dia.toISOString().slice(0, 10);
    return tarefas.filter(t => t.prazo.slice(0, 10) === diaStr);
  }

  return (
    <div className="rounded-lg border border-border bg-card overflow-hidden">
      {/* Nav semana */}
      <div className="flex items-center justify-between border-b border-border px-4 py-2">
        <button
          className="rounded px-2 py-1 text-xs hover:bg-muted"
          onClick={() => setSemana(d => addDays(d, -7))}
        >
          ← Anterior
        </button>
        <span className="text-sm font-medium">
          {dias[0].toLocaleDateString('pt-BR', { day: '2-digit', month: 'short' })} –{' '}
          {dias[6].toLocaleDateString('pt-BR', { day: '2-digit', month: 'short', year: 'numeric' })}
        </span>
        <button
          className="rounded px-2 py-1 text-xs hover:bg-muted"
          onClick={() => setSemana(d => addDays(d, 7))}
        >
          Próxima →
        </button>
      </div>

      {/* Grid */}
      <div className="grid grid-cols-7 divide-x divide-border">
        {dias.map((dia, i) => {
          const isToday = dia.getTime() === hoje.getTime();
          const dayTarefas = tarefasDoDia(dia);

          return (
            <div key={i} className={cn('min-h-32 p-2', isToday && 'bg-primary/5')}>
              <div className={cn('mb-2 text-center', isToday && 'font-bold text-primary')}>
                <div className="text-xs text-muted-foreground">{DIAS[i]}</div>
                <div className="text-sm">{dia.getDate()}</div>
              </div>

              <div className="space-y-1">
                {dayTarefas.map(t => {
                  const vencida =
                    t.status !== 'FEITA' && t.status !== 'CANCELADA' && new Date(t.prazo) < new Date();
                  return (
                    <button
                      key={t.id}
                      onClick={() => onSelect(t.id)}
                      className={cn(
                        'w-full rounded px-1.5 py-1 text-left text-xs leading-snug transition-colors',
                        'flex items-start gap-1',
                        t.status === 'FEITA'
                          ? 'bg-muted/50 text-muted-foreground line-through'
                          : vencida
                          ? 'bg-red-50 text-red-700 hover:bg-red-100'
                          : 'bg-muted hover:bg-muted/80',
                      )}
                    >
                      <span className={cn('mt-0.5 h-2 w-2 flex-shrink-0 rounded-full', PRIORIDADE_DOT[t.prioridade])} />
                      <span className="line-clamp-2">{t.titulo}</span>
                      {vencida && <AlertTriangle className="ml-auto h-3 w-3 flex-shrink-0" />}
                    </button>
                  );
                })}
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
