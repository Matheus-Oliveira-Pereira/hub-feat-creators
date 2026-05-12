// Design tokens portados de docs/specs/design-system — mesmas cores do web
export const colors = {
  primary: '#C2E000',      // lime brand
  ink: '#141414',          // foreground
  background: '#FFFFFF',
  card: '#F5F5F0',
  muted: '#6B7280',
  border: '#E5E7EB',
  destructive: '#EF4444',
  success: '#16A34A',
  warning: '#F59E0B',

  dark: {
    background: '#141414',
    card: '#1F1F1F',
    foreground: '#FAFAF5',
    border: '#2A2A2A',
    muted: '#6B7280',
  },
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  '2xl': 48,
} as const;

export const radius = {
  sm: 6,
  md: 10,
  lg: 16,
  full: 9999,
} as const;

export const typography = {
  display: 'System',   // Bricolage Grotesque via expo-font quando disponível
  body: 'System',
  mono: 'Courier',
  size: {
    xs: 12,
    sm: 14,
    base: 16,
    lg: 18,
    xl: 20,
    '2xl': 24,
    '3xl': 30,
  },
  weight: {
    regular: '400' as const,
    medium: '500' as const,
    semibold: '600' as const,
    bold: '700' as const,
  },
} as const;
