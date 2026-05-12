import { device, element, by, expect as detoxExpect } from 'detox';

describe('Critical flow: login → home → tarefa → upload', () => {
  beforeAll(async () => {
    await device.launchApp({ newInstance: true });
  });

  beforeEach(async () => {
    await device.reloadReactNative();
  });

  it('shows login screen on cold start', async () => {
    await detoxExpect(element(by.testID('input-email'))).toBeVisible();
    await detoxExpect(element(by.testID('input-senha'))).toBeVisible();
  });

  it('logs in and shows home screen', async () => {
    await element(by.testID('input-email')).typeText('creator@e2e-test.com');
    await element(by.testID('input-senha')).typeText('senha123456');
    await element(by.testID('btn-login')).tap();

    // Aguarda lista de tarefas (pode estar vazia no ambiente de teste)
    await detoxExpect(element(by.testID('lista-tarefas'))).toBeVisible();
  });
});
