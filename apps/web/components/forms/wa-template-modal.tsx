'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { whatsapp, type WaAccount } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';

const schema = z.object({
  accountId: z.string().uuid('Selecione uma conta'),
  nome: z.string().min(1, 'Obrigatório').regex(/^[a-z0-9_]+$/, 'Apenas letras minúsculas, números e _'),
  idioma: z.string().min(1),
  categoria: z.enum(['MARKETING', 'UTILITY', 'AUTHENTICATION']),
  corpo: z.string().min(1, 'Obrigatório').max(1024, 'Máx 1024 caracteres'),
});
type Input = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function WaTemplateModal({ open, onOpenChange }: Props) {
  const qc = useQueryClient();

  const { data: accounts = [] } = useQuery({
    queryKey: ['wa-accounts'],
    queryFn: () => whatsapp.accounts.list(),
    enabled: open,
  });

  const criar = useMutation({
    mutationFn: (data: Input) => whatsapp.templates.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wa-templates'] });
      toast.success('Template criado! Use "Submeter" para enviar à Meta.');
      onOpenChange(false);
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao criar template.'),
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<Input>({
    resolver: zodResolver(schema),
    defaultValues: { idioma: 'pt_BR', categoria: 'UTILITY' },
  });

  React.useEffect(() => {
    if (open) reset({ idioma: 'pt_BR', categoria: 'UTILITY' });
  }, [open, reset]);

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title="Novo template HSM"
      description="Templates precisam de aprovação Meta (24-48h) antes do uso."
      onSubmit={handleSubmit((v) => criar.mutate(v))}
      submitLabel="Criar template"
      saving={criar.isPending}
    >
      <div className="space-y-1.5">
        <Label htmlFor="wa-tpl-account">Conta WhatsApp *</Label>
        <select
          id="wa-tpl-account"
          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          {...register('accountId')}
        >
          <option value="">Selecione…</option>
          {(accounts as WaAccount[]).map((a) => (
            <option key={a.id} value={a.id}>{a.displayName} ({a.phoneE164})</option>
          ))}
        </select>
        {errors.accountId && <p className="text-xs text-destructive" role="alert">{errors.accountId.message}</p>}
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="wa-tpl-nome">Nome *</Label>
          <Input id="wa-tpl-nome" placeholder="boas_vindas" aria-invalid={!!errors.nome} {...register('nome')} />
          {errors.nome && <p className="text-xs text-destructive" role="alert">{errors.nome.message}</p>}
          <p className="text-xs text-muted-foreground">snake_case, sem espaços</p>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="wa-tpl-cat">Categoria *</Label>
          <select
            id="wa-tpl-cat"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            {...register('categoria')}
          >
            <option value="UTILITY">Utilidade</option>
            <option value="MARKETING">Marketing</option>
            <option value="AUTHENTICATION">Autenticação</option>
          </select>
        </div>
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="wa-tpl-idioma">Idioma</Label>
        <Input id="wa-tpl-idioma" placeholder="pt_BR" {...register('idioma')} />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="wa-tpl-corpo">Corpo *</Label>
        <textarea
          id="wa-tpl-corpo"
          rows={4}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
          placeholder="Olá {{1}}, sua proposta está disponível."
          aria-invalid={!!errors.corpo}
          {...register('corpo')}
        />
        {errors.corpo && <p className="text-xs text-destructive" role="alert">{errors.corpo.message}</p>}
        <p className="text-xs text-muted-foreground">Use {'{{1}}'}, {'{{2}}'} para variáveis posicionais.</p>
      </div>
    </EntityFormModal>
  );
}
