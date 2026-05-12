import React from 'react';
import { FlatList, View, Text, StyleSheet, RefreshControl } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { useTarefas } from '@/lib/queries';
import { TarefaCard } from '@/components/app/TarefaCard';
import { Spinner } from '@/components/ui/Spinner';
import { EmptyState } from '@/components/ui/EmptyState';
import { colors, spacing, typography } from '@/lib/theme';

export default function HomeScreen() {
  const { data: tarefas, isLoading, refetch, isRefetching } = useTarefas();

  if (isLoading) return <Spinner />;

  return (
    <SafeAreaView style={styles.container} edges={['top']}>
      <View style={styles.header}>
        <Text style={styles.title}>Minhas Tarefas</Text>
      </View>
      <FlatList
        data={tarefas ?? []}
        keyExtractor={(t) => t.id}
        renderItem={({ item }) => <TarefaCard tarefa={item} />}
        contentContainerStyle={styles.list}
        ListEmptyComponent={
          <EmptyState
            title="Nenhuma tarefa"
            description="Você não tem tarefas visíveis no momento."
          />
        }
        refreshControl={
          <RefreshControl
            refreshing={isRefetching}
            onRefresh={refetch}
            tintColor={colors.primary}
          />
        }
        testID="lista-tarefas"
      />
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: colors.background },
  header: { padding: spacing.lg, paddingBottom: spacing.md },
  title: {
    fontSize: typography.size['2xl'],
    fontWeight: typography.weight.bold,
    color: colors.ink,
  },
  list: { paddingHorizontal: spacing.lg, paddingBottom: spacing.xl },
});
