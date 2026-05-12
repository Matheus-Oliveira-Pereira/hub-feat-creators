import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import NetInfo from '@react-native-community/netinfo';
import { colors, spacing, typography } from '@/lib/theme';

export function OfflineBanner() {
  const netInfo = NetInfo.useNetInfo();
  if (netInfo.isConnected !== false) return null;

  return (
    <View style={styles.banner} accessibilityLiveRegion="polite">
      <Text style={styles.text}>Sem conexão — dados em cache</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  banner: {
    backgroundColor: colors.warning,
    paddingVertical: spacing.xs,
    paddingHorizontal: spacing.md,
    alignItems: 'center',
  },
  text: {
    fontSize: typography.size.xs,
    fontWeight: typography.weight.medium,
    color: '#78350F',
  },
});
