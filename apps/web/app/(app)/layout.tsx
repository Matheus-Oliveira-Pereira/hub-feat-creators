'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { type Route } from 'next';
import { auth, clearTokens } from '@/lib/api';

const NAV: { href: Route; label: string; icon: string }[] = [
  { href: '/influenciadores' as Route, label: 'Influenciadores', icon: '👤' },
  { href: '/marcas' as Route, label: 'Marcas', icon: '🏢' },
];

export default function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();

  async function logout() {
    const rt = localStorage.getItem('refreshToken') ?? '';
    try { await auth.logout(rt); } catch {}
    clearTokens();
    router.push('/login');
  }

  return (
    <div className="min-h-screen bg-slate-50 flex">
      <aside className="w-56 bg-white border-r border-slate-200 flex flex-col">
        <div className="px-4 py-5 border-b border-slate-200">
          <span className="font-bold text-slate-900 text-sm">HUB Feat Creator</span>
        </div>
        <nav className="flex-1 p-3 space-y-1">
          {NAV.map(item => {
            const active = pathname.startsWith(item.href);
            return (
              <Link
                key={item.href}
                href={item.href}
                className={`flex items-center gap-2 px-3 py-2 rounded-lg text-sm transition-colors ${
                  active
                    ? 'bg-blue-50 text-blue-700 font-medium'
                    : 'text-slate-600 hover:bg-slate-100'
                }`}
              >
                <span>{item.icon}</span>
                {item.label}
              </Link>
            );
          })}
        </nav>
        <div className="p-3 border-t border-slate-200">
          <button
            onClick={logout}
            className="w-full text-left px-3 py-2 text-sm text-slate-500 hover:text-slate-700 rounded-lg hover:bg-slate-100 transition-colors"
          >
            Sair
          </button>
        </div>
      </aside>
      <main className="flex-1 overflow-auto">{children}</main>
    </div>
  );
}
