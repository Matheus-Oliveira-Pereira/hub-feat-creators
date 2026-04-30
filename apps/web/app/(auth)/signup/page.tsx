'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { auth, setTokens } from '@/lib/api';

export default function SignupPage() {
  const router = useRouter();
  const [form, setForm] = useState({ assessoriaNome: '', slug: '', email: '', senha: '' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  function set(field: string) {
    return (e: React.ChangeEvent<HTMLInputElement>) =>
      setForm(prev => ({ ...prev, [field]: e.target.value }));
  }

  function autoSlug(nome: string) {
    return nome.toLowerCase().replace(/\s+/g, '-').replace(/[^a-z0-9-]/g, '').slice(0, 50);
  }

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const tokens = await auth.signup(form);
      setTokens(tokens.accessToken, tokens.refreshToken);
      router.push('/influenciadores');
    } catch (err: any) {
      setError(err?.error?.message ?? 'Erro ao criar conta.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h2 className="text-lg font-semibold text-slate-800">Criar workspace</h2>
      {error && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
          {error}
        </p>
      )}
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">Nome da assessoria</label>
        <input
          type="text" required value={form.assessoriaNome}
          onChange={e => {
            const nome = e.target.value;
            setForm(prev => ({ ...prev, assessoriaNome: nome, slug: autoSlug(nome) }));
          }}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">Slug</label>
        <input
          type="text" required value={form.slug} onChange={set('slug')}
          pattern="[a-z0-9-]+" minLength={3}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm font-mono focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <p className="text-xs text-slate-400">Somente letras minúsculas, números e hífens</p>
      </div>
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">Seu e-mail</label>
        <input
          type="email" required value={form.email} onChange={set('email')}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">Senha</label>
        <input
          type="password" required value={form.senha} onChange={set('senha')} minLength={8}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <button
        type="submit" disabled={loading}
        className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
      >
        {loading ? 'Criando…' : 'Criar workspace'}
      </button>
      <p className="text-center text-sm text-slate-500">
        Já tem conta?{' '}
        <Link href="/login" className="text-blue-600 hover:underline">Entrar</Link>
      </p>
    </form>
  );
}
