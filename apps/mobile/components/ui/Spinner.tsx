import React from 'react';
import { ActivityIndicator, View, StyleSheet } from 'react-native';
import { colors } from '@/lib/theme';

export function Spinner({ size = 'large' }: { size?: 'small' | 'large' }) {
  return (
    <View style={styles.container}>
      <ActivityIndicator size={size} color={colors.primary} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, alignItems: 'center', justifyContent: 'center' },
});
