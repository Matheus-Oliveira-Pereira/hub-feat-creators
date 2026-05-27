import { create } from 'zustand';

export interface CreatorClaims {
  creatorUserId: string;
  influenciadorId: string;
  email?: string;
}

interface AuthState {
  token: string | null;
  claims: CreatorClaims | null;
  isLoading: boolean;
  setToken: (token: string, claims: CreatorClaims) => void;
  clearAuth: () => void;
  setLoading: (loading: boolean) => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  token: null,
  claims: null,
  isLoading: true,
  setToken: (token, claims) => set({ token, claims, isLoading: false }),
  clearAuth: () => set({ token: null, claims: null, isLoading: false }),
  setLoading: (isLoading) => set({ isLoading }),
}));
