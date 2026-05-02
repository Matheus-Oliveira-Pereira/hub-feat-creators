'use client';

import * as React from 'react';

export interface AuthClaims {
  usuarioId: string;
  assessoriaId: string;
  role: string;
  permissions: string[];
  exp: number;
}

function base64UrlDecode(input: string): string {
  // adiciona padding e troca caracteres URL-safe → base64 padrão
  const pad = input.length % 4 === 0 ? '' : '='.repeat(4 - (input.length % 4));
  const base64 = (input + pad).replace(/-/g, '+').replace(/_/g, '/');
  if (typeof atob !== 'undefined') {
    const decoded = atob(base64);
    try {
      return decodeURIComponent(escape(decoded));
    } catch {
      return decoded;
    }
  }
  // fallback Node (testes)
  return Buffer.from(base64, 'base64').toString('utf8');
}

export function decodeJwt(token: string | null): AuthClaims | null {
  if (!token) return null;
  const parts = token.split('.');
  if (parts.length !== 3) return null;
  try {
    const payload = JSON.parse(base64UrlDecode(parts[1]));
    const perms: unknown = payload.perms;
    return {
      usuarioId: String(payload.sub ?? ''),
      assessoriaId: String(payload.ass ?? ''),
      role: String(payload.role ?? ''),
      permissions: Array.isArray(perms) ? perms.map(String) : [],
      exp: typeof payload.exp === 'number' ? payload.exp : 0,
    };
  } catch {
    return null;
  }
}

interface AuthContextValue {
  claims: AuthClaims | null;
  permissions: string[];
  hasPermission: (codeOrCodes: string | string[]) => boolean;
  refreshFromStorage: () => void;
}

const AuthContext = React.createContext<AuthContextValue | null>(null);

const ACCESS_TOKEN_KEY = 'accessToken';

function readToken(): string | null {
  if (typeof window === 'undefined') return null;
  return localStorage.getItem(ACCESS_TOKEN_KEY);
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [claims, setClaims] = React.useState<AuthClaims | null>(null);

  const refreshFromStorage = React.useCallback(() => {
    setClaims(decodeJwt(readToken()));
  }, []);

  React.useEffect(() => {
    refreshFromStorage();
    function onStorage(e: StorageEvent) {
      if (e.key === ACCESS_TOKEN_KEY) refreshFromStorage();
    }
    window.addEventListener('storage', onStorage);
    return () => window.removeEventListener('storage', onStorage);
  }, [refreshFromStorage]);

  const permissions = claims?.permissions ?? [];
  const role = claims?.role ?? '';

  const hasPermission = React.useCallback(
    (codeOrCodes: string | string[]) => {
      // OWNER coarse e role OWNR bypassam
      if (role === 'OWNER' || permissions.includes('OWNR')) return true;
      const list = Array.isArray(codeOrCodes) ? codeOrCodes : [codeOrCodes];
      return list.some(c => permissions.includes(c));
    },
    [role, permissions]
  );

  const value = React.useMemo(
    () => ({ claims, permissions, hasPermission, refreshFromStorage }),
    [claims, permissions, hasPermission, refreshFromStorage]
  );

  return React.createElement(AuthContext.Provider, { value }, children);
}

export function useAuth(): AuthContextValue {
  const ctx = React.useContext(AuthContext);
  if (!ctx) throw new Error('useAuth: faltou <AuthProvider>');
  return ctx;
}

export function usePermissions() {
  return useAuth().permissions;
}

export function usePermission(codeOrCodes: string | string[]): boolean {
  const { hasPermission } = useAuth();
  return hasPermission(codeOrCodes);
}
