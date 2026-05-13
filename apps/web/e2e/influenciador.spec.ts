import { test, expect } from '@playwright/test';
import { loginAs, mockGet, mockMutation } from './helpers';

const MOCK_INFLUENCIADOR = {
  id: 'inf-001',
  assessoriaId: 'assessoria-test-001',
  nome: 'Mariana Costa',
  nicho: 'MODA',
  handles: { instagram: '@marianacosta' },
  tags: [],
  baseLegal: 'CONTRATO',
  observacoes: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  deletedAt: null,
};

test.describe('Influenciadores', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await mockGet(page, '**/api/v1/notificacoes**', { content: [], totalElements: 0 });
  });

  test('empty state visible when no influenciadores', async ({ page }) => {
    await mockGet(page, '**/api/v1/influenciadores**', { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 });

    await page.goto('/influenciadores');
    await expect(page.getByText(/nenhum influenciador/i)).toBeVisible({ timeout: 8000 });
  });

  test('shows influenciador card after create', async ({ page }) => {
    // Initially empty, then after POST returns the new item
    let postDone = false;
    await page.route('**/api/v1/influenciadores**', route => {
      if (route.request().method() === 'POST') {
        postDone = true;
        return route.fulfill({
          status: 201,
          contentType: 'application/json',
          body: JSON.stringify(MOCK_INFLUENCIADOR),
        });
      }
      const list = postDone
        ? { content: [MOCK_INFLUENCIADOR], totalElements: 1, totalPages: 1, number: 0, size: 20 }
        : { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 };
      return route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(list) });
    });

    await page.goto('/influenciadores');

    // Open create modal via button or ?new=1 param
    await page.goto('/influenciadores?new=1');

    // Fill required fields
    await page.getByLabel(/nome/i).fill('Mariana Costa');
    await page.getByLabel(/base legal/i).first().click();
    // Select CONTRATO from dropdown if visible
    const contratoOption = page.getByRole('option', { name: /contrato/i });
    if (await contratoOption.isVisible({ timeout: 2000 }).catch(() => false)) {
      await contratoOption.click();
    }

    await page.getByRole('button', { name: /salvar|criar/i }).click();

    await expect(page.getByText('Mariana Costa')).toBeVisible({ timeout: 8000 });
  });

  test('list shows existing influenciadores', async ({ page }) => {
    await mockGet(page, '**/api/v1/influenciadores**', {
      content: [MOCK_INFLUENCIADOR],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 20,
    });

    await page.goto('/influenciadores');
    await expect(page.getByText('Mariana Costa')).toBeVisible({ timeout: 8000 });
    await expect(page.getByText('@marianacosta')).toBeVisible();
  });
});
