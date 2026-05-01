'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Contato } from '@/lib/api';
import { contatoSchema, type ContatoInput } from '@/lib/schemas';
import { useCreateContato, useUpdateContato } from '@/lib/queries';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  marcaId: string;
  contato?: Contato | null;
}

function toFormDefaults(c?: Contato | null): ContatoInput {
  return {
    nome: c?.nome ?? '',
    email: c?.email ?? '',
    telefone: c?.telefone ?? '',
    cargo: c?.cargo ?? '',
  };
}

export function ContatoFormModal({ open, onOpenChange, marcaId, contato }: Props) {
  const isEdit = !!contato;
  const create = useCreateContato();
  const update = useUpdateContato();

  const form = useForm<ContatoInput>({
    resolver: zodResolver(contatoSchema),
    defaultValues: toFormDefaults(contato),
  });

  React.useEffect(() => {
    if (open) form.reset(toFormDefaults(contato));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, contato?.id]);

  const saving = create.isPending || update.isPending || form.formState.isSubmitting;

  async function onSubmit(values: ContatoInput) {
    try {
      if (isEdit && contato) {
        await update.mutateAsync({ id: contato.id, marcaId, input: values });
        toast.success('Contato atualizado.');
      } else {
        await create.mutateAsync({ marcaId, input: values });
        toast.success('Contato adicionado.');
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
      title={isEdit ? 'Editar contato' : 'Novo contato'}
      description={isEdit ? 'Atualize os dados do contato.' : 'Adicione uma pessoa de contato da marca.'}
      onSubmit={handleSubmit(onSubmit)}
      submitLabel={isEdit ? 'Salvar alterações' : 'Adicionar'}
      saving={saving}
    >
      <div className="space-y-1.5">
        <Label htmlFor="contato-nome">Nome *</Label>
        <Input
          id="contato-nome"
          placeholder="Ex: Maria Silva"
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
        <Label htmlFor="contato-email">E-mail</Label>
        <Input
          id="contato-email"
          type="email"
          placeholder="contato@empresa.com"
          aria-invalid={!!errors.email}
          {...register('email')}
        />
        {errors.email && (
          <p className="text-xs text-destructive" role="alert">
            {errors.email.message}
          </p>
        )}
      </div>
      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="contato-telefone">Telefone</Label>
          <Input
            id="contato-telefone"
            placeholder="(11) 90000-0000"
            aria-invalid={!!errors.telefone}
            {...register('telefone')}
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="contato-cargo">Cargo</Label>
          <Input
            id="contato-cargo"
            placeholder="Ex: Marketing"
            aria-invalid={!!errors.cargo}
            {...register('cargo')}
          />
        </div>
      </div>
    </EntityFormModal>
  );
}
