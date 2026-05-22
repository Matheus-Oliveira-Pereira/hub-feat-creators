'use client';

import * as React from 'react';

interface AuditEntry {
  id: string;
  adminId: string;
  acao: string;
  assessoriaIdAfetada: string | null;
  recurso: string | null;
  ip: string | null;
  createdAt: string;
}

interface PagedResponse {
  items: AuditEntry[];
  total: number;
  page: number;
  size: number;
}

export default function AdminAuditPage() {
  const [data, setData] = React.useState<PagedResponse | null>(null);
  const [page, setPage] = React.useState(0);

  const token = () => (typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null);

  React.useEffect(() => {
    fetch(`/api/v1/admin/audit?page=${page}&size=50`, {
      headers: { Authorization: `Bearer ${token()}` },
    })
      .then(r => r.json())
      .then(setData);
  }, [page]);

  if (!data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  return (
    <div>
      <h1 className="text-xl font-semibold mb-6">Audit Log ({data.total})</h1>
      <div className="border rounded-lg divide-y">
        {data.items.map(entry => (
          <div key={entry.id} className="px-4 py-3 flex items-start justify-between gap-4">
            <div className="flex-1 min-w-0">
              <p className="text-sm font-mono font-medium">{entry.acao}</p>
              <p className="text-xs text-muted-foreground truncate">
                ADM: {entry.adminId.slice(0, 8)}
                {entry.assessoriaIdAfetada ? ` · Assessoria: ${entry.assessoriaIdAfetada.slice(0, 8)}` : ''}
                {entry.recurso ? ` · ${entry.recurso}` : ''}
              </p>
            </div>
            <div className="text-right shrink-0">
              <p className="text-xs text-muted-foreground">
                {new Date(entry.createdAt).toLocaleString('pt-BR')}
              </p>
              {entry.ip && <p className="text-xs text-muted-foreground">{entry.ip}</p>}
            </div>
          </div>
        ))}
        {data.items.length === 0 && (
          <p className="px-4 py-3 text-sm text-muted-foreground">Nenhuma entrada.</p>
        )}
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
