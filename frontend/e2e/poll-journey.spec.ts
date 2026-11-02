import { test, expect } from '@playwright/test';

test('a House forecast deep link retains its exact district when registry metadata is unavailable', async ({page}) => {
  await page.route('**/api/v1/health', route => route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/elections/2026/house/districts', route => route.fulfill({json:{schemaVersion:'1.0',dataMode:'LIVE_REPORTED',isTestData:false,status:'NOT_IMPORTED',data:{items:[]}}}));
  await page.route('**/api/v1/forecasts/published?*', route => {
    const q=new URL(route.request().url()).searchParams;
    const data=q.get('district')==='07' ? [{forecastId:'test-house',modelName:'Fictional district forecast',provider:'test',office:'HOUSE',state:'TX',district:'07',winProbability:0.6,publicationUrl:'https://example.invalid',license:'TEST',attribution:'Fictional fixture'}] : [];
    return route.fulfill({json:{schemaVersion:'1.0',dataMode:'PUBLISHED_FORECAST',isTestData:false,status:data.length?'AVAILABLE':'NOT_CONNECTED',data}});
  });
  await page.goto('/?mode=polls&office=house&state=TX&district=07&pollView=forecasts&page=race');
  await expect(page.getByText('Fictional district forecast')).toBeVisible();
  await expect(page.getByLabel('House district')).toHaveValue('07');
  await expect(page.getByText('Source district; registry unavailable')).toBeVisible();
  await expect(page).toHaveURL(/district=07/);
  await page.reload(); await expect(page.getByText('Fictional district forecast')).toBeVisible();
  await expect(page.getByLabel('House district')).toHaveValue('07');
  await expect(page.getByText('Source district; registry unavailable')).toBeVisible();
  await expect(page).toHaveURL(/district=07/);
});

test('forecast pages are reachable and reset when the scope changes', async ({page}) => {
  await page.route('**/api/v1/health', route => route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/forecasts/published?*', route => {
    const q=new URL(route.request().url()).searchParams;
    const pageNumber=Number(q.get('page')||0);
    const count=pageNumber===0?50:1;
    const data=Array.from({length:count},(_,i)=>({forecastId:`test-${pageNumber}-${i}`,modelName:pageNumber===0?'Fictional first page':'Fictional second page',provider:'test',office:'HOUSE',state:'TX',publicationUrl:'https://example.invalid',license:'TEST',attribution:'Fictional fixture'}));
    return route.fulfill({json:{schemaVersion:'1.0',dataMode:'PUBLISHED_FORECAST',isTestData:false,status:'AVAILABLE',data}});
  });
  await page.goto('/?mode=polls&office=house&pollView=forecasts');
  await expect(page.getByRole('button',{name:'Next page'})).toBeEnabled();
  await page.getByRole('button',{name:'Next page'}).click();
  await expect(page.getByText('Fictional second page')).toBeVisible();
  await expect(page.getByText('Page 2',{exact:true})).toBeVisible();
  await page.getByRole('button',{name:'Previous page'}).click();
  await expect(page.getByText('Page 1',{exact:true})).toBeVisible();
});
