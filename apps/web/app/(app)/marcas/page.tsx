'use client';

import * as React from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import {
  Plus,
  Download,
  Globe,
  MoreHorizontal,
  Trash2,
  Eye,
  Pencil,
  ExternalLink,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { toast } from 'sonner';
import { Marca } from '@/lib/api';
import { useMarcas, useDeleteMarca } from '@/lib/queries';
import { Button } from '@/components/ui/button';
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
import { EmptyIllustration, EmptyState } from '@/components/app/empty-state';
import { MarcaFormModal } from '@/components/forms/marca-form-modal';

const API_URL = process.env.NEXT_PUBLIC_API_URL ?? 'http://localhost:8080';

function initials(nome: string) {
  return nome
    .split(' ')
    .filter(Boolean)
    .slice(0, 2)
    .map(s => s[0]?.toUpperCase())
    .join('');
}

function hostname(url: string | null) {
  if (!url) return null;
  try {
    return new URL(url.startsWith('http') ? url : `https://${url}`).hostname.replace(/^www\./, '');
  } catch {
    return url;
  }
}

export default function MarcasPage() {
  return (
    <React.Suspense fallback={null}>
      <MarcasInner />
    </React.Suspense>
  );
}

function MarcasInner() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [search, setSearch] = React.useState('');
  const [debounced, setDebounced] = React.useState('');
  const [view, setView] = React.useState<'cards' | 'table'>('cards');
  const [formState, setFormState] = React.useState<{ open: boolean; item: Marca | null }>({
    open: false,
    item: null,
  });
  const [detail, setDetail] = React.useState<Marca | null>(null);

  React.useEffect(() => {
    const t = setTimeout(() => setDebounced(search), 250);
    return () => clearTimeout(t);
  }, [search]);

  const query = useMarcas({ nome: debounced || undefined });
  const data = React.useMemo(
    () => query.data?.pages.flatMap(p => p.data) ?? [],
    [query.data]
  );

  React.useEffect(() => {
    if (query.error) router.push('/login');
  }, [query.error, router]);

  React.useEffect(() => {
    if (searchParams.get('new') === '1') {
      setFormState({ open: true, item: null });
      router.replace('/marcas');
    }
  }, [searchParams, router]);

  const del = useDeleteMarca();
  async function handleDelete(id: string, nome: string) {
    if (!confirm(`Remover "${nome}"?`)) return;
    try {
      await del.mutateAsync(id);
      toast.success('Removida.');
      setDetail(null);
    } catch (err: any) {
      toast.error(err?.error?.message ?? 'Erro ao remover.');
    }
  }

  function openCreate() {
    setFormState({ open: true, item: null });
  }
  function openEdit(item: Marca) {
    setDetail(null);
    setFormState({ open: true, item });
  }

  const loading = query.isLoading;

  return (
    <div className="mx-auto w-full max-w-7xl px-4 py-8 md:px-8 md:py-12">
      <PageHeader
        eyebrow="Cadastros"
        title="Marcas"
        description="Empresas e parceiros que você atende ou prospecta."
        actions={
          <>
            <Button asChild variant="outline">
              <a href={`${API_URL}/api/v1/marcas/export.csv`}>
                <Download className="h-4 w-4" /> Exportar CSV
              </a>
            </Button>
            <Button onClick={openCreate}>
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
        countLabel={data.length === 1 ? 'marca' : 'marcas'}
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
          title={debounced ? 'Nada encontrado' : 'Nenhuma marca ainda'}
          description={
            debounced
              ? `Sem resultados para "${debounced}". Tente outro termo.`
              : 'Cadastre a primeira marca para organizar a operação.'
          }
          action={
            !debounced && (
              <Button onClick={openCreate}>
                <Plus className="h-4 w-4" /> Adicionar marca
              </Button>
            )
          }
        />
      ) : view === 'cards' ? (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          <AnimatePresence mode="popLayout">
            {data.map((marca, i) => (
              <motion.div
                key={marca.id}
                layout
                initial={{ opacity: 0, y: 8 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.96 }}
                transition={{ duration: 0.2, delay: Math.min(i * 0.02, 0.2) }}
              >
                <Card
                  className="group cursor-pointer p-5 transition-all hover:border-border-strong hover:shadow-md"
                  onClick={() => setDetail(marca)}
                >
                  <div className="flex items-start justify-between gap-3">
                    <div className="flex items-center gap-3 min-w-0">
                      <Avatar className="h-11 w-11 shrink-0 rounded-lg">
                        <AvatarFallback className="rounded-lg bg-primary/10 text-foreground font-semibold">
                          {initials(marca.nome)}
                        </AvatarFallback>
                      </Avatar>
                      <div className="min-w-0">
                        <p className="font-display font-semibold truncate">{marca.nome}</p>
                        <p className="text-xs text-muted-foreground truncate">
                          {marca.segmento ?? 'Sem segmento'}
                        </p>
                      </div>
                    </div>
                    <DropdownMenu>
                      <DropdownMenuTrigger asChild onClick={e => e.stopPropagation()}>
                        <Button
                          variant="ghost"
                          size="icon-sm"
                          className="opacity-0 group-hover:opacity-100 transition-opacity"
                        >
                          <MoreHorizontal className="h-4 w-4" />
                        </Button>
                      </DropdownMenuTrigger>
                      <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                        <DropdownMenuItem onClick={() => setDetail(marca)}>
                          <Eye className="h-4 w-4" /> Detalhes
                        </DropdownMenuItem>
                        <DropdownMenuItem onClick={() => openEdit(marca)}>
                          <Pencil className="h-4 w-4" /> Editar
                        </DropdownMenuItem>
                        <DropdownMenuItem
                          onClick={() => handleDelete(marca.id, marca.nome)}
                          className="text-destructive focus:text-destructive"
                        >
                          <Trash2 className="h-4 w-4" /> Remover
                        </DropdownMenuItem>
                      </DropdownMenuContent>
                    </DropdownMenu>
                  </div>
                  <div className="mt-4 flex flex-wrap items-center gap-2 text-xs">
                    {marca.site && (
                      <Badge variant="outline" className="font-mono">
                        <Globe className="h-3 w-3 mr-1" />
                        {hostname(marca.site)}
                      </Badge>
                    )}
                    {marca.tags.slice(0, 2).map(t => (
                      <Badge key={t} variant="secondary">
                        {t}
                      </Badge>
                    ))}
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
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Segmento</th>
                  <th className="text-left px-5 py-3 font-medium text-muted-foreground">Site</th>
                  <th className="px-5 py-3 w-10"></th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border">
                {data.map(marca => (
                  <tr
                    key={marca.id}
                    className="cursor-pointer transition-colors hover:bg-accent/50"
                    onClick={() => setDetail(marca)}
                  >
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-3">
                        <Avatar className="h-7 w-7 rounded-md">
                          <AvatarFallback className="rounded-md bg-primary/10 text-foreground text-xs font-semibold">
                            {initials(marca.nome)}
                          </AvatarFallback>
                        </Avatar>
                        <span className="font-medium">{marca.nome}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-muted-foreground">{marca.segmento ?? '—'}</td>
                    <td className="px-5 py-3">
                      {marca.site ? (
                        <a
                          href={marca.site.startsWith('http') ? marca.site : `https://${marca.site}`}
                          target="_blank"
                          rel="noopener noreferrer"
                          onClick={e => e.stopPropagation()}
                          className="inline-flex items-center gap-1 text-primary hover:underline font-mono text-xs"
                        >
                          {hostname(marca.site)} <ExternalLink className="h-3 w-3" />
                        </a>
                      ) : (
                        <span className="text-muted-foreground">—</span>
                      )}
                    </td>
                    <td className="px-5 py-3 text-right">
                      <DropdownMenu>
                        <DropdownMenuTrigger asChild onClick={e => e.stopPropagation()}>
                          <Button variant="ghost" size="icon-sm">
                            <MoreHorizontal className="h-4 w-4" />
                          </Button>
                        </DropdownMenuTrigger>
                        <DropdownMenuContent align="end" onClick={e => e.stopPropagation()}>
                          <DropdownMenuItem onClick={() => setDetail(marca)}>
                            <Eye className="h-4 w-4" /> Detalhes
                          </DropdownMenuItem>
                          <DropdownMenuItem onClick={() => openEdit(marca)}>
                            <Pencil className="h-4 w-4" /> Editar
                          </DropdownMenuItem>
                          <DropdownMenuItem
                            onClick={() => handleDelete(marca.id, marca.nome)}
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

      {query.hasNextPage && (
        <div className="mt-6 flex justify-center">
          <Button
            variant="outline"
            onClick={() => query.fetchNextPage()}
            disabled={query.isFetchingNextPage}
          >
            {query.isFetchingNextPage ? 'Carregando…' : 'Carregar mais'}
          </Button>
        </div>
      )}

      <MarcaFormModal
        open={formState.open}
        onOpenChange={open => setFormState(prev => ({ ...prev, open }))}
        marca={formState.item}
      />

      <Sheet open={!!detail} onOpenChange={open => !open && setDetail(null)}>
        <SheetContent>
          {detail && (
            <>
              <SheetHeader>
                <div className="flex items-center gap-3 mb-3">
                  <Avatar className="h-14 w-14 rounded-xl">
                    <AvatarFallback className="rounded-xl bg-primary/10 text-foreground text-lg font-bold">
                      {initials(detail.nome)}
                    </AvatarFallback>
                  </Avatar>
                  <div>
                    <SheetTitle>{detail.nome}</SheetTitle>
                    <SheetDescription>
                      {detail.segmento ?? 'Sem segmento definido'}
                    </SheetDescription>
                  </div>
                </div>
              </SheetHeader>

              <div className="mt-6 space-y-5">
                {detail.site && (
                  <section>
                    <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                      Site
                    </h4>
                    <a
                      href={detail.site.startsWith('http') ? detail.site : `https://${detail.site}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="inline-flex items-center gap-1.5 rounded-md border border-border bg-muted/30 px-3 py-2 text-sm font-mono hover:bg-muted hover:text-primary transition-colors"
                    >
                      {hostname(detail.site)}
                      <ExternalLink className="h-3.5 w-3.5" />
                    </a>
                  </section>
                )}

                <section>
                  <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
                    Tags
                  </h4>
                  {detail.tags.length === 0 ? (
                    <p className="text-sm text-muted-foreground">Sem tags.</p>
                  ) : (
                    <div className="flex flex-wrap gap-1.5">
                      {detail.tags.map(t => (
                        <Badge key={t} variant="secondary">
                          {t}
                        </Badge>
                      ))}
                    </div>
                  )}
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
                  <span>Criada em {new Date(detail.createdAt).toLocaleDateString('pt-BR')}</span>
                  <div className="flex gap-1">
                    <Button variant="ghost" size="sm" onClick={() => openEdit(detail)}>
                      <Pencil className="h-4 w-4" /> Editar
                    </Button>
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
              </div>
            </>
          )}
        </SheetContent>
      </Sheet>
    </div>
  );
}
