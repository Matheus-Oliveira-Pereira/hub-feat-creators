'use client';

import * as React from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { portalAuth, setCreatorToken, type InviteInfo } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

const schema = z
  .object({
    senha: z.string().min(8, 'Mínimo 8 caracteres'),
    confirmar: z.string(),
  })
  .refine((d) => d.senha === d.confirmar, { message: 'Senhas não conferem', path: ['confirmar'] });
type Fields = z.infer<typeof schema>;

function ConviteInner() {
  const searchParams = useSearchParams();
  const rawToken = searchParams.get('token') ?? '';
  const router = useRouter();

  const [info, setInfo] = React.useState<InviteInfo | null>(null);
  const [infoError, setInfoError] = React.useState(false);

  React.useEffect(() => {
    if (!rawToken) { setInfoError(true); return; }
    portalAuth.infoConvite(rawToken).then(setInfo).catch(() => setInfoError(true));
  }, [rawToken]);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Fields>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: Fields) => {
    try {
      const resp = await portalAuth.aceitarConvite(rawToken, data.senha);
      setCreatorToken(resp.token);
      toast.success('Conta criada! Bem-vindo(a).');
      router.push('/portal/tarefas' as Route);
    } catch (e: unknown) {
      const err = e as { code?: string };
      if (err?.code === 'INVITE_EXPIRED') toast.error('Convite expirado ou já utilizado.');
      else if (err?.code === 'CREATOR_ALREADY_EXISTS') toast.error('Você já tem uma conta. Faça login.');
      else toast.error('Erro ao criar conta. Tente novamente.');
    }
  };

  if (infoError) {
    return (
      <div className="max-w-sm mx-auto mt-16 text-center space-y-2">
        <p className="text-lg font-semibold">Convite inválido ou expirado</p>
        <p className="text-sm text-muted-foreground">Solicite um novo convite à sua assessoria.</p>
      </div>
    );
  }

  if (!info) {
    return (
      <div className="flex justify-center mt-16">
        <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="max-w-sm mx-auto mt-16 space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-[#141414]">Criar sua conta</h1>
        <p className="text-sm text-muted-foreground mt-1">
          Olá, <strong>{info.nomeInfluenciador}</strong>! Defina uma senha para acessar o portal.
        </p>
        <p className="text-xs text-muted-foreground mt-1">E-mail: {info.email}</p>
      </div>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-1">
          <Label htmlFor="senha">Senha</Label>
          <Input id="senha" type="password" autoComplete="new-password" {...register('senha')} />
          {errors.senha && <p className="text-xs text-red-500">{errors.senha.message}</p>}
        </div>
        <div className="space-y-1">
          <Label htmlFor="confirmar">Confirmar senha</Label>
          <Input id="confirmar" type="password" autoComplete="new-password" {...register('confirmar')} />
          {errors.confirmar && <p className="text-xs text-red-500">{errors.confirmar.message}</p>}
        </div>
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Criar conta e entrar
        </Button>
      </form>
    </div>
  );
}

export default function ConvitePage() {
  return (
    <React.Suspense fallback={<div className="flex justify-center mt-16"><Loader2 className="h-6 w-6 animate-spin" /></div>}>
      <ConviteInner />
    </React.Suspense>
  );
}
