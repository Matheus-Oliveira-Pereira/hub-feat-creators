'use client';

import * as React from 'react';
import { Upload, Download, CheckCircle2, XCircle, Clock, Loader2, FileText } from 'lucide-react';
import { importacoes, ImportJob, ImportStatus } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { PageHeader } from '@/components/app/page-header';
import { EmptyState } from '@/components/app/empty-state';
import { ImportWizard } from '@/components/app/import-wizard';
import { Can } from '@/components/auth/can';
import { cn } from '@/lib/utils';

export default function ImportacaoPage() {
  return (
    <React.Suspense fallback={null}>
      <ImportacaoInner />
    </React.Suspense>
  );
}

function ImportacaoInner() {
  const [wizardOpen, setWizardOpen] = React.useState(false);
  const [jobs, setJobs] = React.useState<ImportJob[]>([]);
  const [loading, setLoading] = React.useState(true);

  async function loadJobs() {
    try {
      const result = await importacoes.list(0, 30);
      setJobs(result.jobs);
    } catch {
      // silently ignore — page shows empty
    } finally {
      setLoading(false);
    }
  }

  React.useEffect(() => { loadJobs(); }, []);

  return (
    <div className="flex flex-col gap-6 p-6">
      <PageHeader
        title="Importação"
        description="Importe influenciadores, marcas e contatos via CSV ou XLSX."
        actions={
          <Can role="CIMP">
            <Button size="sm" onClick={() => setWizardOpen(true)}>
              <Upload className="w-4 h-4 mr-2" />
              Nova importação
            </Button>
          </Can>
        }
      />

      <ImportWizard
        open={wizardOpen}
        onOpenChange={setWizardOpen}
        onDone={loadJobs}
      />

      {loading ? (
        <div className="flex flex-col gap-3">
          {[1, 2, 3].map(i => (
            <Skeleton key={i} className="h-20 w-full rounded-xl" />
          ))}
        </div>
      ) : jobs.length === 0 ? (
        <EmptyState
          title="Nenhuma importação ainda"
          description="Importe uma planilha de creators, marcas ou contatos para começar."
          action={
            <Can role="CIMP">
              <Button size="sm" onClick={() => setWizardOpen(true)}>
                <Upload className="w-4 h-4 mr-2" />
                Nova importação
              </Button>
            </Can>
          }
        />
      ) : (
        <div className="flex flex-col gap-3">
          {jobs.map(job => (
            <JobCard key={job.id} job={job} onRefresh={loadJobs} />
          ))}
        </div>
      )}
    </div>
  );
}

function JobCard({ job, onRefresh }: { job: ImportJob; onRefresh: () => void }) {
  const pct =
    job.totalLinhas && job.totalLinhas > 0
      ? Math.round((job.processadas / job.totalLinhas) * 100)
      : 0;

  return (
    <div className="border rounded-xl p-4 flex items-center gap-4 bg-card">
      <StatusIcon status={job.status} />

      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 mb-0.5">
          <span className="text-sm font-medium truncate">{job.arquivoNome}</span>
          <Badge variant="outline" className="text-[10px] shrink-0">
            {ENTITY_LABEL[job.entidade] ?? job.entidade}
          </Badge>
          <StatusBadge status={job.status} />
        </div>

        {job.status === 'EXECUTANDO' && job.totalLinhas && (
          <div className="mt-1.5">
            <div className="flex justify-between text-[10px] text-muted-foreground mb-0.5">
              <span>{job.processadas} / {job.totalLinhas}</span>
              <span>{pct}%</span>
            </div>
            <div className="w-full h-1.5 bg-muted rounded-full overflow-hidden">
              <div className="h-full bg-primary transition-all" style={{ width: `${pct}%` }} />
            </div>
          </div>
        )}

        {(job.status === 'CONCLUIDO' || job.status === 'CANCELADO') && (
          <div className="flex gap-3 mt-0.5 text-xs text-muted-foreground">
            <span><span className="text-green-600 font-medium">{job.sucesso}</span> criados</span>
            <span><span className="text-red-500 font-medium">{job.falha}</span> erros</span>
            {job.totalLinhas && <span>{job.totalLinhas} linhas</span>}
          </div>
        )}

        <p className="text-[10px] text-muted-foreground mt-0.5">
          {new Date(job.createdAt).toLocaleString('pt-BR')}
        </p>
      </div>

      <div className="flex gap-2 shrink-0">
        {(job.status === 'CONCLUIDO' || job.status === 'FALHOU') && (
          <a href={importacoes.relatorioUrl(job.id)} target="_blank" rel="noopener noreferrer">
            <Button size="sm" variant="outline" className="h-7 text-xs gap-1">
              <Download className="w-3.5 h-3.5" />
              Relatório
            </Button>
          </a>
        )}
      </div>
    </div>
  );
}

const ENTITY_LABEL: Record<string, string> = {
  INFLUENCIADOR: 'Influenciadores',
  MARCA: 'Marcas',
  CONTATO: 'Contatos',
};

const STATUS_LABEL: Record<ImportStatus, string> = {
  UPLOADADO: 'Aguardando',
  VALIDANDO: 'Validando',
  PRONTO_DRY_RUN: 'Pronto',
  EXECUTANDO: 'Importando',
  CONCLUIDO: 'Concluído',
  CANCELADO: 'Cancelado',
  FALHOU: 'Falhou',
};

const STATUS_COLOR: Record<ImportStatus, string> = {
  UPLOADADO: 'bg-muted text-muted-foreground',
  VALIDANDO: 'bg-yellow-100 text-yellow-700',
  PRONTO_DRY_RUN: 'bg-blue-100 text-blue-700',
  EXECUTANDO: 'bg-primary/10 text-primary',
  CONCLUIDO: 'bg-green-100 text-green-700',
  CANCELADO: 'bg-muted text-muted-foreground',
  FALHOU: 'bg-red-100 text-red-600',
};

function StatusBadge({ status }: { status: ImportStatus }) {
  return (
    <span className={cn('text-[10px] px-1.5 py-0.5 rounded font-medium', STATUS_COLOR[status])}>
      {STATUS_LABEL[status]}
    </span>
  );
}

function StatusIcon({ status }: { status: ImportStatus }) {
  if (status === 'EXECUTANDO') return <Loader2 className="w-5 h-5 text-primary animate-spin shrink-0" />;
  if (status === 'CONCLUIDO') return <CheckCircle2 className="w-5 h-5 text-green-500 shrink-0" />;
  if (status === 'CANCELADO' || status === 'FALHOU') return <XCircle className="w-5 h-5 text-red-400 shrink-0" />;
  if (status === 'VALIDANDO') return <Loader2 className="w-5 h-5 text-yellow-500 animate-spin shrink-0" />;
  return <FileText className="w-5 h-5 text-muted-foreground shrink-0" />;
}
