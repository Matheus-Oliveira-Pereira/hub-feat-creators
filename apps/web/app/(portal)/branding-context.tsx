'use client';

import * as React from 'react';
import type { AssessoriaBranding } from '@/lib/api';

export const BrandingContext = React.createContext<AssessoriaBranding | null>(null);

export function useBranding() {
  return React.useContext(BrandingContext);
}
