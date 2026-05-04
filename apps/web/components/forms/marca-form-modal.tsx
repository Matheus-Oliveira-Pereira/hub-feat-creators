'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Marca } from '@/lib/api';
import { marcaSchema, type MarcaInput } from '@/lib/schemas';
import { useCreateMarca, useUpdateMarca } from '@/lib/queries';
import { Controller } from 'react-hook-form';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { TagsInput } from '@/components/ui/tags-input';
import { EntityFormModal } from '@/components/app/entity-form-modal';
import { BaseLegalSelect } from '@/components/forms/base-legal-select';
import type { BaseLegal } from '@/lib/schemas';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  marca?: Marca | null;
}

function toFormDefaults(m?: Marca | null): MarcaInput {
  return {
    nome: m?.nome ?? '',
    segmento: m?.segmento ?? '',
    site: m?.site ?? '',
    observacoes: m?.observacoes ?? '',
    tags: m?.tags ?? [],
    baseLegal: (m?.baseLegal as BaseLegal) ?? 'LEGITIMO_INTERESSE',
  };
}

export function MarcaFormModal({ open, onOpenChange, marca }: Props) {
  const isEdit = !!marca;
  const create = useCreateMarca();
  const update = useUpdateMarca();

  const form = useForm<MarcaInput>({
    resolver: zodResolver(marcaSchema),
    defaultValues: toFormDefaults(marca),
  });

  React.useEffect(() => {
    if (open) form.reset(toFormDefaults(marca));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, marca?.id]);

  const saving = create.isPending || update.isPending || form.formState.isSubmitting;

  async function onSubmit(values: MarcaInput) {
    try {
      if (isEdit && marca) {
        await update.mutateAsync({ id: marca.id, input: values });
        toast.success('Marca atualizada.');
      } else {
        await create.mutateAsync(values);
        toast.success('Marca criada.');
      }
      onOpenChange(false);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao salvar.');
    }
  }

  const { register, handleSubmit, control, watch, formState } = form;
  const { errors } = formState;
  const observacoesWatch = watch('observacoes');

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Editar marca' : 'Nova marca'}
      description={
        isEdit
          ? 'Atualize as informações da marca.'
          : 'Empresa, agência ou parceiro.'
      }
      onSubmit={handleSubmit(onSubmit)}
      submitLabel={isEdit ? 'Salvar alterações' : 'Criar'}
      saving={saving}
    >
      <div className="space-y-1.5">
        <Label htmlFor="nome">Nome *</Label>
        <Input
          id="nome"
          placeholder="Nome da marca"
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
        <Label htmlFor="segmento">Segmento</Label>
        <Input
          id="segmento"
          placeholder="Ex: moda, beleza, tech"
          aria-invalid={!!errors.segmento}
          {...register('segmento')}
        />
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="site">Site</Label>
        <Input
          id="site"
          placeholder="exemplo.com"
          aria-invalid={!!errors.site}
          {...register('site')}
        />
        {errors.site && (
          <p className="text-xs text-destructive" role="alert">
            {errors.site.message}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="tags">Tags</Label>
        <Controller
          control={control}
          name="tags"
          render={({ field }) => (
            <TagsInput
              id="tags"
              value={field.value}
              onChange={field.onChange}
              placeholder="Digite e Enter para adicionar"
              aria-invalid={!!errors.tags}
            />
          )}
        />
        {errors.tags && (
          <p className="text-xs text-destructive" role="alert">
            {errors.tags.message as string}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <Label htmlFor="observacoes">Observações</Label>
          <span className="text-xs text-muted-foreground tabular-nums">
            {observacoesWatch?.length ?? 0}/2000
          </span>
        </div>
        <textarea
          id="observacoes"
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs transition-colors placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 focus-visible:ring-offset-background focus-visible:border-ring disabled:cursor-not-allowed disabled:opacity-50 resize-none"
          placeholder="Notas internas sobre a marca…"
          aria-invalid={!!errors.observacoes}
          {...register('observacoes')}
        />
        {errors.observacoes && (
          <p className="text-xs text-destructive" role="alert">
            {errors.observacoes.message}
          </p>
        )}
      </div>
      <Controller
        control={control}
        name="baseLegal"
        render={({ field }) => (
          <BaseLegalSelect
            value={field.value}
            onChange={field.onChange}
            error={errors.baseLegal?.message}
            required
          />
        )}
      />
    </EntityFormModal>
  );
}
