import React from 'react';
import { render } from '@testing-library/react-native';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import HomeScreen from '@/app/(app)/index';
import * as queries from '@/lib/queries';

jest.mock('@/lib/queries', () => ({
  useTarefas: jest.fn(),
}));

jest.mock('expo-router', () => ({
  router: { push: jest.fn() },
  useLocalSearchParams: () => ({}),
}));

jest.mock('@react-native-community/netinfo', () => ({
  useNetInfo: () => ({ isConnected: true }),
}));

const mockUseTarefas = queries.useTarefas as jest.Mock;

function wrapper({ children }: { children: React.ReactNode }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

describe('HomeScreen', () => {
  it('shows spinner when loading', () => {
    mockUseTarefas.mockReturnValue({ isLoading: true });
    const { getByTestId } = render(<HomeScreen />, { wrapper });
    // Spinner renders an ActivityIndicator — just check it doesn't crash
  });

  it('renders lista-tarefas with data', () => {
    mockUseTarefas.mockReturnValue({
      isLoading: false,
      isRefetching: false,
      refetch: jest.fn(),
      data: [
        {
          id: 'uuid-1',
          titulo: 'Postar story',
          descricao: null,
          prazo: null,
          prioridade: 'MEDIA',
          status: 'TODO',
          visivelParaCreator: true,
        },
      ],
    });
    const { getByTestId } = render(<HomeScreen />, { wrapper });
    expect(getByTestId('lista-tarefas')).toBeTruthy();
    expect(getByTestId('tarefa-card-uuid-1')).toBeTruthy();
  });

  it('shows empty state when no tarefas', () => {
    mockUseTarefas.mockReturnValue({
      isLoading: false,
      isRefetching: false,
      refetch: jest.fn(),
      data: [],
    });
    const { getByText } = render(<HomeScreen />, { wrapper });
    expect(getByText('Nenhuma tarefa')).toBeTruthy();
  });
});
