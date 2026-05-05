'use client';

import * as React from 'react';
import { Bell } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { useNotificacaoContagem, useNotificacaoSSE } from '@/lib/queries';
import { NotificationDrawer } from './notification-drawer';

export function NotificationBell() {
  const [open, setOpen] = React.useState(false);
  const { data } = useNotificacaoContagem();
  const count = data?.naoLidas ?? 0;

  useNotificacaoSSE(true);

  return (
    <>
      <Button
        variant="ghost"
        size="icon"
        className="relative"
        onClick={() => setOpen(true)}
        aria-label={`Notificações${count > 0 ? ` (${count} não lidas)` : ''}`}
      >
        <Bell className="h-5 w-5" />
        {count > 0 && (
          <span
            className="absolute right-1 top-1 flex h-4 min-w-4 items-center justify-center rounded-full bg-primary px-1 text-[10px] font-bold text-primary-foreground"
            aria-live="polite"
          >
            {count > 99 ? '99+' : count}
          </span>
        )}
      </Button>
      <NotificationDrawer open={open} onOpenChange={setOpen} />
    </>
  );
}
