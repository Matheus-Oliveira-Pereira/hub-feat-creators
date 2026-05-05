'use client';

import * as React from 'react';
import {
  CheckSquare,
  Mail,
  Search,
  Bell,
  MessageCircle,
  UserCheck,
  CheckCheck,
} from 'lucide-react';
import { Sheet, SheetContent, SheetHeader, SheetTitle } from '@/components/ui/sheet';
import { Button } from '@/components/ui/button';
import { Tabs, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { EmptyState } from '@/components/app/empty-state';
import {
  useNotificacoes,
  useMarcarLida,
  useMarcarTodasLidas,
} from '@/lib/queries';
import type { Notificacao } from '@/lib/api';
import { cn } from '@/lib/utils';

function timeAgo(iso: string): string {
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60_000);
  if (m < 1) return 'agora';
  if (m < 60) return `${m}min atrás`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h atrás`;
  const d = Math.floor(h / 24);
  return `${d}d atrás`;
}

const TIPO_ICON: Record<string, React.ElementType> = {
  TAREFA_VENCENDO: CheckSquare,
  TAREFA_ATRASADA: CheckSquare,
  EMAIL_BOUNCED: Mail,
  EMAIL_AUTH_FALHOU: Mail,
  WHATSAPP_FALHOU: MessageCircle,
  PROSPECCAO_MUDOU_STATUS: Search,
  MENTION_COMENTARIO: MessageCircle,
  CONVITE_ACEITO: UserCheck,
  DIGEST_DIARIO: Bell,
};

interface Props {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function NotificationDrawer({ open, onOpenChange }: Props) {
  const [tab, setTab] = React.useState<'todas' | 'nao-lidas'>('todas');
  const apenasNaoLidas = tab === 'nao-lidas';

  const { data, isLoading } = useNotificacoes({ apenasNaoLidas, size: 30 });
  const marcarLida = useMarcarLida();
  const marcarTodasLidas = useMarcarTodasLidas();

  const items = data?.data ?? [];

  return (
    <Sheet open={open} onOpenChange={onOpenChange}>
      <SheetContent side="right" className="flex w-full max-w-md flex-col p-0">
        <SheetHeader className="border-b border-border px-4 py-4">
          <div className="flex items-center justify-between">
            <SheetTitle>Notificações</SheetTitle>
            {items.some(n => !n.lidaEm) && (
              <Button
                variant="ghost"
                size="sm"
                className="text-muted-foreground"
                onClick={() => marcarTodasLidas.mutate()}
              >
                <CheckCheck className="mr-1.5 h-4 w-4" />
                Marcar todas lidas
              </Button>
            )}
          </div>
          <Tabs value={tab} onValueChange={v => setTab(v as typeof tab)}>
            <TabsList className="mt-2">
              <TabsTrigger value="todas">Todas</TabsTrigger>
              <TabsTrigger value="nao-lidas">Não lidas</TabsTrigger>
            </TabsList>
          </Tabs>
        </SheetHeader>

        <div className="flex-1 overflow-y-auto">
          {isLoading ? (
            <div className="flex h-32 items-center justify-center text-sm text-muted-foreground">
              Carregando...
            </div>
          ) : items.length === 0 ? (
            <div className="p-4">
              <EmptyState
                icon={<Bell />}
                title="Nenhuma notificação"
                description={apenasNaoLidas ? 'Tudo lido por aqui.' : 'Você não tem notificações ainda.'}
              />
            </div>
          ) : (
            <ul className="divide-y divide-border">
              {items.map(n => (
                <NotificacaoItem
                  key={n.id}
                  notificacao={n}
                  onRead={() => marcarLida.mutate(n.id)}
                />
              ))}
            </ul>
          )}
        </div>
      </SheetContent>
    </Sheet>
  );
}

function NotificacaoItem({
  notificacao: n,
  onRead,
}: {
  notificacao: Notificacao;
  onRead: () => void;
}) {
  const Icon = TIPO_ICON[n.tipo] ?? Bell;
  const lida = !!n.lidaEm;

  return (
    <li
      className={cn(
        'flex gap-3 px-4 py-3 transition-colors hover:bg-muted/50',
        !lida && 'bg-primary/5'
      )}
      onClick={() => { if (!lida) onRead(); }}
      role={!lida ? 'button' : undefined}
      tabIndex={!lida ? 0 : undefined}
      onKeyDown={e => { if (!lida && (e.key === 'Enter' || e.key === ' ')) onRead(); }}
    >
      <div className={cn(
        'mt-0.5 flex h-8 w-8 shrink-0 items-center justify-center rounded-full',
        lida ? 'bg-muted text-muted-foreground' : 'bg-primary/10 text-primary'
      )}>
        <Icon className="h-4 w-4" />
      </div>
      <div className="min-w-0 flex-1">
        <p className={cn('text-sm', !lida && 'font-medium')}>{n.titulo}</p>
        <p className="mt-0.5 text-xs text-muted-foreground line-clamp-2">{n.mensagem}</p>
        <div className="mt-1 flex items-center gap-2">
          <span className="text-[10px] text-muted-foreground">
            {timeAgo(n.createdAt)}
          </span>
          {n.agrupadas > 1 && (
            <span className="rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-medium text-muted-foreground">
              +{n.agrupadas - 1}
            </span>
          )}
          {!lida && (
            <span className="ml-auto h-1.5 w-1.5 rounded-full bg-primary" aria-label="não lida" />
          )}
        </div>
      </div>
    </li>
  );
}
