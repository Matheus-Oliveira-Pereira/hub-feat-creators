'use client';

import * as React from 'react';
import { Upload, ChevronRight, ChevronLeft, CheckCircle2, XCircle, AlertCircle, Download, Loader2 } from 'lucide-react';
import { toast } from 'sonner';
import {
  ImportJob,
  ImportEntidade,
  ImportLinha,
  ColumnSuggestion,
  ImportTemplate,
  DedupStrategy,
  importacoes,
} from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Badge } from '@/components/ui/badge';
import { cn } from '@/lib/utils';

// ─── Types ────────────────────────────────────────────────────────────────────

type Step = 'upload' | 'mapping' | 'dryrun' | 'progress' | 'done';

const BASE_LEGAL_OPTIONS = [
  { value: 'LEGITIMO_INTERESSE', label: 'Legítimo interesse' },
  { value: 'CONSENTIMENTO', label: 'Consentimento' },
  { value: 'EXECUCAO_CONTRATO', label: 'Execução de contrato' },
  { value: 'OBRIGACAO_LEGAL', label: 'Obrigação legal' },
];

const DEDUP_OPTIONS = [
  { value: 'SKIP', label: 'Ignorar duplicatas' },
  { value: 'UPDATE', label: 'Atualizar campos mapeados' },
  { value: 'DUPLICATE', label: 'Criar mesmo duplicado' },
];

const ENTITY_FIELDS: Record<ImportEntidade, string[]> = {
  INFLUENCIADOR: ['nome', 'email', 'telefone', 'instagram', 'youtube', 'tiktok', 'nicho', 'audiencia_total', 'observacoes', 'cpf'],
  MARCA: ['nome', 'cnpj', 'segmento', 'site', 'observacoes'],
  CONTATO: ['nome', 'email', 'telefone', 'cargo', 'marca_id'],
};

// ─── Props ────────────────────────────────────────────────────────────────────

interface ImportWizardProps {
  open: boolean;
  onOpenChange: (v: boolean) => void;
  onDone: () => void;
}

// ─── Component ────────────────────────────────────────────────────────────────

export function ImportWizard({ open, onOpenChange, onDone }: ImportWizardProps) {
  const [step, setStep] = React.useState<Step>('upload');
  const [file, setFile] = React.useState<File | null>(null);
  const [entidade, setEntidade] = React.useState<ImportEntidade>('INFLUENCIADOR');
  const [job, setJob] = React.useState<ImportJob | null>(null);
  const [headers, setHeaders] = React.useState<string[]>([]);
  const [suggestions, setSuggestions] = React.useState<ColumnSuggestion[]>([]);
  const [mapeamento, setMapeamento] = React.useState<Record<string, string>>({});
  const [baseLegal, setBaseLegal] = React.useState('LEGITIMO_INTERESSE');
  const [dedup, setDedup] = React.useState<DedupStrategy>('SKIP');
  const [linhas, setLinhas] = React.useState<ImportLinha[]>([]);
  const [dryJob, setDryJob] = React.useState<ImportJob | null>(null);
  const [progress, setProgress] = React.useState({ processadas: 0, total: 0, sucesso: 0, falha: 0 });
  const [loading, setLoading] = React.useState(false);
  const [templates, setTemplates] = React.useState<ImportTemplate[]>([]);
  const [saveTemplateName, setSaveTemplateName] = React.useState('');
  const fileRef = React.useRef<HTMLInputElement>(null);
  const sseRef = React.useRef<EventSource | null>(null);

  function reset() {
    setStep('upload');
    setFile(null);
    setJob(null);
    setHeaders([]);
    setSuggestions([]);
    setMapeamento({});
    setLinhas([]);
    setDryJob(null);
    setProgress({ processadas: 0, total: 0, sucesso: 0, falha: 0 });
    setSaveTemplateName('');
    sseRef.current?.close();
  }

  function handleClose() {
    reset();
    onOpenChange(false);
  }

  // ── Step 1: Upload ──────────────────────────────────────────────────────────

  async function handleUpload() {
    if (!file) return;
    setLoading(true);
    try {
      const j = await importacoes.upload(file, entidade);
      setJob(j);
      const probe = await importacoes.probe(j.id);
      setHeaders(probe.headers);
      setSuggestions(probe.suggestions);
      const initialMap: Record<string, string> = {};
      probe.suggestions.forEach(s => {
        if (s.suggestedField && s.distance < 3) initialMap[s.csvHeader] = s.suggestedField;
      });
      setMapeamento(initialMap);
      const tpls = await importacoes.listTemplates(entidade);
      setTemplates(tpls);
      setStep('mapping');
    } catch (e: unknown) {
      toast.error((e as { message?: string })?.message ?? 'Erro ao fazer upload');
    } finally {
      setLoading(false);
    }
  }

  function handleDrop(e: React.DragEvent) {
    e.preventDefault();
    const f = e.dataTransfer.files[0];
    if (f) setFile(f);
  }

  // ── Step 2: Mapping ─────────────────────────────────────────────────────────

  function applyTemplate(t: ImportTemplate) {
    setMapeamento({ ...t.mapeamento });
    setEntidade(t.entidade);
  }

  async function handleSaveTemplate() {
    if (!saveTemplateName.trim()) return;
    try {
      await importacoes.createTemplate(saveTemplateName.trim(), entidade, mapeamento);
      toast.success('Template salvo');
      setSaveTemplateName('');
    } catch {
      toast.error('Erro ao salvar template');
    }
  }

  async function handleDryRun() {
    if (!job) return;
    setLoading(true);
    try {
      await importacoes.setMapeamento(job.id, mapeamento, baseLegal, dedup);
      const dry = await importacoes.dryRun(job.id);
      setDryJob(dry);
      const result = await importacoes.listDryRun(job.id, 0, 500);
      setLinhas(result.linhas);
      setStep('dryrun');
    } catch (e: unknown) {
      toast.error((e as { message?: string })?.message ?? 'Erro no dry-run');
    } finally {
      setLoading(false);
    }
  }

  // ── Step 3: Dry-run ─────────────────────────────────────────────────────────

  async function handleExecute() {
    if (!job) return;
    setLoading(true);
    try {
      await importacoes.execute(job.id);
      setStep('progress');
      startSSE(job.id);
    } catch (e: unknown) {
      toast.error((e as { message?: string })?.message ?? 'Erro ao iniciar importação');
    } finally {
      setLoading(false);
    }
  }

  // ── Step 4: Progress ────────────────────────────────────────────────────────

  function startSSE(jobId: string) {
    const token = typeof window !== 'undefined' ? localStorage.getItem('accessToken') : null;
    const url = `${process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080'}/api/v1/imports/${jobId}/progress`;
    const es = new EventSource(url, { withCredentials: true });
    sseRef.current = es;

    es.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        setProgress({ processadas: data.processadas, total: data.total, sucesso: data.sucesso, falha: data.falha });
        if (['CONCLUIDO', 'CANCELADO', 'FALHOU'].includes(data.status)) {
          es.close();
          setStep('done');
        }
      } catch {}
    };

    es.onerror = () => {
      es.close();
      // Fallback: poll once
      importacoes.get(jobId).then(j => {
        setProgress({ processadas: j.processadas, total: j.totalLinhas ?? 0, sucesso: j.sucesso, falha: j.falha });
        if (['CONCLUIDO', 'CANCELADO', 'FALHOU'].includes(j.status)) setStep('done');
      });
    };
  }

  async function handleCancel() {
    if (!job) return;
    sseRef.current?.close();
    try {
      await importacoes.cancel(job.id);
    } catch {}
    setStep('done');
  }

  // ── Render ──────────────────────────────────────────────────────────────────

  const erros = linhas.filter(l => l.status === 'ERRO');
  const oks = linhas.filter(l => l.status === 'OK');
  const pct = progress.total > 0 ? Math.round((progress.processadas / progress.total) * 100) : 0;

  return (
    <Sheet open={open} onOpenChange={v => { if (!v) handleClose(); }}>
      <SheetContent side="right" className="w-full sm:max-w-2xl flex flex-col gap-0 p-0">
        <SheetHeader className="px-6 pt-6 pb-4 border-b">
          <SheetTitle className="flex items-center gap-2">
            <Upload className="w-4 h-4 text-primary" />
            Importar CSV / XLSX
          </SheetTitle>
          <StepIndicator step={step} />
        </SheetHeader>

        <div className="flex-1 overflow-y-auto px-6 py-4">

          {/* ── Step 1: Upload ── */}
          {step === 'upload' && (
            <div className="flex flex-col gap-4">
              <div>
                <label className="text-sm font-medium mb-1.5 block">Entidade</label>
                <Select value={entidade} onValueChange={v => setEntidade(v as ImportEntidade)}>
                  <SelectTrigger>
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="INFLUENCIADOR">Influenciadores</SelectItem>
                    <SelectItem value="MARCA">Marcas</SelectItem>
                    <SelectItem value="CONTATO">Contatos</SelectItem>
                  </SelectContent>
                </Select>
              </div>

              <div
                className={cn(
                  'border-2 border-dashed rounded-lg p-10 flex flex-col items-center gap-3 cursor-pointer transition-colors',
                  file ? 'border-primary/50 bg-primary/5' : 'border-muted hover:border-primary/30'
                )}
                onDrop={handleDrop}
                onDragOver={e => e.preventDefault()}
                onClick={() => fileRef.current?.click()}
              >
                <Upload className={cn('w-8 h-8', file ? 'text-primary' : 'text-muted-foreground')} />
                {file ? (
                  <>
                    <span className="text-sm font-medium">{file.name}</span>
                    <span className="text-xs text-muted-foreground">{(file.size / 1024).toFixed(0)} KB</span>
                  </>
                ) : (
                  <>
                    <span className="text-sm font-medium">Arraste ou clique para selecionar</span>
                    <span className="text-xs text-muted-foreground">CSV, XLSX — máximo 5 MB, até 10 000 linhas</span>
                  </>
                )}
                <input
                  ref={fileRef}
                  type="file"
                  accept=".csv,.xlsx,.xls"
                  className="hidden"
                  onChange={e => setFile(e.target.files?.[0] ?? null)}
                />
              </div>
            </div>
          )}

          {/* ── Step 2: Mapping ── */}
          {step === 'mapping' && (
            <div className="flex flex-col gap-5">
              {templates.length > 0 && (
                <div>
                  <p className="text-xs text-muted-foreground mb-2">Reusar template salvo</p>
                  <div className="flex flex-wrap gap-2">
                    {templates.map(t => (
                      <button
                        key={t.id}
                        onClick={() => applyTemplate(t)}
                        className="text-xs px-2 py-1 rounded border border-border hover:bg-muted"
                      >
                        {t.nome}
                      </button>
                    ))}
                  </div>
                </div>
              )}

              <div>
                <p className="text-xs text-muted-foreground mb-2">
                  Mapeie cada coluna do arquivo para um campo da entidade.{' '}
                  <span className="text-primary">Auto-sugerido</span> = distância &lt; 3.
                </p>
                <div className="border rounded-lg overflow-hidden">
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="border-b bg-muted/40">
                        <th className="text-left px-3 py-2 font-medium">Coluna CSV</th>
                        <th className="text-left px-3 py-2 font-medium">Campo da entidade</th>
                      </tr>
                    </thead>
                    <tbody>
                      {headers.map((h, i) => {
                        const sug = suggestions[i];
                        const isAutoSug = sug?.suggestedField && sug.distance < 3;
                        const fields = ENTITY_FIELDS[entidade];
                        return (
                          <tr key={h} className="border-b last:border-0">
                            <td className="px-3 py-2">
                              <span className="font-mono text-xs bg-muted px-1.5 py-0.5 rounded">{h}</span>
                              {isAutoSug && (
                                <span className="ml-2 text-[10px] text-primary">auto</span>
                              )}
                            </td>
                            <td className="px-3 py-2">
                              <Select
                                value={mapeamento[h] ?? '__none__'}
                                onValueChange={v =>
                                  setMapeamento(prev => {
                                    const next = { ...prev };
                                    if (v === '__none__') delete next[h];
                                    else next[h] = v;
                                    return next;
                                  })
                                }
                              >
                                <SelectTrigger className="h-7 text-xs">
                                  <SelectValue placeholder="— ignorar —" />
                                </SelectTrigger>
                                <SelectContent>
                                  <SelectItem value="__none__">— ignorar —</SelectItem>
                                  {fields.map(f => (
                                    <SelectItem key={f} value={f}>{f}</SelectItem>
                                  ))}
                                </SelectContent>
                              </Select>
                            </td>
                          </tr>
                        );
                      })}
                    </tbody>
                  </table>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="text-xs font-medium mb-1 block">Base legal LGPD</label>
                  <Select value={baseLegal} onValueChange={setBaseLegal}>
                    <SelectTrigger className="h-8 text-xs">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {BASE_LEGAL_OPTIONS.map(o => (
                        <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
                <div>
                  <label className="text-xs font-medium mb-1 block">Duplicatas</label>
                  <Select value={dedup} onValueChange={v => setDedup(v as DedupStrategy)}>
                    <SelectTrigger className="h-8 text-xs">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      {DEDUP_OPTIONS.map(o => (
                        <SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="flex items-center gap-2">
                <input
                  className="flex-1 h-7 text-xs border rounded px-2 outline-none focus:ring-1 focus:ring-primary"
                  placeholder="Nome do template para salvar…"
                  value={saveTemplateName}
                  onChange={e => setSaveTemplateName(e.target.value)}
                />
                <Button
                  size="sm"
                  variant="outline"
                  className="h-7 text-xs"
                  disabled={!saveTemplateName.trim()}
                  onClick={handleSaveTemplate}
                >
                  Salvar template
                </Button>
              </div>
            </div>
          )}

          {/* ── Step 3: Dry-run ── */}
          {step === 'dryrun' && dryJob && (
            <div className="flex flex-col gap-4">
              <div className="flex gap-3">
                <Chip icon={<CheckCircle2 className="w-3.5 h-3.5 text-green-500" />} label={`${oks.length} OK`} />
                <Chip icon={<XCircle className="w-3.5 h-3.5 text-red-500" />} label={`${erros.length} erro${erros.length !== 1 ? 's' : ''}`} />
                <Chip icon={<AlertCircle className="w-3.5 h-3.5 text-yellow-500" />} label={`${linhas.length} linhas`} />
              </div>

              {erros.length > 0 && (
                <div className="border rounded-lg overflow-hidden">
                  <div className="px-3 py-2 bg-muted/40 border-b text-xs font-medium text-muted-foreground">
                    Linhas com erro ({erros.length})
                  </div>
                  <div className="max-h-64 overflow-y-auto">
                    {erros.slice(0, 200).map(l => (
                      <div key={l.linha} className="flex gap-2 px-3 py-2 border-b last:border-0 text-xs">
                        <span className="text-muted-foreground w-10 shrink-0">L{l.linha}</span>
                        <span className="text-red-600">{l.erros.join(' · ')}</span>
                      </div>
                    ))}
                    {erros.length > 200 && (
                      <p className="px-3 py-2 text-xs text-muted-foreground">
                        + {erros.length - 200} erros não exibidos
                      </p>
                    )}
                  </div>
                </div>
              )}

              {erros.length === 0 && (
                <p className="text-sm text-muted-foreground">
                  Nenhum erro encontrado. Pronto para executar!
                </p>
              )}
            </div>
          )}

          {/* ── Step 4: Progress ── */}
          {step === 'progress' && (
            <div className="flex flex-col gap-6 items-center py-8">
              <Loader2 className="w-8 h-8 animate-spin text-primary" />
              <div className="w-full">
                <div className="flex justify-between text-xs text-muted-foreground mb-1.5">
                  <span>{progress.processadas} / {progress.total} linhas</span>
                  <span>{pct}%</span>
                </div>
                <div className="w-full h-2 bg-muted rounded-full overflow-hidden">
                  <div
                    className="h-full bg-primary transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                </div>
                <div className="flex gap-4 mt-3 text-xs text-muted-foreground">
                  <span><span className="text-green-600 font-medium">{progress.sucesso}</span> criados</span>
                  <span><span className="text-red-500 font-medium">{progress.falha}</span> erros</span>
                </div>
              </div>
            </div>
          )}

          {/* ── Step 5: Done ── */}
          {step === 'done' && (
            <div className="flex flex-col gap-4 items-center py-8">
              <CheckCircle2 className="w-10 h-10 text-green-500" />
              <p className="text-sm font-medium">Importação concluída</p>
              <div className="flex gap-6 text-sm text-muted-foreground">
                <span><strong className="text-foreground">{progress.sucesso}</strong> criados</span>
                <span><strong className="text-foreground">{progress.falha}</strong> erros</span>
              </div>
              {job && (
                <a
                  href={importacoes.relatorioUrl(job.id)}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="flex items-center gap-1.5 text-xs text-primary underline underline-offset-2"
                >
                  <Download className="w-3.5 h-3.5" />
                  Baixar relatório CSV
                </a>
              )}
            </div>
          )}
        </div>

        {/* Footer actions */}
        <div className="border-t px-6 py-4 flex justify-between gap-3">
          <Button variant="ghost" onClick={handleClose} size="sm">
            {step === 'done' ? 'Fechar' : 'Cancelar'}
          </Button>

          <div className="flex gap-2">
            {step === 'mapping' && (
              <Button variant="outline" size="sm" onClick={() => setStep('upload')}>
                <ChevronLeft className="w-4 h-4 mr-1" />
                Voltar
              </Button>
            )}
            {step === 'dryrun' && (
              <Button variant="outline" size="sm" onClick={() => setStep('mapping')}>
                <ChevronLeft className="w-4 h-4 mr-1" />
                Voltar
              </Button>
            )}

            {step === 'upload' && (
              <Button size="sm" disabled={!file || loading} onClick={handleUpload}>
                {loading ? <Loader2 className="w-4 h-4 mr-1 animate-spin" /> : null}
                Próximo
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            )}
            {step === 'mapping' && (
              <Button size="sm" disabled={loading} onClick={handleDryRun}>
                {loading ? <Loader2 className="w-4 h-4 mr-1 animate-spin" /> : null}
                Validar
                <ChevronRight className="w-4 h-4 ml-1" />
              </Button>
            )}
            {step === 'dryrun' && (
              <Button size="sm" disabled={loading} onClick={handleExecute}>
                {loading ? <Loader2 className="w-4 h-4 mr-1 animate-spin" /> : null}
                {erros.length > 0
                  ? `Importar ${oks.length} linhas válidas`
                  : 'Importar'}
              </Button>
            )}
            {step === 'progress' && (
              <Button size="sm" variant="destructive" onClick={handleCancel}>
                Cancelar importação
              </Button>
            )}
            {step === 'done' && (
              <Button size="sm" onClick={() => { onDone(); handleClose(); }}>
                Concluído
              </Button>
            )}
          </div>
        </div>
      </SheetContent>
    </Sheet>
  );
}

// ─── Sub-components ───────────────────────────────────────────────────────────

function StepIndicator({ step }: { step: Step }) {
  const steps: { key: Step; label: string }[] = [
    { key: 'upload', label: 'Upload' },
    { key: 'mapping', label: 'Mapeamento' },
    { key: 'dryrun', label: 'Validação' },
    { key: 'progress', label: 'Importando' },
    { key: 'done', label: 'Concluído' },
  ];
  const idx = steps.findIndex(s => s.key === step);

  return (
    <div className="flex items-center gap-1 mt-1">
      {steps.map((s, i) => (
        <React.Fragment key={s.key}>
          <div className={cn(
            'text-xs px-2 py-0.5 rounded',
            i === idx ? 'bg-primary text-primary-foreground' :
            i < idx ? 'text-muted-foreground' : 'text-muted-foreground/40'
          )}>
            {s.label}
          </div>
          {i < steps.length - 1 && (
            <ChevronRight className={cn('w-3 h-3', i < idx ? 'text-muted-foreground' : 'text-muted-foreground/30')} />
          )}
        </React.Fragment>
      ))}
    </div>
  );
}

function Chip({ icon, label }: { icon: React.ReactNode; label: string }) {
  return (
    <div className="flex items-center gap-1.5 text-xs border rounded px-2 py-1">
      {icon}
      {label}
    </div>
  );
}
