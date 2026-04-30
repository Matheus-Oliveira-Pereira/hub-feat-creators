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
} from 'lucide-react';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  ResponsiveContainer,
  Tooltip,
  CartesianGrid,
} from 'recharts';
import { motion } from 'framer-motion';
import { influenciadores, marcas } from '@/lib/api';
import { Card, CardHeader, CardTitle, CardDescription, CardContent } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { StatCard } from '@/components/app/stat-card';
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';

const FUNIL_PLACEHOLDER = [
  { etapa: 'Identificadas', total: 124 },
  { etapa: 'Contato', total: 78 },
  { etapa: 'Negociação', total: 32 },
  { etapa: 'Fechadas', total: 11 },
];

export default function DashboardPage() {
  const router = useRouter();
  const [stats, setStats] = React.useState<{
    influenciadores: number | null;
    marcas: number | null;
  }>({ influenciadores: null, marcas: null });
  const [loading, setLoading] = React.useState(true);

  React.useEffect(() => {
    let alive = true;
    (async () => {
      try {
        const [inf, mar] = await Promise.all([
          influenciadores.list({ size: 100 }),
          marcas.list({ page: 0 }),
        ]);
        if (!alive) return;
        setStats({ influenciadores: inf.data.length, marcas: mar.data.length });
      } catch {
        if (!alive) return;
        router.push('/login');
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [router]);

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
          <Button asChild variant="outline">
            <Link href={'/influenciadores?new=1' as Route}>
              <Plus className="h-4 w-4" /> Influenciador
            </Link>
          </Button>
          <Button asChild>
            <Link href={'/marcas?new=1' as Route}>
              <Plus className="h-4 w-4" /> Marca
            </Link>
          </Button>
        </div>
      </motion.div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <StatCard
          label="Influenciadores"
          value={stats.influenciadores}
          icon={Users}
          loading={loading}
          accent
        />
        <StatCard
          label="Marcas"
          value={stats.marcas}
          icon={Building2}
          loading={loading}
        />
        <StatCard
          label="Prospecções ativas"
          value="—"
          icon={SearchIcon}
          hint="Disponível em breve"
          loading={false}
        />
        <StatCard
          label="E-mails (7d)"
          value="—"
          icon={Mail}
          hint="Disponível em breve"
          loading={false}
        />
      </div>

      <div className="mt-8 grid gap-4 lg:grid-cols-3">
        <Card className="lg:col-span-2 p-6">
          <CardHeader className="p-0">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle>Funil de prospecção</CardTitle>
                <CardDescription>Volume por etapa — placeholder até PRD-002</CardDescription>
              </div>
              <Badge variant="warning">Mock</Badge>
            </div>
          </CardHeader>
          <CardContent className="p-0 mt-6">
            <div className="h-72 w-full">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={FUNIL_PLACEHOLDER} margin={{ top: 8, right: 8, left: -20, bottom: 0 }}>
                  <defs>
                    <linearGradient id="barFill" x1="0" y1="0" x2="0" y2="1">
                      <stop offset="0%" stopColor="hsl(var(--primary))" stopOpacity={1} />
                      <stop offset="100%" stopColor="hsl(var(--primary))" stopOpacity={0.6} />
                    </linearGradient>
                  </defs>
                  <CartesianGrid strokeDasharray="3 3" stroke="hsl(var(--border))" vertical={false} />
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
                  <Bar dataKey="total" fill="url(#barFill)" radius={[6, 6, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </CardContent>
        </Card>

        <Card className="p-6">
          <CardHeader className="p-0">
            <CardTitle>Atividade recente</CardTitle>
            <CardDescription>Últimas movimentações no workspace</CardDescription>
          </CardHeader>
          <CardContent className="p-0 mt-4">
            <EmptyState
              illustration={<EmptyIllustration variant="inbox" />}
              title="Sem atividade ainda"
              description="Quando você começar a usar o sistema, as ações aparecem aqui."
              action={
                <Button asChild variant="subtle" size="sm">
                  <Link href={'/influenciadores' as Route}>
                    <Activity className="h-4 w-4" /> Ver cadastros
                  </Link>
                </Button>
              }
            />
          </CardContent>
        </Card>
      </div>

      <div className="mt-4 grid gap-4 md:grid-cols-2">
        <Card className="p-6 flex items-center justify-between">
          <div>
            <CardTitle className="text-base">Influenciadores</CardTitle>
            <CardDescription>Gerencie creators do seu workspace</CardDescription>
          </div>
          <Button asChild variant="ghost" size="sm">
            <Link href={'/influenciadores' as Route}>
              Abrir <ArrowUpRight className="h-4 w-4" />
            </Link>
          </Button>
        </Card>
        <Card className="p-6 flex items-center justify-between">
          <div>
            <CardTitle className="text-base">Marcas</CardTitle>
            <CardDescription>Cadastro de empresas e parceiros</CardDescription>
          </div>
          <Button asChild variant="ghost" size="sm">
            <Link href={'/marcas' as Route}>
              Abrir <ArrowUpRight className="h-4 w-4" />
            </Link>
          </Button>
        </Card>
      </div>
    </div>
  );
}
