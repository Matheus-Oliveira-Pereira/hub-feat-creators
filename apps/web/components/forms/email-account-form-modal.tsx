'use client';

import * as React from 'react';
import { useForm } from 'react-hook-form';
import { type EmailAccount, type EmailAccountPayload, type EmailAccountUpdatePayload, type TlsMode } from '@/lib/api';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
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
  onClose: () => void;
  account: EmailAccount | null;
  onSave: (data: EmailAccountPayload | EmailAccountUpdatePayload) => void;
  saving?: boolean;
}

type FormValues = {
  nome: string;
  host: string;
  port: string;
  username: string;
  password: string;
  fromAddress: string;
  fromName: string;
  tlsMode: TlsMode;
  dailyQuota: string;
};

function toDefaults(account: EmailAccount | null): FormValues {
  return {
    nome: account?.nome ?? '',
    host: account?.host ?? '',
    port: String(account?.port ?? 587),
    username: account?.username ?? '',
    password: '',
    fromAddress: account?.fromAddress ?? '',
    fromName: account?.fromName ?? '',
    tlsMode: account?.tlsMode ?? 'STARTTLS',
    dailyQuota: String(account?.dailyQuota ?? 500),
  };
}

export function EmailAccountFormModal({ open, onClose, account, onSave, saving }: Props) {
  const { register, handleSubmit, reset, setValue, watch } = useForm<FormValues>({
    defaultValues: toDefaults(account),
  });

  React.useEffect(() => {
    reset(toDefaults(account));
  }, [account, open, reset]);

  const tlsMode = watch('tlsMode');

  const onSubmit = handleSubmit((values) => {
    const data: EmailAccountPayload | EmailAccountUpdatePayload = {
      nome: values.nome,
      host: values.host,
      port: parseInt(values.port, 10),
      username: values.username,
      fromAddress: values.fromAddress,
      fromName: values.fromName,
      tlsMode: values.tlsMode,
      dailyQuota: parseInt(values.dailyQuota, 10),
      ...(values.password ? { password: values.password } : {}),
    };
    if (!account && !values.password) return; // password required for create
    onSave(data);
  });

  return (
    <EntityFormModal
      open={open}
      onOpenChange={(v) => !v && onClose()}
      title={account ? 'Editar conta SMTP' : 'Nova conta SMTP'}
      onSubmit={onSubmit}
      saving={saving}
    >
      <div className="grid gap-4">
        <div className="grid gap-1.5">
          <Label htmlFor="nome">Nome</Label>
          <Input id="nome" placeholder="Gmail corporativo" {...register('nome', { required: true })} />
        </div>
        <div className="grid grid-cols-3 gap-3">
          <div className="col-span-2 grid gap-1.5">
            <Label htmlFor="host">Host SMTP</Label>
            <Input id="host" placeholder="smtp.gmail.com" {...register('host', { required: true })} />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="port">Porta</Label>
            <Input id="port" type="number" placeholder="587" {...register('port', { required: true })} />
          </div>
        </div>
        <div className="grid gap-1.5">
          <Label>TLS</Label>
          <Select value={tlsMode} onValueChange={(v) => setValue('tlsMode', v as TlsMode)}>
            <SelectTrigger>
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="STARTTLS">STARTTLS (recomendado)</SelectItem>
              <SelectItem value="SSL">SSL/TLS</SelectItem>
            </SelectContent>
          </Select>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="username">Usuário SMTP</Label>
          <Input id="username" {...register('username', { required: true })} />
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="password">Senha{account ? ' (deixe em branco para não alterar)' : ''}</Label>
          <Input id="password" type="password" autoComplete="new-password" {...register('password')} />
        </div>
        <div className="grid grid-cols-2 gap-3">
          <div className="grid gap-1.5">
            <Label htmlFor="fromAddress">E-mail remetente</Label>
            <Input id="fromAddress" type="email" placeholder="contato@empresa.com" {...register('fromAddress', { required: true })} />
          </div>
          <div className="grid gap-1.5">
            <Label htmlFor="fromName">Nome remetente</Label>
            <Input id="fromName" placeholder="Empresa Nome" {...register('fromName', { required: true })} />
          </div>
        </div>
        <div className="grid gap-1.5">
          <Label htmlFor="dailyQuota">Cota diária (e-mails/dia)</Label>
          <Input id="dailyQuota" type="number" {...register('dailyQuota')} />
        </div>
      </div>
    </EntityFormModal>
  );
}
