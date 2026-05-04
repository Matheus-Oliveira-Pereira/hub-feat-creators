'use client';

import * as React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { type EmailAccount, type EmailTemplate, type EmailEnvioPayload } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onClose: () => void;
  accounts: EmailAccount[];
  templates: EmailTemplate[];
  onSend: (data: EmailEnvioPayload) => void;
  saving?: boolean;
  /** Pre-fill context (e.g. from prospecção or contato) */
  defaultDestinatarioEmail?: string;
  defaultDestinatarioNome?: string;
  defaultContexto?: Record<string, unknown>;
}

type FormValues = {
  accountId: string;
  templateId: string;
  destinatarioEmail: string;
  destinatarioNome: string;
  trackingEnabled: boolean;
};

export function EmailComposeModal({
  open,
  onClose,
  accounts,
  templates,
  onSend,
  saving,
  defaultDestinatarioEmail = '',
  defaultDestinatarioNome = '',
  defaultContexto,
}: Props) {
  const { register, handleSubmit, reset, control } = useForm<FormValues>({
    defaultValues: {
      accountId: '',
      templateId: '',
      destinatarioEmail: defaultDestinatarioEmail,
      destinatarioNome: defaultDestinatarioNome,
      trackingEnabled: true,
    },
  });

  React.useEffect(() => {
    reset({
      accountId: accounts[0]?.id ?? '',
      templateId: templates[0]?.id ?? '',
      destinatarioEmail: defaultDestinatarioEmail,
      destinatarioNome: defaultDestinatarioNome,
      trackingEnabled: true,
    });
  }, [open, accounts, templates, defaultDestinatarioEmail, defaultDestinatarioNome, reset]);

  const onSubmit = handleSubmit((values) => {
    onSend({
      accountId: values.accountId,
      templateId: values.templateId,
      destinatarioEmail: values.destinatarioEmail,
      destinatarioNome: values.destinatarioNome || undefined,
      vars: {},
      contexto: defaultContexto ?? {},
      trackingEnabled: values.trackingEnabled,
    });
  });

  const activeAccounts = accounts.filter((a) => a.status === 'ATIVA');

  return (
    <EntityFormModal
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title="Compor e-mail"
      submitLabel="Enviar"
      onSubmit={onSubmit}
      saving={saving}
    >
      <div className="grid gap-4">
        <div className="grid gap-1.5">
          <Label>Conta remetente</Label>
          <Controller
            control={control}
            name="accountId"
            rules={{ required: true }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecione uma conta" />
                </SelectTrigger>
                <SelectContent>
                  {activeAccounts.map((a) => (
                    <SelectItem key={a.id} value={a.id}>{a.nome} — {a.fromAddress}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
          {activeAccounts.length === 0 && (
            <p className="text-xs text-destructive">Nenhuma conta SMTP ativa. Configure uma conta primeiro.</p>
          )}
        </div>
        <div className="grid gap-1.5">
          <Label>Template</Label>
          <Controller
            control={control}
            name="templateId"
            rules={{ required: true }}
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue placeholder="Selecione um template" />
                </SelectTrigger>
                <SelectContent>
                  {templates.map((t) => (
                    <SelectItem key={t.id} value={t.id}>{t.nome}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="grid gap-1.5">
            <Label htmlFor="destinatarioEmail">E-mail destinatário</Label>
            <Input
              id="destinatarioEmail"
              type="email"
              {...register('destinatarioEmail', { required: true })}
            />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="destinatarioNome">Nome destinatário</Label>
            <Input id="destinatarioNome" {...register('destinatarioNome')} />
          </div>
        </div>
      </div>
    </EntityFormModal>
  );
}
