'use client';

import * as React from 'react';
import { toast } from 'sonner';
import { Sparkles, ThumbsUp, ThumbsDown, Minus, Play, Star } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  useMatchSugestoes,
  useRunMatch,
  useMatchFeedback,
} from '@/lib/queries';
import type { MatchSugestao } from '@/lib/api';

interface Props {
  prospeccaoId: string;
}

export function MatchTab({ prospeccaoId }: Props) {
  const { data: sugestoes, isLoading } = useMatchSugestoes(prospeccaoId);
  const runMatch = useRunMatch();
  const feedback = useMatchFeedback();

  function handleRun() {
    runMatch.mutate(prospeccaoId, {
      onSuccess: () => toast.success('Match executado com sucesso'),
      onError: () => toast.error('Erro ao executar match'),
    });
  }

  function handleFeedback(sugestaoId: string, sinal: 'BOA' | 'OK' | 'RUIM') {
    feedback.mutate(
      { sugestaoId, sinal },
      { onSuccess: () => toast.success('Feedback registrado') },
    );
  }

  return (
    <div className="space-y-4 py-2">
      <div className="flex items-center justify-between">
        <p className="text-sm text-muted-foreground">
          {sugestoes?.length
            ? `${sugestoes.length} creator(s) sugerido(s)`
            : 'Nenhuma sugestão ainda'}
        </p>
        <Button
          size="sm"
          variant="outline"
          onClick={handleRun}
          disabled={runMatch.isPending}
        >
          <Play className="h-3.5 w-3.5" />
          {runMatch.isPending ? 'Executando…' : 'Executar Match'}
        </Button>
      </div>

      {isLoading && (
        <p className="text-sm text-muted-foreground animate-pulse">Carregando sugestões…</p>
      )}

      <div className="space-y-3">
        {sugestoes?.map(s => (
          <SugestaoCard key={s.id} sugestao={s} onFeedback={handleFeedback} />
        ))}
      </div>
    </div>
  );
}

function SugestaoCard({
  sugestao,
  onFeedback,
}: {
  sugestao: MatchSugestao;
  onFeedback: (id: string, sinal: 'BOA' | 'OK' | 'RUIM') => void;
}) {
  const scorePercent = Math.round(sugestao.score * 100);

  return (
    <div className="rounded-lg border border-border p-3 space-y-2">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <Star className="h-3.5 w-3.5 text-primary" />
          <span className="text-xs font-mono text-muted-foreground">
            {sugestao.influenciadorId.slice(0, 8)}…
          </span>
        </div>
        <Badge
          variant={scorePercent >= 70 ? 'default' : scorePercent >= 40 ? 'secondary' : 'outline'}
          className="text-xs font-mono"
        >
          {scorePercent}%
        </Badge>
      </div>

      {sugestao.razoes.length > 0 && (
        <ul className="text-xs text-muted-foreground space-y-0.5 pl-1">
          {sugestao.razoes.slice(0, 3).map((r, i) => (
            <li key={i} className="flex gap-1.5 items-start">
              <span className="text-primary mt-px">•</span>
              {r.descricao}
            </li>
          ))}
        </ul>
      )}

      <div className="flex gap-1.5 pt-1">
        <Button
          size="sm"
          variant="ghost"
          className="h-7 px-2 text-xs"
          onClick={() => onFeedback(sugestao.id, 'BOA')}
        >
          <ThumbsUp className="h-3 w-3" /> Boa
        </Button>
        <Button
          size="sm"
          variant="ghost"
          className="h-7 px-2 text-xs"
          onClick={() => onFeedback(sugestao.id, 'OK')}
        >
          <Minus className="h-3 w-3" /> Ok
        </Button>
        <Button
          size="sm"
          variant="ghost"
          className="h-7 px-2 text-xs text-destructive hover:text-destructive"
          onClick={() => onFeedback(sugestao.id, 'RUIM')}
        >
          <ThumbsDown className="h-3 w-3" /> Ruim
        </Button>
      </div>
    </div>
  );
}
