'use client';

import * as React from 'react';
import Link from 'next/link';
import { type Route } from 'next';
import { useRouter } from 'next/navigation';
import {
  Users,
  Building2,
  Search as SearchIcon,
  Mail,
  ArrowUpRight,
  Activity,
  Plus,
  Target,
  Trophy,
  Timer,
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  ResponsiveContainer,
  Tooltip,
  CartesianGrid,
  Cell,
} from 'recharts';
import { motion } from 'framer-motion';
import { influenciadores, marcas } from '@/lib/api';
import { useProspeccaoDashboard } from '@/lib/queries';
import {
  Card,
  CardHeader,
  CardTitle,
  CardDescription,
  CardContent,
} from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { StatCard } from '@/components/app/stat-card';
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';
import { Can } from '@/components/auth/can';
import { STATUS_LABEL, STATUS_TONE, STATUS_ORDER } from '@/lib/prospeccao';

export default function DashboardPage() {
  const router = useRouter();
  const [stats, setStats] = React.useState<{
    influenciadores: number | null;
    marcas: number | null;
  }>({ influenciadores: null, marcas: null });
  const [loadingCadastros, setLoadingCadastros] = React.useState(true);
  const dashQ = useProspeccaoDashboard();

  React.useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const [inf, mar] = await Promise.all([
          influenciadores.list({ size: 100 }),
          marcas.list({ size: 100 }),
        ]);
        if (!alive) return;
        setStats({ influenciadores: inf.data.length, marcas: mar.data.length });
      } catch {
        if (!alive) return;
        router.push('/login');
      } finally {
        if (alive) setLoadingCadastros(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [router]);

  const ds = dashQ.data;
  const ativas = ds
    ? (ds.porStatus.NOVA ?? 0) +
      (ds.porStatus.CONTATADA ?? 0) +
      (ds.porStatus.NEGOCIANDO ?? 0)
    : null;

  const funilData = STATUS_ORDER.map(s => ({
    etapa: STATUS_LABEL[s],
    status: s,
    total: ds?.porStatus[s] ?? 0,
  }));

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-12">
      <motion.div
        initial={{ opacity: 0, y: 8 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between"
      >
        <div className="space-y-2">
          <Badge variant="muted" className="font-mono">Visão geral</Badge>
          <h1 className="font-display text-3xl md:text-4xl font-bold tracking-tight">
            Bom dia 👋
          </h1>
          <p className="text-sm text-muted-foreground max-w-md">
            Resumo da operação. Atalho de busca e ações: pressione{' '}
            <kbd className="rounded border border-border bg-muted px-1.5 py-0.5 font-mono text-[10px]">
              ⌘K
            </kbd>
            .
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <Can role="CPRO">
            <Button asChild>
              <Link href={'/prospeccao?new=1' as Route}>
                <Plus className="h-4 w-4" /> Prospecção
              </Link>
            </Button>
          </Can>
          <Can role="CINF">
            <Button asChild variant="outline">
              <Link href={'/influenciadores?new=1' as Route}>
                <Plus className="h-4 w-4" /> Influenciador
              </Link>
            </Button>
          </Can>
          <Can role="CMAR">
            <Button asChild variant="outline">
              <Link href={'/marcas?new=1' as Route}>
                <Plus className="h-4 w-4" /> Marca
              </Link>
            </Button>
          </Can>
        </div>
      </motion.div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Prospecções ativas"
          value={ativas}
          icon={Target}
          loading={dashQ.isLoading}
          accent
        />
        <StatCard
          label="Fechadas no mês"
          value={ds?.fechadasMes ?? null}
          icon={Trophy}
          loading={dashQ.isLoading}
        />
        <StatCard
          label="Taxa de conversão"
          value={
            ds
              ? `${(ds.taxaConversao * 100).toLocaleString('pt-BR', {
                  maximumFractionDigits: 1,
                })}%`
              : null
          }
          icon={SearchIcon}
          hint="Ganhas / (Ganhas + Perdidas)"
          loading={dashQ.isLoading}
        />
        <StatCard
          label="Time-to-close (dias)"
          value={
            ds
              ? Number(ds.timeToCloseDiasMedio.toFixed(1)).toLocaleString('pt-BR', {
                  maximumFractionDigits: 1,
                })
              : null
          }
          icon={Timer}
          hint="Média de NOVA → FECHADA_GANHA"
          loading={dashQ.isLoading}
        />
      </div>

      <div className="mt-8 grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2 p-6">
          <CardHeader className="p-0">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Funil de prospecção</CardTitle>
                <CardDescription>Volume por etapa — dados reais da assessoria</CardDescription>
              </div>
              <Badge variant="muted">tempo real</Badge>
            </div>
          </CardHeader>
          <CardContent className="p-0 mt-6">
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={funilData} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                  <CartesianGrid
                    strokeDasharray="3 3"
                    stroke="hsl(var(--border))"
                    vertical={false}
                  />
                  <XAxis
                    dataKey="etapa"
                    stroke="hsl(var(--muted-foreground))"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                  />
                  <YAxis
                    stroke="hsl(var(--muted-foreground))"
                    fontSize={12}
                    tickLine={false}
                    axisLine={false}
                    allowDecimals={false}
                  />
                  <Tooltip
                    cursor={{ fill: 'hsl(var(--accent))' }}
                    contentStyle={{
                      background: 'hsl(var(--popover))',
                      border: '1px solid hsl(var(--border))',
                      borderRadius: 'var(--radius)',
                      fontSize: '12px',
                      color: 'hsl(var(--foreground))',
                    }}
                  />
                  <Bar dataKey="total" radius={[6, 6, 0, 0]}>
                    {funilData.map(d => {
                      const tone = STATUS_TONE[d.status as keyof typeof STATUS_TONE];
                      const fill =
                        d.status === 'FECHADA_GANHA'
                          ? 'hsl(var(--primary))'
                          : d.status === 'FECHADA_PERDIDA'
                            ? 'hsl(var(--destructive))'
                            : 'hsl(var(--border-strong))';
                      return <Cell key={d.status} fill={fill} />;
                    })}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="p-6">
          <CardHeader className="p-0">
            <CardTitle>Cadastros</CardTitle>
            <CardDescription>Base ativa da assessoria</CardDescription>
          </CardHeader>
          <CardContent className="p-0 mt-4 space-y-3">
            <MiniStat
              label="Influenciadores"
              value={stats.influenciadores}
              icon={Users}
              href={'/influenciadores' as Route}
              loading={loadingCadastros}
            />
            <MiniStat
              label="Marcas"
              value={stats.marcas}
              icon={Building2}
              href={'/marcas' as Route}
              loading={loadingCadastros}
            />
            <div className="rounded-md border border-dashed border-border p-3 text-xs text-muted-foreground">
              <Mail className="inline h-3.5 w-3.5 mr-1" />
              E-mail outbound em PRD-004.
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}

function MiniStat({
  label,
  value,
  icon: Icon,
  href,
  loading,
}: {
  label: string;
  value: number | null;
  icon: React.ElementType;
  href: Route;
  loading: boolean;
}) {
  return (
    <Link
      href={href}
      className="flex items-center justify-between rounded-md border border-border bg-muted/30 px-3 py-2.5 text-sm transition-colors hover:bg-muted"
    >
      <span className="inline-flex items-center gap-2">
        <Icon className="h-4 w-4 text-muted-foreground" />
        {label}
      </span>
      <span className="font-display font-semibold tabular-nums">
        {loading ? '…' : (value ?? '—')}
      </span>
    </Link>
  );
}
