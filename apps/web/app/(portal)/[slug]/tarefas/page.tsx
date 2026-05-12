'use client';

import * as React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery } from '@tanstack/react-query';
import { Loader2, ChevronRight, Clock } from 'lucide-react';
import { portalMe, getCreatorToken } from '@/lib/api';
import type { Route } from 'next';

const STATUS_LABEL: Record<string, string> = {
  TODO: 'A fazer',
  IN_PROGRESS: 'Em andamento',
  REVIEW: 'Em revisão',
  FEITA: 'Concluída',
  CANCELADA: 'Cancelada',
};

const PRIORIDADE_COLOR: Record<string, string> = {
  BAIXA: 'bg-green-100 text-green-700',
  MEDIA: 'bg-yellow-100 text-yellow-700',
  ALTA: 'bg-orange-100 text-orange-700',
  URGENTE: 'bg-red-100 text-red-700',
};

export default function PortalTarefasPage() {
  const { slug } = useParams<{ slug: string }>();
  const router = useRouter();

  React.useEffect(() => {
    if (!getCreatorToken()) {
      router.replace(`/portal/${slug}/login` as Route);
    }
  }, [slug, router]);

  const { data: tarefas, isLoading, error } = useQuery({
    queryKey: ['portal', 'tarefas'],
    queryFn: () => portalMe.tarefas(),
    enabled: !!getCreatorToken(),
  });

  if (isLoading) {
    return (
      <div className="flex justify-center mt-16">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (error) {
    return <p className="text-sm text-red-500 mt-8">Erro ao carregar tarefas.</p>;
  }

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-[#141414]">Suas tarefas</h1>

      {!tarefas?.length && (
        <p className="text-sm text-muted-foreground">Nenhuma tarefa disponível no momento.</p>
      )}

      <ul className="space-y-2">
        {tarefas?.map((t) => (
          <li key={t.id}>
            <button
              className="w-full text-left border rounded-lg px-4 py-3 hover:bg-gray-50 transition flex items-center gap-3"
              onClick={() => router.push(`/portal/${slug}/tarefas/${t.id}` as Route)}
            >
              <div className="flex-1 min-w-0">
                <p className="font-medium truncate">{t.titulo}</p>
                <div className="flex items-center gap-2 mt-1">
                  <span className="text-xs text-muted-foreground flex items-center gap-1">
                    <Clock className="h-3 w-3" />
                    {new Date(t.prazo).toLocaleDateString('pt-BR')}
                  </span>
                  <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${PRIORIDADE_COLOR[t.prioridade] ?? ''}`}>
                    {t.prioridade}
                  </span>
                  <span className="text-xs text-muted-foreground">{STATUS_LABEL[t.status] ?? t.status}</span>
                </div>
              </div>
              <ChevronRight className="h-4 w-4 text-muted-foreground shrink-0" />
            </button>
          </li>
        ))}
      </ul>
    </div>
  );
}
