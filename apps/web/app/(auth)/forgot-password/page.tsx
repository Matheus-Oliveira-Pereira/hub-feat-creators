'use client';

import * as React from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2, Mail } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { forgotPasswordSchema, type ForgotPasswordInput } from '@/lib/schemas';
import { auth } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

export default function ForgotPasswordPage() {
  const [sent, setSent] = React.useState(false);

  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<ForgotPasswordInput>({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  async function onSubmit(values: ForgotPasswordInput) {
    try {
      await auth.forgotPassword(values.email);
      setSent(true);
    } catch {
      toast.error('Erro ao enviar. Tente novamente.');
    }
  }

  if (sent) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
        className="flex flex-col items-center gap-4 text-center"
      >
        <Mail className="h-12 w-12 text-primary" />
        <h1 className="font-display text-2xl font-bold">Verifique sua caixa de entrada</h1>
        <p className="text-sm text-muted-foreground max-w-xs">
          Se o e-mail estiver cadastrado, você receberá as instruções em breve. O link expira em 1 hora.
        </p>
        <Link
          href={'/login' as Route}
          className="text-sm font-medium text-foreground underline-offset-4 hover:underline"
        >
          Voltar ao login
        </Link>
      </motion.div>
    );
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-foreground">
          Esqueceu a senha?
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Digite seu e-mail e enviaremos um link de redefinição.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="email">E-mail</Label>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            placeholder="voce@assessoria.com"
            aria-invalid={!!errors.email}
            {...register('email')}
          />
          {errors.email && (
            <p className="text-xs text-destructive" role="alert">{errors.email.message}</p>
          )}
        </div>
        <Button type="submit" disabled={isSubmitting} size="lg" className="w-full mt-2">
          {isSubmitting ? <Loader2 className="h-4 w-4 animate-spin" /> : <>Enviar link <ArrowRight className="h-4 w-4" /></>}
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Lembrou?{' '}
        <Link href={'/login' as Route} className="font-medium text-foreground underline-offset-4 hover:underline">
          Voltar ao login
        </Link>
      </p>
    </motion.div>
  );
}
