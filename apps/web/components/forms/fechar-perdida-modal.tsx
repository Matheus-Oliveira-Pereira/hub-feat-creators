'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { useMudarStatusProspeccao } from '@/lib/queries';
import { statusChangeSchema, type StatusChangeInput } from '@/lib/schemas';
import { type MotivoPerda } from '@/lib/api';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  prospeccaoId: string;
}

const MOTIVOS: { value: MotivoPerda; label: string }[] = [
  { value: 'SEM_FIT', label: 'Sem fit (creator x marca)' },
  { value: 'ORCAMENTO', label: 'Orçamento' },
  { value: 'TIMING', label: 'Timing' },
  { value: 'CONCORRENTE', label: 'Concorrente' },
  { value: 'SEM_RESPOSTA', label: 'Sem resposta' },
  { value: 'OUTRO', label: 'Outro (descrever)' },
];

export function FecharPerdidaModal({ open, onOpenChange, prospeccaoId }: Props) {
  const mutate = useMudarStatusProspeccao();
  const form = useForm<StatusChangeInput>({
    resolver: zodResolver(statusChangeSchema),
    defaultValues: {
      status: 'FECHADA_PERDIDA',
      motivoPerda: '' as any,
      motivoPerdaDetalhe: '',
    },
  });

  React.useEffect(() => {
    if (open)
      form.reset({
        status: 'FECHADA_PERDIDA',
        motivoPerda: '' as any,
        motivoPerdaDetalhe: '',
      });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open]);

  const motivo = form.watch('motivoPerda');

  async function onSubmit(values: StatusChangeInput) {
    try {
      await mutate.mutateAsync({
        id: prospeccaoId,
        status: 'FECHADA_PERDIDA',
        motivoPerda: values.motivoPerda as MotivoPerda,
        motivoPerdaDetalhe: values.motivoPerdaDetalhe || undefined,
      });
      toast.success('Prospecção fechada como perdida.');
      onOpenChange(false);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao fechar.');
    }
  }

  const { register, handleSubmit, formState } = form;

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title="Fechar como perdida"
      description="Registre o motivo para entender padrões e ajustar abordagem."
      onSubmit={handleSubmit(onSubmit)}
      submitLabel="Fechar"
      saving={mutate.isPending}
    >
      <div className="space-y-1.5">
        <Label htmlFor="motivo">Motivo *</Label>
        <select
          id="motivo"
          className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
          aria-invalid={!!formState.errors.motivoPerda}
          {...register('motivoPerda')}
        >
          <option value="">Selecione…</option>
          {MOTIVOS.map(m => (
            <option key={m.value} value={m.value}>
              {m.label}
            </option>
          ))}
        </select>
        {formState.errors.motivoPerda && (
          <p className="text-xs text-destructive" role="alert">
            {formState.errors.motivoPerda.message}
          </p>
        )}
      </div>
      {motivo === 'OUTRO' && (
        <div className="space-y-1.5">
          <Label htmlFor="motivo-det">Detalhe *</Label>
          <Input
            id="motivo-det"
            placeholder="Descreva brevemente"
            aria-invalid={!!formState.errors.motivoPerdaDetalhe}
            {...register('motivoPerdaDetalhe')}
          />
          {formState.errors.motivoPerdaDetalhe && (
            <p className="text-xs text-destructive" role="alert">
              {formState.errors.motivoPerdaDetalhe.message}
            </p>
          )}
        </div>
      )}
    </EntityFormModal>
  );
}
