'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { convites } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';

const schema = z.object({
  email: z.string().trim().email('E-mail inválido'),
  role: z.enum(['OWNER', 'ASSESSOR']).default('ASSESSOR'),
});
type Input = z.infer<typeof schema>;

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function ConviteModal({ open, onOpenChange }: Props) {
  const qc = useQueryClient();
  const criar = useMutation({
    mutationFn: (data: Input) => convites.criar(data),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['convites'] });
      toast.success('Convite enviado!');
      onOpenChange(false);
    },
    onError: (err: any) => toast.error(err?.error?.message ?? 'Erro ao convidar.'),
  });

  const { register, handleSubmit, reset, formState: { errors } } = useForm<Input>({
    resolver: zodResolver(schema),
    defaultValues: { email: '', role: 'ASSESSOR' },
  });

  React.useEffect(() => {
    if (open) reset({ email: '', role: 'ASSESSOR' });
  }, [open, reset]);

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title="Convidar membro"
      description="O convidado receberá um e-mail com instruções para criar a conta."
      onSubmit={handleSubmit((v) => criar.mutate(v))}
      submitLabel="Enviar convite"
      saving={criar.isPending}
    >
      <div className="space-y-1.5">
        <Label htmlFor="convite-email">E-mail *</Label>
        <Input
          id="convite-email"
          type="email"
          placeholder="colaborador@empresa.com"
          aria-invalid={!!errors.email}
          {...register('email')}
        />
        {errors.email && (
          <p className="text-xs text-destructive" role="alert">{errors.email.message}</p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="convite-role">Função</Label>
        <select
          id="convite-role"
          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:border-ring"
          {...register('role')}
        >
          <option value="ASSESSOR">Assessor</option>
          <option value="OWNER">Owner</option>
        </select>
      </div>
    </EntityFormModal>
  );
}
