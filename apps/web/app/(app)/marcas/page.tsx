'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { marcas, Marca } from '@/lib/api';

export default function MarcasPage() {
  const router = useRouter();
  const [data, setData] = useState<Marca[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({ nome: '', segmento: '', site: '' });
  const [saving, setSaving] = useState(false);

  async function load(nome?: string) {
    setLoading(true);
    try {
      const res = await marcas.list({ nome: nome || undefined });
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
      await marcas.create({ nome: form.nome, segmento: form.segmento || null, site: form.site || null, tags: [] });
      setShowForm(false);
      setForm({ nome: '', segmento: '', site: '' });
      load();
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Remover marca?')) return;
    await marcas.delete(id);
    load(search || undefined);
  }

  return (
    <div className="p-6">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-xl font-semibold text-slate-900">Marcas</h1>
        <div className="flex gap-2">
          <a
            href="http://localhost:8080/api/v1/marcas/export.csv"
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
          <h2 className="font-medium text-slate-800">Nova marca</h2>
          <input type="text" placeholder="Nome *" required value={form.nome}
            onChange={e => setForm(p => ({ ...p, nome: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          <input type="text" placeholder="Segmento" value={form.segmento}
            onChange={e => setForm(p => ({ ...p, segmento: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
          <input type="url" placeholder="Site" value={form.site}
            onChange={e => setForm(p => ({ ...p, site: e.target.value }))}
            className="w-full px-3 py-2 border border-slate-300 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500" />
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
        <input type="text" placeholder="Buscar por nome…" value={search}
          onChange={e => { setSearch(e.target.value); load(e.target.value || undefined); }}
          className="px-3 py-2 border border-slate-300 rounded-lg text-sm w-64 focus:outline-none focus:ring-2 focus:ring-blue-500" />
      </div>

      <div className="bg-white border border-slate-200 rounded-xl overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-sm text-slate-400">Carregando…</div>
        ) : data.length === 0 ? (
          <div className="p-8 text-center text-sm text-slate-400">Nenhuma marca encontrada.</div>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-slate-50 border-b border-slate-200">
              <tr>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Nome</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Segmento</th>
                <th className="text-left px-4 py-3 font-medium text-slate-600">Site</th>
                <th className="px-4 py-3"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100">
              {data.map(marca => (
                <tr key={marca.id} className="hover:bg-slate-50">
                  <td className="px-4 py-3 font-medium text-slate-900">{marca.nome}</td>
                  <td className="px-4 py-3 text-slate-500">{marca.segmento ?? '—'}</td>
                  <td className="px-4 py-3">
                    {marca.site ? (
                      <a href={marca.site} target="_blank" rel="noopener noreferrer"
                        className="text-blue-600 hover:underline truncate max-w-xs block">{marca.site}</a>
                    ) : '—'}
                  </td>
                  <td className="px-4 py-3 text-right">
                    <button onClick={() => handleDelete(marca.id)}
                      className="text-red-500 hover:text-red-700 text-xs">
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
