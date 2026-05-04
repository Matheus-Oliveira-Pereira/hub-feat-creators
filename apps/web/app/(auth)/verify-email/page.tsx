'use client';

import * as React from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, Loader2 } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { auth } from '@/lib/api';
import type { Route } from 'next';

function VerifyEmailInner() {
  const params = useSearchParams();
  const router = useRouter();
  const token = params.get('token');

  const [state, setState] = React.useState<'loading' | 'success' | 'error'>('loading');
  const [errorMsg, setErrorMsg] = React.useState('');

  React.useEffect(() => {
    if (!token) {
      setState('error');
      setErrorMsg('Token inválido ou ausente.');
      return;
    }

    auth.verifyEmail(token)
      .then(() => setState('success'))
      .catch((err: any) => {
        setState('error');
        setErrorMsg(err?.error?.message ?? 'Link expirado ou já utilizado.');
      });
  }, [token]);

  if (state === 'loading') {
    return (
      <div className="flex flex-col items-center gap-4">
        <Loader2 className="h-10 w-10 animate-spin text-primary" />
        <p className="text-sm text-muted-foreground">Verificando e-mail…</p>
      </div>
    );
  }

  if (state === 'success') {
    return (
      <div className="flex flex-col items-center gap-4 text-center">
        <CheckCircle2 className="h-12 w-12 text-primary" />
        <h1 className="font-display text-2xl font-bold">E-mail verificado!</h1>
        <p className="text-sm text-muted-foreground">Sua conta está ativa. Faça login para continuar.</p>
        <Button size="lg" onClick={() => router.push('/login' as Route)} className="mt-2 w-full">
          Ir para o login
        </Button>
      </div>
    );
  }

  return (
    <div className="flex flex-col items-center gap-4 text-center">
      <XCircle className="h-12 w-12 text-destructive" />
      <h1 className="font-display text-2xl font-bold">Não foi possível verificar</h1>
      <p className="text-sm text-muted-foreground">{errorMsg}</p>
      <p className="text-sm text-muted-foreground">
        Faça{' '}
        <Link href={'/login' as Route} className="font-medium text-foreground underline-offset-4 hover:underline">
          login
        </Link>{' '}
        para reenviar o link de verificação.
      </p>
    </div>
  );
}

export default function VerifyEmailPage() {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
    >
      <React.Suspense fallback={<Loader2 className="h-8 w-8 animate-spin" />}>
        <VerifyEmailInner />
      </React.Suspense>
    </motion.div>
  );
}
