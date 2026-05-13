import { test, expect } from '@playwright/test';
import { mockGet, mockMutation, OWNER_TOKEN } from './helpers';

test.describe('Login flow', () => {
  test.beforeEach(async ({ page }) => {
    // Mock all authenticated API calls that the home page fires on load
    await mockGet(page, '**/api/v1/prospeccoes**', {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 20,
    });
    await mockGet(page, '**/api/v1/notificacoes**', { content: [], totalElements: 0 });
    await mockGet(page, '**/api/v1/tarefas**', { content: [], totalElements: 0 });
    await mockGet(page, '**/api/v1/relatorios/**', {});
  });

  test('successful login redirects to dashboard', async ({ page }) => {
    await mockMutation(page, 'POST', '**/api/v1/auth/login', {
      accessToken: OWNER_TOKEN,
      refreshToken: 'refresh-fake',
    });

    await page.goto('/login');
    await page.getByLabel(/e-mail/i).fill('admin@test.com');
    await page.getByLabel(/senha/i).fill('Senha@123');
    await page.getByRole('button', { name: /entrar/i }).click();

    await expect(page).toHaveURL('/', { timeout: 8000 });
  });

  test('invalid credentials shows error toast', async ({ page }) => {
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({
        status: 401,
        contentType: 'application/json',
        body: JSON.stringify({ error: { code: 'INVALID_CREDENTIALS', message: 'Credenciais inválidas.' } }),
      })
    );

    await page.goto('/login');
    await page.getByLabel(/e-mail/i).fill('wrong@test.com');
    await page.getByLabel(/senha/i).fill('wrong');
    await page.getByRole('button', { name: /entrar/i }).click();

    await expect(page.getByText(/credenciais inválidas/i)).toBeVisible({ timeout: 5000 });
  });

  test('MFA required shows code field', async ({ page }) => {
    await page.route('**/api/v1/auth/login', route =>
      route.fulfill({
        status: 422,
        contentType: 'application/json',
        body: JSON.stringify({ error: { code: 'MFA_REQUIRED', message: 'MFA necessário.' } }),
      })
    );

    await page.goto('/login');
    await page.getByLabel(/e-mail/i).fill('mfa@test.com');
    await page.getByLabel(/senha/i).fill('Senha@123');
    await page.getByRole('button', { name: /entrar/i }).click();

    // MFA code input must appear
    await expect(page.getByLabel(/código.*autenticador|mfa/i)).toBeVisible({ timeout: 5000 });
  });
});
