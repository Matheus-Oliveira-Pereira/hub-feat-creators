'use client';

import * as React from 'react';
import { useParams, useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { aceitarConviteSchema, type AceitarConviteInput } from '@/lib/schemas';
import { auth } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

export default function AceitarConvitePage() {
  const params = useParams();
  const router = useRouter();
  const token = typeof params.token === 'string' ? params.token : '';

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<AceitarConviteInput>({
    resolver: zodResolver(aceitarConviteSchema),
    defaultValues: { senha: '', confirmar: '' },
  });

  async function onSubmit(values: AceitarConviteInput) {
    try {
      await auth.aceitarConvite(token, values.senha);
      toast.success('Conta criada! Faça login para continuar.');
      router.push('/login' as Route);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Convite inválido ou expirado.');
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-foreground">
          Aceitar convite
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Crie sua senha para acessar o workspace.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="senha">Senha</Label>
          <Input
            id="senha"
            type="password"
            autoComplete="new-password"
            placeholder="Mínimo de 8 caracteres"
            aria-invalid={!!errors.senha}
            {...register('senha')}
          />
          {errors.senha && (
            <p className="text-xs text-destructive" role="alert">{errors.senha.message}</p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="confirmar">Confirmar senha</Label>
          <Input
            id="confirmar"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.confirmar}
            {...register('confirmar')}
          />
          {errors.confirmar && (
            <p className="text-xs text-destructive" role="alert">{errors.confirmar.message}</p>
          )}
        </div>
        <Button type="submit" disabled={isSubmitting} size="lg" className="w-full mt-2">
          {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <>Criar conta <ArrowRight className="h-4 w-4" /></>}
        </Button>
      </form>
    </motion.div>
  );
}
