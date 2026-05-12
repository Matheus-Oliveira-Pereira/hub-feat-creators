import React from 'react';
import { render, fireEvent, waitFor } from '@testing-library/react-native';
import * as authLib from '@/lib/auth';
import LoginScreen from '@/app/(auth)/login';

jest.mock('@/lib/auth', () => ({
  login: jest.fn(),
  isBiometriaEnabled: jest.fn().mockResolvedValue(false),
  persistToken: jest.fn(),
  logout: jest.fn(),
  loadStoredAuth: jest.fn(),
  setBiometria: jest.fn(),
}));

jest.mock('expo-router', () => ({
  router: { push: jest.fn(), replace: jest.fn() },
  useLocalSearchParams: () => ({}),
}));

jest.mock('expo-local-authentication', () => ({
  hasHardwareAsync: jest.fn().mockResolvedValue(false),
  isEnrolledAsync: jest.fn().mockResolvedValue(false),
  authenticateAsync: jest.fn(),
}));

const mockLogin = authLib.login as jest.Mock;

describe('LoginScreen', () => {
  beforeEach(() => jest.clearAllMocks());

  it('renders email and senha inputs', () => {
    const { getByTestId } = render(<LoginScreen />);
    expect(getByTestId('input-email')).toBeTruthy();
    expect(getByTestId('input-senha')).toBeTruthy();
  });

  it('btn-login disabled when fields empty', () => {
    const { getByTestId } = render(<LoginScreen />);
    expect(getByTestId('btn-login').props.accessibilityState?.disabled).toBe(true);
  });

  it('calls login with email and senha', async () => {
    mockLogin.mockResolvedValue({ token: 'tok.abc.def' });
    const { getByTestId } = render(<LoginScreen />);

    fireEvent.changeText(getByTestId('input-email'), 'creator@test.com');
    fireEvent.changeText(getByTestId('input-senha'), 'senha123');
    fireEvent.press(getByTestId('btn-login'));

    await waitFor(() => {
      expect(mockLogin).toHaveBeenCalledWith('creator@test.com', 'senha123');
    });
  });

  it('navigates to mfa when mfaRequired', async () => {
    mockLogin.mockResolvedValue({ mfaRequired: true });
    const { getByTestId } = render(<LoginScreen />);
    const { router } = require('expo-router');

    fireEvent.changeText(getByTestId('input-email'), 'creator@test.com');
    fireEvent.changeText(getByTestId('input-senha'), 'senha123');
    fireEvent.press(getByTestId('btn-login'));

    await waitFor(() => {
      expect(router.push).toHaveBeenCalledWith(
        expect.objectContaining({ pathname: '/(auth)/mfa' }),
      );
    });
  });
});
