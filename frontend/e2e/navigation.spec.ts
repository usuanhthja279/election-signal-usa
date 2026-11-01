import { test, expect } from '@playwright/test';

test('global filters survive state, office, race and browser navigation', async ({ page }, testInfo) => {
  await page.goto('/');
  await expect(page.getByRole('tab', { name: 'Live Results' })).toHaveAttribute('aria-selected','true');
  await expect(page.getByText('DEMO DATA.', { exact: false }).first()).toBeVisible();
  await expect(page.locator('.compact-state-list').getByRole('button', { name: /Texas TX/ })).toBeVisible();
  await expect(page.locator('svg path[aria-label="Texas"]')).toBeVisible();
  await expect(page.getByText('Fictional fixtures — no live election feed is connected.', {exact:true})).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase1-${testInfo.project.name}.png`, fullPage: true });
  await page.getByRole('tab', { name: 'Past Results' }).click();
  await page.getByLabel('Election year').selectOption('2000');
  await page.screenshot({ path: `../docs/screenshots/phase1-history-${testInfo.project.name}.png`, fullPage: true });
  await page.locator('.tile-grid').getByRole('button', { name: /Texas TX/ }).click();
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase1-state-${testInfo.project.name}.png`, fullPage: true });
  await page.locator('.office-tabs').getByRole('button', { name: 'House' }).click();
  await page.getByRole('button', { name: 'Open district 07' }).click();
  await expect(page.getByRole('heading', { name: 'Texas · House District 07' })).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase1-race-${testInfo.project.name}.png`, fullPage: true });
  await expect(page).toHaveURL(/year=2000/);
  await page.goBack();
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await page.getByRole('tab', { name: 'Polls & Forecasts' }).click();
  await page.screenshot({ path: `../docs/screenshots/phase1-polls-${testInfo.project.name}.png`, fullPage: true });
  await page.getByLabel('Provider').selectOption('ddhq');
  await page.getByLabel('View').selectOption('forecasts');
  await page.getByRole('button', { name: 'Open district 07' }).click();
  await expect(page).toHaveURL(/provider=ddhq/);
  await expect(page).toHaveURL(/pollView=forecasts/);
  await expect(page.getByText('No survey or publisher forecast for this exact race is connected.')).toBeVisible();
});

test('live race remains demo and no model appears on national overview', async ({ page }) => {
  await page.goto('/');
  await expect(page.getByText('Model unavailable')).toHaveCount(0);
  await page.locator('.compact-state-list').getByRole('button', { name: /Texas TX/ }).click();
  await page.locator('.office-tabs').getByRole('button', { name: 'House' }).click();
  await page.getByRole('button', { name: 'Open district 07' }).click();
  await expect(page.getByText('Model unavailable')).toBeVisible();
  await expect(page.getByText('FICTIONAL EXAMPLE').first()).toBeVisible();
});

test('keyboard geography, county context, source status and exit survey remain distinct', async ({ page },testInfo) => {
  await page.goto('/?mode=history&year=2000&office=senate');
  const texas = page.locator('svg path[aria-label="Texas"]');
  await texas.focus(); await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Texas elections' })).toBeVisible();
  await page.getByRole('button', { name: 'Explore counties' }).click();
  await expect(page.getByRole('heading', { name: 'Texas counties' })).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase1-county-${testInfo.project.name}.png`, fullPage: true });
  await expect(page.locator('svg path[aria-label="Harris"]')).toBeVisible();
  await page.locator('svg path[aria-label="Harris"]').focus(); await page.keyboard.press('Enter');
  await expect(page.getByText('Harris: no verified county results are available.')).toBeVisible();
  await expect(page).toHaveURL(/year=2000/);
  await page.getByRole('tab', { name: 'Exit Polls' }).click();
  await expect(page).toHaveURL(/county=48201/);
  await expect(page.getByText('County-level survey estimates are not available.')).toBeVisible();
  await page.getByLabel('Survey year').selectOption('2026');
  await expect(page).toHaveURL(/year=2026/);
  await expect(page).toHaveURL(/county=48201/);
  await page.getByRole('button', { name: 'Sources', exact: true }).click();
  await expect(page.getByText('No external provider is connected in demo mode.')).toBeVisible();
  await expect(page.getByText(/License unverified/).first()).toBeVisible();
});

test('non-demo mode never requests demo races', async ({ page }) => {
  const demoRequests:string[]=[];
  page.on('request',request=>{if(request.url().includes('/api/v1/demo/')) demoRequests.push(request.url());});
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/live/races?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'LIVE_REPORTED',isTestData:false,status:'NOT_CONNECTED',source:{provider:null,sourceAsOf:null,attribution:'No provider'},data:[]}}));
  await page.goto('/');
  await expect(page.getByText('NO LIVE FEED.',{exact:false}).first()).toBeVisible();
  await expect(page.getByText('No licensed provider is connected.')).toBeVisible();
  expect(demoRequests).toEqual([]);
  await expect(page.getByText('DEMO DATA.',{exact:false})).toHaveCount(0);
});

test('recovers when the health endpoint is briefly unavailable', async ({page})=>{
  let attempts=0;
  await page.route('**/api/v1/health',route=>{
    attempts++;
    return attempts===1 ? route.fulfill({status:503,body:'unavailable'}) : route.continue();
  });
  await page.goto('/');
  await expect(page.getByText('Fictional fixtures — no live election feed is connected.', {exact:true})).toBeVisible();
  expect(attempts).toBeGreaterThanOrEqual(2);
});

test('keyboard arrows change the global context', async ({page})=>{
  await page.goto('/');
  const live=page.getByRole('tab',{name:'Live Results'});
  await live.focus(); await page.keyboard.press('ArrowRight');
  const history=page.getByRole('tab',{name:'Past Results'});
  await expect(history).toHaveAttribute('aria-selected','true');
  await expect(history).toBeFocused();
  await expect(page).toHaveURL(/mode=history/);
});

test('poll requests carry the exact state, district and provider', async ({page})=>{
  const requestPromise=page.waitForRequest(request=>request.url().includes('/api/v1/polls?'));
  await page.goto('/?mode=polls&year=2026&office=house&state=TX&district=07&provider=ddhq&pollView=surveys&page=race');
  const request=await requestPromise;
  const params=new URL(request.url()).searchParams;
  expect(params.get('office')).toBe('house');
  expect(params.get('state')).toBe('TX');
  expect(params.get('district')).toBe('07');
  expect(params.get('provider')).toBe('ddhq');
  await expect(page.getByText('No survey or publisher forecast for this exact race is connected.')).toBeVisible();
});

test('V7 office navigation, state dashboard and historical year picker stay scoped', async ({page},testInfo)=>{
  await page.goto('/');
  if(testInfo.project.name!=='desktop') await page.getByRole('button',{name:'Open navigation'}).click();
  await page.getByRole('button',{name:/US Senate/}).first().click();
  await expect(page.getByRole('heading',{name:'Senate races across the country.'})).toBeVisible();
  await page.screenshot({ path: `../docs/screenshots/phase1-office-${testInfo.project.name}.png`, fullPage: true });
  await page.getByRole('button',{name:'Texas TX · Reference geography'}).click();
  await expect(page.getByRole('heading',{name:'Texas elections.'})).toBeVisible();
  await expect(page.locator('.office-tabs').getByRole('button',{name:'Senate'})).toHaveAttribute('class',/active/);
  await page.getByRole('tab',{name:'Past Results'}).click();
  await page.getByLabel('Election year').selectOption('2000');
  await expect(page).toHaveURL(/year=2000/);
  await expect(page.getByRole('heading',{name:'Texas elections.'})).toBeVisible();
  await page.reload();
  await expect(page.getByRole('heading',{name:'Texas elections.'})).toBeVisible();
  await expect(page).toHaveURL(/year=2000/);
});

test('overview keyboard focus reports fictional state snapshot without national model values', async ({page})=>{
  await page.goto('/');
  const texas=page.locator('svg[aria-label="Select a state"] path[aria-label="Texas"]').first();
  await texas.focus();
  await expect(page.getByRole('heading',{name:'Texas'})).toBeVisible();
  await expect(page.getByText('Source: fictional fixture · Source timestamp unavailable',{exact:true})).toBeVisible();
  await expect(page.getByText(/Model unavailable/)).toHaveCount(0);
  await expect(page.getByText(/No win probabilities on the national Overview/)).toBeVisible();
});
