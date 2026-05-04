'use client';

import * as React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { type Tarefa, type TarefaPrioridade, type EntidadeTipo } from '@/lib/api';
import { tarefaSchema, type TarefaInput } from '@/lib/schemas';
import { useCreateTarefa, useUpdateTarefa } from '@/lib/queries';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Textarea } from '@/components/ui/textarea';
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
  onOpenChange: (open: boolean) => void;
  tarefa?: Tarefa | null;
  /** pré-vincular a uma entidade */
  defaultEntidadeTipo?: EntidadeTipo;
  defaultEntidadeId?: string;
}

const PRIORIDADE_OPTIONS: { value: TarefaPrioridade; label: string }[] = [
  { value: 'BAIXA',   label: 'Baixa' },
  { value: 'MEDIA',   label: 'Média' },
  { value: 'ALTA',    label: 'Alta' },
  { value: 'URGENTE', label: 'Urgente' },
];

function toFormDefaults(tarefa?: Tarefa | null, tipo?: EntidadeTipo, entidadeId?: string): TarefaInput {
  if (tarefa) {
    return {
      titulo: tarefa.titulo,
      descricao: tarefa.descricao ?? '',
      prazo: tarefa.prazo.slice(0, 16), // datetime-local format
      prioridade: tarefa.prioridade,
      responsavelId: tarefa.responsavelId ?? '',
      entidadeTipo: tarefa.entidadeTipo ?? '',
      entidadeId: tarefa.entidadeId ?? '',
    };
  }
  // Hora default: 23:59 quando só data (AC open question resolvido)
  const agora = new Date();
  agora.setHours(23, 59, 0, 0);
  const prazoDefault = agora.toISOString().slice(0, 16);
  return {
    titulo: '',
    descricao: '',
    prazo: prazoDefault,
    prioridade: 'MEDIA',
    responsavelId: '',
    entidadeTipo: tipo ?? '',
    entidadeId: entidadeId ?? '',
  };
}

export function TarefaFormModal({
  open,
  onOpenChange,
  tarefa,
  defaultEntidadeTipo,
  defaultEntidadeId,
}: Props) {
  const isEdit = !!tarefa;
  const createTarefa = useCreateTarefa();
  const updateTarefa = useUpdateTarefa();

  const form = useForm<TarefaInput>({
    resolver: zodResolver(tarefaSchema),
    defaultValues: toFormDefaults(tarefa, defaultEntidadeTipo, defaultEntidadeId),
  });

  React.useEffect(() => {
    if (open) {
      form.reset(toFormDefaults(tarefa, defaultEntidadeTipo, defaultEntidadeId));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, tarefa]);

  function onSubmit(data: TarefaInput) {
    // Converter datetime-local para ISO
    const prazoIso = new Date(data.prazo).toISOString();
    const payload = { ...data, prazo: prazoIso };

    if (isEdit) {
      updateTarefa.mutate(
        { id: tarefa!.id, input: payload },
        {
          onSuccess: () => { toast.success('Tarefa atualizada'); onOpenChange(false); },
          onError: () => toast.error('Erro ao atualizar tarefa'),
        },
      );
    } else {
      createTarefa.mutate(payload, {
        onSuccess: () => { toast.success('Tarefa criada'); onOpenChange(false); },
        onError: () => toast.error('Erro ao criar tarefa'),
      });
    }
  }

  const isPending = createTarefa.isPending || updateTarefa.isPending;

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? 'Editar tarefa' : 'Nova tarefa'}
      description={isEdit ? 'Atualize os dados da tarefa' : 'Preencha os dados para criar uma nova tarefa'}
      onSubmit={form.handleSubmit(onSubmit)}
      saving={isPending}
      submitLabel={isEdit ? 'Salvar' : 'Criar tarefa'}
    >
      <div className="space-y-4">
        {/* Título */}
        <div className="space-y-1.5">
          <Label htmlFor="titulo">
            Título <span className="text-destructive">*</span>
          </Label>
          <Input
            id="titulo"
            placeholder="Ex: Ligar para contato da marca"
            {...form.register('titulo')}
          />
          {form.formState.errors.titulo && (
            <p className="text-xs text-destructive">{form.formState.errors.titulo.message}</p>
          )}
        </div>

        {/* Prazo */}
        <div className="space-y-1.5">
          <Label htmlFor="prazo">
            Prazo <span className="text-destructive">*</span>
          </Label>
          <Input
            id="prazo"
            type="datetime-local"
            {...form.register('prazo')}
          />
          {form.formState.errors.prazo && (
            <p className="text-xs text-destructive">{form.formState.errors.prazo.message}</p>
          )}
        </div>

        {/* Prioridade */}
        <div className="space-y-1.5">
          <Label>Prioridade</Label>
          <Controller
            control={form.control}
            name="prioridade"
            render={({ field }) => (
              <Select value={field.value} onValueChange={field.onChange}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {PRIORIDADE_OPTIONS.map(p => (
                    <SelectItem key={p.value} value={p.value}>{p.label}</SelectItem>
                  ))}
                </SelectContent>
              </Select>
            )}
          />
        </div>

        {/* Descrição */}
        <div className="space-y-1.5">
          <Label htmlFor="descricao">Descrição</Label>
          <Textarea
            id="descricao"
            placeholder="Detalhes opcionais sobre a tarefa..."
            rows={3}
            className="resize-none"
            {...form.register('descricao')}
          />
        </div>
      </div>
    </EntityFormModal>
  );
}
