'use client';

import * as React from 'react';
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
import {
  Download,
  Save,
  Trash2,
  TrendingUp,
  TrendingDown,
  Minus,
  Loader2,
} from 'lucide-react';
import {
  relatorios,
  FunilResult,
  PerformanceResult,
  TarefaSlaResult,
  RelatorioSalvo,
  RelatorioTipo,
} from '@/lib/api';
import { PageHeader } from '@/components/app/page-header';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger, TabsContent } from '@/components/ui/tabs';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from '@/components/ui/dialog';
import { Can } from '@/components/auth/can';
import { cn } from '@/lib/utils';

// ─── helpers ─────────────────────────────────────────────────────────────────

const BRL_CENT = (c: number) =>
  new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(c / 100);

function DeltaBadge({ delta }: { delta: number }) {
  if (Math.abs(delta) < 0.5) return <span className="text-muted-foreground text-xs flex items-center gap-0.5"><Minus className="w-3 h-3" />0%</span>;
  const up = delta > 0;
  return (
    <span className={cn('text-xs flex items-center gap-0.5 font-medium', up ? 'text-green-600' : 'text-red-500')}>
      {up ? <TrendingUp className="w-3 h-3" /> : <TrendingDown className="w-3 h-3" />}
      {up ? '+' : ''}{delta.toFixed(1)}%
    </span>
  );
}

// ─── Period picker ────────────────────────────────────────────────────────────

function defaultPeriod() {
  const now = new Date();
  const from = new Date(now.getFullYear(), now.getMonth() - 1, 1);
  const to = new Date(now.getFullYear(), now.getMonth(), 0, 23, 59, 59);
  return {
    from: from.toISOString().slice(0, 10),
    to: to.toISOString().slice(0, 10),
  };
}

function toInstant(date: string, end = false) {
  return end ? `${date}T23:59:59Z` : `${date}T00:00:00Z`;
}

// ─── Save modal ───────────────────────────────────────────────────────────────

function SaveModal({
  open,
  tipo,
  filtros,
  onClose,
  onSaved,
}: {
  open: boolean;
  tipo: RelatorioTipo;
  filtros: Record<string, unknown>;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [nome, setNome] = React.useState('');
  const [saving, setSaving] = React.useState(false);

  async function handleSave() {
    if (!nome.trim()) return;
    setSaving(true);
    try {
      await relatorios.salvar(nome.trim(), tipo, filtros);
      onSaved();
      onClose();
      setNome('');
    } finally {
      setSaving(false);
    }
  }

  return (
    <Dialog open={open} onOpenChange={(v) => !v && onClose()}>
      <DialogContent className="max-w-sm">
        <DialogHeader>
          <DialogTitle>Salvar relatório</DialogTitle>
        </DialogHeader>
        <div className="flex flex-col gap-2 py-2">
          <Label htmlFor="nome-relatorio">Nome</Label>
          <Input
            id="nome-relatorio"
            value={nome}
            onChange={(e) => setNome(e.target.value)}
            placeholder="Ex: Funil Janeiro 2025"
            onKeyDown={(e) => e.key === 'Enter' && handleSave()}
          />
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>Cancelar</Button>
          <Button onClick={handleSave} disabled={!nome.trim() || saving}>
            {saving && <Loader2 className="w-4 h-4 mr-2 animate-spin" />}
            Salvar
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  );
}

// ─── Funil tab ────────────────────────────────────────────────────────────────

const STATUS_COLOR: Record<string, string> = {
  NOVA: '#6366f1',
  CONTATADA: '#3b82f6',
  NEGOCIANDO: '#f59e0b',
  FECHADA_GANHA: '#22c55e',
  FECHADA_PERDIDA: '#ef4444',
};

const STATUS_LABEL: Record<string, string> = {
  NOVA: 'Nova',
  CONTATADA: 'Contatada',
  NEGOCIANDO: 'Negociando',
  FECHADA_GANHA: 'Ganha',
  FECHADA_PERDIDA: 'Perdida',
};

function FunilTab({ from, to }: { from: string; to: string }) {
  const [data, setData] = React.useState<FunilResult | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [saveOpen, setSaveOpen] = React.useState(false);
  const [savedList, setSavedList] = React.useState<RelatorioSalvo[]>([]);

  async function load() {
    setLoading(true);
    try {
      const result = await relatorios.funil(toInstant(from), toInstant(to, true));
      setData(result);
    } finally {
      setLoading(false);
    }
  }

  async function loadSaved() {
    try {
      const list = await relatorios.listarSalvos();
      setSavedList(list.filter((r) => r.relatorioTipo === 'FUNIL'));
    } catch { /* ignore */ }
  }

  React.useEffect(() => { load(); loadSaved(); }, [from, to]);

  const chartData = data?.itens.map((i) => ({
    name: STATUS_LABEL[i.status] ?? i.status,
    status: i.status,
    qtd: i.qtd,
  })) ?? [];

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div className="text-xs text-muted-foreground">
          {data && <>Atual: <b>{data.periodoAtual}</b> · Anterior: {data.periodoAnterior}</>}
        </div>
        <div className="flex gap-2">
          <Can role="EXPT">
            <a href={relatorios.exportUrl('funil', toInstant(from), toInstant(to, true))} download>
              <Button size="sm" variant="outline" className="h-7 text-xs gap-1">
                <Download className="w-3.5 h-3.5" />CSV
              </Button>
            </a>
          </Can>
          <Button size="sm" variant="outline" className="h-7 text-xs gap-1" onClick={() => setSaveOpen(true)}>
            <Save className="w-3.5 h-3.5" />Salvar
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-muted-foreground" /></div>
      ) : data ? (
        <>
          <div className="grid grid-cols-5 gap-3">
            {data.itens.map((item) => (
              <div key={item.status} className="border rounded-xl p-4 flex flex-col gap-1">
                <span className="text-xs text-muted-foreground">{STATUS_LABEL[item.status] ?? item.status}</span>
                <span className="text-2xl font-bold">{item.qtd}</span>
                <DeltaBadge delta={item.deltaPercent} />
                {item.valorTotalCentavos > 0 && (
                  <span className="text-[10px] text-muted-foreground mt-1">{BRL_CENT(item.valorTotalCentavos)}</span>
                )}
              </div>
            ))}
          </div>

          <div className="border rounded-xl p-4">
            <ResponsiveContainer width="100%" height={280}>
              <BarChart data={chartData} margin={{ top: 8, right: 8, left: 0, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" stroke="var(--border)" />
                <XAxis dataKey="name" tick={{ fontSize: 11 }} />
                <YAxis allowDecimals={false} tick={{ fontSize: 11 }} />
                <Tooltip />
                <Bar dataKey="qtd" radius={[4, 4, 0, 0]}>
                  {chartData.map((entry) => (
                    <Cell key={entry.status} fill={STATUS_COLOR[entry.status] ?? '#6366f1'} />
                  ))}
                </Bar>
              </BarChart>
            </ResponsiveContainer>
          </div>
        </>
      ) : null}

      {savedList.length > 0 && (
        <div className="flex flex-col gap-2">
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Salvos</span>
          {savedList.map((s) => (
            <SavedRow key={s.id} saved={s} onDeleted={loadSaved} />
          ))}
        </div>
      )}

      <SaveModal
        open={saveOpen}
        tipo="FUNIL"
        filtros={{ from, to }}
        onClose={() => setSaveOpen(false)}
        onSaved={loadSaved}
      />
    </div>
  );
}

// ─── Performance tab ──────────────────────────────────────────────────────────

function PerformanceTab({ from, to }: { from: string; to: string }) {
  const [data, setData] = React.useState<PerformanceResult | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [saveOpen, setSaveOpen] = React.useState(false);
  const [savedList, setSavedList] = React.useState<RelatorioSalvo[]>([]);

  async function load() {
    setLoading(true);
    try {
      const result = await relatorios.performanceAssessor(toInstant(from), toInstant(to, true));
      setData(result);
    } finally {
      setLoading(false);
    }
  }

  async function loadSaved() {
    try {
      const list = await relatorios.listarSalvos();
      setSavedList(list.filter((r) => r.relatorioTipo === 'PERFORMANCE_ASSESSOR'));
    } catch { /* ignore */ }
  }

  React.useEffect(() => { load(); loadSaved(); }, [from, to]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div className="text-xs text-muted-foreground">
          {data && <>Atual: <b>{data.periodoAtual}</b> · Anterior: {data.periodoAnterior}</>}
        </div>
        <div className="flex gap-2">
          <Can role="EXPT">
            <a href={relatorios.exportUrl('performance-assessor', toInstant(from), toInstant(to, true))} download>
              <Button size="sm" variant="outline" className="h-7 text-xs gap-1">
                <Download className="w-3.5 h-3.5" />CSV
              </Button>
            </a>
          </Can>
          <Button size="sm" variant="outline" className="h-7 text-xs gap-1" onClick={() => setSaveOpen(true)}>
            <Save className="w-3.5 h-3.5" />Salvar
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-muted-foreground" /></div>
      ) : data && data.assessores.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-12">Nenhum assessor com dados no período.</p>
      ) : data ? (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-xs text-muted-foreground">
                <th className="text-left py-2 pr-4">Assessor</th>
                <th className="text-right pr-4">Abertas</th>
                <th className="text-right pr-4">Ganhas</th>
                <th className="text-right pr-4">Perdidas</th>
                <th className="text-right pr-4">Ticket médio</th>
                <th className="text-right">Tempo médio (d)</th>
              </tr>
            </thead>
            <tbody>
              {data.assessores.map((a) => {
                const deltaGanhas = a.ganhasAnterior === 0
                  ? (a.ganhas > 0 ? 100 : 0)
                  : ((a.ganhas - a.ganhasAnterior) * 100 / a.ganhasAnterior);
                return (
                  <tr key={a.assessorId} className="border-b hover:bg-muted/30 transition-colors">
                    <td className="py-2.5 pr-4 font-medium">{a.assessorEmail}</td>
                    <td className="text-right pr-4">{a.abertas}</td>
                    <td className="text-right pr-4">
                      <span className="flex items-center justify-end gap-1.5">
                        {a.ganhas}
                        <DeltaBadge delta={deltaGanhas} />
                      </span>
                    </td>
                    <td className="text-right pr-4 text-red-500">{a.perdidas}</td>
                    <td className="text-right pr-4">{BRL_CENT(a.ticketMedioCentavos)}</td>
                    <td className="text-right">{a.tempoMedioFechamentoDias.toFixed(1)}</td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}

      {savedList.length > 0 && (
        <div className="flex flex-col gap-2">
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Salvos</span>
          {savedList.map((s) => (
            <SavedRow key={s.id} saved={s} onDeleted={loadSaved} />
          ))}
        </div>
      )}

      <SaveModal
        open={saveOpen}
        tipo="PERFORMANCE_ASSESSOR"
        filtros={{ from, to }}
        onClose={() => setSaveOpen(false)}
        onSaved={loadSaved}
      />
    </div>
  );
}

// ─── SLA tab ──────────────────────────────────────────────────────────────────

function SlaTab({ from, to }: { from: string; to: string }) {
  const [data, setData] = React.useState<TarefaSlaResult | null>(null);
  const [loading, setLoading] = React.useState(false);
  const [saveOpen, setSaveOpen] = React.useState(false);
  const [savedList, setSavedList] = React.useState<RelatorioSalvo[]>([]);

  async function load() {
    setLoading(true);
    try {
      const result = await relatorios.tarefasSla(toInstant(from), toInstant(to, true));
      setData(result);
    } finally {
      setLoading(false);
    }
  }

  async function loadSaved() {
    try {
      const list = await relatorios.listarSalvos();
      setSavedList(list.filter((r) => r.relatorioTipo === 'TAREFAS_SLA'));
    } catch { /* ignore */ }
  }

  React.useEffect(() => { load(); loadSaved(); }, [from, to]);

  return (
    <div className="flex flex-col gap-6">
      <div className="flex items-center justify-between">
        <div className="text-xs text-muted-foreground">
          {data && <>Atual: <b>{data.periodoAtual}</b> · Anterior: {data.periodoAnterior}</>}
        </div>
        <div className="flex gap-2">
          <Can role="EXPT">
            <a href={relatorios.exportUrl('tarefas-sla', toInstant(from), toInstant(to, true))} download>
              <Button size="sm" variant="outline" className="h-7 text-xs gap-1">
                <Download className="w-3.5 h-3.5" />CSV
              </Button>
            </a>
          </Can>
          <Button size="sm" variant="outline" className="h-7 text-xs gap-1" onClick={() => setSaveOpen(true)}>
            <Save className="w-3.5 h-3.5" />Salvar
          </Button>
        </div>
      </div>

      {loading ? (
        <div className="flex justify-center py-16"><Loader2 className="w-6 h-6 animate-spin text-muted-foreground" /></div>
      ) : data && data.assessores.length === 0 ? (
        <p className="text-sm text-muted-foreground text-center py-12">Nenhuma tarefa no período.</p>
      ) : data ? (
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b text-xs text-muted-foreground">
                <th className="text-left py-2 pr-4">Responsável</th>
                <th className="text-right pr-4">Total</th>
                <th className="text-right pr-4">No prazo</th>
                <th className="text-right pr-4">Atrasadas</th>
                <th className="text-right pr-4">Atraso médio (h)</th>
                <th className="text-right">vs anterior</th>
              </tr>
            </thead>
            <tbody>
              {data.assessores.map((a) => {
                const deltaTot = a.totalAnterior === 0
                  ? (a.total > 0 ? 100 : 0)
                  : ((a.total - a.totalAnterior) * 100 / a.totalAnterior);
                const slaOk = a.total > 0 ? Math.round((a.noPrazo / a.total) * 100) : 0;
                return (
                  <tr key={a.responsavelId} className="border-b hover:bg-muted/30 transition-colors">
                    <td className="py-2.5 pr-4 font-medium">{a.responsavelEmail}</td>
                    <td className="text-right pr-4">{a.total}</td>
                    <td className="text-right pr-4 text-green-600">{a.noPrazo} <span className="text-muted-foreground text-[10px]">({slaOk}%)</span></td>
                    <td className="text-right pr-4 text-red-500">{a.atrasadas}</td>
                    <td className="text-right pr-4">{a.atrasoMedioHoras.toFixed(1)}</td>
                    <td className="text-right"><DeltaBadge delta={deltaTot} /></td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      ) : null}

      {savedList.length > 0 && (
        <div className="flex flex-col gap-2">
          <span className="text-xs font-medium text-muted-foreground uppercase tracking-wider">Salvos</span>
          {savedList.map((s) => (
            <SavedRow key={s.id} saved={s} onDeleted={loadSaved} />
          ))}
        </div>
      )}

      <SaveModal
        open={saveOpen}
        tipo="TAREFAS_SLA"
        filtros={{ from, to }}
        onClose={() => setSaveOpen(false)}
        onSaved={loadSaved}
      />
    </div>
  );
}

// ─── Saved row ────────────────────────────────────────────────────────────────

function SavedRow({ saved, onDeleted }: { saved: RelatorioSalvo; onDeleted: () => void }) {
  const [deleting, setDeleting] = React.useState(false);
  async function handleDelete() {
    setDeleting(true);
    try { await relatorios.deletarSalvo(saved.id); onDeleted(); } finally { setDeleting(false); }
  }
  return (
    <div className="flex items-center justify-between border rounded-lg px-3 py-2 text-sm">
      <div>
        <span className="font-medium">{saved.nome}</span>
        <span className="ml-2 text-xs text-muted-foreground">{new Date(saved.createdAt).toLocaleDateString('pt-BR')}</span>
      </div>
      <Button size="sm" variant="ghost" className="h-6 w-6 p-0" onClick={handleDelete} disabled={deleting}>
        {deleting ? <Loader2 className="w-3 h-3 animate-spin" /> : <Trash2 className="w-3 h-3 text-muted-foreground" />}
      </Button>
    </div>
  );
}

// ─── Page ─────────────────────────────────────────────────────────────────────

export default function RelatoriosPage() {
  return (
    <React.Suspense fallback={null}>
      <RelatoriosInner />
    </React.Suspense>
  );
}

function RelatoriosInner() {
  const def = defaultPeriod();
  const [from, setFrom] = React.useState(def.from);
  const [to, setTo] = React.useState(def.to);
  const [tab, setTab] = React.useState('funil');

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Relatórios"
        description="Funil de prospecção, performance por assessor e SLA de tarefas."
        actions={
          <div className="flex items-center gap-2">
            <div className="flex items-center gap-1.5">
              <Label htmlFor="from-date" className="text-xs text-muted-foreground shrink-0">De</Label>
              <Input
                id="from-date"
                type="date"
                value={from}
                onChange={(e) => setFrom(e.target.value)}
                className="h-8 text-xs w-36"
              />
            </div>
            <div className="flex items-center gap-1.5">
              <Label htmlFor="to-date" className="text-xs text-muted-foreground shrink-0">Até</Label>
              <Input
                id="to-date"
                type="date"
                value={to}
                onChange={(e) => setTo(e.target.value)}
                className="h-8 text-xs w-36"
              />
            </div>
          </div>
        }
      />

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="funil">Funil</TabsTrigger>
          <TabsTrigger value="performance">Performance</TabsTrigger>
          <TabsTrigger value="sla">SLA Tarefas</TabsTrigger>
        </TabsList>
        <TabsContent value="funil" className="mt-6">
          <FunilTab from={from} to={to} />
        </TabsContent>
        <TabsContent value="performance" className="mt-6">
          <PerformanceTab from={from} to={to} />
        </TabsContent>
        <TabsContent value="sla" className="mt-6">
          <SlaTab from={from} to={to} />
        </TabsContent>
      </Tabs>
    </div>
  );
}
