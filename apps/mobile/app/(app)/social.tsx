import React from 'react';
import {
  View,
  Text,
  FlatList,
  TouchableOpacity,
  StyleSheet,
  ActivityIndicator,
  Alert,
} from 'react-native';
import * as WebBrowser from 'expo-web-browser';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '@/lib/api';
import { colors, spacing, typography, radius } from '@/lib/theme';

const API_URL = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
const PLATAFORMAS = ['INSTAGRAM', 'YOUTUBE', 'TIKTOK'] as const;

type SocialAccount = {
  id: string;
  plataforma: string;
  handle: string | null;
  status: 'ATIVA' | 'TOKEN_EXPIRADO' | 'REVOGADA' | 'ERRO';
  ultimoSyncEm: string | null;
};

const STATUS_COLOR: Record<string, string> = {
  ATIVA: '#22c55e',
  TOKEN_EXPIRADO: '#ef4444',
  REVOGADA: '#6b7280',
  ERRO: '#ef4444',
};

function useSocialAccounts() {
  return useQuery<SocialAccount[]>({
    queryKey: ['portal', 'social'],
    queryFn: () => api.get<SocialAccount[]>('/api/v1/portal/me/social'),
  });
}

function useDisconnect() {
  const qc = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => api.delete(`/api/v1/portal/me/social/${id}`),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['portal', 'social'] }),
  });
}

export default function SocialScreen() {
  const { token } = useAuthStore();
  const { data: accounts = [], isLoading, refetch } = useSocialAccounts();
  const disconnect = useDisconnect();

  const connected = new Set(
    accounts.filter(a => a.status !== 'REVOGADA').map(a => a.plataforma),
  );

  async function handleConnect(plataforma: string) {
    const url = `${API_URL}/api/v1/portal/me/social/auth/${plataforma}/start`;
    const result = await WebBrowser.openAuthSessionAsync(url, 'featcreators://social-callback');
    if (result.type === 'success') refetch();
  }

  function handleDisconnect(account: SocialAccount) {
    Alert.alert(
      'Desconectar',
      `Desconectar conta ${account.plataforma.toLowerCase()}? Os snapshots históricos serão mantidos.`,
      [
        { text: 'Cancelar', style: 'cancel' },
        {
          text: 'Desconectar',
          style: 'destructive',
          onPress: () => disconnect.mutate(account.id),
        },
      ],
    );
  }

  if (isLoading) {
    return (
      <View style={styles.center}>
        <ActivityIndicator color={colors.primary} />
      </View>
    );
  }

  const active = accounts.filter(a => a.status !== 'REVOGADA');
  const unconnected = PLATAFORMAS.filter(p => !connected.has(p));

  return (
    <View style={styles.container}>
      <Text style={styles.heading}>Redes Sociais</Text>

      {active.length === 0 && (
        <Text style={styles.empty}>Nenhuma conta conectada ainda.</Text>
      )}

      <FlatList
        data={active}
        keyExtractor={item => item.id}
        renderItem={({ item }) => (
          <View style={styles.card}>
            <View style={styles.cardLeft}>
              <Text style={styles.platform}>{item.plataforma.toLowerCase()}</Text>
              {item.handle && (
                <Text style={styles.handle}>@{item.handle}</Text>
              )}
              <View style={[styles.dot, { backgroundColor: STATUS_COLOR[item.status] }]} />
              <Text style={styles.sync}>
                {item.ultimoSyncEm
                  ? `Sync: ${new Date(item.ultimoSyncEm).toLocaleDateString('pt-BR')}`
                  : 'Aguardando sync'}
              </Text>
            </View>
            <TouchableOpacity
              onPress={() => handleDisconnect(item)}
              style={styles.disconnectBtn}
            >
              <Text style={styles.disconnectText}>Revogar</Text>
            </TouchableOpacity>
          </View>
        )}
        style={{ flex: 1 }}
      />

      {unconnected.length > 0 && (
        <View style={styles.connectSection}>
          <Text style={styles.sectionLabel}>Conectar conta</Text>
          <View style={styles.connectRow}>
            {unconnected.map(p => (
              <TouchableOpacity
                key={p}
                style={styles.connectBtn}
                onPress={() => handleConnect(p)}
              >
                <Text style={styles.connectBtnText}>
                  {p.charAt(0) + p.slice(1).toLowerCase()}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
        </View>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
    paddingHorizontal: spacing[4],
    paddingTop: spacing[8],
  },
  center: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.background,
  },
  heading: {
    ...typography.h2,
    color: colors.foreground,
    marginBottom: spacing[4],
  },
  empty: {
    color: colors.muted,
    textAlign: 'center',
    marginVertical: spacing[6],
  },
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    padding: spacing[3],
    marginBottom: spacing[2],
    backgroundColor: colors.surface,
  },
  cardLeft: {
    flex: 1,
    gap: 2,
  },
  platform: {
    fontSize: 14,
    fontWeight: '600',
    color: colors.foreground,
    textTransform: 'capitalize',
  },
  handle: {
    fontSize: 12,
    color: colors.muted,
    fontFamily: 'monospace',
  },
  dot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    marginTop: 4,
  },
  sync: {
    fontSize: 11,
    color: colors.muted,
  },
  disconnectBtn: {
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[1],
  },
  disconnectText: {
    fontSize: 13,
    color: '#ef4444',
    fontWeight: '500',
  },
  connectSection: {
    paddingVertical: spacing[4],
    borderTopWidth: 1,
    borderTopColor: colors.border,
  },
  sectionLabel: {
    fontSize: 12,
    fontWeight: '600',
    color: colors.muted,
    textTransform: 'uppercase',
    letterSpacing: 0.8,
    marginBottom: spacing[2],
  },
  connectRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing[2],
  },
  connectBtn: {
    borderWidth: 1,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing[3],
    paddingVertical: spacing[2],
  },
  connectBtnText: {
    fontSize: 13,
    color: colors.foreground,
    fontWeight: '500',
  },
});
