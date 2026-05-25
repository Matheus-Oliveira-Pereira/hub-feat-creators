'use client';

import * as React from 'react';
import Link from 'next/link';

interface Assessoria {
  id: string;
  nome: string;
  slug: string;
  plano: string;
  createdAt: string;
}

interface PagedResponse {
  items: Assessoria[];
  total: number;
  page: number;
  size: number;
}

export default function AdminAssessoriasPage() {
  const [data, setData] = React.useState<PagedResponse | null>(null);
  const [page, setPage] = React.useState(0);

  const token = () => (typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null);

  React.useEffect(() => {
    fetch(`/api/v1/admin/assessorias?page=${page}&size=20`, {
      headers: { Authorization: `Bearer ${token()}` },
    })
      .then(r => r.json())
      .then(setData);
  }, [page]);

  if (!data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  return (
    <div>
      <h1 className="text-xl font-semibold mb-6">Assessorias ({data.total})</h1>
      <div className="border rounded-lg divide-y">
        {data.items.map(a => (
          <div key={a.id} className="flex items-center justify-between px-4 py-3">
            <div>
              <p className="font-medium text-sm">{a.nome}</p>
              <p className="text-xs text-muted-foreground">{a.slug} · {a.plano}</p>
            </div>
            <div className="flex items-center gap-3">
              <span className="text-xs text-muted-foreground">
                {new Date(a.createdAt).toLocaleDateString('pt-BR')}
              </span>
              <Link
                href={`/admin/assessorias/${a.id}` as any}
                className="text-xs text-primary underline"
              >
                Ver
              </Link>
            </div>
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
          Página {page + 1} de {Math.ceil(data.total / data.size) || 1}
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
