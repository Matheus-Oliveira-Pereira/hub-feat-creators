import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
  variant?: 'full' | 'mark' | 'wordmark';
  size?: 'sm' | 'md' | 'lg' | 'xl';
}

const SIZES = {
  sm: { mark: 24, text: 'text-base' },
  md: { mark: 32, text: 'text-lg' },
  lg: { mark: 40, text: 'text-2xl' },
  xl: { mark: 56, text: 'text-4xl' },
};

function Mark({ size }: { size: number }) {
  return (
    <span
      className="relative inline-flex shrink-0 items-center justify-center rounded-lg bg-foreground"
      style={{ width: size, height: size }}
      aria-hidden="true"
    >
      <svg
        viewBox="0 0 40 40"
        fill="none"
        width={size * 0.65}
        height={size * 0.65}
        xmlns="http://www.w3.org/2000/svg"
      >
        <path
          d="M10 32L16 8h18l-2 6H22l-1.5 5h11l-2 6H19l-1.5 5h12l-2 6H10z"
          fill="hsl(var(--primary))"
        />
        <circle cx="32" cy="30" r="3.5" fill="hsl(var(--primary))" />
      </svg>
    </span>
  );
}

function Wordmark({ size }: { size: keyof typeof SIZES }) {
  return (
    <span
      className={cn(
        'font-display font-bold tracking-tight text-foreground leading-none',
        SIZES[size].text
      )}
    >
      feat<span className="text-primary">.</span> creators
    </span>
  );
}

export function Logo({ className, variant = 'full', size = 'md' }: LogoProps) {
  const config = SIZES[size];
  if (variant === 'mark') return <Mark size={config.mark} />;
  if (variant === 'wordmark') return <Wordmark size={size} />;
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <Mark size={config.mark} />
      <Wordmark size={size} />
    </span>
  );
}
