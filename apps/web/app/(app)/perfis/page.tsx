'use client';

import * as React from 'react';
import {
  Plus,
  Lock,
  MoreHorizontal,
  Pencil,
  Trash2,
  ShieldCheck,
  Users as UsersIcon,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { Perfil } from '@/lib/api';
import { usePerfis, useDeletePerfil } from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Card } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { PageHeader } from '@/components/app/page-header';
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';
import { PerfilFormModal } from '@/components/forms/perfil-form-modal';
import { Can } from '@/components/auth/can';

export default function PerfisPage() {
  const [formState, setFormState] = React.useState<{ open: boolean; item: Perfil | null }>({
    open: false,
    item: null,
  });

  const query = usePerfis();
  const del = useDeletePerfil();
  const data = query.data ?? [];

  async function handleDelete(p: Perfil) {
    if (p.isSystem) {
      toast.error('Perfis do sistema não podem ser removidos.');
      return;
    }
    if (!confirm(`Remover perfil "${p.nome}"?`)) return;
    try {
      await del.mutateAsync(p.id);
      toast.success('Perfil removido.');
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao remover.');
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-12">
      <PageHeader
        eyebrow="RBAC"
        title="Perfis"
        description="Defina permissões e atribua a usuários da assessoria."
        actions={
          <Can role="CPRF">
            <Button onClick={() => setFormState({ open: true, item: null })}>
              <Plus className="h-4 w-4" /> Novo perfil
            </Button>
          </Can>
        }
      />

      {query.isLoading ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {Array.from({ length: 3 }).map((_, i) => (
            <Skeleton key={i} className="h-44 w-full rounded-xl" />
          ))}
        </div>
      ) : data.length === 0 ? (
        <EmptyState
          illustration={<EmptyIllustration variant="sparkles" />}
          title="Nenhum perfil ainda"
          description="O signup costuma criar 3 perfis seed automaticamente. Se está vazio, contate suporte."
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <AnimatePresence mode="popLayout">
            {data.map((p, i) => (
              <motion.div
                key={p.id}
                layout
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2, delay: Math.min(i * 0.02, 0.2) }}
              >
                <Card className="group p-5 flex flex-col h-full">
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div className="flex items-center gap-2.5 min-w-0">
                      <span
                        className={
                          p.isSystem
                            ? 'flex h-9 w-9 items-center justify-center rounded-lg bg-foreground text-primary'
                            : 'flex h-9 w-9 items-center justify-center rounded-lg bg-primary/10 text-foreground'
                        }
                      >
                        <ShieldCheck className="h-4.5 w-4.5" />
                      </span>
                      <div className="min-w-0">
                        <p className="font-display font-semibold flex items-center gap-1.5 truncate">
                          {p.nome}
                          {p.isSystem && (
                            <Lock
                              className="h-3 w-3 text-muted-foreground"
                              aria-label="Perfil do sistema"
                            />
                          )}
                        </p>
                        <p className="text-xs text-muted-foreground truncate">
                          {p.descricao || 'Sem descrição'}
                        </p>
                      </div>
                    </div>
                    <Can role={['EPRF', 'DPRF']}>
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild>
                          <Button
                            variant="ghost"
                            size="icon-sm"
                            className="opacity-0 group-hover:opacity-100 transition-opacity"
                          >
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end">
                          <Can role="EPRF">
                            <DropdownMenuItem onClick={() => setFormState({ open: true, item: p })}>
                              <Pencil className="h-4 w-4" />
                              {p.isSystem ? 'Visualizar' : 'Editar'}
                            </DropdownMenuItem>
                          </Can>
                          {!p.isSystem && (
                            <Can role="DPRF">
                              <DropdownMenuItem
                                onClick={() => handleDelete(p)}
                                className="text-destructive focus:text-destructive"
                              >
                                <Trash2 className="h-4 w-4" /> Remover
                              </DropdownMenuItem>
                            </Can>
                          )}
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </Can>
                  </div>

                  <div className="flex-1 mb-4">
                    <p className="text-xs text-muted-foreground mb-2">
                      {p.roles.length} permiss{p.roles.length === 1 ? 'ão' : 'ões'}
                    </p>
                    <div className="flex flex-wrap gap-1">
                      {p.roles.slice(0, 8).map(r => (
                        <Badge key={r} variant="secondary" className="font-mono text-[10px]">
                          {r}
                        </Badge>
                      ))}
                      {p.roles.length > 8 && (
                        <Badge variant="muted" className="text-[10px]">
                          +{p.roles.length - 8}
                        </Badge>
                      )}
                    </div>
                  </div>

                  <div className="flex items-center justify-between border-t border-border pt-3 text-xs text-muted-foreground">
                    <span className="inline-flex items-center gap-1">
                      <UsersIcon className="h-3.5 w-3.5" />
                      {p.usuariosCount} usuário{p.usuariosCount === 1 ? '' : 's'}
                    </span>
                    {p.isSystem && (
                      <Badge variant="muted" className="text-[10px]">
                        sistema
                      </Badge>
                    )}
                  </div>
                </Card>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      )}

      <PerfilFormModal
        open={formState.open}
        onOpenChange={open => setFormState(prev => ({ ...prev, open }))}
        perfil={formState.item}
      />
    </div>
  );
}
