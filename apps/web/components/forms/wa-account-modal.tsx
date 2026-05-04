'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { whatsapp } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';

const schema = z.object({
  wabaId: z.string().min(1, 'Obrigatório'),
  phoneNumberId: z.string().min(1, 'Obrigatório'),
  phoneE164: z.string().regex(/^\+\d{10,15}$/, 'Formato: +5511999999999'),
  displayName: z.string().min(1, 'Obrigatório'),
  accessToken: z.string().min(1, 'Obrigatório'),
  appSecret: z.string().min(1, 'Obrigatório'),
});
type Input = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function WaAccountModal({ open, onOpenChange }: Props) {
  const qc = useQueryClient();
  const criar = useMutation({
    mutationFn: (data: Input) => whatsapp.accounts.create(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['wa-accounts'] });
      toast.success('Conta adicionada!');
      onOpenChange(false);
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao adicionar conta.'),
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<Input>({
    resolver: zodResolver(schema),
  });

  React.useEffect(() => {
    if (open) reset();
  }, [open, reset]);

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title="Adicionar conta WhatsApp"
      description="Informe os dados da sua WABA (WhatsApp Business Account)."
      onSubmit={handleSubmit((v) => criar.mutate(v))}
      submitLabel="Salvar"
      saving={criar.isPending}
    >
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="wa-waba-id">WABA ID *</Label>
          <Input id="wa-waba-id" placeholder="123456789" aria-invalid={!!errors.wabaId} {...register('wabaId')} />
          {errors.wabaId && <p className="text-xs text-destructive" role="alert">{errors.wabaId.message}</p>}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="wa-phone-id">Phone Number ID *</Label>
          <Input id="wa-phone-id" placeholder="987654321" aria-invalid={!!errors.phoneNumberId} {...register('phoneNumberId')} />
          {errors.phoneNumberId && <p className="text-xs text-destructive" role="alert">{errors.phoneNumberId.message}</p>}
        </div>
      </div>
      <div className="grid grid-cols-2 gap-4">
        <div className="space-y-1.5">
          <Label htmlFor="wa-e164">Número (E.164) *</Label>
          <Input id="wa-e164" placeholder="+5511999999999" aria-invalid={!!errors.phoneE164} {...register('phoneE164')} />
          {errors.phoneE164 && <p className="text-xs text-destructive" role="alert">{errors.phoneE164.message}</p>}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="wa-name">Nome de exibição *</Label>
          <Input id="wa-name" placeholder="Constellation WA" aria-invalid={!!errors.displayName} {...register('displayName')} />
          {errors.displayName && <p className="text-xs text-destructive" role="alert">{errors.displayName.message}</p>}
        </div>
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="wa-token">Access Token *</Label>
        <Input id="wa-token" type="password" placeholder="EAAxx…" aria-invalid={!!errors.accessToken} {...register('accessToken')} />
        {errors.accessToken && <p className="text-xs text-destructive" role="alert">{errors.accessToken.message}</p>}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="wa-secret">App Secret *</Label>
        <Input id="wa-secret" type="password" placeholder="hex…" aria-invalid={!!errors.appSecret} {...register('appSecret')} />
        {errors.appSecret && <p className="text-xs text-destructive" role="alert">{errors.appSecret.message}</p>}
        <p className="text-xs text-muted-foreground">Encontrado em Meta for Developers → App → App Secret. Cifrado em repouso.</p>
      </div>
    </EntityFormModal>
  );
}
