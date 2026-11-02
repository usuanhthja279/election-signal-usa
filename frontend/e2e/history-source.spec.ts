import { test, expect } from '@playwright/test';
test('source-reported historical rows appear in the selected state and year',async ({page})=>{
  await page.route('**/api/v1/health',route=>route.fulfill({json:{dataMode:'LIVE_REPORTED',isTestData:false}}));
  await page.route('**/api/v1/history/results?*',route=>route.fulfill({json:{schemaVersion:'1.0',dataMode:'HISTORICAL_COMPILED',isTestData:false,status:'AVAILABLE',source:{provider:'fixture',sourceAsOf:null,attribution:'Fictional browser test'},data:[{contestId:'fixture',office:'SENATE',state:'TX',subtype:'REGULAR',resultStatus:'SOURCE_REPORTED',certifyingAuthority:null,sourceTitle:'Fictional test source',sourceUrl:'https://example.invalid',sourceSha256:'a'.repeat(64),geoId:'48',geoType:'STATE',candidateId:'fixture',candidateName:'Fictional Test Candidate',party:'TEST',votes:123,geographyVersion:'Test only'}]}}));
  await page.goto('/?mode=history&year=2024&office=senate&state=TX');
  await expect(page.getByText('Fictional Test Candidate')).toBeVisible();
  await expect(page.getByText('Source-reported official; certification not independently verified')).toBeVisible();
  await expect(page.getByText(/Historical results have not been imported/)).toHaveCount(0);
  await page.reload();await expect(page.getByText('Fictional Test Candidate')).toBeVisible();
});
