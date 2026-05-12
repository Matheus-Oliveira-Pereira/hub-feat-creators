import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet, Switch, Alert } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import * as LocalAuthentication from 'expo-local-authentication';
import { logout, isBiometriaEnabled, setBiometria } from '@/lib/auth';
import { useAuthStore } from '@/store/auth';
import { Button } from '@/components/ui/Button';
import { Card } from '@/components/ui/Card';
import { colors, spacing, typography } from '@/lib/theme';

export default function PerfilScreen() {
  const { claims } = useAuthStore();
  const [biometriaEnabled, setBiometriaState] = useState(false);
  const [hasHardware, setHasHardware] = useState(false);
  const [loggingOut, setLoggingOut] = useState(false);

  useEffect(() => {
    (async () => {
      const hw = await LocalAuthentication.hasHardwareAsync();
      const enrolled = await LocalAuthentication.isEnrolledAsync();
      setHasHardware(hw && enrolled);
      if (hw && enrolled) {
        setBiometriaState(await isBiometriaEnabled());
      }
    })();
  }, []);

  async function handleToggleBiometria(value: boolean) {
    if (value) {
      const result = await LocalAuthentication.authenticateAsync({
        promptMessage: 'Confirme para ativar biometria',
      });
      if (!result.success) return;
    }
    await setBiometria(value);
    setBiometriaState(value);
  }

  async function handleLogout() {
    Alert.alert('Sair', 'Deseja sair da sua conta?', [
      { text: 'Cancelar', style: 'cancel' },
      {
        text: 'Sair',
        style: 'destructive',
        onPress: async () => {
          setLoggingOut(true);
          await logout();
        },
      },
    ]);
  }

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Perfil</Text>
      </View>

      <View style={styles.content}>
        <Card>
          <Text style={styles.label}>E-mail</Text>
          <Text style={styles.value}>{claims?.creatorUserId ?? '—'}</Text>
        </Card>

        {hasHardware && (
          <Card style={styles.row}>
            <Text style={styles.label}>Biometria</Text>
            <Switch
              value={biometriaEnabled}
              onValueChange={handleToggleBiometria}
              trackColor={{ true: colors.primary }}
              testID="switch-biometria"
            />
          </Card>
        )}

        <Button
          onPress={handleLogout}
          label="Sair da conta"
          variant="outline"
          loading={loggingOut}
          testID="btn-logout"
        />
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { padding: spacing.lg, paddingBottom: spacing.md },
  title: {
    fontSize: typography.size['2xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
  },
  content: { padding: spacing.lg, gap: spacing.md },
  row: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  label: { fontSize: typography.size.sm, color: colors.muted, marginBottom: 2 },
  value: { fontSize: typography.size.base, color: colors.ink, fontWeight: typography.weight.medium },
});
