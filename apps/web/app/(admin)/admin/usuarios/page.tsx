'use client';

import * as React from 'react';

interface UsuarioItem {
  id: string;
  email: string;
  tipo: string;
  role: string;
  status: string;
  assessoriaId: string | null;
  createdAt: string;
}

interface PagedResponse {
  items: UsuarioItem[];
  total: number;
  page: number;
  size: number;
}

export default function AdminUsuariosPage() {
  const [data, setData] = React.useState<PagedResponse | null>(null);
  const [page, setPage] = React.useState(0);

  const token = () => (typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null);

  React.useEffect(() => {
    fetch(`/api/v1/admin/usuarios?page=${page}&size=50`, {
      headers: { Authorization: `Bearer ${token()}` },
    })
      .then(r => r.json())
      .then(setData);
  }, [page]);

  if (!data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  return (
    <div>
      <h1 className="text-xl font-semibold mb-6">Usuários ({data.total})</h1>
      <div className="border rounded-lg divide-y">
        {data.items.map(u => (
          <div key={u.id} className="flex items-center justify-between px-4 py-3">
            <div>
              <p className="font-medium text-sm">{u.email}</p>
              <p className="text-xs text-muted-foreground">
                {u.tipo} · {u.role} · {u.assessoriaId?.slice(0, 8) ?? 'sem assessoria'}
              </p>
            </div>
            <span
              className={`text-xs px-2 py-0.5 rounded font-medium ${
                u.status === 'ATIVO'
                  ? 'bg-green-100 text-green-800'
                  : 'bg-red-100 text-red-800'
              }`}
            >
              {u.status}
            </span>
          </div>
        ))}
      </div>
      <div className="flex gap-2 mt-4">
        <button
          disabled={page === 0}
          onClick={() => setPage(p => p - 1)}
          className="px-3 py-1 text-xs border rounded disabled:opacity-50"
        >
          Anterior
        </button>
        <span className="text-xs text-muted-foreground self-center">
          Página {page + 1}
        </span>
        <button
          disabled={(page + 1) * data.size >= data.total}
          onClick={() => setPage(p => p + 1)}
          className="px-3 py-1 text-xs border rounded disabled:opacity-50"
        >
          Próxima
        </button>
      </div>
    </div>
  );
}
