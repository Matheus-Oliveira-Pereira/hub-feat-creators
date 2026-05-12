import React from 'react';
import { TouchableOpacity, View, Text, StyleSheet } from 'react-native';
import { router } from 'expo-router';
import { Tarefa } from '@/lib/api';
import { Card } from '@/components/ui/Card';
import { Badge } from '@/components/ui/Badge';
import { colors, spacing, typography } from '@/lib/theme';

interface Props {
  tarefa: Tarefa;
}

const prioridadeVariant = {
  BAIXA: 'default',
  MEDIA: 'warning',
  ALTA: 'destructive',
  CRITICA: 'destructive',
} as const;

export function TarefaCard({ tarefa }: Props) {
  return (
    <TouchableOpacity
      onPress={() => router.push(`/(app)/tarefa/${tarefa.id}` as any)}
      accessibilityRole="button"
      accessibilityLabel={`Tarefa ${tarefa.titulo}`}
      testID={`tarefa-card-${tarefa.id}`}
    >
      <Card style={styles.card}>
        <View style={styles.header}>
          <Text style={styles.titulo} numberOfLines={2}>
            {tarefa.titulo}
          </Text>
          <Badge label={tarefa.prioridade} variant={prioridadeVariant[tarefa.prioridade]} />
        </View>
        {tarefa.prazo && (
          <Text style={styles.prazo}>
            Prazo: {new Date(tarefa.prazo).toLocaleDateString('pt-BR')}
          </Text>
        )}
        <Badge label={tarefa.status} variant="outline" />
      </Card>
    </TouchableOpacity>
  );
}

const styles = StyleSheet.create({
  card: { marginBottom: spacing.sm },
  header: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: spacing.xs,
    gap: spacing.sm,
  },
  titulo: {
    flex: 1,
    fontSize: typography.size.base,
    fontWeight: typography.weight.semibold,
    color: colors.ink,
  },
  prazo: {
    fontSize: typography.size.sm,
    color: colors.muted,
    marginBottom: spacing.xs,
  },
});
