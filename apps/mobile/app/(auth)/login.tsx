import React, { useEffect, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { router } from 'expo-router';
import * as LocalAuthentication from 'expo-local-authentication';
import { login, isBiometriaEnabled } from '@/lib/auth';
import { Button } from '@/components/ui/Button';
import { colors, spacing, typography, radius } from '@/lib/theme';

export default function LoginScreen() {
  const [email, setEmail] = useState('');
  const [senha, setSenha] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    tryBiometria();
  }, []);

  async function tryBiometria() {
    const enabled = await isBiometriaEnabled();
    if (!enabled) return;
    const hasHardware = await LocalAuthentication.hasHardwareAsync();
    if (!hasHardware) return;
    const enrolled = await LocalAuthentication.isEnrolledAsync();
    if (!enrolled) return;

    const result = await LocalAuthentication.authenticateAsync({
      promptMessage: 'Entrar no feat. creators',
      fallbackLabel: 'Usar senha',
    });
    if (result.success) {
      router.replace('/(app)/');
    }
  }

  async function handleLogin() {
    if (!email || !senha) return;
    setLoading(true);
    try {
      const result = await login(email.trim().toLowerCase(), senha);
      if (result.mfaRequired) {
        router.push({ pathname: '/(auth)/mfa', params: { email: email.trim() } } as any);
      } else {
        router.replace('/(app)/');
      }
    } catch (err: any) {
      Alert.alert('Erro ao entrar', err.message ?? 'Verifique suas credenciais');
    } finally {
      setLoading(false);
    }
  }

  return (
    <KeyboardAvoidingView
      style={styles.container}
      behavior={Platform.OS === 'ios' ? 'padding' : undefined}
    >
      <View style={styles.inner}>
        <Text style={styles.logo}>feat. creators</Text>
        <Text style={styles.subtitle}>Acesse sua conta</Text>

        <View style={styles.form}>
          <TextInput
            style={styles.input}
            placeholder="E-mail"
            placeholderTextColor={colors.muted}
            value={email}
            onChangeText={setEmail}
            keyboardType="email-address"
            autoCapitalize="none"
            autoComplete="email"
            testID="input-email"
            accessibilityLabel="E-mail"
          />
          <TextInput
            style={styles.input}
            placeholder="Senha"
            placeholderTextColor={colors.muted}
            value={senha}
            onChangeText={setSenha}
            secureTextEntry
            autoComplete="password"
            testID="input-senha"
            accessibilityLabel="Senha"
          />
          <Button
            onPress={handleLogin}
            label="Entrar"
            loading={loading}
            disabled={!email || !senha}
            testID="btn-login"
          />
        </View>
      </View>
    </KeyboardAvoidingView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  inner: {
    flex: 1,
    justifyContent: 'center',
    padding: spacing.xl,
  },
  logo: {
    fontSize: typography.size['3xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
    marginBottom: spacing.xs,
  },
  subtitle: {
    fontSize: typography.size.base,
    color: colors.muted,
    marginBottom: spacing['2xl'],
  },
  form: { gap: spacing.md },
  input: {
    height: 48,
    borderWidth: 1.5,
    borderColor: colors.border,
    borderRadius: radius.md,
    paddingHorizontal: spacing.md,
    fontSize: typography.size.base,
    color: colors.ink,
    backgroundColor: colors.background,
  },
});
