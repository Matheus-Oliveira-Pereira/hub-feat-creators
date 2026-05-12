import React, { useRef, useState } from 'react';
import {
  View,
  Text,
  TextInput,
  StyleSheet,
  KeyboardAvoidingView,
  Platform,
  Alert,
} from 'react-native';
import { router, useLocalSearchParams } from 'expo-router';
import { persistToken } from '@/lib/auth';
import { Button } from '@/components/ui/Button';
import { colors, spacing, typography, radius } from '@/lib/theme';

export default function MfaScreen() {
  const { email } = useLocalSearchParams<{ email: string }>();
  const [code, setCode] = useState('');
  const [loading, setLoading] = useState(false);
  const inputRef = useRef<TextInput>(null);

  async function handleVerify() {
    if (code.length < 6) return;
    setLoading(true);
    try {
      const apiUrl = process.env.EXPO_PUBLIC_API_URL ?? 'http://localhost:8080';
      const res = await fetch(`${apiUrl}/api/v1/portal/auth/mfa/verify`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, code }),
      });
      if (!res.ok) throw new Error('Código inválido ou expirado');
      const data = await res.json();
      await persistToken(data.token);
      router.replace('/(app)/');
    } catch (err: any) {
      Alert.alert('Erro', err.message ?? 'Código inválido');
      setCode('');
      inputRef.current?.focus();
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
        <Text style={styles.title}>Verificação em dois fatores</Text>
        <Text style={styles.subtitle}>
          Digite o código de 6 dígitos do seu autenticador
        </Text>

        <TextInput
          ref={inputRef}
          style={styles.codeInput}
          value={code}
          onChangeText={setCode}
          keyboardType="number-pad"
          maxLength={6}
          autoFocus
          textAlign="center"
          testID="input-mfa-code"
          accessibilityLabel="Código MFA"
        />

        <Button
          onPress={handleVerify}
          label="Verificar"
          loading={loading}
          disabled={code.length < 6}
          testID="btn-mfa-verify"
        />
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
    gap: spacing.lg,
  },
  title: {
    fontSize: typography.size.xl,
    fontWeight: typography.weight.bold,
    color: colors.ink,
  },
  subtitle: {
    fontSize: typography.size.base,
    color: colors.muted,
  },
  codeInput: {
    height: 64,
    borderWidth: 2,
    borderColor: colors.primary,
    borderRadius: radius.md,
    fontSize: typography.size['3xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
    letterSpacing: 16,
  },
});
