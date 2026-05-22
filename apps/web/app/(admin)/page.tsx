'use client';

import * as React from 'react';
import { useAuth } from '@/lib/auth';

interface DashboardData {
  totalAssessorias: number;
  totalUsuariosInternos: number;
  totalAdms: number;
  auditActionsLast24h: number;
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="rounded-lg border bg-card p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="text-2xl font-bold mt-1">{value}</p>
    </div>
  );
}

export default function AdminDashboardPage() {
  const { claims } = useAuth();
  const [data, setData] = React.useState<DashboardData | null>(null);
  const [error, setError] = React.useState<string | null>(null);

  React.useEffect(() => {
    if (!claims) return;
    const token = localStorage.getItem('accessToken');
    fetch('/api/v1/admin/dashboard', {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(r => r.json())
      .then(setData)
      .catch(e => setError(String(e)));
  }, [claims]);

  if (error) return <p className="text-destructive text-sm">{error}</p>;
  if (!data) return <p className="text-sm text-muted-foreground">Carregando…</p>;

  return (
    <div>
      <h1 className="text-xl font-semibold mb-6">Dashboard</h1>
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <StatCard label="Assessorias" value={data.totalAssessorias} />
        <StatCard label="Usuários Internos" value={data.totalUsuariosInternos} />
        <StatCard label="ADMs" value={data.totalAdms} />
        <StatCard label="Ações Audit (24h)" value={data.auditActionsLast24h} />
      </div>
    </div>
  );
}
