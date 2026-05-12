import * as SecureStore from 'expo-secure-store';
import { CreatorClaims, useAuthStore } from '@/store/auth';

const TOKEN_KEY = 'creator_token';
const BIOMETRIA_KEY = 'biometria_enabled';

function decodeToken(token: string): CreatorClaims {
  const payload = token.split('.')[1];
  const decoded = JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/')));
  return {
    creatorUserId: decoded.sub,
    assessoriaId: decoded.ass,
    influenciadorId: decoded.inf,
  };
}

export async function loadStoredAuth(): Promise<void> {
  const store = useAuthStore.getState();
  try {
    const token = await SecureStore.getItemAsync(TOKEN_KEY);
    if (token) {
      const claims = decodeToken(token);
      store.setToken(token, claims);
    } else {
      store.clearAuth();
    }
  } catch {
    store.clearAuth();
  }
}

export async function login(
  email: string,
  senha: string,
): Promise<{ mfaRequired?: boolean; token?: string }> {
  const apiUrl = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
  const res = await fetch(`${apiUrl}/api/v1/portal/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, senha }),
  });
  if (!res.ok) {
    const err = await res.json().catch(() => ({}));
    throw new Error(err.message ?? 'Credenciais inválidas');
  }
  const data = await res.json();
  if (data.token) {
    await persistToken(data.token);
  }
  return data;
}

export async function persistToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(TOKEN_KEY, token);
  const claims = decodeToken(token);
  useAuthStore.getState().setToken(token, claims);
}

export async function logout(): Promise<void> {
  await SecureStore.deleteItemAsync(TOKEN_KEY);
  useAuthStore.getState().clearAuth();
}

export async function isBiometriaEnabled(): Promise<boolean> {
  const val = await SecureStore.getItemAsync(BIOMETRIA_KEY);
  return val === 'true';
}

export async function setBiometria(enabled: boolean): Promise<void> {
  await SecureStore.setItemAsync(BIOMETRIA_KEY, enabled ? 'true' : 'false');
}
