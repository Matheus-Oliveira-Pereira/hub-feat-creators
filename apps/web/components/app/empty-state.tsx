import * as React from 'react';
import { cn } from '@/lib/utils';

interface EmptyStateProps {
  icon?: React.ReactNode;
  illustration?: React.ReactNode;
  title: string;
  description?: string;
  action?: React.ReactNode;
  className?: string;
}

export function EmptyState({
  icon,
  illustration,
  title,
  description,
  action,
  className,
}: EmptyStateProps) {
  return (
    <div
      className={cn(
        'flex flex-col items-center justify-center rounded-xl border border-dashed border-border bg-card/50 px-6 py-16 text-center',
        className
      )}
    >
      {illustration ?? (
        <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-2xl bg-muted text-muted-foreground">
          {icon}
        </div>
      )}
      <h3 className="font-display text-lg font-semibold text-foreground">{title}</h3>
      {description && (
        <p className="mt-1.5 max-w-sm text-sm text-muted-foreground">{description}</p>
      )}
      {action && <div className="mt-5">{action}</div>}
    </div>
  );
}

export function EmptyIllustration({ variant = 'sparkles' }: { variant?: 'sparkles' | 'inbox' | 'chart' }) {
  if (variant === 'inbox') {
    return (
      <svg
        viewBox="0 0 200 140"
        className="mb-4 h-32 w-auto"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <rect x="30" y="40" width="140" height="78" rx="10" fill="hsl(var(--muted))" />
        <rect x="40" y="55" width="120" height="48" rx="6" fill="hsl(var(--card))" stroke="hsl(var(--border))" />
        <circle cx="100" cy="36" r="14" fill="hsl(var(--primary))" />
        <path d="M93 36l5 5 9-9" stroke="hsl(var(--primary-foreground))" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
        <line x1="55" y1="68" x2="120" y2="68" stroke="hsl(var(--border-strong))" strokeWidth="2" strokeLinecap="round" />
        <line x1="55" y1="78" x2="100" y2="78" stroke="hsl(var(--border))" strokeWidth="2" strokeLinecap="round" />
        <line x1="55" y1="88" x2="135" y2="88" stroke="hsl(var(--border))" strokeWidth="2" strokeLinecap="round" />
      </svg>
    );
  }
  if (variant === 'chart') {
    return (
      <svg
        viewBox="0 0 200 140"
        className="mb-4 h-32 w-auto"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        aria-hidden="true"
      >
        <rect x="20" y="20" width="160" height="100" rx="10" fill="hsl(var(--muted))" />
        <rect x="40" y="80" width="20" height="30" rx="3" fill="hsl(var(--border-strong))" />
        <rect x="70" y="60" width="20" height="50" rx="3" fill="hsl(var(--border-strong))" />
        <rect x="100" y="40" width="20" height="70" rx="3" fill="hsl(var(--primary))" />
        <rect x="130" y="55" width="20" height="55" rx="3" fill="hsl(var(--border-strong))" />
      </svg>
    );
  }
  return (
    <svg
      viewBox="0 0 200 140"
      className="mb-4 h-32 w-auto"
      fill="none"
      xmlns="http://www.w3.org/2000/svg"
      aria-hidden="true"
    >
      <circle cx="100" cy="70" r="44" fill="hsl(var(--muted))" />
      <path
        d="M100 40l6 18 18 6-18 6-6 18-6-18-18-6 18-6z"
        fill="hsl(var(--primary))"
      />
      <circle cx="60" cy="40" r="3" fill="hsl(var(--primary))" opacity="0.6" />
      <circle cx="150" cy="100" r="3" fill="hsl(var(--primary))" opacity="0.6" />
      <circle cx="160" cy="50" r="2" fill="hsl(var(--primary))" opacity="0.4" />
    </svg>
  );
}
