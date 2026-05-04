'use client';

import * as React from 'react';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2, Mail } from 'lucide-react';
import { toast } from 'sonner';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { signupSchema, type SignupInput } from '@/lib/schemas';
import { useSignupMutation } from '@/lib/queries';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import type { Route } from 'next';

function autoSlug(nome: string) {
  return nome
    .toLowerCase()
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/\s+/g, '-')
    .replace(/[^a-z0-9-]/g, '')
    .slice(0, 50);
}

export default function SignupPage() {
  const signup = useSignupMutation();
  const [email, setEmail] = React.useState<string | null>(null);

  const {
    register,
    handleSubmit,
    setValue,
    watch,
    formState: { errors, isSubmitting, dirtyFields },
  } = useForm<SignupInput>({
    resolver: zodResolver(signupSchema),
    defaultValues: { assessoriaNome: '', slug: '', email: '', senha: '' },
  });

  const assessoriaNome = watch('assessoriaNome');
  const slugTouched = !!dirtyFields.slug;

  // auto-fill slug enquanto não foi tocado manualmente
  React.useEffect(() => {
    if (!slugTouched) {
      setValue('slug', autoSlug(assessoriaNome ?? ''), { shouldValidate: false });
    }
  }, [assessoriaNome, slugTouched, setValue]);

  async function onSubmit(values: SignupInput) {
    try {
      await signup.mutateAsync(values);
      setEmail(values.email);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao criar conta.');
    }
  }

  if (email) {
    return (
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
        className="flex flex-col items-center gap-4 text-center"
      >
        <Mail className="h-12 w-12 text-primary" />
        <h1 className="font-display text-2xl font-bold">Quase lá!</h1>
        <p className="text-sm text-muted-foreground max-w-xs">
          Enviamos um link de verificação para <strong>{email}</strong>. Abra o e-mail e clique no link para ativar sua conta.
        </p>
        <Link
          href={'/login' as Route}
          className="text-sm font-medium text-foreground underline-offset-4 hover:underline"
        >
          Já verifiquei, ir para o login
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
          Criar workspace
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Comece a organizar sua assessoria em menos de 1 minuto.
        </p>
      </div>

      <form onSubmit={handleSubmit(onSubmit)} className="space-y-4" noValidate>
        <div className="space-y-1.5">
          <Label htmlFor="assessoriaNome">Nome da assessoria</Label>
          <Input
            id="assessoriaNome"
            placeholder="Ex: Constellation Talent"
            aria-invalid={!!errors.assessoriaNome}
            {...register('assessoriaNome')}
          />
          {errors.assessoriaNome && (
            <p className="text-xs text-destructive" role="alert">
              {errors.assessoriaNome.message}
            </p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="slug">Slug do workspace</Label>
          <div className="flex items-center rounded-md border border-input bg-background shadow-xs focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background focus-within:border-ring">
            <span className="pl-3 text-xs text-muted-foreground font-mono select-none">
              hub.app/
            </span>
            <input
              id="slug"
              className="flex-1 h-10 px-2 bg-transparent text-sm font-mono outline-none placeholder:text-muted-foreground"
              placeholder="constellation"
              aria-invalid={!!errors.slug}
              {...register('slug')}
            />
          </div>
          {errors.slug ? (
            <p className="text-xs text-destructive" role="alert">
              {errors.slug.message}
            </p>
          ) : (
            <p className="text-xs text-muted-foreground">
              Apenas letras minúsculas, números e hífens.
            </p>
          )}
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="email">Seu e-mail</Label>
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
            autoComplete="new-password"
            placeholder="Mínimo de 8 caracteres"
            aria-invalid={!!errors.senha}
            {...register('senha')}
          />
          {errors.senha && (
            <p className="text-xs text-destructive" role="alert">
              {errors.senha.message}
            </p>
          )}
        </div>
        <Button type="submit" disabled={isSubmitting} size="lg" className="w-full mt-2">
          {isSubmitting ? (
            <Loader2 className="h-4 w-4 animate-spin" />
          ) : (
            <>
              Criar workspace <ArrowRight className="h-4 w-4" />
            </>
          )}
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Já tem uma conta?{' '}
        <Link
          href={'/login' as Route}
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Entrar
        </Link>
      </p>
    </motion.div>
  );
}
