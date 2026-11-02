import { test, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';
test.beforeAll(async()=>{await mkdir('../docs/screenshots/phase3',{recursive:true});});

test('licensed polling records stay scoped to polling mode', async ({page},info)=>{
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/polls?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'POLL',isTestData:false,status:'AVAILABLE',source:{provider:null,sourceAsOf:null,attribution:'Test only'},data:[{pollId:'fixture-poll',pollster:'Fictional Pollster',office:'SENATE',state:'TX',fieldStart:'2026-10-01',fieldEnd:'2026-10-03',sampleSize:null,population:'LV',provider:'fixture-provider',publicationUrl:'https://example.invalid/poll',license:'TEST',attribution:'Fictional test fixture',publishedAt:'2026-10-04T12:00:00Z',fetchedAt:'2026-10-05T12:00:00Z',answers:[{choiceId:'a',label:'Fictional candidate',candidateId:'a',percent:42}]}]}}));
  await page.route('**/api/v1/live/races?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'LIVE_REPORTED',isTestData:false,status:'NOT_CONNECTED',source:{provider:null,sourceAsOf:null,attribution:'No provider'},data:[]}}));
  await page.goto('/?mode=polls&year=2026&office=senate&state=TX');
  await expect(page.getByRole('heading',{name:'Fictional Pollster'})).toBeVisible();
  await expect(page.getByText('Fictional candidate: 42%')).toBeVisible();
  await expect(page.getByText(/Sample: Unavailable/)).toBeVisible();
  await expect(page.getByRole('link',{name:'Original publication ↗'})).toHaveAttribute('href','https://example.invalid/poll');
  await page.screenshot({path:`../docs/screenshots/phase3/${info.project.name}-state-survey.png`,fullPage:true,style:'nextjs-portal { display:none !important; }'});
  await page.getByRole('tab',{name:'Live Results'}).click();
  await expect(page.getByText('Fictional candidate: 42%')).toHaveCount(0);
  await page.goBack();
  await expect(page).toHaveURL(/mode=polls/);
  await expect(page.getByRole('heading',{name:'Fictional Pollster'})).toBeVisible();
  await page.goto('/?mode=polls&year=2026&office=senate&state=TX&page=race');
  await expect(page.getByRole('heading',{name:'Fictional Pollster'})).toBeVisible();
  await page.screenshot({path:`../docs/screenshots/phase3/${info.project.name}-race-survey.png`,fullPage:true,style:'nextjs-portal { display:none !important; }'});
});

test('polling averages remain separate from surveys and forecasts',async ({page})=>{
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/polls/averages?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'POLL',isTestData:false,status:'AVAILABLE',source:{provider:null,sourceAsOf:null,attribution:'Fixture'},data:[{seriesId:'fictional',candidateId:'test',averageShare:48.5,includedPollCount:null,provider:'fixture',office:'SENATE',state:'TX',publishedAt:'2026-10-01T00:00:00Z',publicationUrl:'https://example.invalid',license:'TEST',attribution:'Fictional fixture'}]}}));
  await page.goto('/?mode=polls&year=2026&office=senate&state=TX&pollView=averages');
  await expect(page.getByText('Published average: 48.5%',{exact:false})).toBeVisible();
  await expect(page.getByText('Included polls: Unavailable',{exact:false})).toBeVisible();
  await page.reload(); await expect(page.locator('.filters').getByLabel('View')).toHaveValue('averages');
});

test('publisher probabilities are displayed only in forecast mode',async ({page},info)=>{
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/forecasts/published?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'PUBLISHED_FORECAST',isTestData:false,status:'AVAILABLE',source:{provider:null,sourceAsOf:null,attribution:'Fixture'},data:[{forecastId:'fake-forecast',modelName:'Fictional model',provider:'fixture',office:'SENATE',state:'TX',candidateId:'fake',expectedShare:51,winProbability:0.71,intervalLow:null,intervalHigh:null,publishedAt:'2026-10-01T00:00:00Z',publicationUrl:'https://example.invalid',license:'TEST',attribution:'Fictional fixture'}]}}));
  await page.goto('/?mode=polls&year=2026&office=senate&state=TX&pollView=forecasts');
  await expect(page.getByText('Publisher win probability: 71.0%',{exact:false})).toBeVisible();
  await expect(page.getByText('Expected vote share: 51%',{exact:false})).toBeVisible();
  await expect(page.getByText('Interval: Unavailable',{exact:false})).toBeVisible();
  await page.screenshot({path:`../docs/screenshots/phase3/${info.project.name}-forecast-fixture.png`,fullPage:true,style:'nextjs-portal { display:none !important; }'});
});

test('polling controls remain usable at 320px with reduced motion',async ({page})=>{
  await page.setViewportSize({width:320,height:720});
  await page.emulateMedia({reducedMotion:'reduce'});
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.goto('/?mode=polls&year=2026&office=senate&state=TX');
  await expect(page.locator('.filters').getByLabel('View')).toBeVisible();
  expect(await page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth)).toBeTruthy();
  await page.locator('.filters').getByLabel('View').focus();
  await expect(page.locator('.filters').getByLabel('View')).toBeFocused();
});
