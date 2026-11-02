import { test, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

test.beforeAll(async () => { await mkdir('../docs/screenshots/phase2', { recursive: true }); });

test('four global modes preserve their own cycle and navigation scope', async ({ page }, testInfo) => {
  await page.goto('/');
  await expect(page.getByRole('tab', { name: 'Live Results' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.getByText('DEMO DATA.', { exact: false }).first()).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-live.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });

  await page.getByRole('tab', { name: 'Past Results' }).click();
  await page.getByLabel('Election year').selectOption('2000');
  await page.locator('.tile-grid').getByRole('button', { name: /Texas TX/ }).click();
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await expect(page).toHaveURL(/mode=history/);
  await expect(page).toHaveURL(/year=2000/);
  await expect(page.getByText('Historical results have not been imported.').first()).toBeVisible();
  await expect(page.getByText('No verified 2026 House district registry is loaded.')).toHaveCount(0);
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-history-2000.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });

  await page.getByRole('tab', { name: 'Live Results' }).click();
  await expect(page).toHaveURL(/year=2026/);
  await expect(page.getByText('Fictional fixtures — no live election feed is connected.', { exact: false }).first()).toBeVisible();

  await page.getByRole('tab', { name: 'Polls & Forecasts' }).click();
  await expect(page).toHaveURL(/year=2026/);
  await page.getByLabel('Provider').selectOption('ddhq');
  await page.locator('.filters').getByLabel('View').selectOption('forecasts');
  await expect(page).toHaveURL(/provider=ddhq/);
  await expect(page).toHaveURL(/pollView=forecasts/);
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-polls.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });

  await page.getByRole('tab', { name: 'Exit Polls' }).click();
  await page.getByLabel('Survey year').selectOption('2024');
  await expect(page).toHaveURL(/year=2024/);
  await expect(page.getByText('No authorized survey is available.').first()).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-exit.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });
});

test('inconsistent deep-link districts are removed before scoped data requests', async ({ page }) => {
  const resultRequests: string[] = [];
  page.on('request', request => {
    if (/\/api\/v1\/(history\/results|live\/races|polls)\?/.test(request.url())) resultRequests.push(request.url());
  });
  await page.goto('/?mode=history&year=2000&office=house&state=TX&district=07&page=race');
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await expect(page).toHaveURL(/year=2000/);
  await expect(page).not.toHaveURL(/district=07/);
  await expect.poll(() => resultRequests.length).toBeGreaterThan(0);
  const request = new URL(resultRequests.at(-1)!);
  expect(request.searchParams.get('year')).toBe('2000');
  expect(request.searchParams.get('state')).toBe('TX');
  expect(request.searchParams.get('office')).toBe('house');
  expect(request.searchParams.has('district')).toBe(false);
});

test('poll requests use current election-cycle metadata and never retain an invalid district', async ({ page }) => {
  const requestPromise = page.waitForRequest(request => request.url().includes('/api/v1/polls?'));
  await page.goto('/?mode=polls&year=2024&office=house&state=TX&district=07&provider=ddhq&pollView=surveys&page=race');
  const request = new URL((await requestPromise).url());
  expect(request.searchParams.get('year')).toBe('2026');
  expect(request.searchParams.get('office')).toBe('house');
  expect(request.searchParams.get('state')).toBe('TX');
  expect(request.searchParams.get('provider')).toBe('ddhq');
  expect(request.searchParams.get('pollView')).toBe('surveys');
  expect(request.searchParams.has('district')).toBe(false);
  await expect(page).not.toHaveURL(/district=07/);
});

test('keyboard state selection, county navigation and exit mode remain distinct', async ({ page }, testInfo) => {
  await page.goto('/?mode=history&year=2000&office=senate');
  const texas = page.locator('svg path[aria-label="Texas"]');
  await texas.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await page.getByRole('button', { name: 'Explore counties' }).click();
  await expect(page.getByRole('heading', { name: 'Texas counties' })).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-county.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });
  await expect(page.locator('svg path[aria-label="Harris"]')).toBeVisible();
  await page.locator('svg path[aria-label="Harris"]').focus();
  await page.keyboard.press('Enter');
  await expect(page.getByText('Harris: no verified county results are available.')).toBeVisible();
  await expect(page).toHaveURL(/year=2000/);
  await page.getByRole('tab', { name: 'Exit Polls' }).click();
  await expect(page).toHaveURL(/year=2000/);
  await expect(page.getByText('County-level survey estimates are not available.')).toBeVisible();
});

test('national Overview has no win probabilities and demo results remain labeled fictional', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText(/Model unavailable/)).toHaveCount(0);
  await expect(page.getByText(/No win probabilities on the national Overview/)).toBeVisible();
  await expect(page.getByText('Fictional fixtures — no live election feed is connected.', { exact: false }).first()).toBeVisible();
});

test('local profile never requests fictional demo races and reports registry gaps', async ({ page }) => {
  const demoRequests: string[] = [];
  page.on('request', request => { if (request.url().includes('/api/v1/demo/')) demoRequests.push(request.url()); });
  await page.route('**/api/v1/health', route => route.fulfill({ json: { dataMode: 'LIVE_REPORTED', isTestData: false } }));
  await page.route('**/api/v1/elections/2026/house/districts', route => route.fulfill({
    json: { schemaVersion: '1.0', dataMode: 'ELECTION_REGISTRY', isTestData: false, status: 'NOT_IMPORTED', source: { provider: 'Census', sourceAsOf: null, attribution: 'Test response' }, data: { items: [] } }
  }));
  await page.route('**/api/v1/live/races?*', route => route.fulfill({
    json: { schemaVersion: '1.0', dataMode: 'LIVE_REPORTED', isTestData: false, status: 'NOT_CONNECTED', source: { provider: null, sourceAsOf: null, attribution: 'No provider' }, data: [] }
  }));
  await page.goto('/?state=TX');
  await expect(page.getByText('NO LIVE FEED.', { exact: false }).first()).toBeVisible();
  await page.locator('.office-tabs').getByRole('button', { name: 'House' }).click();
  await expect(page.getByText('No verified 2026 House district registry is loaded.')).toBeVisible();
  expect(demoRequests).toEqual([]);
  await expect(page.getByRole('region',{name:'Fictional reporting replay'})).toHaveCount(0);
  await expect(page.getByText('DEMO DATA.', { exact: false })).toHaveCount(0);
});

test('global tabs support keyboard arrows and preserve browser navigation', async ({ page }) => {
  await page.goto('/');
  const live = page.getByRole('tab', { name: 'Live Results' });
  await live.focus();
  await page.keyboard.press('ArrowRight');
  const history = page.getByRole('tab', { name: 'Past Results' });
  await expect(history).toHaveAttribute('aria-selected', 'true');
  await expect(history).toBeFocused();
  await expect(page).toHaveURL(/mode=history/);
  await page.goBack();
  await expect(page).toHaveURL(/mode=live/);
  await expect(page.getByRole('tab', { name: 'Live Results' })).toHaveAttribute('aria-selected', 'true');
});
