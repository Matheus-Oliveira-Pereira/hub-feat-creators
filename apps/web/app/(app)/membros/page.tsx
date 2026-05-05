'use client';

import * as React from 'react';
import { toast } from 'sonner';
import { UserPlus, MoreHorizontal, UserCheck, UserX, Trash2 } from 'lucide-react';
import { membros as membrosApi, convites as convitesApi, type Membro } from '@/lib/api';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { PageHeader } from '@/components/app/page-header';
import { ConviteModal } from '@/components/forms/convite-modal';

const STATUS_LABELS: Record<string, string> = {
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
  BLOQUEADO: 'Bloqueado',
};

const STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive'> = {
  ATIVO: 'default',
  INATIVO: 'secondary',
  BLOQUEADO: 'destructive',
};

function MembrosPage() {
  const qc = useQueryClient();
  const [conviteOpen, setConviteOpen] = React.useState(false);

  const { data = [], isLoading } = useQuery({
    queryKey: ['membros'],
    queryFn: () => membrosApi.list(),
  });

  const setStatus = useMutation({
    mutationFn: ({ id, status }: { id: string; status: string }) =>
      membrosApi.setStatus(id, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['membros'] });
      toast.success('Status atualizado.');
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao atualizar.'),
  });

  const remove = useMutation({
    mutationFn: (id: string) => membrosApi.remove(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['membros'] });
      toast.success('Membro removido.');
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao remover.'),
  });

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Equipe"
        description="Membros e convites do workspace."
        actions={
          <Button onClick={() => setConviteOpen(true)} size="sm">
            <UserPlus className="h-4 w-4 mr-2" />
            Convidar
          </Button>
        }
      />

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando…</p>
      ) : (
        <div className="rounded-lg border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">E-mail</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Função</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">E-mail</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">MFA</th>
                <th className="px-4 py-3 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {(data as Membro[]).map((m) => (
                <tr key={m.id} className="hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs">{m.email}</td>
                  <td className="px-4 py-3 text-muted-foreground">{m.role}</td>
                  <td className="px-4 py-3">
                    <Badge variant={STATUS_VARIANT[m.status] ?? 'secondary'}>
                      {STATUS_LABELS[m.status] ?? m.status}
                    </Badge>
                  </td>
                  <td className="px-4 py-3">
                    {m.emailVerificado ? (
                      <Badge variant="default">Verificado</Badge>
                    ) : (
                      <Badge variant="secondary">Pendente</Badge>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {m.mfaAtivo ? (
                      <Badge variant="default">Ativo</Badge>
                    ) : (
                      <span className="text-muted-foreground text-xs">—</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        {m.status === 'ATIVO' ? (
                          <DropdownMenuItem
                            onClick={() => setStatus.mutate({ id: m.id, status: 'INATIVO' })}
                          >
                            <UserX className="h-4 w-4 mr-2" />
                            Desativar
                          </DropdownMenuItem>
                        ) : (
                          <DropdownMenuItem
                            onClick={() => setStatus.mutate({ id: m.id, status: 'ATIVO' })}
                          >
                            <UserCheck className="h-4 w-4 mr-2" />
                            Ativar
                          </DropdownMenuItem>
                        )}
                        <DropdownMenuSeparator />
                        <DropdownMenuItem
                          className="text-destructive focus:text-destructive"
                          onClick={() => {
                            if (confirm('Remover este membro?')) remove.mutate(m.id);
                          }}
                        >
                          <Trash2 className="h-4 w-4 mr-2" />
                          Remover
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <ConviteModal open={conviteOpen} onOpenChange={setConviteOpen} />
    </div>
  );
}

export default function MembrosPageWrapper() {
  return <MembrosPage />;
}
