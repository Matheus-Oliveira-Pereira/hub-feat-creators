'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Influenciador } from '@/lib/api';
import { influenciadorSchema, type InfluenciadorInput } from '@/lib/schemas';
import { useCreateInfluenciador, useUpdateInfluenciador } from '@/lib/queries';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  influenciador?: Influenciador | null;
}

function toFormDefaults(inf?: Influenciador | null): InfluenciadorInput {
  return {
    nome: inf?.nome ?? '',
    nicho: inf?.nicho ?? '',
    instagram: inf?.handles?.instagram ?? '',
    audienciaTotal: inf?.audienciaTotal != null ? String(inf.audienciaTotal) : '',
    observacoes: inf?.observacoes ?? '',
    tags: inf?.tags ?? [],
  };
}

export function InfluenciadorFormModal({ open, onOpenChange, influenciador }: Props) {
  const isEdit = !!influenciador;
  const create = useCreateInfluenciador();
  const update = useUpdateInfluenciador();

  const form = useForm<InfluenciadorInput>({
    resolver: zodResolver(influenciadorSchema),
    defaultValues: toFormDefaults(influenciador),
  });

  // Reset form quando muda entre create/edit
  React.useEffect(() => {
    if (open) form.reset(toFormDefaults(influenciador));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, influenciador?.id]);

  const saving = create.isPending || update.isPending || form.formState.isSubmitting;

  async function onSubmit(values: InfluenciadorInput) {
    try {
      if (isEdit && influenciador) {
        await update.mutateAsync({ id: influenciador.id, input: values });
        toast.success('Influenciador atualizado.');
      } else {
        await create.mutateAsync(values);
        toast.success('Influenciador criado.');
      }
      onOpenChange(false);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao salvar.');
    }
  }

  const { register, handleSubmit, formState } = form;
  const { errors } = formState;

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Editar influenciador' : 'Novo influenciador'}
      description={
        isEdit
          ? 'Atualize as informações do creator.'
          : 'Informações básicas. Você pode editar depois.'
      }
      onSubmit={handleSubmit(onSubmit)}
      submitLabel={isEdit ? 'Salvar alterações' : 'Criar'}
      saving={saving}
    >
      <div className="space-y-1.5">
        <Label htmlFor="nome">Nome *</Label>
        <Input
          id="nome"
          placeholder="Nome do creator"
          aria-invalid={!!errors.nome}
          {...register('nome')}
        />
        {errors.nome && (
          <p className="text-xs text-destructive" role="alert">
            {errors.nome.message}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="nicho">Nicho</Label>
        <Input
          id="nicho"
          placeholder="Ex: fitness, moda, gaming"
          aria-invalid={!!errors.nicho}
          {...register('nicho')}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="instagram">Instagram</Label>
        <div className="flex items-center rounded-md border border-input bg-background shadow-xs focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background focus-within:border-ring">
          <span className="pl-3 text-sm text-muted-foreground select-none">@</span>
          <input
            id="instagram"
            className="flex-1 h-10 px-2 bg-transparent text-sm font-mono outline-none placeholder:text-muted-foreground"
            placeholder="usuario"
            aria-invalid={!!errors.instagram}
            {...register('instagram')}
          />
        </div>
        {errors.instagram && (
          <p className="text-xs text-destructive" role="alert">
            {errors.instagram.message}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="audienciaTotal">Audiência total</Label>
        <Input
          id="audienciaTotal"
          inputMode="numeric"
          placeholder="Ex: 150000"
          aria-invalid={!!errors.audienciaTotal}
          {...register('audienciaTotal')}
        />
        {errors.audienciaTotal && (
          <p className="text-xs text-destructive" role="alert">
            {errors.audienciaTotal.message}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="observacoes">Observações</Label>
        <textarea
          id="observacoes"
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50 resize-none"
          placeholder="Notas internas sobre o creator…"
          aria-invalid={!!errors.observacoes}
          {...register('observacoes')}
        />
      </div>
    </EntityFormModal>
  );
}
