import { cn } from '@/lib/utils';

interface LogoProps {
  className?: string;
  variant?: 'full' | 'mark' | 'wordmark';
  size?: 'sm' | 'md' | 'lg' | 'xl';
  tone?: 'default' | 'invert';
}

const SIZES = {
  sm: { mark: 24, text: 'text-base' },
  md: { mark: 32, text: 'text-lg' },
  lg: { mark: 40, text: 'text-2xl' },
  xl: { mark: 56, text: 'text-4xl' },
};

function Mark({ size, tone }: { size: number; tone: 'default' | 'invert' }) {
  return (
    <span
      className={cn(
        'relative inline-flex shrink-0 items-center justify-center rounded-lg',
        tone === 'invert' ? 'bg-background' : 'bg-foreground'
      )}
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

function Wordmark({
  size,
  tone,
}: {
  size: keyof typeof SIZES;
  tone: 'default' | 'invert';
}) {
  return (
    <span
      className={cn(
        'font-display font-bold tracking-tight leading-none',
        tone === 'invert' ? 'text-background' : 'text-foreground',
        SIZES[size].text
      )}
    >
      feat<span className="text-primary">.</span> creators
    </span>
  );
}

export function Logo({ className, variant = 'full', size = 'md', tone = 'default' }: LogoProps) {
  const config = SIZES[size];
  if (variant === 'mark') return <Mark size={config.mark} tone={tone} />;
  if (variant === 'wordmark') return <Wordmark size={size} tone={tone} />;
  return (
    <span className={cn('inline-flex items-center gap-2.5', className)}>
      <Mark size={config.mark} tone={tone} />
      <Wordmark size={size} tone={tone} />
    </span>
  );
}
