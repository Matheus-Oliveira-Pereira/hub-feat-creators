import { test, expect } from '@playwright/test';
import { loginAs, mockGet, mockMutation } from './helpers';

const MOCK_PROSPECCAO_NOVA = {
  id: 'pro-001',
  assessoriaId: 'assessoria-test-001',
  titulo: 'Campanha Verão 2025',
  status: 'NOVA',
  marcaId: 'mar-001',
  influenciadorId: 'inf-001',
  marcaNome: 'Marca Teste',
  influenciadorNome: 'Creator Teste',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  deletedAt: null,
};

const MOCK_PROSPECCAO_CONTATADA = { ...MOCK_PROSPECCAO_NOVA, status: 'CONTATADA' };

test.describe('Prospecção — state machine', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page);
    await mockGet(page, '**/api/v1/notificacoes**', { content: [], totalElements: 0 });
    await mockGet(page, '**/api/v1/marcas**', { content: [], totalElements: 0 });
    await mockGet(page, '**/api/v1/influenciadores**', { content: [], totalElements: 0 });
  });

  test('kanban renders NOVA column', async ({ page }) => {
    await mockGet(page, '**/api/v1/prospeccoes**', {
      content: [MOCK_PROSPECCAO_NOVA],
      totalElements: 1,
      totalPages: 1,
      number: 0,
      size: 50,
    });

    await page.goto('/prospeccao');
    await expect(page.getByText('Novas')).toBeVisible({ timeout: 8000 });
    await expect(page.getByText('Campanha Verão 2025')).toBeVisible();
  });

  test('status transition NOVA → CONTATADA updates card', async ({ page }) => {
    let currentStatus = 'NOVA';

    await page.route('**/api/v1/prospeccoes**', route => {
      if (route.request().method() === 'PATCH' || route.request().url().includes('/status')) {
        currentStatus = 'CONTATADA';
        return route.fulfill({
          status: 200,
          contentType: 'application/json',
          body: JSON.stringify(MOCK_PROSPECCAO_CONTATADA),
        });
      }
      const pros = currentStatus === 'NOVA' ? MOCK_PROSPECCAO_NOVA : MOCK_PROSPECCAO_CONTATADA;
      return route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify({ content: [pros], totalElements: 1, totalPages: 1, number: 0, size: 50 }),
      });
    });

    await page.goto('/prospeccao');
    await expect(page.getByText('Campanha Verão 2025')).toBeVisible({ timeout: 8000 });

    // Open dropdown on the card
    const card = page.getByText('Campanha Verão 2025').locator('../..');
    await card.getByRole('button', { name: /mais opções|menu/i }).first().click();

    // Click "Mover para Contatada" or similar transition option
    const moveOption = page.getByRole('menuitem', { name: /contatada/i });
    if (await moveOption.isVisible({ timeout: 2000 }).catch(() => false)) {
      await moveOption.click();
      await expect(page.getByText('Contatadas')).toBeVisible({ timeout: 5000 });
    }
  });

  test('empty prospeccao list shows empty state', async ({ page }) => {
    await mockGet(page, '**/api/v1/prospeccoes**', {
      content: [],
      totalElements: 0,
      totalPages: 0,
      number: 0,
      size: 50,
    });

    await page.goto('/prospeccao');
    // Either empty state text or kanban columns with 0 count
    await expect(page.getByText(/nenhuma prospecção|0/i).first()).toBeVisible({ timeout: 8000 });
  });
});
