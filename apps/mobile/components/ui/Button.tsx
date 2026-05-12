import React from 'react';
import {
  TouchableOpacity,
  Text,
  ActivityIndicator,
  StyleSheet,
  ViewStyle,
} from 'react-native';
import { colors, radius, typography } from '@/lib/theme';

interface ButtonProps {
  onPress: () => void;
  label: string;
  variant?: 'primary' | 'outline' | 'ghost';
  loading?: boolean;
  disabled?: boolean;
  style?: ViewStyle;
  testID?: string;
}

export function Button({
  onPress,
  label,
  variant = 'primary',
  loading = false,
  disabled = false,
  style,
  testID,
}: ButtonProps) {
  const isPrimary = variant === 'primary';
  const isOutline = variant === 'outline';

  return (
    <TouchableOpacity
      onPress={onPress}
      disabled={disabled || loading}
      style={[
        styles.base,
        isPrimary && styles.primary,
        isOutline && styles.outline,
        !isPrimary && !isOutline && styles.ghost,
        (disabled || loading) && styles.disabled,
        style,
      ]}
      accessibilityRole="button"
      accessibilityLabel={label}
      testID={testID}
    >
      {loading ? (
        <ActivityIndicator color={isPrimary ? colors.ink : colors.primary} size="small" />
      ) : (
        <Text
          style={[
            styles.label,
            isPrimary && styles.labelPrimary,
            isOutline && styles.labelOutline,
          ]}
        >
          {label}
        </Text>
      )}
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  base: {
    height: 48,
    borderRadius: radius.md,
    alignItems: 'center',
    justifyContent: 'center',
    paddingHorizontal: 20,
    minWidth: 80,
  },
  primary: { backgroundColor: colors.primary },
  outline: { borderWidth: 1.5, borderColor: colors.primary },
  ghost: { backgroundColor: 'transparent' },
  disabled: { opacity: 0.5 },
  label: {
    fontSize: typography.size.base,
    fontWeight: typography.weight.semibold,
    color: colors.muted,
  },
  labelPrimary: { color: colors.ink },
  labelOutline: { color: colors.primary },
});
