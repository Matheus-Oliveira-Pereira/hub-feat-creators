import { type Page } from '@playwright/test';

/** Builds a structurally valid (but unsigned) JWT for e2e tests. */
export function fakeJwt(claims: object): string {
  const header = Buffer.from(JSON.stringify({ alg: 'HS256', typ: 'JWT' })).toString('base64url');
  const payload = Buffer.from(JSON.stringify(claims)).toString('base64url');
  return `${header}.${payload}.fakesig`;
}

export const OWNER_TOKEN = fakeJwt({
  sub: 'user-test-001',
  ass: 'assessoria-test-001',
  role: 'OWNER',
  perms: ['OWNR'],
  exp: Math.floor(Date.now() / 1000) + 3600,
});

/** Sets the auth token in localStorage before page load. */
export async function loginAs(page: Page, token = OWNER_TOKEN) {
  await page.addInitScript(t => {
    localStorage.setItem('accessToken', t);
  }, token);
}

/** Intercepts a GET request and returns JSON. */
export async function mockGet(page: Page, urlPattern: string | RegExp, body: unknown) {
  await page.route(urlPattern, route =>
    route.fulfill({ status: 200, contentType: 'application/json', body: JSON.stringify(body) })
  );
}

/** Intercepts a POST/PUT/PATCH and returns JSON. */
export async function mockMutation(
  page: Page,
  method: 'POST' | 'PUT' | 'PATCH' | 'DELETE',
  urlPattern: string | RegExp,
  body: unknown,
  status = 200
) {
  await page.route(urlPattern, route => {
    if (route.request().method() === method) {
      return route.fulfill({
        status,
        contentType: 'application/json',
        body: JSON.stringify(body),
      });
    }
    return route.continue();
  });
}
