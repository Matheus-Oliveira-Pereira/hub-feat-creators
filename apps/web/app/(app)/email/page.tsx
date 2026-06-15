'use client';

import * as React from 'react';
import { useSearchParams } from 'next/navigation';
import { Mail, Plus, FileText, Layout, Send } from 'lucide-react';
import { toast } from 'sonner';
import { type EmailTemplate } from '@/lib/api';
import {
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
import { Skeleton } from '@/components/ui/skeleton';
import { PageHeader } from '@/components/app/page-header';
import { EmptyState } from '@/components/app/empty-state';
import { Can } from '@/components/auth/can';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { EmailTemplateFormModal } from '@/components/forms/email-template-form-modal';
import { EmailComposeModal } from '@/components/forms/email-compose-modal';
import { EmailLayoutEditor } from '@/components/app/email-layout-editor';
import { cn } from '@/lib/utils';

const ENVIO_STATUS_BADGE: Record<string, { label: string; className: string }> = {
  ENFILEIRADO: { label: 'Enfileirado', className: 'bg-blue-100 text-blue-700' },
  ENVIADO:     { label: 'Enviado',     className: 'bg-green-100 text-green-700' },
  FALHOU:      { label: 'Falhou',      className: 'bg-red-100 text-red-700' },
  BOUNCED:     { label: 'Bounce',      className: 'bg-orange-100 text-orange-700' },
};

function EmailPageInner() {
  const searchParams = useSearchParams();
  const defaultTab = searchParams.get('tab') ?? 'envios';

  const [showTemplateModal, setShowTemplateModal] = React.useState(false);
  const [editingTemplate, setEditingTemplate] = React.useState<EmailTemplate | null>(null);
  const [showComposeModal, setShowComposeModal] = React.useState(false);

  const { data: templates = [], isLoading: loadingTemplates } = useEmailTemplates();
  const { data: enviosPage, isLoading: loadingEnvios } = useEmailEnvios();

  const createTemplate = useCreateEmailTemplate();
  const updateTemplate = useUpdateEmailTemplate();
  const deleteTemplate = useDeleteEmailTemplate();
  const saveLayout = useSaveEmailLayout();
  const sendEmail = useSendEmail();

  // suppress unused warning — saveLayout is used inside EmailLayoutEditor via hook
  void saveLayout;

  const envios = enviosPage?.data ?? [];

  return (
    <div className="flex flex-col gap-6">
      <PageHeader
        title="E-mail"
        description="Envie e-mails através da conta de sistema configurada."
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

        {/* ── Layout ── */}
        <TabsContent value="layout" className="mt-4">
          <EmailLayoutEditor />
        </TabsContent>
      </Tabs>

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
