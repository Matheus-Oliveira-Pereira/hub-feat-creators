'use client';

import * as React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { type EmailTemplate, type EmailEnvioPayload } from '@/lib/api';
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
  templates: EmailTemplate[];
  onSend: (data: EmailEnvioPayload) => void;
  saving?: boolean;
  defaultDestinatarioEmail?: string;
  defaultDestinatarioNome?: string;
  defaultContexto?: Record<string, unknown>;
}

type FormValues = {
  templateId: string;
  destinatarioEmail: string;
  destinatarioNome: string;
  trackingEnabled: boolean;
};

export function EmailComposeModal({
  open,
  onClose,
  templates,
  onSend,
  saving,
  defaultDestinatarioEmail = '',
  defaultDestinatarioNome = '',
  defaultContexto,
}: Props) {
  const { register, handleSubmit, reset, control } = useForm<FormValues>({
    defaultValues: {
      templateId: '',
      destinatarioEmail: defaultDestinatarioEmail,
      destinatarioNome: defaultDestinatarioNome,
      trackingEnabled: true,
    },
  });

  React.useEffect(() => {
    reset({
      templateId: templates[0]?.id ?? '',
      destinatarioEmail: defaultDestinatarioEmail,
      destinatarioNome: defaultDestinatarioNome,
      trackingEnabled: true,
    });
  }, [open, templates, defaultDestinatarioEmail, defaultDestinatarioNome, reset]);

  const onSubmit = handleSubmit((values) => {
    onSend({
      templateId: values.templateId,
      destinatarioEmail: values.destinatarioEmail,
      destinatarioNome: values.destinatarioNome || undefined,
      vars: {},
      contexto: defaultContexto ?? {},
      trackingEnabled: values.trackingEnabled,
    });
  });

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
