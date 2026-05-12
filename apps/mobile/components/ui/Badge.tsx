import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { colors, radius, typography } from '@/lib/theme';

type Variant = 'default' | 'success' | 'warning' | 'destructive' | 'outline';

interface BadgeProps {
  label: string;
  variant?: Variant;
}

const variantStyle: Record<Variant, { bg: string; text: string }> = {
  default: { bg: colors.card, text: colors.muted },
  success: { bg: '#DCFCE7', text: colors.success },
  warning: { bg: '#FEF3C7', text: '#92400E' },
  destructive: { bg: '#FEE2E2', text: colors.destructive },
  outline: { bg: 'transparent', text: colors.muted },
};

export function Badge({ label, variant = 'default' }: BadgeProps) {
  const vs = variantStyle[variant];
  return (
    <View style={[styles.badge, { backgroundColor: vs.bg }]}>
      <Text style={[styles.text, { color: vs.text }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: 8,
    paddingVertical: 2,
    borderRadius: radius.full,
    alignSelf: 'flex-start',
  },
  text: {
    fontSize: typography.size.xs,
    fontWeight: typography.weight.medium,
  },
});
