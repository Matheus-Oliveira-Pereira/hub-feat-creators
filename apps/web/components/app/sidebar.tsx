'use client';

import * as React from 'react';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { type Route } from 'next';
import {
  ChevronsLeft,
  ChevronsRight,
  LayoutDashboard,
  Users,
  Building2,
  Search,
  Mail,
  CheckSquare,
} from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { ShieldCheck } from 'lucide-react';
import { cn } from '@/lib/utils';
import { Logo } from '@/components/brand/logo';
import { Tooltip, TooltipContent, TooltipTrigger, TooltipProvider } from '@/components/ui/tooltip';
import { useAuth } from '@/lib/auth';
import { useTarefaAlerta } from '@/lib/queries';

interface NavItem {
  href: Route;
  label: string;
  icon: React.ElementType;
  badge?: string;
  disabled?: boolean;
  /** Permissão necessária pra exibir item (any-of). Vazio = sempre exibir. */
  requires?: string[];
}

const NAV: NavItem[] = [
  { href: '/' as Route, label: 'Visão geral', icon: LayoutDashboard, requires: ['BREL'] },
  { href: '/influenciadores' as Route, label: 'Influenciadores', icon: Users, requires: ['BINF'] },
  { href: '/marcas' as Route, label: 'Marcas', icon: Building2, requires: ['BMAR'] },
  { href: '/prospeccao' as Route, label: 'Prospecção', icon: Search, requires: ['BPRO'] },
  { href: '/tarefas' as Route, label: 'Tarefas', icon: CheckSquare, requires: ['BTAR'] },
  { href: '/email' as Route, label: 'E-mail', icon: Mail, badge: 'em breve', disabled: true, requires: ['BEML'] },
  { href: '/perfis' as Route, label: 'Perfis', icon: ShieldCheck, requires: ['BPRF'] },
];

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
  onCommandOpen: () => void;
}

export function Sidebar({ collapsed, onToggle, onCommandOpen }: SidebarProps) {
  const pathname = usePathname();
  const { hasPermission } = useAuth();
  const visibleNav = NAV.filter(item => !item.requires || hasPermission(item.requires));
  const { data: alerta } = useTarefaAlerta();
  const alertaCount = alerta?.count ?? 0;

  return (
    <TooltipProvider delayDuration={150}>
      <aside
        className={cn(
          'sticky top-0 hidden md:flex h-screen flex-col border-r border-border bg-card/50 backdrop-blur transition-[width] duration-200 ease-out',
          collapsed ? 'w-[64px]' : 'w-[248px]'
        )}
      >
        <div className={cn('flex h-16 items-center border-b border-border', collapsed ? 'justify-center px-2' : 'justify-between px-4')}>
          {collapsed ? (
            <Logo variant="mark" size="sm" />
          ) : (
            <Logo variant="full" size="sm" />
          )}
        </div>

        <button
          onClick={onCommandOpen}
          className={cn(
            'mx-3 mt-3 flex items-center gap-2 rounded-lg border border-border bg-muted/40 text-sm text-muted-foreground transition-colors hover:bg-muted hover:text-foreground',
            collapsed ? 'h-9 w-9 justify-center self-center mx-0' : 'h-9 px-3'
          )}
          aria-label="Abrir busca rápida"
        >
          <Search className="h-4 w-4" />
          {!collapsed && (
            <>
              <span className="flex-1 text-left">Buscar…</span>
              <kbd className="rounded border border-border bg-background px-1.5 py-0.5 font-mono text-[10px] text-muted-foreground">
                ⌘K
              </kbd>
            </>
          )}
        </button>

        <nav className={cn('flex-1 space-y-1 mt-4', collapsed ? 'px-2' : 'px-3')}>
          {visibleNav.map(item => {
            const active = item.href === '/' ? pathname === '/' : pathname.startsWith(item.href);
            const Icon = item.icon;
            const content = (
              <Link
                href={item.disabled ? ('#' as Route) : item.href}
                aria-disabled={item.disabled}
                onClick={e => {
                  if (item.disabled) e.preventDefault();
                }}
                className={cn(
                  'group relative flex items-center gap-3 rounded-lg text-sm font-medium transition-colors',
                  collapsed ? 'h-9 w-9 justify-center' : 'px-3 py-2',
                  active && !item.disabled
                    ? 'bg-primary/10 text-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-foreground',
                  item.disabled && 'opacity-50 cursor-not-allowed hover:bg-transparent'
                )}
              >
                {active && !item.disabled && (
                  <motion.span
                    layoutId="sidebar-active-indicator"
                    className="absolute left-0 top-1/2 h-5 w-0.5 -translate-y-1/2 rounded-r-full bg-primary"
                    transition={{ type: 'spring', stiffness: 350, damping: 30 }}
                  />
                )}
                <Icon className="h-4 w-4 shrink-0" />
                <AnimatePresence initial={false}>
                  {!collapsed && (
                    <motion.span
                      initial={{ opacity: 0, width: 0 }}
                      animate={{ opacity: 1, width: 'auto' }}
                      exit={{ opacity: 0, width: 0 }}
                      transition={{ duration: 0.15 }}
                      className="truncate"
                    >
                      {item.label}
                    </motion.span>
                  )}
                </AnimatePresence>
                {!collapsed && item.badge && (
                  <span className="ml-auto rounded-full bg-muted px-1.5 py-0.5 text-[10px] font-medium uppercase tracking-wide text-muted-foreground">
                    {item.badge}
                  </span>
                )}
                {!collapsed && !item.badge && item.href === '/tarefas' && alertaCount > 0 && (
                  <span className="ml-auto flex h-5 min-w-5 items-center justify-center rounded-full bg-red-500 px-1 text-[10px] font-bold text-white">
                    {alertaCount > 99 ? '99+' : alertaCount}
                  </span>
                )}
              </Link>
            );
            return collapsed ? (
              <Tooltip key={item.href}>
                <TooltipTrigger asChild>{content}</TooltipTrigger>
                <TooltipContent side="right" className="font-medium">
                  {item.label}
                  {item.badge && <span className="ml-1 text-muted-foreground">· {item.badge}</span>}
                </TooltipContent>
              </Tooltip>
            ) : (
              <React.Fragment key={item.href}>{content}</React.Fragment>
            );
          })}
        </nav>

        <div className={cn('border-t border-border', collapsed ? 'p-2' : 'p-3')}>
          <button
            onClick={onToggle}
            className={cn(
              'flex h-9 items-center gap-2 rounded-lg text-sm text-muted-foreground transition-colors hover:bg-accent hover:text-foreground',
              collapsed ? 'w-9 justify-center' : 'w-full px-3'
            )}
            aria-label={collapsed ? 'Expandir sidebar' : 'Colapsar sidebar'}
          >
            {collapsed ? (
              <ChevronsRight className="h-4 w-4" />
            ) : (
              <>
                <ChevronsLeft className="h-4 w-4" />
                <span>Colapsar</span>
              </>
            )}
          </button>
        </div>
      </aside>
    </TooltipProvider>
  );
}
