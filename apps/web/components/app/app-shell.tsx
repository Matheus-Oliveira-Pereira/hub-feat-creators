'use client';

import * as React from 'react';
import { motion } from 'framer-motion';
import { Sheet, SheetContent } from '@/components/ui/sheet';
import { Sidebar } from './sidebar';
import { Topbar } from './topbar';
import { CommandPalette } from './command-palette';

const COLLAPSED_KEY = 'hub:sidebar-collapsed';

export function AppShell({ children }: { children: React.ReactNode }) {
  const [collapsed, setCollapsed] = React.useState(false);
  const [paletteOpen, setPaletteOpen] = React.useState(false);
  const [mobileOpen, setMobileOpen] = React.useState(false);

  React.useEffect(() => {
    const stored = localStorage.getItem(COLLAPSED_KEY);
    if (stored === '1') setCollapsed(true);
  }, []);

  function toggleCollapsed() {
    setCollapsed(prev => {
      const next = !prev;
      localStorage.setItem(COLLAPSED_KEY, next ? '1' : '0');
      return next;
    });
  }

  return (
    <div className="flex min-h-screen bg-background">
      <Sidebar
        collapsed={collapsed}
        onToggle={toggleCollapsed}
        onCommandOpen={() => setPaletteOpen(true)}
      />

      <Sheet open={mobileOpen} onOpenChange={setMobileOpen}>
        <SheetContent side="left" className="w-72 p-0">
          <div className="md:hidden">
            <Sidebar
              collapsed={false}
              onToggle={() => setMobileOpen(false)}
              onCommandOpen={() => {
                setMobileOpen(false);
                setPaletteOpen(true);
              }}
            />
          </div>
        </SheetContent>
      </Sheet>

      <div className="flex flex-1 flex-col min-w-0">
        <Topbar onMobileMenuOpen={() => setMobileOpen(true)} />
        <motion.main
          key="content"
          initial={{ opacity: 0, y: 6 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.25, ease: [0.4, 0, 0.2, 1] }}
          className="flex-1 overflow-x-hidden"
        >
          {children}
        </motion.main>
      </div>

      <CommandPalette open={paletteOpen} onOpenChange={setPaletteOpen} />
    </div>
  );
}
