'use client';

import * as React from 'react';

interface Flag {
  key: string;
  enabled: boolean;
}

export default function FeatureFlagsPage() {
  const [flags, setFlags] = React.useState<Flag[]>([]);
  const [loading, setLoading] = React.useState(true);

  const token = () => (typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null);

  React.useEffect(() => {
    fetch('/api/v1/admin/feature-flags', {
      headers: { Authorization: `Bearer ${token()}` },
    })
      .then(r => r.json())
      .then(setFlags)
      .finally(() => setLoading(false));
  }, []);

  async function toggle(key: string, current: boolean) {
    const res = await fetch(`/api/v1/admin/feature-flags/${encodeURIComponent(key)}`, {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${token()}`,
      },
      body: JSON.stringify({ enabled: !current }),
    });
    if (res.ok) {
      const updated: Flag = await res.json();
      setFlags(prev => prev.map(f => (f.key === key ? updated : f)));
    }
  }

  if (loading) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  return (
    <div>
      <h1 className="text-xl font-semibold mb-6">Feature Flags</h1>
      <div className="border rounded-lg divide-y">
        {flags.map(flag => (
          <div key={flag.key} className="flex items-center justify-between px-4 py-3">
            <span className="font-mono text-sm">{flag.key}</span>
            <button
              onClick={() => toggle(flag.key, flag.enabled)}
              className={`px-3 py-1 rounded text-xs font-medium transition-colors ${
                flag.enabled
                  ? 'bg-green-100 text-green-800 hover:bg-green-200'
                  : 'bg-muted text-muted-foreground hover:bg-muted/80'
              }`}
            >
              {flag.enabled ? 'Ativo' : 'Inativo'}
            </button>
          </div>
        ))}
        {flags.length === 0 && (
          <p className="px-4 py-3 text-sm text-muted-foreground">Nenhuma flag cadastrada.</p>
        )}
      </div>
    </div>
  );
}
