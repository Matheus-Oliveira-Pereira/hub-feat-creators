'use client';

import * as React from 'react';
import { usePermission } from '@/lib/auth';

interface CanProps {
  /** Code 4-letter ou array (any-of). */
  role: string | string[];
  fallback?: React.ReactNode;
  children: React.ReactNode;
}

/**
 * Renderiza children se o usuário autenticado tem ao menos uma das permissões.
 * OWNER coarse e role OWNR bypassam.
 *
 * <Can role="CPRO"><Button>+ Adicionar</Button></Can>
 * <Can role={['BPRO','BREL']} fallback={<Disabled />}>...</Can>
 */
export function Can({ role, fallback = null, children }: CanProps) {
  const ok = usePermission(role);
  return <>{ok ? children : fallback}</>;
}
