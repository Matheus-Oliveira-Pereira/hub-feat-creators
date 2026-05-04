'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { type EmailTemplate, type EmailTemplatePayload } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onClose: () => void;
  template: EmailTemplate | null;
  onSave: (data: EmailTemplatePayload | Partial<EmailTemplatePayload>) => void;
  saving?: boolean;
}

type FormValues = {
  nome: string;
  assunto: string;
  corpoHtml: string;
  corpoTexto: string;
  variaveis: string;
};

function toDefaults(t: EmailTemplate | null): FormValues {
  return {
    nome: t?.nome ?? '',
    assunto: t?.assunto ?? '',
    corpoHtml: t?.corpoHtml ?? '',
    corpoTexto: t?.corpoTexto ?? '',
    variaveis: t?.variaveis?.join(', ') ?? '',
  };
}

export function EmailTemplateFormModal({ open, onClose, template, onSave, saving }: Props) {
  const { register, handleSubmit, reset } = useForm<FormValues>({
    defaultValues: toDefaults(template),
  });

  React.useEffect(() => {
    reset(toDefaults(template));
  }, [template, open, reset]);

  const onSubmit = handleSubmit((values) => {
    const variaveis = values.variaveis
      ? values.variaveis.split(',').map((v) => v.trim()).filter(Boolean)
      : [];
    onSave({
      nome: values.nome,
      assunto: values.assunto,
      corpoHtml: values.corpoHtml,
      corpoTexto: values.corpoTexto || undefined,
      variaveis,
    });
  });

  return (
    <EntityFormModal
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title={template ? 'Editar template' : 'Novo template'}
      onSubmit={onSubmit}
      saving={saving}
    >
      <div className="grid gap-4">
        <div className="grid gap-1.5">
          <Label htmlFor="nome">Nome interno</Label>
          <Input id="nome" placeholder="Boas-vindas" {...register('nome', { required: true })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="assunto">Assunto</Label>
          <Input id="assunto" placeholder="Bem-vindo à {{empresa}}!" {...register('assunto', { required: true })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="corpoHtml">Corpo HTML</Label>
          <Textarea
            id="corpoHtml"
            rows={8}
            className="font-mono text-xs"
            placeholder="<p>Olá {{nome}},</p>"
            {...register('corpoHtml', { required: true })}
          />
          <p className="text-xs text-muted-foreground">Use {'{{variavel}}'} para inserir variáveis Mustache.</p>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="variaveis">Variáveis declaradas</Label>
          <Input id="variaveis" placeholder="nome, empresa, link" {...register('variaveis')} />
          <p className="text-xs text-muted-foreground">Separe por vírgula.</p>
        </div>
      </div>
    </EntityFormModal>
  );
}
