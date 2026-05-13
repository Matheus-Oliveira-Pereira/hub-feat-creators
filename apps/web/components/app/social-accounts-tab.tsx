'use client';

import * as React from 'react';
import { toast } from 'sonner';
import { Loader2, Link2Off, ExternalLink } from 'lucide-react';
import { useSocialAccounts, useDisconnectSocial } from '@/lib/queries';
import { social } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';

const PLATAFORMAS = ['INSTAGRAM', 'YOUTUBE', 'TIKTOK'] as const;

const STATUS_LABEL: Record<string, { label: string; variant: 'default' | 'secondary' | 'destructive' | 'outline' }> = {
  ATIVA: { label: 'Ativa', variant: 'default' },
  TOKEN_EXPIRADO: { label: 'Token expirado', variant: 'destructive' },
  REVOGADA: { label: 'Revogada', variant: 'secondary' },
  ERRO: { label: 'Erro', variant: 'destructive' },
};

function fmtDate(iso: string | null) {
  if (!iso) return '—';
  return new Date(iso).toLocaleDateString('pt-BR', { day: '2-digit', month: '2-digit', year: 'numeric' });
}

export function SocialAccountsTab({ influenciadorId }: { influenciadorId: string }) {
  const { data: accounts = [], isLoading } = useSocialAccounts(influenciadorId);
  const disconnect = useDisconnectSocial();

  async function handleDisconnect(accountId: string, plataforma: string) {
    if (!confirm(`Desconectar conta ${plataforma}? Os dados históricos serão mantidos.`)) return;
    try {
      await disconnect.mutateAsync(accountId);
      toast.success('Conta desconectada');
    } catch {
      toast.error('Erro ao desconectar');
    }
  }

  const connected = new Set(accounts.filter(a => a.status !== 'REVOGADA').map(a => a.plataforma));

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-8">
        <Loader2 className="h-5 w-5 animate-spin text-muted-foreground" />
      </div>
    );
  }

  return (
    <div className="mt-2 space-y-4">
      {/* Active / errored accounts */}
      {accounts.filter(a => a.status !== 'REVOGADA').map(acc => {
        const s = STATUS_LABEL[acc.status] ?? { label: acc.status, variant: 'outline' as const };
        return (
          <div
            key={acc.id}
            className="flex items-center justify-between rounded-md border border-border bg-muted/30 px-3 py-2.5"
          >
            <div className="flex flex-col gap-0.5">
              <div className="flex items-center gap-2">
                <span className="text-sm font-medium capitalize">{acc.plataforma.toLowerCase()}</span>
                {acc.handle && (
                  <span className="text-xs font-mono text-muted-foreground">@{acc.handle}</span>
                )}
                <Badge variant={s.variant} className="text-[10px] px-1.5 py-0">
                  {s.label}
                </Badge>
              </div>
              <span className="text-[11px] text-muted-foreground">
                Último sync: {fmtDate(acc.ultimoSyncEm)}
              </span>
            </div>
            <Button
              variant="ghost"
              size="sm"
              className="text-destructive hover:text-destructive"
              onClick={() => handleDisconnect(acc.id, acc.plataforma)}
              disabled={disconnect.isPending}
            >
              <Link2Off className="h-3.5 w-3.5 mr-1" />
              Desconectar
            </Button>
          </div>
        );
      })}

      {/* Connect buttons for unconnected platforms */}
      <section>
        <h4 className="text-xs font-medium uppercase tracking-wider text-muted-foreground mb-2">
          Conectar nova conta
        </h4>
        <div className="flex flex-wrap gap-2">
          {PLATAFORMAS.filter(p => !connected.has(p)).map(p => (
            <a
              key={p}
              href={social.startOAuth(influenciadorId, p)}
              target="_blank"
              rel="noreferrer"
            >
              <Button variant="outline" size="sm" className="gap-1.5">
                <ExternalLink className="h-3.5 w-3.5" />
                {p.charAt(0) + p.slice(1).toLowerCase()}
              </Button>
            </a>
          ))}
          {PLATAFORMAS.every(p => connected.has(p)) && (
            <p className="text-sm text-muted-foreground">Todas as plataformas conectadas.</p>
          )}
        </div>
      </section>
    </div>
  );
}
