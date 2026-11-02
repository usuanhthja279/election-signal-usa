import { test, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

test.beforeAll(async () => { await mkdir('../docs/screenshots/phase2/v14-review', { recursive: true }); });

test('V14 navigation shell and state finder work across viewport sizes', async ({ page }, testInfo) => {
  await page.goto('/');
  await expect(page.locator('.v14-heading')).toContainText('ELECTION SIGNAL / THE DESK');
  await expect(page.getByRole('tablist', { name: 'Election data context' })).toBeVisible();
  await expect(page.getByRole('tab', { name: 'Live Results' })).toHaveAttribute('aria-selected', 'true');

  const width = await page.evaluate(() => ({ viewport: window.innerWidth, content: document.documentElement.scrollWidth }));
  expect(width.content).toBeLessThanOrEqual(width.viewport);

  await page.getByRole('button', { name: 'Switch to light theme' }).click();
  await expect(page.locator('.v14-app')).toHaveAttribute('data-theme', 'light');
  await page.getByRole('button', { name: 'Switch to dark theme' }).click();
  await expect(page.locator('.v14-app')).toHaveAttribute('data-theme', 'dark');

  await page.getByRole('button', { name: /Find a state/ }).click();
  const search = page.getByRole('textbox', { name: 'Search state or abbreviation' });
  await search.fill('Texas');
  await page.getByRole('button', { name: 'Texas TX' }).last().click();
  await expect(page).toHaveURL(/state=TX/);
  await expect(page.locator('.v14-heading strong')).toContainText('Texas');

  await page.getByRole('tab', { name: 'Past Results' }).click();
  await expect(page.getByRole('tab', { name: 'Past Results' })).toHaveAttribute('aria-selected', 'true');
  await expect(page.locator('.date-context')).toContainText('HISTORICAL CYCLE');

  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-v14-shell.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });
});

test('House county selection opens the schematic district inspector directly', async ({ page }, testInfo) => {
  await page.goto('/?office=house&state=TX');
  await page.getByRole('button', { name: 'Open House county map' }).click();
  await expect(page.getByRole('heading', { name: 'Texas House county map.' })).toBeVisible();
  await page.locator('svg path[aria-label="Harris"]').click();
  await expect(page).toHaveURL(/\/elections\/2026\/states\/TX\/house\/counties\/48201\/districts/);
  await expect(page.getByRole('heading', { name: 'Harris House district map.' })).toBeVisible();
  await expect(page.getByText('DEMO DISTRICT DIVISIONS · NOT VERIFIED 2026 BOUNDARIES')).toBeVisible();
  const segmentB = page.getByRole('button', { name: /Select illustrative section B/ });
  await segmentB.focus();
  await page.keyboard.press('Enter');
  await expect(page.getByRole('heading', { name: 'Segment B' })).toBeVisible();
  await page.evaluate(() => window.scrollTo(0, 0));
  await page.screenshot({ path: `../docs/screenshots/phase2/${testInfo.project.name}-v14-county-districts.png`, fullPage: true, style: 'nextjs-portal { display: none !important; }' });
});

test('V14 context labels, state office navigation and map zoom', async ({ page }) => {
  await page.goto('/?mode=history&year=2000&state=TX&office=senate');
  await expect(page.getByRole('heading', {name:'Texas elections.'})).toBeVisible();
  if (await page.getByRole('button', {name:'Open navigation'}).isVisible()) await page.getByRole('button', {name:'Open navigation'}).click();
  await page.getByRole('button', {name:'US House',exact:true}).click();
  await expect(page).toHaveURL(/state=TX/);
  await expect(page).toHaveURL(/office=house/);
  await page.getByRole('tab', {name:'Exit Polls'}).click();
  await page.getByLabel('Survey year').selectOption('2024');
  await expect(page.locator('.hero .chip')).toHaveText('TX · 2024');
  await page.getByRole('button', {name:'Open House county map'}).click();
  await expect(page.locator('.hero .chip')).toContainText('2024');
  await page.goto('/');
  const map=page.getByRole('group', {name:'Select a state'});
  const original=await map.getAttribute('viewBox');
  await page.getByRole('button',{name:'Zoom map in'}).click();
  await expect(map).not.toHaveAttribute('viewBox',original!);
  await page.getByRole('button',{name:'Reset map zoom'}).click();
  await expect(map).toHaveAttribute('viewBox',original!);
});

test('county inspector identifies unavailable exit surveys', async ({page}) => {
  await page.goto('/elections/2026/states/TX/house/counties/48201/districts?mode=live&year=2026&office=house&state=TX');
  await expect(page.getByRole('heading',{name:'Harris House district map.'})).toBeVisible();
  await page.getByRole('tab',{name:'Exit Polls'}).click();
  await expect(page.locator('.intersection-notice')).toContainText('No authorized exit survey');
});

test('fictional replay is isolated and county layers remain unavailable', async ({page}, testInfo) => {
  await page.goto('/');
  const replay=page.getByRole('region',{name:'Fictional reporting replay'});
  await expect(replay).toBeVisible();
  await replay.getByRole('button',{name:'Open replay'}).click();
  const firstVotes=page.locator('.race-board .candidate strong').first();
  await expect(firstVotes).toBeVisible();
  const latest=await firstVotes.innerText();
  await replay.getByRole('button',{name:'Reset',exact:true}).click();
  await expect(firstVotes).not.toHaveText(latest);
  await replay.getByRole('button',{name:'Replay speed 1 times'}).click();
  await expect(replay.getByRole('button',{name:'Replay speed 2 times'})).toBeVisible();
  await replay.getByRole('button',{name:'Play replay'}).click();
  await expect(replay.getByRole('button',{name:'Pause replay'})).toBeVisible();
  await replay.getByRole('button',{name:'Pause replay'}).click();
  await replay.getByRole('button',{name:'Next batch'}).click();
  await expect(replay).toContainText('Wave 2 / 6');
  await replay.getByLabel('Simulation timeline').fill('4');
  await expect(replay).toContainText('Wave 5 / 6');
  await replay.getByRole('button',{name:'Reset',exact:true}).click();
  await expect(replay).toContainText('Wave 1 / 6');
  await page.getByRole('tab',{name:'Past Results'}).click();
  await expect(replay).toHaveCount(0);
  await page.goto('/?mode=live&year=2026&state=TX&office=senate&page=county');
  await expect(page.getByRole('heading',{name:'Texas counties.'})).toBeVisible();
  await page.getByRole('button',{name:'Counted',exact:true}).click();
  await expect(page.getByText('Reporting measure unavailable',{exact:false}).first()).toBeVisible();
  await page.getByLabel('Find a county').fill('Harris');
  const table=page.getByRole('table');
  await expect(table.getByRole('button',{name:'Harris',exact:true})).toBeVisible();
  await table.getByRole('button',{name:'Harris',exact:true}).click();
  await expect(page.getByText('Harris: no verified county results are available.',{exact:false})).toBeVisible();
  await page.screenshot({path:`../docs/screenshots/phase2/${testInfo.project.name}-county-layers.png`,fullPage:true,style:'nextjs-portal { display: none !important; }'});
});

test('race layout separates fictional vote shares, reporting and model status', async ({page}) => {
  await page.goto('/?mode=live&year=2026&state=TX&office=senate&page=race');
  await expect(page.getByRole('heading',{name:'Texas · Senate.'})).toBeVisible();
  await expect(page.getByLabel('Fictional reported vote shares')).toContainText('fictional reported votes');
  await expect(page.getByText('Estimated vote counted:',{exact:false})).toBeVisible();
  await expect(page.getByText('Model unavailable')).toBeVisible();
  await expect(page.getByRole('img',{name:/Fictional candidate A vote share/})).toBeVisible();
});

test('House district view keeps geography unavailable without a crosswalk', async ({page}, testInfo) => {
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/elections/2026/house/districts',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'ELECTION_REGISTRY',isTestData:false,status:'PARTIAL',source:{provider:null,sourceAsOf:null,attribution:'Test registry'},data:{items:[{state:'TX',district:'01'}]}}}));
  await page.route('**/api/v1/live/races?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'LIVE_REPORTED',isTestData:false,status:'NOT_CONNECTED',source:{provider:null,sourceAsOf:null,attribution:'No provider'},data:[]}}));
  await page.goto('/?mode=live&year=2026&state=TX&office=house&district=01&page=race');
  await expect(page.getByRole('heading',{name:'Texas · House District 01.'})).toBeVisible();
  await expect(page.getByText('Verified district boundaries and county intersections are not loaded.',{exact:false})).toBeVisible();
  await expect(page.getByText('Model unavailable')).toBeVisible();
  await expect(page.getByRole('region',{name:'Fictional reporting replay'})).toHaveCount(0);
  await page.screenshot({path:`../docs/screenshots/phase2/v14-review/${testInfo.project.name}-house-district.png`,style:'nextjs-portal { display:none !important; }'});
});

test('header controls do not overlap at desktop and mobile widths', async ({page}) => {
  for(const width of [320,390,768,1024,1226,1440]){
    await page.setViewportSize({width,height:900}); await page.goto('/');
    await expect(page.getByRole('tab',{name:'Live Results'})).toBeVisible();
    const boxes=await page.evaluate(()=>{
      const rect=(selector:string)=>{const e=document.querySelector<HTMLElement>(selector);if(!e||getComputedStyle(e).display==='none')return null;const b=e.getBoundingClientRect();return {left:b.left,right:b.right,top:b.top,bottom:b.bottom};};
      return {tabs:rect('.v14-top .mode-tabs'),tools:rect('.v14-top .v14-tools'),heading:rect('.v14-top .v14-heading'),width:document.documentElement.scrollWidth};
    });
    const overlap=(a:NonNullable<typeof boxes.tabs>,b:NonNullable<typeof boxes.tools>)=>Math.min(a.right,b.right)>Math.max(a.left,b.left)+1&&Math.min(a.bottom,b.bottom)>Math.max(a.top,b.top)+1;
    expect(boxes.width,`page width ${width}`).toBeLessThanOrEqual(width);
    expect(boxes.tabs&&boxes.tools&&overlap(boxes.tabs,boxes.tools),`tabs and tools at ${width}`).toBeFalsy();
    expect(boxes.heading&&boxes.tools&&overlap(boxes.heading,boxes.tools),`heading and tools at ${width}`).toBeFalsy();
  }
});
