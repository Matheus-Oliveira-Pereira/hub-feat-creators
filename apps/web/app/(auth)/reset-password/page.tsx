'use client';

import * as React from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2, CheckCircle2 } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { resetPasswordSchema, type ResetPasswordInput } from '@/lib/schemas';
import { auth } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

function ResetPasswordInner() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get('token') ?? '';
  const [done, setDone] = React.useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ResetPasswordInput>({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { novaSenha: '', confirmar: '' },
  });

  async function onSubmit(values: ResetPasswordInput) {
    try {
      await auth.resetPassword(token, values.novaSenha);
      setDone(true);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Link expirado ou inválido.');
    }
  }

  if (done) {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <CheckCircle2 className="h-12 w-12 text-primary" />
        <h1 className="font-display text-2xl font-bold">Senha redefinida!</h1>
        <p className="text-sm text-muted-foreground">Faça login com sua nova senha.</p>
        <Button size="lg" onClick={() => router.push('/login' as Route)} className="mt-2 w-full">
          Ir para o login
        </Button>
      </div>
    );
  }

  return (
    <>
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-foreground">
          Nova senha
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Crie uma senha com no mínimo 8 caracteres.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="novaSenha">Nova senha</Label>
          <Input
            id="novaSenha"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.novaSenha}
            {...register('novaSenha')}
          />
          {errors.novaSenha && (
            <p className="text-xs text-destructive" role="alert">{errors.novaSenha.message}</p>
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
          {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <>Redefinir senha <ArrowRight className="h-4 w-4" /></>}
        </Button>
      </form>
    </>
  );
}

export default function ResetPasswordPage() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <React.Suspense fallback={<Loader2 className="h-8 w-8 animate-spin" />}>
        <ResetPasswordInner />
      </React.Suspense>
    </motion.div>
  );
}
