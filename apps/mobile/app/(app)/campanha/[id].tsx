import React from 'react';
import { View, Text, ScrollView, StyleSheet, TouchableOpacity } from 'react-native';
import { useLocalSearchParams, router } from 'expo-router';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTarefa } from '@/lib/queries';
import { Spinner } from '@/components/ui/Spinner';
import { colors, spacing, typography } from '@/lib/theme';

export default function CampanhaDetailScreen() {
  const { id } = useLocalSearchParams<{ id: string }>();
  const { data: campanha, isLoading } = useTarefa(id);

  if (isLoading) return <Spinner />;
  if (!campanha) return (
    <View style={styles.container}>
      <Text>Campanha não encontrada.</Text>
    </View>
  );

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <TouchableOpacity onPress={() => router.back()} style={styles.back} accessibilityRole="button">
        <Text style={styles.backText}>← Voltar</Text>
      </TouchableOpacity>
      <ScrollView contentContainerStyle={styles.content}>
        <Text style={styles.titulo}>{campanha.titulo}</Text>
        {campanha.descricao && (
          <Text style={styles.descricao}>{campanha.descricao}</Text>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  back: { padding: spacing.lg, paddingBottom: spacing.sm },
  backText: { color: colors.primary, fontSize: typography.size.base },
  content: { padding: spacing.lg, paddingTop: 0 },
  titulo: {
    fontSize: typography.size['2xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
    marginBottom: spacing.md,
  },
  descricao: { fontSize: typography.size.base, color: colors.muted, lineHeight: 24 },
});
