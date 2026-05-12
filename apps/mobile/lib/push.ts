import * as Notifications from 'expo-notifications';
import * as Device from 'expo-device';
import { Platform } from 'react-native';
import { api } from '@/lib/api';
import { router } from 'expo-router';

Notifications.setNotificationHandler({
  handleNotification: async () => ({
    shouldShowAlert: true,
    shouldPlaySound: true,
    shouldSetBadge: true,
  }),
});

export async function registerForPushNotifications(): Promise<void> {
  if (!Device.isDevice) return; // simulador não tem push real

  const { status: existing } = await Notifications.getPermissionsAsync();
  let finalStatus = existing;
  if (existing !== 'granted') {
    const { status } = await Notifications.requestPermissionsAsync();
    finalStatus = status;
  }
  if (finalStatus !== 'granted') return;

  const tokenData = await Notifications.getExpoPushTokenAsync();
  const canal = Platform.OS === 'ios' ? 'APNS' : 'FCM';
  const plataforma = `${Platform.OS} ${Platform.Version}`;

  await api.post('/api/v1/devices/register', {
    canal,
    token: tokenData.data,
    plataforma,
  });
}

export function setupNotificationListeners(): () => void {
  const sub = Notifications.addNotificationResponseReceivedListener((response) => {
    const url = response.notification.request.content.data?.url as string | undefined;
    if (url && url !== '/') {
      // url formato: /tarefas?id=UUID
      router.push(url as any);
    }
  });
  return () => sub.remove();
}
