'use client';

import * as React from 'react';
import { toast } from 'sonner';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { Plus, Smartphone, FileText, Send, RefreshCw, MoreHorizontal, Trash2 } from 'lucide-react';
import { whatsapp, type WaAccount, type WaTemplate } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { PageHeader } from '@/components/app/page-header';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { WaAccountModal } from '@/components/forms/wa-account-modal';
import { WaTemplateModal } from '@/components/forms/wa-template-modal';

const TEMPLATE_STATUS_LABEL: Record<string, string> = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovado',
  REJECTED: 'Rejeitado',
  PAUSED: 'Pausado',
};

const TEMPLATE_STATUS_VARIANT: Record<string, 'default' | 'secondary' | 'destructive'> = {
  PENDING: 'secondary',
  APPROVED: 'default',
  REJECTED: 'destructive',
  PAUSED: 'secondary',
};

function AccountsTab() {
  const qc = useQueryClient();
  const [open, setOpen] = React.useState(false);

  const { data: accounts = [], isLoading } = useQuery({
    queryKey: ['wa-accounts'],
    queryFn: () => whatsapp.accounts.list(),
  });

  const remove = useMutation({
    mutationFn: (id: string) => whatsapp.accounts.delete(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wa-accounts'] });
      toast.success('Conta removida.');
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao remover.'),
  });

  return (
    <>
      <div className="flex justify-end mb-4">
        <Button size="sm" onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Adicionar conta
        </Button>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando…</p>
      ) : accounts.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-center">
          <Smartphone className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm text-muted-foreground">Nenhuma conta WhatsApp configurada.</p>
          <Button size="sm" onClick={() => setOpen(true)}>Adicionar conta</Button>
        </div>
      ) : (
        <div className="rounded-lg border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Número</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Nome</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Enviados hoje</th>
                <th className="px-4 py-3 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {(accounts as WaAccount[]).map((a) => (
                <tr key={a.id} className="hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3 font-mono text-xs">{a.phoneE164}</td>
                  <td className="px-4 py-3">{a.displayName}</td>
                  <td className="px-4 py-3">
                    <Badge variant={a.status === 'ATIVO' ? 'default' : a.status === 'ERRO' ? 'destructive' : 'secondary'}>
                      {a.status}
                    </Badge>
                  </td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">
                    {a.dailySent} / {a.dailyLimit}
                  </td>
                  <td className="px-4 py-3">
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild>
                        <Button variant="ghost" size="icon" className="h-8 w-8">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end">
                        <DropdownMenuItem
                          className="text-destructive focus:text-destructive"
                          onClick={() => {
                            if (confirm('Remover esta conta?')) remove.mutate(a.id);
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

      <WaAccountModal open={open} onOpenChange={setOpen} />
    </>
  );
}

function TemplatesTab() {
  const qc = useQueryClient();
  const [open, setOpen] = React.useState(false);

  const { data: templates = [], isLoading } = useQuery({
    queryKey: ['wa-templates'],
    queryFn: () => whatsapp.templates.list(),
  });

  const submit = useMutation({
    mutationFn: (id: string) => whatsapp.templates.submit(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wa-templates'] });
      toast.success('Template enviado para aprovação Meta.');
    },
    onError: (err: any) => toast.error(err?.message ?? 'Erro ao submeter.'),
  });

  return (
    <>
      <div className="flex justify-end mb-4">
        <Button size="sm" onClick={() => setOpen(true)}>
          <Plus className="h-4 w-4 mr-2" />
          Novo template
        </Button>
      </div>

      {isLoading ? (
        <p className="text-sm text-muted-foreground">Carregando…</p>
      ) : templates.length === 0 ? (
        <div className="flex flex-col items-center gap-3 py-16 text-center">
          <FileText className="h-10 w-10 text-muted-foreground/40" />
          <p className="text-sm text-muted-foreground">Nenhum template criado.</p>
          <Button size="sm" onClick={() => setOpen(true)}>Criar template</Button>
        </div>
      ) : (
        <div className="rounded-lg border bg-card overflow-hidden">
          <table className="w-full text-sm">
            <thead className="border-b bg-muted/50">
              <tr>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Nome</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Categoria</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Idioma</th>
                <th className="px-4 py-3 text-left font-medium text-muted-foreground">Status</th>
                <th className="px-4 py-3 w-10" />
              </tr>
            </thead>
            <tbody className="divide-y">
              {(templates as WaTemplate[]).map((t) => (
                <tr key={t.id} className="hover:bg-muted/30 transition-colors">
                  <td className="px-4 py-3 font-medium">{t.nome}</td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">{t.categoria}</td>
                  <td className="px-4 py-3 text-muted-foreground text-xs">{t.idioma}</td>
                  <td className="px-4 py-3">
                    <Badge variant={TEMPLATE_STATUS_VARIANT[t.status] ?? 'secondary'}>
                      {TEMPLATE_STATUS_LABEL[t.status] ?? t.status}
                    </Badge>
                    {t.motivoRejeicao && (
                      <span className="ml-2 text-xs text-destructive">{t.motivoRejeicao}</span>
                    )}
                  </td>
                  <td className="px-4 py-3">
                    {t.status === 'PENDING' && !t.metaTemplateId && (
                      <Button
                        variant="ghost"
                        size="icon"
                        className="h-8 w-8"
                        title="Submeter para aprovação"
                        onClick={() => submit.mutate(t.id)}
                        disabled={submit.isPending}
                      >
                        <Send className="h-4 w-4" />
                      </Button>
                    )}
                    {t.status === 'PENDING' && t.metaTemplateId && (
                      <RefreshCw className="h-4 w-4 text-muted-foreground animate-spin" />
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      <WaTemplateModal open={open} onOpenChange={setOpen} />
    </>
  );
}

function WhatsappPage() {
  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="WhatsApp"
        description="Contas, templates HSM e envios via Meta Cloud API."
      />
      <Tabs defaultValue="accounts">
        <TabsList>
          <TabsTrigger value="accounts">
            <Smartphone className="h-4 w-4 mr-2" />
            Contas
          </TabsTrigger>
          <TabsTrigger value="templates">
            <FileText className="h-4 w-4 mr-2" />
            Templates HSM
          </TabsTrigger>
        </TabsList>
        <TabsContent value="accounts" className="mt-4">
          <AccountsTab />
        </TabsContent>
        <TabsContent value="templates" className="mt-4">
          <TemplatesTab />
        </TabsContent>
      </Tabs>
    </div>
  );
}

export default function WhatsappPageWrapper() {
  return <WhatsappPage />;
}
