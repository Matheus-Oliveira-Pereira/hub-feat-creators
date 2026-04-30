'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { influenciadores, Influenciador } from '@/lib/api';

export default function InfluenciadoresPage() {
  const router = useRouter();
  const [data, setData] = useState<Influenciador[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ nome: '', nicho: '', instagram: '' });
  const [saving, setSaving] = useState(false);

  async function load(nome?: string) {
    setLoading(true);
    try {
      const res = await influenciadores.list({ nome: nome || undefined });
      setData(res.data);
    } catch {
      router.push('/login');
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { load(); }, []);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await influenciadores.create({
        nome: form.nome,
        nicho: form.nicho || null,
        handles: form.instagram ? { instagram: form.instagram } : {},
        tags: [],
      });
      setShowForm(false);
      setForm({ nome: '', nicho: '', instagram: '' });
      load();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Remover influenciador?')) return;
    await influenciadores.delete(id);
    load(search || undefined);
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Influenciadores</h1>
        <div className="flex gap-2">
          <a
            href="http://localhost:8080/api/v1/influenciadores/export.csv"
            className="px-3 py-2 text-sm border border-slate-300 rounded-lg text-slate-600 hover:bg-slate-100 transition-colors"
          >
            Exportar CSV
          </a>
          <button
            onClick={() => setShowForm(!showForm)}
            className="px-4 py-2 bg-blue-600 hover:bg-blue-700 text-white text-sm font-medium rounded-lg transition-colors"
          >
            + Adicionar
          </button>
        </div>
      </div>

      {showForm && (
        <form onSubmit={handleCreate} className="mb-6 p-4 bg-white border border-slate-200 rounded-xl space-y-3">
          <h2 className="font-medium text-slate-800">Novo influenciador</h2>
          <input
            type="text" placeholder="Nome *" required value={form.nome}
            onChange={e => setForm(p => ({ ...p, nome: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <input
            type="text" placeholder="Nicho (ex: fitness, moda)" value={form.nicho}
            onChange={e => setForm(p => ({ ...p, nicho: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <input
            type="text" placeholder="@instagram" value={form.instagram}
            onChange={e => setForm(p => ({ ...p, instagram: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500"
          />
          <div className="flex gap-2">
            <button type="submit" disabled={saving}
              className="px-4 py-2 bg-blue-600 text-white text-sm rounded-lg disabled:opacity-50">
              {saving ? 'Salvando…' : 'Salvar'}
            </button>
            <button type="button" onClick={() => setShowForm(false)}
              className="px-4 py-2 text-sm text-slate-600 border border-slate-300 rounded-lg">
              Cancelar
            </button>
          </div>
        </form>
      )}

      <div className="mb-4">
        <input
          type="text" placeholder="Buscar por nome…" value={search}
          onChange={e => { setSearch(e.target.value); load(e.target.value || undefined); }}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-400">Carregando…</div>
        ) : data.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-400">Nenhum influenciador encontrado.</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Nome</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Nicho</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Instagram</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Tags</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.map(inf => (
                <tr key={inf.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{inf.nome}</td>
                  <td className="px-4 py-3 text-slate-500">{inf.nicho ?? '—'}</td>
                  <td className="px-4 py-3 text-slate-500">{inf.handles?.instagram ?? '—'}</td>
                  <td className="px-4 py-3">
                    <div className="flex gap-1 flex-wrap">
                      {inf.tags.map(t => (
                        <span key={t} className="px-2 py-0.5 bg-blue-50 text-blue-700 text-xs rounded-full">{t}</span>
                      ))}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button
                      onClick={() => handleDelete(inf.id)}
                      className="text-red-500 hover:text-red-700 text-xs"
                    >
                      Remover
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </div>
  );
}
