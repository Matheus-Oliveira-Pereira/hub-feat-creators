'use client';

import * as React from 'react';
import { useRouter, usePathname } from 'next/navigation';
import Link from 'next/link';
import { type Route } from 'next';
import { LogOut, Menu, ChevronRight } from 'lucide-react';
import { auth, clearTokens } from '@/lib/api';
import { Button } from '@/components/ui/button';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { ThemeToggle } from '@/components/theme-toggle';
import { NotificationBell } from '@/components/app/notification-bell';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuLabel,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';

const SEGMENT_LABEL: Record<string, string> = {
  '/': 'Visão geral',
  '/influenciadores': 'Influenciadores',
  '/marcas': 'Marcas',
  '/prospeccao': 'Prospecção',
  '/tarefas': 'Tarefas',
  '/email': 'E-mail',
};

interface TopbarProps {
  onMobileMenuOpen: () => void;
}

export function Topbar({ onMobileMenuOpen }: TopbarProps) {
  const pathname = usePathname();
  const router = useRouter();

  async function logout() {
    const rt = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') ?? '' : '';
    try {
      await auth.logout(rt);
    } catch {}
    clearTokens();
    router.push('/login');
  }

  const segments = pathname.split('/').filter(Boolean);
  const currentLabel = SEGMENT_LABEL[pathname] ?? segments[segments.length - 1] ?? 'Visão geral';

  return (
    <header className="sticky top-0 z-30 flex h-16 items-center gap-3 border-b border-border bg-background/80 px-4 backdrop-blur md:px-8">
      <Button variant="ghost" size="icon" className="md:hidden" onClick={onMobileMenuOpen} aria-label="Abrir menu">
        <Menu className="h-5 w-5" />
      </Button>
      <nav aria-label="breadcrumb" className="flex items-center gap-1.5 text-sm">
        <Link href={'/' as Route} className="text-muted-foreground hover:text-foreground transition-colors">
          HUB
        </Link>
        {pathname !== '/' && (
          <>
            <ChevronRight className="h-3.5 w-3.5 text-subtle" />
            <span className="font-medium text-foreground capitalize">{currentLabel}</span>
          </>
        )}
      </nav>

      <div className="ml-auto flex items-center gap-1">
        <NotificationBell />
        <ThemeToggle />
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <button className="flex items-center gap-2 rounded-full border border-border bg-card p-0.5 pr-3 text-sm transition-colors hover:bg-accent">
              <Avatar className="h-7 w-7">
                <AvatarFallback className="bg-foreground text-primary text-xs font-bold">
                  FC
                </AvatarFallback>
              </Avatar>
              <span className="font-medium hidden sm:inline">Conta</span>
            </button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end" className="w-56">
            <DropdownMenuLabel>Minha conta</DropdownMenuLabel>
            <DropdownMenuSeparator />
            <DropdownMenuItem onClick={logout}>
              <LogOut className="h-4 w-4" /> Sair
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      </div>
    </header>
  );
}
