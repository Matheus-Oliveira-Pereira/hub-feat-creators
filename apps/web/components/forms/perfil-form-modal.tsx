'use client';

import * as React from 'react';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { toast } from 'sonner';
import { Lock } from 'lucide-react';
import { Perfil } from '@/lib/api';
import { perfilSchema, type PerfilInput } from '@/lib/schemas';
import { useCreatePerfil, useUpdatePerfil } from '@/lib/queries';
import { ROLE_GROUPS } from '@/lib/rbac';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { EntityFormModal } from '@/components/app/entity-form-modal';
import { cn } from '@/lib/utils';

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  perfil?: Perfil | null;
}

function toFormDefaults(p?: Perfil | null): PerfilInput {
  return {
    nome: p?.nome ?? '',
    descricao: p?.descricao ?? '',
    roles: p?.roles ?? [],
  };
}

export function PerfilFormModal({ open, onOpenChange, perfil }: Props) {
  const isEdit = !!perfil;
  const isSystem = !!perfil?.isSystem;
  const create = useCreatePerfil();
  const update = useUpdatePerfil();

  const form = useForm<PerfilInput>({
    resolver: zodResolver(perfilSchema),
    defaultValues: toFormDefaults(perfil),
  });

  React.useEffect(() => {
    if (open) form.reset(toFormDefaults(perfil));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, perfil?.id]);

  const saving = create.isPending || update.isPending || form.formState.isSubmitting;

  async function onSubmit(values: PerfilInput) {
    try {
      if (isEdit && perfil) {
        await update.mutateAsync({ id: perfil.id, input: values });
        toast.success('Perfil atualizado.');
      } else {
        await create.mutateAsync(values);
        toast.success('Perfil criado.');
      }
      onOpenChange(false);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao salvar.');
    }
  }

  const { register, handleSubmit, control, formState } = form;
  const { errors } = formState;

  return (
    <EntityFormModal
      open={open}
      onOpenChange={onOpenChange}
      title={isEdit ? (isSystem ? 'Perfil do sistema' : 'Editar perfil') : 'Novo perfil'}
      description={
        isSystem
          ? 'Perfis seed do sistema só permitem editar a descrição.'
          : isEdit
            ? 'Atualize nome, descrição e permissões.'
            : 'Crie um perfil customizado para a assessoria.'
      }
      onSubmit={handleSubmit(onSubmit)}
      submitLabel={isEdit ? 'Salvar alterações' : 'Criar'}
      saving={saving}
    >
      <div className="space-y-1.5">
        <Label htmlFor="perfil-nome">Nome *</Label>
        <Input
          id="perfil-nome"
          placeholder="Ex: Comercial Sênior"
          aria-invalid={!!errors.nome}
          disabled={isSystem}
          {...register('nome')}
        />
        {errors.nome && (
          <p className="text-xs text-destructive" role="alert">
            {errors.nome.message}
          </p>
        )}
      </div>
      <div className="space-y-1.5">
        <Label htmlFor="perfil-descricao">Descrição</Label>
        <Input
          id="perfil-descricao"
          placeholder="Resumo do que esse perfil pode fazer"
          aria-invalid={!!errors.descricao}
          {...register('descricao')}
        />
      </div>

      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Label>Permissões *</Label>
          {isSystem && (
            <span className="inline-flex items-center gap-1 text-xs text-muted-foreground">
              <Lock className="h-3 w-3" /> Imutável
            </span>
          )}
        </div>
        {errors.roles && (
          <p className="text-xs text-destructive" role="alert">
            {errors.roles.message as string}
          </p>
        )}

        <Controller
          control={control}
          name="roles"
          render={({ field }) => (
            <RolesPicker selected={field.value} onChange={field.onChange} disabled={isSystem} />
          )}
        />
      </div>
    </EntityFormModal>
  );
}

interface RolesPickerProps {
  selected: string[];
  onChange: (next: string[]) => void;
  disabled?: boolean;
}

function RolesPicker({ selected, onChange, disabled }: RolesPickerProps) {
  function toggle(code: string, on: boolean) {
    if (disabled) return;
    if (on) {
      if (!selected.includes(code)) onChange([...selected, code]);
    } else {
      onChange(selected.filter(c => c !== code));
    }
  }

  function toggleGroup(codes: string[]) {
    if (disabled) return;
    const allOn = codes.every(c => selected.includes(c));
    if (allOn) {
      onChange(selected.filter(c => !codes.includes(c)));
    } else {
      const next = new Set(selected);
      codes.forEach(c => next.add(c));
      onChange(Array.from(next));
    }
  }

  return (
    <div className="grid gap-3 max-h-[42vh] overflow-y-auto pr-1 scrollbar-thin">
      {ROLE_GROUPS.map(group => {
        const codes = group.roles.map(r => r.code);
        const allOn = codes.every(c => selected.includes(c));
        const someOn = codes.some(c => selected.includes(c));
        return (
          <div
            key={group.key}
            className="rounded-lg border border-border bg-muted/30 p-3"
          >
            <div className="flex items-center justify-between mb-2">
              <span className="text-xs font-semibold uppercase tracking-wider text-muted-foreground">
                {group.label}
              </span>
              <button
                type="button"
                onClick={() => toggleGroup(codes)}
                disabled={disabled}
                className={cn(
                  'text-xs font-medium transition-colors',
                  allOn
                    ? 'text-destructive hover:text-destructive/80'
                    : 'text-primary hover:text-primary-hover',
                  disabled && 'opacity-50 cursor-not-allowed'
                )}
              >
                {allOn ? 'Desmarcar todas' : someOn ? 'Marcar todas' : 'Marcar todas'}
              </button>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-1.5">
              {group.roles.map(role => {
                const checked = selected.includes(role.code);
                return (
                  <label
                    key={role.code}
                    className={cn(
                      'flex items-start gap-2 rounded-md border px-2 py-1.5 cursor-pointer transition-colors text-xs',
                      checked
                        ? 'border-primary bg-primary/10 text-foreground'
                        : 'border-border bg-card hover:bg-accent text-muted-foreground',
                      disabled && 'cursor-not-allowed opacity-60'
                    )}
                    title={role.description}
                  >
                    <input
                      type="checkbox"
                      className="mt-0.5 accent-primary h-3.5 w-3.5"
                      checked={checked}
                      onChange={e => toggle(role.code, e.target.checked)}
                      disabled={disabled}
                    />
                    <div className="min-w-0">
                      <div className="font-mono text-[10px] uppercase tracking-wider">
                        {role.code}
                      </div>
                      <div className="font-medium leading-tight">{role.label}</div>
                    </div>
                  </label>
                );
              })}
            </div>
          </div>
        );
      })}
    </div>
  );
}
