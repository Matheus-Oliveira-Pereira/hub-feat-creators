'use client';

import * as React from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Plus,
  Download,
  AtSign,
  MoreHorizontal,
  Trash2,
  Eye,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { influenciadores, Influenciador } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Card, CardContent } from '@/components/ui/card';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import {
  Sheet,
  SheetContent,
  SheetDescription,
  SheetHeader,
  SheetTitle,
} from '@/components/ui/sheet';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { PageHeader } from '@/components/app/page-header';
import { FilterBar } from '@/components/app/filter-bar';
import { EntityFormModal } from '@/components/app/entity-form-modal';
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function initials(nome: string) {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(s => s[0]?.toUpperCase())
    .join('');
}

export default function InfluenciadoresPage() {
  return (
    <React.Suspense fallback={null}>
      <InfluenciadoresInner />
    </React.Suspense>
  );
}

function InfluenciadoresInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [data, setData] = React.useState<Influenciador[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [search, setSearch] = React.useState('');
  const [view, setView] = React.useState<'cards' | 'table'>('cards');
  const [formOpen, setFormOpen] = React.useState(false);
  const [form, setForm] = React.useState({ nome: '', nicho: '', instagram: '' });
  const [saving, setSaving] = React.useState(false);
  const [detail, setDetail] = React.useState<Influenciador | null>(null);

  React.useEffect(() => {
    if (searchParams.get('new') === '1') {
      setFormOpen(true);
      router.replace('/influenciadores');
    }
  }, [searchParams, router]);

  const load = React.useCallback(
    async (nome?: string) => {
      setLoading(true);
      try {
        const res = await influenciadores.list({ nome: nome || undefined });
        setData(res.data);
      } catch {
        router.push('/login');
      } finally {
        setLoading(false);
      }
    },
    [router]
  );

  React.useEffect(() => {
    load();
  }, [load]);

  React.useEffect(() => {
    const t = setTimeout(() => load(search || undefined), 250);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [search]);

  async function handleCreate(e: React.FormEvent) {
    e.preventDefault();
    setSaving(true);
    try {
      await influenciadores.create({
        nome: form.nome,
        nicho: form.nicho || null,
        handles: form.instagram ? { instagram: form.instagram } : {},
        tags: [],
      });
      toast.success('Influenciador criado.');
      setFormOpen(false);
      setForm({ nome: '', nicho: '', instagram: '' });
      load();
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao criar.');
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(id: string, nome: string) {
    if (!confirm(`Remover "${nome}"?`)) return;
    try {
      await influenciadores.delete(id);
      toast.success('Removido.');
      setDetail(null);
      load(search || undefined);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao remover.');
    }
  }

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-12">
      <PageHeader
        eyebrow="Cadastros"
        title="Influenciadores"
        description="Gerencie os creators do seu workspace, com nicho, handles e tags."
        actions={
          <>
            <Button asChild variant="outline">
              <a href={`${API_URL}/api/v1/influenciadores/export.csv`}>
                <Download className="h-4 w-4" /> Exportar CSV
              </a>
            </Button>
            <Button onClick={() => setFormOpen(true)}>
              <Plus className="h-4 w-4" /> Adicionar
            </Button>
          </>
        }
      />

      <FilterBar
        search={search}
        onSearchChange={setSearch}
        searchPlaceholder="Buscar por nome…"
        view={view}
        onViewChange={setView}
        count={data.length}
        countLabel={data.length === 1 ? 'creator' : 'creators'}
      />

      {loading ? (
        view === 'cards' ? (
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {Array.from({ length: 6 }).map((_, i) => (
              <Skeleton key={i} className="h-40 w-full rounded-xl" />
            ))}
          </div>
        ) : (
          <Skeleton className="h-96 w-full rounded-xl" />
        )
      ) : data.length === 0 ? (
        <EmptyState
          illustration={<EmptyIllustration variant="sparkles" />}
          title={search ? 'Nada encontrado' : 'Nenhum influenciador ainda'}
          description={
            search
              ? `Sem resultados para "${search}". Tente outro termo.`
              : 'Adicione o primeiro creator do seu workspace para começar.'
          }
          action={
            !search && (
              <Button onClick={() => setFormOpen(true)}>
                <Plus className="h-4 w-4" /> Adicionar influenciador
              </Button>
            )
          }
        />
      ) : view === 'cards' ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <AnimatePresence mode="popLayout">
            {data.map((inf, i) => (
              <motion.div
                key={inf.id}
                layout
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2, delay: i * 0.02 }}
              >
                <Card
                  className="group cursor-pointer p-5 transition-all hover:border-border-strong hover:shadow-md"
                  onClick={() => setDetail(inf)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <Avatar className="h-11 w-11 shrink-0">
                        <AvatarFallback className="bg-foreground text-primary font-semibold">
                          {initials(inf.nome)}
                        </AvatarFallback>
                      </Avatar>
                      <div className="min-w-0">
                        <p className="font-display font-semibold truncate">{inf.nome}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {inf.nicho ?? 'Sem nicho'}
                        </p>
                      </div>
                    </div>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild onClick={e => e.stopPropagation()}>
                        <Button variant="ghost" size="icon-sm" className="opacity-0 group-hover:opacity-100 transition-opacity">
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                        <DropdownMenuItem onClick={() => setDetail(inf)}>
                          <Eye className="h-4 w-4" /> Detalhes
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleDelete(inf.id, inf.nome)}
                          className="text-destructive focus:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" /> Remover
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                  <div className="mt-4 flex items-center gap-2 text-xs">
                    {inf.handles?.instagram && (
                      <Badge variant="outline" className="font-mono">
                        <AtSign className="h-3 w-3 mr-1" />
                        {inf.handles.instagram}
                      </Badge>
                    )}
                    {inf.tags.slice(0, 2).map(t => (
                      <Badge key={t} variant="secondary">
                        {t}
                      </Badge>
                    ))}
                    {inf.tags.length > 2 && (
                      <Badge variant="muted">+{inf.tags.length - 2}</Badge>
                    )}
                  </div>
                </Card>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      ) : (
        <Card className="overflow-hidden">
          <CardContent className="p-0">
            <table className="w-full text-sm">
              <thead className="bg-muted/50 border-b border-border">
                <tr>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Nome</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Nicho</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Instagram</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Tags</th>
                  <th className="px-5 py-3 w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {data.map(inf => (
                  <tr
                    key={inf.id}
                    className="group cursor-pointer transition-colors hover:bg-accent/50"
                    onClick={() => setDetail(inf)}
                  >
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar className="h-7 w-7">
                          <AvatarFallback className="bg-foreground text-primary text-xs font-semibold">
                            {initials(inf.nome)}
                          </AvatarFallback>
                        </Avatar>
                        <span className="font-medium">{inf.nome}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-muted-foreground">{inf.nicho ?? '—'}</td>
                    <td className="px-5 py-3 text-muted-foreground font-mono text-xs">
                      {inf.handles?.instagram ?? '—'}
                    </td>
                    <td className="px-5 py-3">
                      <div className="flex gap-1 flex-wrap">
                        {inf.tags.slice(0, 3).map(t => (
                          <Badge key={t} variant="secondary">{t}</Badge>
                        ))}
                      </div>
                    </td>
                    <td className="px-5 py-3 text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={e => e.stopPropagation()}>
                          <Button variant="ghost" size="icon-sm">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                          <DropdownMenuItem onClick={() => setDetail(inf)}>
                            <Eye className="h-4 w-4" /> Detalhes
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleDelete(inf.id, inf.nome)}
                            className="text-destructive focus:text-destructive"
                          >
                            <Trash2 className="h-4 w-4" /> Remover
                          </DropdownMenuItem>
                        </DropdownMenuContent>
                      </DropdownMenu>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </CardContent>
        </Card>
      )}

      <EntityFormModal
        open={formOpen}
        onOpenChange={setFormOpen}
        title="Novo influenciador"
        description="Informações básicas. Você pode editar depois."
        onSubmit={handleCreate}
        submitLabel="Criar"
        saving={saving}
      >
        <div className="space-y-1.5">
          <Label htmlFor="nome">Nome *</Label>
          <Input
            id="nome"
            required
            value={form.nome}
            onChange={e => setForm(p => ({ ...p, nome: e.target.value }))}
            placeholder="Nome do creator"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="nicho">Nicho</Label>
          <Input
            id="nicho"
            value={form.nicho}
            onChange={e => setForm(p => ({ ...p, nicho: e.target.value }))}
            placeholder="Ex: fitness, moda, gaming"
          />
        </div>
        <div className="space-y-1.5">
          <Label htmlFor="instagram">Instagram</Label>
          <Input
            id="instagram"
            value={form.instagram}
            onChange={e => setForm(p => ({ ...p, instagram: e.target.value }))}
            placeholder="@usuario"
          />
        </div>
      </EntityFormModal>

      <Sheet open={!!detail} onOpenChange={open => !open && setDetail(null)}>
        <SheetContent>
          {detail && (
            <>
              <SheetHeader>
                <div className="flex items-center gap-3 mb-3">
                  <Avatar className="h-14 w-14">
                    <AvatarFallback className="bg-foreground text-primary text-lg font-bold">
                      {initials(detail.nome)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <SheetTitle>{detail.nome}</SheetTitle>
                    <SheetDescription>
                      {detail.nicho ?? 'Sem nicho definido'}
                    </SheetDescription>
                  </div>
                </div>
              </SheetHeader>

              <div className="mt-6 space-y-5">
                <section>
                  <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                    Handles
                  </h4>
                  {Object.keys(detail.handles ?? {}).length === 0 ? (
                    <p className="text-sm text-muted-foreground">Nenhum handle cadastrado.</p>
                  ) : (
                    <div className="space-y-2">
                      {Object.entries(detail.handles).map(([k, v]) => (
                        <div
                          key={k}
                          className="flex items-center justify-between rounded-md border border-border bg-muted/30 px-3 py-2 text-sm"
                        >
                          <span className="capitalize text-muted-foreground">{k}</span>
                          <span className="font-mono">{v}</span>
                        </div>
                      ))}
                    </div>
                  )}
                </section>

                <section>
                  <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                    Tags
                  </h4>
                  {detail.tags.length === 0 ? (
                    <p className="text-sm text-muted-foreground">Sem tags.</p>
                  ) : (
                    <div className="flex flex-wrap gap-1.5">
                      {detail.tags.map(t => (
                        <Badge key={t} variant="secondary">{t}</Badge>
                      ))}
                    </div>
                  )}
                </section>

                <section>
                  <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                    Audiência
                  </h4>
                  <p className="font-display text-2xl font-semibold tabular-nums">
                    {detail.audienciaTotal?.toLocaleString('pt-BR') ?? '—'}
                  </p>
                </section>

                {detail.observacoes && (
                  <section>
                    <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                      Observações
                    </h4>
                    <p className="text-sm text-foreground whitespace-pre-wrap">
                      {detail.observacoes}
                    </p>
                  </section>
                )}

                <div className="pt-4 border-t border-border flex justify-between items-center text-xs text-muted-foreground">
                  <span>Criado em {new Date(detail.createdAt).toLocaleDateString('pt-BR')}</span>
                  <Button
                    variant="ghost"
                    size="sm"
                    onClick={() => handleDelete(detail.id, detail.nome)}
                    className="text-destructive hover:text-destructive"
                  >
                    <Trash2 className="h-4 w-4" /> Remover
                  </Button>
                </div>
              </div>
            </>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
