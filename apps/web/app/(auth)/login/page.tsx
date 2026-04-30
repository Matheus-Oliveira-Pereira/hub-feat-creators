'use client';

import { useState, FormEvent } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { auth, setTokens } from '@/lib/api';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const tokens = await auth.login({ email, senha });
      setTokens(tokens.accessToken, tokens.refreshToken);
      router.push('/influenciadores');
    } catch (err: any) {
      setError(err?.error?.message ?? 'Credenciais inválidas.');
    } finally {
      setLoading(false);
    }
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4">
      <h2 className="text-lg font-semibold text-slate-800">Entrar</h2>
      {error && (
        <p className="text-sm text-red-600 bg-red-50 border border-red-200 rounded-lg px-3 py-2">
          {error}
        </p>
      )}
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">E-mail</label>
        <input
          type="email" required value={email} onChange={e => setEmail(e.target.value)}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div className="space-y-1">
        <label className="text-sm font-medium text-slate-700">Senha</label>
        <input
          type="password" required value={senha} onChange={e => setSenha(e.target.value)}
          className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <button
        type="submit" disabled={loading}
        className="w-full py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors disabled:opacity-50"
      >
        {loading ? 'Entrando…' : 'Entrar'}
      </button>
      <p className="text-center text-sm text-slate-500">
        Não tem conta?{' '}
        <Link href="/signup" className="text-blue-600 hover:underline">Criar workspace</Link>
      </p>
    </form>
  );
}
