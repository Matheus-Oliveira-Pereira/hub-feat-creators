import React from 'react';
import { Redirect, Stack } from 'expo-router';
import { useAuthStore } from '@/store/auth';

export default function AuthLayout() {
  const { token } = useAuthStore();
  if (token) return <Redirect href="/(app)/" />;

  return (
    <Stack screenOptions={{ headerShown: false }}>
      <Stack.Screen name="login" />
      <Stack.Screen name="mfa" />
    </Stack>
  );
}
