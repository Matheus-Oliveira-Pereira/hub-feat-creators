'use client';

import * as React from 'react';
import { useSearchParams } from 'next/navigation';
import { Mail, Plus, Settings, FileText, Layout, Send } from 'lucide-react';
import { toast } from 'sonner';
import { type EmailAccount, type EmailTemplate } from '@/lib/api';
import {
  useEmailAccounts,
  useCreateEmailAccount,
  useUpdateEmailAccount,
  useDeleteEmailAccount,
  useTestEmailAccount,
  useEmailTemplates,
  useCreateEmailTemplate,
  useUpdateEmailTemplate,
  useDeleteEmailTemplate,
  useEmailLayout,
  useSaveEmailLayout,
  useEmailEnvios,
  useSendEmail,
} from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { PageHeader } from '@/components/app/page-header';
import { EmptyState } from '@/components/app/empty-state';
import { Can } from '@/components/auth/can';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { EmailAccountFormModal } from '@/components/forms/email-account-form-modal';
import { EmailTemplateFormModal } from '@/components/forms/email-template-form-modal';
import { EmailComposeModal } from '@/components/forms/email-compose-modal';
import { EmailLayoutEditor } from '@/components/app/email-layout-editor';
import { cn } from '@/lib/utils';

const STATUS_BADGE: Record<string, { label: string; variant: 'default' | 'secondary' | 'destructive' }> = {
  ATIVA:      { label: 'Ativa',     variant: 'default' },
  PAUSADA:    { label: 'Pausada',   variant: 'secondary' },
  FALHA_AUTH: { label: 'Falha Auth', variant: 'destructive' },
};

const ENVIO_STATUS_BADGE: Record<string, { label: string; className: string }> = {
  ENFILEIRADO: { label: 'Enfileirado', className: 'bg-blue-100 text-blue-700' },
  ENVIADO:     { label: 'Enviado',     className: 'bg-green-100 text-green-700' },
  FALHOU:      { label: 'Falhou',      className: 'bg-red-100 text-red-700' },
  BOUNCED:     { label: 'Bounce',      className: 'bg-orange-100 text-orange-700' },
};

function EmailPageInner() {
  const searchParams = useSearchParams();
  const defaultTab = searchParams.get('tab') ?? 'envios';

  const [showAccountModal, setShowAccountModal] = React.useState(false);
  const [editingAccount, setEditingAccount] = React.useState<EmailAccount | null>(null);
  const [showTemplateModal, setShowTemplateModal] = React.useState(false);
  const [editingTemplate, setEditingTemplate] = React.useState<EmailTemplate | null>(null);
  const [showComposeModal, setShowComposeModal] = React.useState(false);

  const { data: accounts = [], isLoading: loadingAccounts } = useEmailAccounts();
  const { data: templates = [], isLoading: loadingTemplates } = useEmailTemplates();
  const { data: enviosPage, isLoading: loadingEnvios } = useEmailEnvios();

  const createAccount = useCreateEmailAccount();
  const updateAccount = useUpdateEmailAccount();
  const deleteAccount = useDeleteEmailAccount();
  const testAccount = useTestEmailAccount();
  const createTemplate = useCreateEmailTemplate();
  const updateTemplate = useUpdateEmailTemplate();
  const deleteTemplate = useDeleteEmailTemplate();
  const saveLayout = useSaveEmailLayout();
  const sendEmail = useSendEmail();

  const envios = enviosPage?.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="E-mail"
        description="Envie e-mails a partir de contas SMTP configuradas pela sua assessoria."
        actions={
          <Can role="CEML">
            <Button onClick={() => setShowComposeModal(true)}>
              <Send className="mr-2 h-4 w-4" />
              Compor
            </Button>
          </Can>
        }
      />

      <Tabs defaultValue={defaultTab}>
        <TabsList>
          <TabsTrigger value="envios"><Mail className="mr-1 h-4 w-4" />Envios</TabsTrigger>
          <TabsTrigger value="templates"><FileText className="mr-1 h-4 w-4" />Templates</TabsTrigger>
          <TabsTrigger value="contas"><Settings className="mr-1 h-4 w-4" />Contas</TabsTrigger>
          <TabsTrigger value="layout"><Layout className="mr-1 h-4 w-4" />Layout</TabsTrigger>
        </TabsList>

        {/* ── Envios ── */}
        <TabsContent value="envios" className="mt-4">
          {loadingEnvios ? (
            <div className="space-y-2">{Array.from({ length: 5 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : envios.length === 0 ? (
            <EmptyState
              icon={<Mail />}
              title="Nenhum envio ainda"
              description="Componha um e-mail para começar."
            />
          ) : (
            <div className="rounded-md border divide-y">
              {envios.map((envio) => {
                const s = ENVIO_STATUS_BADGE[envio.status] ?? { label: envio.status, className: '' };
                return (
                  <div key={envio.id} className="flex items-center gap-4 px-4 py-3 text-sm">
                    <span className="flex-1 truncate font-medium">{envio.destinatarioEmail}</span>
                    <span className="text-muted-foreground truncate max-w-xs">{envio.assunto}</span>
                    <span className={cn('rounded-full px-2 py-0.5 text-xs font-medium', s.className)}>{s.label}</span>
                    <span className="text-muted-foreground text-xs">{new Date(envio.createdAt).toLocaleDateString('pt-BR')}</span>
                  </div>
                );
              })}
            </div>
          )}
        </TabsContent>

        {/* ── Templates ── */}
        <TabsContent value="templates" className="mt-4">
          <div className="flex justify-end mb-4">
            <Can role="CEML">
              <Button size="sm" onClick={() => { setEditingTemplate(null); setShowTemplateModal(true); }}>
                <Plus className="mr-2 h-4 w-4" />Template
              </Button>
            </Can>
          </div>
          {loadingTemplates ? (
            <div className="space-y-2">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : templates.length === 0 ? (
            <EmptyState icon={<FileText />} title="Nenhum template" description="Crie um template para reusar em envios." />
          ) : (
            <div className="rounded-md border divide-y">
              {templates.map((t) => (
                <div key={t.id} className="flex items-center gap-4 px-4 py-3 text-sm">
                  <span className="flex-1 font-medium">{t.nome}</span>
                  <span className="text-muted-foreground">{t.assunto}</span>
                  <div className="flex gap-2">
                    <Can role="EEML">
                      <Button size="sm" variant="ghost" onClick={() => { setEditingTemplate(t); setShowTemplateModal(true); }}>Editar</Button>
                    </Can>
                    <Can role="DEML">
                      <Button size="sm" variant="ghost" className="text-destructive" onClick={() =>
                        deleteTemplate.mutate(t.id, { onSuccess: () => toast.success('Template removido') })
                      }>Excluir</Button>
                    </Can>
                  </div>
                </div>
              ))}
            </div>
          )}
        </TabsContent>

        {/* ── Contas ── */}
        <TabsContent value="contas" className="mt-4">
          <div className="flex justify-end mb-4">
            <Can role="CEML">
              <Button size="sm" onClick={() => { setEditingAccount(null); setShowAccountModal(true); }}>
                <Plus className="mr-2 h-4 w-4" />Conta
              </Button>
            </Can>
          </div>
          {loadingAccounts ? (
            <div className="space-y-2">{Array.from({ length: 3 }).map((_, i) => <Skeleton key={i} className="h-12 w-full" />)}</div>
          ) : accounts.length === 0 ? (
            <EmptyState icon={<Settings />} title="Nenhuma conta SMTP" description="Configure uma conta para enviar e-mails." />
          ) : (
            <div className="rounded-md border divide-y">
              {accounts.map((acc) => {
                const s = STATUS_BADGE[acc.status] ?? { label: acc.status, variant: 'secondary' as const };
                return (
                  <div key={acc.id} className="flex items-center gap-4 px-4 py-3 text-sm">
                    <div className="flex-1 min-w-0">
                      <p className="font-medium">{acc.nome}</p>
                      <p className="text-muted-foreground text-xs">{acc.fromAddress} · {acc.host}:{acc.port}</p>
                    </div>
                    <Badge variant={s.variant}>{s.label}</Badge>
                    <div className="flex gap-2">
                      <Can role="EEML">
                        <Button size="sm" variant="outline" onClick={() =>
                          testAccount.mutate(acc.id, {
                            onSuccess: () => toast.success('Conexão OK'),
                            onError: () => toast.error('Falha na conexão SMTP'),
                          })
                        }>Testar</Button>
                        <Button size="sm" variant="ghost" onClick={() => { setEditingAccount(acc); setShowAccountModal(true); }}>Editar</Button>
                      </Can>
                      <Can role="DEML">
                        <Button size="sm" variant="ghost" className="text-destructive" onClick={() =>
                          deleteAccount.mutate(acc.id, { onSuccess: () => toast.success('Conta removida') })
                        }>Excluir</Button>
                      </Can>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </TabsContent>

        {/* ── Layout ── */}
        <TabsContent value="layout" className="mt-4">
          <EmailLayoutEditor />
        </TabsContent>
      </Tabs>

      <EmailAccountFormModal
        open={showAccountModal}
        account={editingAccount}
        onClose={() => setShowAccountModal(false)}
        onSave={(data) => {
          if (editingAccount) {
            updateAccount.mutate({ id: editingAccount.id, data }, {
              onSuccess: () => { toast.success('Conta atualizada'); setShowAccountModal(false); },
              onError: () => toast.error('Erro ao atualizar conta'),
            });
          } else {
            createAccount.mutate(data as Parameters<typeof createAccount.mutate>[0], {
              onSuccess: () => { toast.success('Conta criada'); setShowAccountModal(false); },
              onError: () => toast.error('Erro ao criar conta'),
            });
          }
        }}
        saving={editingAccount ? updateAccount.isPending : createAccount.isPending}
      />

      <EmailTemplateFormModal
        open={showTemplateModal}
        template={editingTemplate}
        onClose={() => setShowTemplateModal(false)}
        onSave={(data) => {
          if (editingTemplate) {
            updateTemplate.mutate({ id: editingTemplate.id, data }, {
              onSuccess: () => { toast.success('Template atualizado'); setShowTemplateModal(false); },
              onError: () => toast.error('Erro ao atualizar template'),
            });
          } else {
            createTemplate.mutate(data as Parameters<typeof createTemplate.mutate>[0], {
              onSuccess: () => { toast.success('Template criado'); setShowTemplateModal(false); },
              onError: () => toast.error('Erro ao criar template'),
            });
          }
        }}
        saving={editingTemplate ? updateTemplate.isPending : createTemplate.isPending}
      />

      <EmailComposeModal
        open={showComposeModal}
        accounts={accounts}
        templates={templates}
        onClose={() => setShowComposeModal(false)}
        onSend={(data) => {
          sendEmail.mutate(data, {
            onSuccess: () => { toast.success('E-mail enfileirado'); setShowComposeModal(false); },
            onError: (e: unknown) => {
              const msg = (e as { message?: string })?.message ?? 'Erro ao enviar';
              toast.error(msg);
            },
          });
        }}
        saving={sendEmail.isPending}
      />
    </div>
  );
}

export default function EmailPage() {
  return (
    <React.Suspense>
      <EmailPageInner />
    </React.Suspense>
  );
}
