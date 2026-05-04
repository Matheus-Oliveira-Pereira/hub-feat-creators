'use client';

import * as React from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { setTokens } from '@/lib/api';
import { loginSchema, type LoginInput } from '@/lib/schemas';
import { useLoginMutation } from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

export default function LoginPage() {
  const router = useRouter();
  const login = useLoginMutation();
  const [showMfa, setShowMfa] = React.useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: '', senha: '', mfaCode: '' },
  });

  async function onSubmit(values: LoginInput) {
    try {
      const tokens = await login.mutateAsync(values);
      setTokens(tokens.accessToken, tokens.refreshToken);
      toast.success('Bem-vindo de volta!');
      router.push('/');
    } catch (err: any) {
      const code = err?.error?.code;
      if (code === 'MFA_REQUIRED') {
        setShowMfa(true);
        return;
      }
      toast.error(err?.error?.message ?? 'Credenciais inválidas.');
    }
  }

  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <div className="mb-8">
        <h1 className="font-display text-3xl font-bold tracking-tight text-foreground">Entrar</h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Acesse sua conta para gerenciar a operação.
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
            <p className="text-xs text-destructive" role="alert">
              {errors.email.message}
            </p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="senha">Senha</Label>
          <Input
            id="senha"
            type="password"
            autoComplete="current-password"
            placeholder="••••••••"
            aria-invalid={!!errors.senha}
            {...register('senha')}
          />
          {errors.senha && (
            <p className="text-xs text-destructive" role="alert">
              {errors.senha.message}
            </p>
          )}
        </div>
        {showMfa && (
          <div className="space-y-1.5">
            <Label htmlFor="mfaCode">Código MFA</Label>
            <Input
              id="mfaCode"
              type="text"
              inputMode="numeric"
              autoComplete="one-time-code"
              placeholder="000000"
              maxLength={6}
              aria-invalid={!!errors.mfaCode}
              {...register('mfaCode')}
            />
            {errors.mfaCode && (
              <p className="text-xs text-destructive" role="alert">{errors.mfaCode.message}</p>
            )}
          </div>
        )}
        <div className="flex items-center justify-end">
          <Link
            href={'/forgot-password' as Route}
            className="text-xs text-muted-foreground underline-offset-4 hover:underline"
          >
            Esqueceu a senha?
          </Link>
        </div>
        <Button type="submit" disabled={isSubmitting} size="lg" className="w-full mt-2">
          {isSubmitting ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <>
              Entrar <ArrowRight className="h-4 w-4" />
            </>
          )}
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Ainda não tem uma conta?{' '}
        <Link
          href={'/signup' as Route}
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Criar workspace
        </Link>
      </p>
    </motion.div>
  );
}
