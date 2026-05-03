'use client';

import * as React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Prospeccao } from '@/lib/api';
import { prospeccaoSchema, type ProspeccaoInput } from '@/lib/schemas';
import {
  useCreateProspeccao,
  useUpdateProspeccao,
  useMarcas,
  useInfluenciadores,
} from '@/lib/queries';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { TagsInput } from '@/components/ui/tags-input';
import { EntityFormModal } from '@/components/app/entity-form-modal';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  prospeccao?: Prospeccao | null;
  /** marcaId pré-selecionada (criar a partir de uma marca específica) */
  defaultMarcaId?: string;
}

function toFormDefaults(p?: Prospeccao | null, defaultMarcaId?: string): ProspeccaoInput {
  return {
    marcaId: p?.marcaId ?? defaultMarcaId ?? '',
    influenciadorId: p?.influenciadorId ?? '',
    assessorResponsavelId: p?.assessorResponsavelId ?? '',
    titulo: p?.titulo ?? '',
    valorEstimado:
      p?.valorEstimadoCentavos != null
        ? (p.valorEstimadoCentavos / 100).toLocaleString('pt-BR', {
            minimumFractionDigits: 2,
            maximumFractionDigits: 2,
          })
        : '',
    proximaAcao: p?.proximaAcao ?? '',
    proximaAcaoEm: p?.proximaAcaoEm ?? '',
    observacoes: p?.observacoes ?? '',
    tags: p?.tags ?? [],
  };
}

export function ProspeccaoFormModal({ open, onOpenChange, prospeccao, defaultMarcaId }: Props) {
  const isEdit = !!prospeccao;
  const create = useCreateProspeccao();
  const update = useUpdateProspeccao();
  const marcasQ = useMarcas();
  const influQ = useInfluenciadores();

  const marcas = marcasQ.data?.pages.flatMap(p => p.data) ?? [];
  const influs = influQ.data?.pages.flatMap(p => p.data) ?? [];

  const form = useForm<ProspeccaoInput>({
    resolver: zodResolver(prospeccaoSchema),
    defaultValues: toFormDefaults(prospeccao, defaultMarcaId),
  });

  React.useEffect(() => {
    if (open) form.reset(toFormDefaults(prospeccao, defaultMarcaId));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, prospeccao?.id, defaultMarcaId]);

  const saving = create.isPending || update.isPending || form.formState.isSubmitting;

  async function onSubmit(values: ProspeccaoInput) {
    try {
      if (isEdit && prospeccao) {
        await update.mutateAsync({ id: prospeccao.id, input: values });
        toast.success('Prospecção atualizada.');
      } else {
        await create.mutateAsync(values);
        toast.success('Prospecção criada.');
      }
      onOpenChange(false);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao salvar.');
    }
  }

  const { register, handleSubmit, control, watch, formState } = form;
  const { errors } = formState;
  const observ = watch('observacoes');

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Editar prospecção' : 'Nova prospecção'}
      description={
        isEdit
          ? 'Atualize informações da oportunidade.'
          : 'Registre a oportunidade no momento que ela surge.'
      }
      onSubmit={handleSubmit(onSubmit)}
      submitLabel={isEdit ? 'Salvar alterações' : 'Criar'}
      saving={saving}
    >
      <div className="space-y-1.5">
        <Label htmlFor="prosp-titulo">Título *</Label>
        <Input
          id="prosp-titulo"
          placeholder="Ex: Campanha Verão / @creator"
          aria-invalid={!!errors.titulo}
          {...register('titulo')}
        />
        {errors.titulo && (
          <p className="text-xs text-destructive" role="alert">
            {errors.titulo.message}
          </p>
        )}
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="prosp-marca">Marca *</Label>
          <select
            id="prosp-marca"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2"
            aria-invalid={!!errors.marcaId}
            {...register('marcaId')}
          >
            <option value="">Selecione…</option>
            {marcas.map(m => (
              <option key={m.id} value={m.id}>
                {m.nome}
              </option>
            ))}
          </select>
          {errors.marcaId && (
            <p className="text-xs text-destructive" role="alert">
              {errors.marcaId.message}
            </p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="prosp-inf">Influenciador</Label>
          <select
            id="prosp-inf"
            className="flex h-10 w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring"
            {...register('influenciadorId')}
          >
            <option value="">— sem creator —</option>
            {influs.map(i => (
              <option key={i.id} value={i.id}>
                {i.nome}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div className="space-y-1.5">
          <Label htmlFor="prosp-valor">Valor estimado (R$)</Label>
          <Input
            id="prosp-valor"
            inputMode="decimal"
            placeholder="Ex: 12.500,00"
            aria-invalid={!!errors.valorEstimado}
            {...register('valorEstimado')}
          />
          {errors.valorEstimado && (
            <p className="text-xs text-destructive" role="alert">
              {errors.valorEstimado.message}
            </p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="prosp-prox-em">Próxima ação em</Label>
          <Input id="prosp-prox-em" type="date" {...register('proximaAcaoEm')} />
        </div>
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="prosp-prox">Próxima ação</Label>
        <Input
          id="prosp-prox"
          placeholder="Ex: enviar proposta com KPIs"
          {...register('proximaAcao')}
        />
      </div>

      <div className="space-y-1.5">
        <Label htmlFor="prosp-tags">Tags</Label>
        <Controller
          control={control}
          name="tags"
          render={({ field }) => (
            <TagsInput
              id="prosp-tags"
              value={field.value}
              onChange={field.onChange}
              placeholder="Digite e Enter"
            />
          )}
        />
      </div>

      <div className="space-y-1.5">
        <div className="flex items-center justify-between">
          <Label htmlFor="prosp-obs">Observações</Label>
          <span className="text-xs text-muted-foreground tabular-nums">
            {observ?.length ?? 0}/2000
          </span>
        </div>
        <textarea
          id="prosp-obs"
          rows={3}
          className="flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm shadow-xs placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring resize-none"
          placeholder="Notas internas sobre a oportunidade…"
          {...register('observacoes')}
        />
      </div>
    </EntityFormModal>
  );
}
