'use client';

import * as React from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { toast } from 'sonner';
import { Loader2 } from 'lucide-react';
import { portalAuth, setCreatorToken } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

const schema = z.object({
  email: z.string().email('E-mail inválido'),
  senha: z.string().min(1, 'Senha obrigatória'),
});
type Fields = z.infer<typeof schema>;

export default function PortalLoginPage() {
  const router = useRouter();
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<Fields>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: Fields) => {
    try {
      const resp = await portalAuth.login(data.email, data.senha);
      setCreatorToken(resp.token);
      router.push('/portal/tarefas' as Route);
    } catch {
      toast.error('E-mail ou senha incorretos.');
    }
  };

  return (
    <div className="max-w-sm mx-auto mt-16 space-y-6">
      <h1 className="text-2xl font-bold text-[#141414]">Entrar no portal</h1>
      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
        <div className="space-y-1">
          <Label htmlFor="email">E-mail</Label>
          <Input id="email" type="email" autoComplete="email" {...register('email')} />
          {errors.email && <p className="text-xs text-red-500">{errors.email.message}</p>}
        </div>
        <div className="space-y-1">
          <Label htmlFor="senha">Senha</Label>
          <Input id="senha" type="password" autoComplete="current-password" {...register('senha')} />
          {errors.senha && <p className="text-xs text-red-500">{errors.senha.message}</p>}
        </div>
        <Button type="submit" className="w-full" disabled={isSubmitting}>
          {isSubmitting && <Loader2 className="mr-2 h-4 w-4 animate-spin" />}
          Entrar
        </Button>
      </form>
    </div>
  );
}
