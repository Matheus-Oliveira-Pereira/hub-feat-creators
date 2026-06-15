'use client';

import * as React from 'react';
import { Loader2, Save, Wifi } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useSystemEmailConfig, useUpdateSystemEmailConfig, useTestSystemEmail } from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { PageHeader } from '@/components/app/page-header';
import { Can } from '@/components/auth/can';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';

const schema = z.object({
  host: z.string().min(1, 'Obrigatório'),
  port: z.coerce.number().int().min(1).max(65535),
  username: z.string().min(1, 'Obrigatório'),
  password: z.string().optional(),
  fromAddress: z.string().email('E-mail inválido'),
  fromName: z.string().min(1, 'Obrigatório'),
  tlsMode: z.enum(['STARTTLS', 'SSL']),
  dailyQuota: z.coerce.number().int().min(1),
});
type FormValues = z.infer<typeof schema>;

const STATUS_BADGE: Record<string, { label: string; variant: 'default' | 'secondary' | 'destructive' }> = {
  ATIVA:           { label: 'Ativa',           variant: 'default' },
  FALHA_AUTH:      { label: 'Falha de auth',   variant: 'destructive' },
  NAO_CONFIGURADO: { label: 'Não configurado', variant: 'secondary' },
};

function EmailConfigPageInner() {
  const { data: cfg, isLoading } = useSystemEmailConfig();
  const update = useUpdateSystemEmailConfig();
  const testConn = useTestSystemEmail();

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isDirty },
  } = useForm<FormValues>({
    resolver: zodResolver(schema),
    defaultValues: {
      host: '', port: 587, username: '', password: '',
      fromAddress: '', fromName: 'feat. creators',
      tlsMode: 'STARTTLS', dailyQuota: 500,
    },
  });

  React.useEffect(() => {
    if (!cfg) return;
    setValue('host', cfg.host ?? '');
    setValue('port', cfg.port ?? 587);
    setValue('username', cfg.username ?? '');
    setValue('fromAddress', cfg.fromAddress ?? '');
    setValue('fromName', cfg.fromName ?? 'feat. creators');
    setValue('tlsMode', (cfg.tlsMode as 'STARTTLS' | 'SSL') ?? 'STARTTLS');
    setValue('dailyQuota', cfg.dailyQuota ?? 500);
  }, [cfg, setValue]);

  async function onSubmit(values: FormValues) {
    try {
      const payload = { ...values };
      if (!payload.password) delete (payload as Record<string, unknown>).password;
      await update.mutateAsync(payload);
      toast.success('Configuração salva.');
    } catch {
      toast.error('Erro ao salvar configuração.');
    }
  }

  async function onTest() {
    try {
      await testConn.mutateAsync();
      toast.success('Conexão SMTP OK.');
    } catch (err: unknown) {
      const msg = (err as { error?: { message?: string } })?.error?.message ?? 'Conexão falhou.';
      toast.error(msg);
    }
  }

  const status = cfg?.status ?? 'NAO_CONFIGURADO';
  const badge = STATUS_BADGE[status] ?? STATUS_BADGE.NAO_CONFIGURADO;
  const tlsMode = watch('tlsMode');

  if (isLoading) return <div className="p-8"><Loader2 className="h-5 w-5 animate-spin" /></div>;

  return (
    <div className="max-w-xl space-y-6">
      <PageHeader
        title="Conta de e-mail"
        description="Configure o SMTP de saída do sistema."
        actions={
          <Badge variant={badge.variant}>{badge.label}</Badge>
        }
      />

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="grid grid-cols-3 gap-3">
          <div className="col-span-2 space-y-1.5">
            <Label htmlFor="host">Servidor SMTP</Label>
            <Input id="host" placeholder="smtp.gmail.com" {...register('host')} />
            {errors.host && <p className="text-xs text-destructive">{errors.host.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="port">Porta</Label>
            <Input id="port" type="number" placeholder="587" {...register('port')} />
            {errors.port && <p className="text-xs text-destructive">{errors.port.message}</p>}
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="tlsMode">Segurança</Label>
          <Select value={tlsMode} onValueChange={(v) => setValue('tlsMode', v as 'STARTTLS' | 'SSL')}>
            <SelectTrigger id="tlsMode">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="STARTTLS">STARTTLS (porta 587)</SelectItem>
              <SelectItem value="SSL">SSL/TLS (porta 465)</SelectItem>
            </SelectContent>
          </Select>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="username">Usuário</Label>
          <Input id="username" type="email" placeholder="voce@gmail.com" {...register('username')} />
          {errors.username && <p className="text-xs text-destructive">{errors.username.message}</p>}
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="password">
            Senha / App Password{cfg?.passwordSet && <span className="ml-1 text-xs text-muted-foreground">(configurada — deixe em branco para manter)</span>}
          </Label>
          <Input id="password" type="password" autoComplete="new-password"
            placeholder={cfg?.passwordSet ? '••••••••' : 'Senha ou app password'}
            {...register('password')} />
        </div>

        <div className="grid grid-cols-2 gap-3">
          <div className="space-y-1.5">
            <Label htmlFor="fromAddress">E-mail remetente</Label>
            <Input id="fromAddress" type="email" placeholder="noreply@suaempresa.com" {...register('fromAddress')} />
            {errors.fromAddress && <p className="text-xs text-destructive">{errors.fromAddress.message}</p>}
          </div>
          <div className="space-y-1.5">
            <Label htmlFor="fromName">Nome remetente</Label>
            <Input id="fromName" placeholder="feat. creators" {...register('fromName')} />
            {errors.fromName && <p className="text-xs text-destructive">{errors.fromName.message}</p>}
          </div>
        </div>

        <div className="space-y-1.5">
          <Label htmlFor="dailyQuota">Cota diária (e-mails/dia)</Label>
          <Input id="dailyQuota" type="number" placeholder="500" {...register('dailyQuota')} />
          {errors.dailyQuota && <p className="text-xs text-destructive">{errors.dailyQuota.message}</p>}
        </div>

        <div className="flex gap-2 pt-2">
          <Button type="submit" disabled={update.isPending || !isDirty}>
            {update.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Save className="h-4 w-4" />}
            Salvar
          </Button>
          <Button type="button" variant="outline" onClick={onTest} disabled={testConn.isPending}>
            {testConn.isPending ? <Loader2 className="h-4 w-4 animate-spin" /> : <Wifi className="h-4 w-4" />}
            Testar conexão
          </Button>
        </div>
      </form>
    </div>
  );
}

export default function EmailConfigPage() {
  return (
    <Can role="OWNR" fallback={<p className="p-8 text-sm text-muted-foreground">Sem permissão.</p>}>
      <EmailConfigPageInner />
    </Can>
  );
}
