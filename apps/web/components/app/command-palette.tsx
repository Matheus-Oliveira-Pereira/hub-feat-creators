'use client';

import * as React from 'react';
import { useRouter } from 'next/navigation';
import { type Route } from 'next';
import {
  LayoutDashboard,
  Users,
  Building2,
  Plus,
  Sun,
  Moon,
  Monitor,
  LogOut,
} from 'lucide-react';
import { useTheme } from 'next-themes';
import {
  CommandDialog,
  CommandEmpty,
  CommandGroup,
  CommandInput,
  CommandItem,
  CommandList,
  CommandSeparator,
  CommandShortcut,
} from '@/components/ui/command';
import { auth, clearTokens } from '@/lib/api';

interface CommandPaletteProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
}

export function CommandPalette({ open, onOpenChange }: CommandPaletteProps) {
  const router = useRouter();
  const { setTheme } = useTheme();

  React.useEffect(() => {
    function handler(e: KeyboardEvent) {
      if ((e.key === 'k' || e.key === 'K') && (e.metaKey || e.ctrlKey)) {
        e.preventDefault();
        onOpenChange(!open);
      }
    }
    document.addEventListener('keydown', handler);
    return () => document.removeEventListener('keydown', handler);
  }, [open, onOpenChange]);

  function go(href: Route) {
    return () => {
      onOpenChange(false);
      router.push(href);
    };
  }

  function newEntity(type: 'influenciador' | 'marca') {
    return () => {
      onOpenChange(false);
      const path = type === 'influenciador' ? '/influenciadores' : '/marcas';
      router.push(`${path}?new=1` as Route);
    };
  }

  async function logout() {
    onOpenChange(false);
    const rt = localStorage.getItem('refreshToken') ?? '';
    try {
      await auth.logout(rt);
    } catch {}
    clearTokens();
    router.push('/login');
  }

  return (
    <CommandDialog open={open} onOpenChange={onOpenChange}>
      <CommandInput placeholder="Buscar páginas, ações…" />
      <CommandList>
        <CommandEmpty>Nada encontrado.</CommandEmpty>
        <CommandGroup heading="Navegar">
          <CommandItem onSelect={go('/' as Route)}>
            <LayoutDashboard /> Visão geral
            <CommandShortcut>G H</CommandShortcut>
          </CommandItem>
          <CommandItem onSelect={go('/influenciadores' as Route)}>
            <Users /> Influenciadores
            <CommandShortcut>G I</CommandShortcut>
          </CommandItem>
          <CommandItem onSelect={go('/marcas' as Route)}>
            <Building2 /> Marcas
            <CommandShortcut>G M</CommandShortcut>
          </CommandItem>
        </CommandGroup>
        <CommandSeparator />
        <CommandGroup heading="Criar">
          <CommandItem onSelect={newEntity('influenciador')}>
            <Plus /> Novo influenciador
          </CommandItem>
          <CommandItem onSelect={newEntity('marca')}>
            <Plus /> Nova marca
          </CommandItem>
        </CommandGroup>
        <CommandSeparator />
        <CommandGroup heading="Tema">
          <CommandItem onSelect={() => setTheme('light')}>
            <Sun /> Tema claro
          </CommandItem>
          <CommandItem onSelect={() => setTheme('dark')}>
            <Moon /> Tema escuro
          </CommandItem>
          <CommandItem onSelect={() => setTheme('system')}>
            <Monitor /> Tema do sistema
          </CommandItem>
        </CommandGroup>
        <CommandSeparator />
        <CommandGroup heading="Conta">
          <CommandItem onSelect={logout}>
            <LogOut /> Sair
          </CommandItem>
        </CommandGroup>
      </CommandList>
    </CommandDialog>
  );
}
