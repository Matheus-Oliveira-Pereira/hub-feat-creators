'use client';

import * as React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Loader2, ArrowLeft, Upload, Send } from 'lucide-react';
import { toast } from 'sonner';
import { portalMe, getCreatorToken } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';
import type { Route } from 'next';

const STATUS_BADGE: Record<string, string> = {
  ENVIADO: 'bg-blue-100 text-blue-700',
  EM_REVISAO: 'bg-yellow-100 text-yellow-700',
  APROVADO: 'bg-green-100 text-green-700',
  SOLICITADA_REVISAO: 'bg-orange-100 text-orange-700',
};

export default function PortalTarefaDetailPage() {
  const { tarefaId } = useParams<{ tarefaId: string }>();
  const router = useRouter();
  const qc = useQueryClient();
  const fileRef = React.useRef<HTMLInputElement>(null);
  const [texto, setTexto] = React.useState('');

  React.useEffect(() => {
    if (!getCreatorToken()) {
      router.replace('/portal/login' as Route);
    }
  }, [router]);

  const { data: tarefa, isLoading: loadingTarefa } = useQuery({
    queryKey: ['portal', 'tarefa', tarefaId],
    queryFn: () => portalMe.tarefa(tarefaId),
    enabled: !!getCreatorToken(),
  });

  const { data: comentarios } = useQuery({
    queryKey: ['portal', 'comentarios', tarefaId],
    queryFn: () => portalMe.comentarios(tarefaId),
    enabled: !!getCreatorToken(),
  });

  const { data: entregaveis } = useQuery({
    queryKey: ['portal', 'entregaveis', tarefaId],
    queryFn: () => portalMe.entregaveis(tarefaId),
    enabled: !!getCreatorToken(),
  });

  const comentarMutation = useMutation({
    mutationFn: () => portalMe.comentar(tarefaId, texto),
    onSuccess: () => {
      setTexto('');
      qc.invalidateQueries({ queryKey: ['portal', 'comentarios', tarefaId] });
    },
    onError: () => toast.error('Erro ao enviar comentário.'),
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => portalMe.enviarEntregavel(tarefaId, file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['portal', 'entregaveis', tarefaId] });
      toast.success('Entregável enviado!');
    },
    onError: () => toast.error('Erro ao enviar arquivo.'),
  });

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) uploadMutation.mutate(file);
    e.target.value = '';
  };

  if (loadingTarefa) {
    return (
      <div className="flex justify-center mt-16">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  if (!tarefa) return null;

  return (
    <div className="space-y-6">
      <button
        className="flex items-center gap-1 text-sm text-muted-foreground hover:text-foreground"
        onClick={() => router.push('/portal/tarefas' as Route)}
      >
        <ArrowLeft className="h-4 w-4" /> Voltar
      </button>

      {/* Header */}
      <div className="space-y-1">
        <h1 className="text-xl font-semibold text-[#141414]">{tarefa.titulo}</h1>
        {tarefa.descricao && <p className="text-sm text-muted-foreground">{tarefa.descricao}</p>}
        <p className="text-xs text-muted-foreground">
          Prazo: {new Date(tarefa.prazo).toLocaleDateString('pt-BR', { day: '2-digit', month: 'long', year: 'numeric' })}
        </p>
      </div>

      {/* Entregáveis */}
      <section className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="font-medium text-sm">Entregáveis</h2>
          <Button
            size="sm"
            variant="outline"
            onClick={() => fileRef.current?.click()}
            disabled={uploadMutation.isPending}
          >
            {uploadMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Upload className="h-4 w-4" />
            )}
            <span className="ml-1">Enviar arquivo</span>
          </Button>
          <input ref={fileRef} type="file" className="hidden" onChange={handleFileChange} />
        </div>

        {!entregaveis?.length && (
          <p className="text-xs text-muted-foreground">Nenhum entregável ainda.</p>
        )}

        <ul className="space-y-2">
          {entregaveis?.map((e) => (
            <li key={e.id} className="border rounded-lg px-3 py-2">
              <div className="flex items-center justify-between">
                <div className="min-w-0">
                  <p className="text-sm font-medium truncate">{e.filename}</p>
                  <p className="text-xs text-muted-foreground">
                    {(e.sizeBytes / 1024).toFixed(1)} KB
                  </p>
                </div>
                <div className="flex items-center gap-2 shrink-0">
                  <span className={`text-xs px-1.5 py-0.5 rounded font-medium ${STATUS_BADGE[e.status] ?? ''}`}>
                    {e.status.replace('_', ' ')}
                  </span>
                  <a
                    href={portalMe.downloadUrl(e.id)}
                    target="_blank"
                    rel="noreferrer"
                    className="text-xs underline"
                  >
                    Baixar
                  </a>
                </div>
              </div>
              {e.feedback && (
                <p className="text-xs text-muted-foreground mt-1 border-t pt-1">{e.feedback}</p>
              )}
            </li>
          ))}
        </ul>
      </section>

      {/* Comentários */}
      <section className="space-y-3">
        <h2 className="font-medium text-sm">Comentários</h2>

        <ul className="space-y-2">
          {comentarios?.map((c) => (
            <li
              key={c.id}
              className={`rounded-lg px-3 py-2 text-sm ${
                c.autorTipo === 'CREATOR' ? 'bg-lime-50 ml-4' : 'bg-gray-50 mr-4'
              }`}
            >
              <p>{c.texto}</p>
              <p className="text-xs text-muted-foreground mt-0.5">
                {c.autorTipo === 'CREATOR' ? 'Você' : 'Assessoria'} ·{' '}
                {new Date(c.createdAt).toLocaleString('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' })}
              </p>
            </li>
          ))}
        </ul>

        <div className="flex gap-2">
          <Textarea
            value={texto}
            onChange={(e) => setTexto(e.target.value)}
            placeholder="Escreva um comentário..."
            className="min-h-[80px] flex-1"
          />
          <Button
            size="sm"
            className="self-end"
            onClick={() => comentarMutation.mutate()}
            disabled={!texto.trim() || comentarMutation.isPending}
          >
            {comentarMutation.isPending ? (
              <Loader2 className="h-4 w-4 animate-spin" />
            ) : (
              <Send className="h-4 w-4" />
            )}
          </Button>
        </div>
      </section>
    </div>
  );
}
