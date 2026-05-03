import { type ProspeccaoStatus } from '@/lib/api';

export const STATUS_LABEL: Record<ProspeccaoStatus, string> = {
  NOVA: 'Novas',
  CONTATADA: 'Contatadas',
  NEGOCIANDO: 'Negociando',
  FECHADA_GANHA: 'Ganhas',
  FECHADA_PERDIDA: 'Perdidas',
};

/** Cores por status — usadas em badges, headers de coluna kanban */
export const STATUS_TONE: Record<
  ProspeccaoStatus,
  { dot: string; bg: string; text: string; ring: string }
> = {
  NOVA: {
    dot: 'bg-sky-500',
    bg: 'bg-sky-500/10',
    text: 'text-sky-600 dark:text-sky-400',
    ring: 'ring-sky-500/30',
  },
  CONTATADA: {
    dot: 'bg-violet-500',
    bg: 'bg-violet-500/10',
    text: 'text-violet-600 dark:text-violet-400',
    ring: 'ring-violet-500/30',
  },
  NEGOCIANDO: {
    dot: 'bg-amber-500',
    bg: 'bg-amber-500/10',
    text: 'text-amber-700 dark:text-amber-400',
    ring: 'ring-amber-500/30',
  },
  FECHADA_GANHA: {
    dot: 'bg-primary',
    bg: 'bg-primary/15',
    text: 'text-foreground',
    ring: 'ring-primary/40',
  },
  FECHADA_PERDIDA: {
    dot: 'bg-destructive',
    bg: 'bg-destructive/10',
    text: 'text-destructive',
    ring: 'ring-destructive/30',
  },
};

export const STATUS_ORDER: ProspeccaoStatus[] = [
  'NOVA',
  'CONTATADA',
  'NEGOCIANDO',
  'FECHADA_GANHA',
  'FECHADA_PERDIDA',
];

/** Espelha ProspeccaoStateMachine.java do backend. */
const TRANSICOES: Record<ProspeccaoStatus, ProspeccaoStatus[]> = {
  NOVA: ['CONTATADA'],
  CONTATADA: ['NEGOCIANDO', 'FECHADA_PERDIDA'],
  NEGOCIANDO: ['FECHADA_GANHA', 'FECHADA_PERDIDA'],
  FECHADA_PERDIDA: ['NOVA'],
  FECHADA_GANHA: [],
};

export function isValidTransition(from: ProspeccaoStatus, to: ProspeccaoStatus): boolean {
  return TRANSICOES[from]?.includes(to) ?? false;
}

export function formatBRL(centavos: number | null): string {
  if (centavos == null) return '—';
  return (centavos / 100).toLocaleString('pt-BR', {
    style: 'currency',
    currency: 'BRL',
    minimumFractionDigits: 2,
  });
}
