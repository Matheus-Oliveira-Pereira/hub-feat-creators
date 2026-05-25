'use client';

import * as React from 'react';
import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useAuth, isAdmClaims } from '@/lib/auth';
import { cn } from '@/lib/utils';

const NAV_ITEMS = [
  { href: '/admin', label: 'Dashboard', exact: true },
  { href: '/admin/assessorias', label: 'Assessorias' },
  { href: '/admin/usuarios', label: 'Usuários' },
  { href: '/admin/feature-flags', label: 'Feature Flags' },
  { href: '/admin/audit', label: 'Audit Log' },
  { href: '/admin/integrations/email', label: 'E-mail' },
  { href: '/admin/integrations/whatsapp', label: 'WhatsApp' },
] as const;

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const { claims } = useAuth();
  const router = useRouter();
  const pathname = usePathname();

  React.useEffect(() => {
    if (claims !== null && !isAdmClaims(claims)) {
      router.replace('/');
    }
  }, [claims, router]);

  if (claims === null) return null;
  if (!isAdmClaims(claims)) return null;

  return (
    <div className="flex min-h-screen bg-muted/30">
      <aside className="w-56 border-r bg-background flex flex-col">
        <div className="flex items-center gap-2 px-4 py-4 border-b">
          <span className="text-xs font-bold bg-destructive text-destructive-foreground px-2 py-0.5 rounded">
            ADMIN
          </span>
          <span className="text-sm font-semibold text-foreground">Plataforma</span>
        </div>
        <nav className="flex-1 py-4 px-2 space-y-1">
          {NAV_ITEMS.map(item => {
            const active = item.exact ? pathname === item.href : pathname.startsWith(item.href) && item.href !== '/admin';
            return (
              <Link
                key={item.href}
                href={item.href}
                className={cn(
                  'block px-3 py-2 rounded text-sm transition-colors',
                  active
                    ? 'bg-destructive/10 text-destructive font-medium'
                    : 'text-muted-foreground hover:text-foreground hover:bg-muted'
                )}
              >
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="p-3 border-t text-xs text-muted-foreground">
          {claims.usuarioId.slice(0, 8)}…
        </div>
      </aside>
      <main className="flex-1 overflow-auto p-6">{children}</main>
    </div>
  );
}
