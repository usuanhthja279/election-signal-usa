import { test, expect } from '@playwright/test';
test.skip(process.env.ELECTION_E2E_LOCAL !== '1', 'Requires the disposable PostgreSQL local profile.');

test('local profile displays unavailable data and never requests fixtures', async ({page})=>{
  const demoRequests:string[]=[];
  page.on('request',request=>{if(request.url().includes('/api/v1/demo/')) demoRequests.push(request.url());});
  await page.goto('/');
  await expect(page.getByText('NO LIVE FEED.',{exact:false}).first()).toBeVisible();
  await expect(page.getByText('No licensed provider is connected.')).toBeVisible();
  await page.locator('.compact-state-list').getByRole('button',{name:/Texas TX/}).click();
  await page.locator('.office-tabs').getByRole('button',{name:'House'}).click();
  await page.getByRole('button',{name:'Open district 07'}).click();
  await expect(page.getByText('Model unavailable')).toBeVisible();
  await expect(page.getByText('FICTIONAL EXAMPLE')).toHaveCount(0);
  await expect(page.getByText('DEMO DATA.',{exact:false})).toHaveCount(0);
  expect(demoRequests).toEqual([]);
});
