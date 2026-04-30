'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { motion } from 'framer-motion';
import { ArrowRight, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import { auth, setTokens } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';

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
  const router = useRouter();
  const [form, setForm] = useState({ assessoriaNome: '', slug: '', email: '', senha: '' });
  const [loading, setLoading] = useState(false);
  const [slugTouched, setSlugTouched] = useState(false);

  function set<K extends keyof typeof form>(field: K) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const tokens = await auth.signup(form);
      setTokens(tokens.accessToken, tokens.refreshToken);
      toast.success('Workspace criado!');
      router.push('/');
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao criar conta.');
    } finally {
      setLoading(false);
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
          Criar workspace
        </h1>
        <p className="mt-2 text-sm text-muted-foreground">
          Comece a organizar sua assessoria em menos de 1 minuto.
        </p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-4">
        <div className="space-y-1.5">
          <Label htmlFor="assessoriaNome">Nome da assessoria</Label>
          <Input
            id="assessoriaNome"
            required
            value={form.assessoriaNome}
            onChange={e => {
              const nome = e.target.value;
              setForm(prev => ({
                ...prev,
                assessoriaNome: nome,
                slug: slugTouched ? prev.slug : autoSlug(nome),
              }));
            }}
            placeholder="Ex: Constellation Talent"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="slug">Slug do workspace</Label>
          <div className="flex items-center rounded-md border border-input bg-background shadow-xs focus-within:ring-2 focus-within:ring-ring focus-within:ring-offset-2 focus-within:ring-offset-background focus-within:border-ring">
            <span className="pl-3 text-xs text-muted-foreground font-mono select-none">hub.app/</span>
            <input
              id="slug"
              required
              minLength={3}
              pattern="[a-z0-9-]+"
              value={form.slug}
              onChange={e => {
                setSlugTouched(true);
                setForm(prev => ({ ...prev, slug: e.target.value }));
              }}
              className="flex-1 h-10 px-2 bg-transparent text-sm font-mono outline-none placeholder:text-muted-foreground"
              placeholder="constellation"
            />
          </div>
          <p className="text-xs text-muted-foreground">
            Apenas letras minúsculas, números e hífens.
          </p>
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="email">Seu e-mail</Label>
          <Input
            id="email"
            type="email"
            required
            autoComplete="email"
            value={form.email}
            onChange={set('email')}
            placeholder="voce@assessoria.com"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="senha">Senha</Label>
          <Input
            id="senha"
            type="password"
            required
            minLength={8}
            autoComplete="new-password"
            value={form.senha}
            onChange={set('senha')}
            placeholder="Mínimo de 8 caracteres"
          />
        </div>
        <Button type="submit" disabled={loading} size="lg" className="w-full mt-2">
          {loading ? <Loader2 className="h-4 w-4 animate-spin" /> : <>Criar workspace <ArrowRight className="h-4 w-4" /></>}
        </Button>
      </form>

      <p className="mt-8 text-center text-sm text-muted-foreground">
        Já tem uma conta?{' '}
        <Link
          href="/login"
          className="font-medium text-foreground underline-offset-4 hover:underline"
        >
          Entrar
        </Link>
      </p>
    </motion.div>
  );
}
