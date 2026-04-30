import { Logo } from '@/components/brand/logo';

export default function AuthLayout({ children }: { children: React.ReactNode }) {
  return (
    <div className="min-h-screen grid lg:grid-cols-[1fr_minmax(0,560px)]">
      <aside className="relative hidden overflow-hidden bg-foreground text-background lg:block">
        <div className="absolute inset-0 gradient-mesh opacity-40" aria-hidden="true" />
        <div
          className="absolute -bottom-32 -left-24 h-[480px] w-[480px] rounded-full opacity-30 blur-3xl"
          style={{ background: 'radial-gradient(circle, hsl(var(--primary)) 0%, transparent 70%)' }}
          aria-hidden="true"
        />
        <div
          className="absolute -top-40 -right-20 h-[420px] w-[420px] rounded-full opacity-25 blur-3xl"
          style={{ background: 'radial-gradient(circle, hsl(var(--primary)) 0%, transparent 70%)' }}
          aria-hidden="true"
        />

        <div className="relative z-10 flex h-full flex-col p-12">
          <Logo variant="full" size="md" tone="invert" />

          <div className="mt-auto max-w-lg space-y-6">
            <p className="font-display text-5xl font-bold leading-[1.05] tracking-tight">
              O HUB que organiza
              <br />
              sua assessoria de
              <span className="text-primary"> creators.</span>
            </p>
            <p className="text-base text-background/70 leading-relaxed">
              Centralize cadastros, prospecção, tarefas e e-mails — tudo em um só lugar,
              feito sob medida para quem trabalha com criadores de conteúdo.
            </p>
            <div className="flex items-center gap-6 text-xs uppercase tracking-widest text-background/50">
              <span>LGPD pronto</span>
              <span className="h-px w-6 bg-background/20" aria-hidden="true" />
              <span>Multi-tenant</span>
              <span className="h-px w-6 bg-background/20" aria-hidden="true" />
              <span>Mobile em breve</span>
            </div>
          </div>
        </div>
      </aside>

      <main className="flex items-center justify-center bg-background px-6 py-12 sm:px-12">
        <div className="w-full max-w-sm">
          <div className="mb-8 lg:hidden">
            <Logo variant="full" size="md" />
          </div>
          {children}
        </div>
      </main>
    </div>
  );
}
