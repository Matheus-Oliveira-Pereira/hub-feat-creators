import React, { useEffect } from 'react';
import { Redirect, Tabs } from 'expo-router';
import { useAuthStore } from '@/store/auth';
import { registerForPushNotifications } from '@/lib/push';
import { colors } from '@/lib/theme';
import { OfflineBanner } from '@/components/app/OfflineBanner';
import { View } from 'react-native';

export default function AppLayout() {
  const { token } = useAuthStore();
  if (!token) return <Redirect href="/(auth)/login" />;

  useEffect(() => {
    registerForPushNotifications();
  }, []);

  return (
    <View style={{ flex: 1 }}>
      <OfflineBanner />
      <Tabs
        screenOptions={{
          tabBarActiveTintColor: colors.primary,
          tabBarInactiveTintColor: colors.muted,
          tabBarStyle: { backgroundColor: colors.background, borderTopColor: colors.border },
          headerShown: false,
        }}
      >
        <Tabs.Screen name="index" options={{ title: 'Tarefas' }} />
        <Tabs.Screen name="mensagens" options={{ title: 'Mensagens' }} />
        <Tabs.Screen name="perfil" options={{ title: 'Perfil' }} />
        <Tabs.Screen name="campanha/[id]" options={{ href: null }} />
        <Tabs.Screen name="tarefa/[id]" options={{ href: null }} />
      </Tabs>
    </View>
  );
}
