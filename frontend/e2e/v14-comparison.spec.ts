import { test, expect } from '@playwright/test';
import { mkdir } from 'node:fs/promises';

test('capture V14 application views and check layout width', async ({page}, info) => {
  test.setTimeout(120000);
  const folder='../docs/screenshots/phase2/v14-review'; await mkdir(folder,{recursive:true});
  const views:[string,string,string][]=[
    ['overview','/','Real-time results. State by state.'],
    ['office','/?office=senate&page=office','Senate races across the country.'],
    ['state','/?state=TX','Texas elections.'],
    ['state-senate','/?state=TX&office=senate','Texas elections.'],
    ['state-house','/?state=TX&office=house','Texas elections.'],
    ['state-governor','/?state=GA&office=governor','Georgia elections.'],
    ['governor-race','/?state=GA&office=governor&page=race','Georgia · Governors.'],
    ['race','/?state=TX&office=senate&page=race','Texas · Senate.'],
    ['county','/?state=TX&office=senate&page=county','Texas counties.'],
    ['county-district','/elections/2026/states/TX/house/counties/48201/districts','Harris House district map.'],
    ['history','/?mode=history&year=2022&page=history','See how the map changed.'],
    ['history-state','/?mode=history&year=2022&state=TX','Texas elections.'],
    ['polls-state','/?mode=polls&state=TX','Texas elections.'],
    ['exit-state','/?mode=exit&year=2024&state=TX','Texas elections.'],
    ['polls','/?mode=polls','Compare polls and published forecasts.'],
    ['exit','/?mode=exit&year=2024','Election-day voter surveys, source by source.'],
    ['sources','/?page=sources','Source before story.']
  ];
  for(const [name,url,title] of views){
    await page.goto(url); await expect(page.getByRole('heading',{name:title,exact:true})).toBeVisible();
    await expect(page.locator('.status-chip')).not.toContainText('STATUS UNKNOWN');
    if(['overview','office','history'].includes(name)) await expect(page.locator('svg path[aria-label="Texas"]').first()).toBeVisible();
    const width=await page.evaluate(()=>({viewport:innerWidth,document:document.documentElement.scrollWidth}));
    expect(width.document,`${name} must fit the viewport`).toBeLessThanOrEqual(width.viewport);
    await page.screenshot({path:`${folder}/${info.project.name}-${name}.png`,style:'nextjs-portal { display:none !important; }'});
  }
});
