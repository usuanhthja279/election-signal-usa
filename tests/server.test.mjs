import test from 'node:test';
import assert from 'node:assert/strict';
import {createApp} from '../src/server.mjs';

const app=createApp();
let base;
test.before(async()=>{
  await new Promise(resolve=>app.listen(0,'127.0.0.1',resolve));
  base=`http://127.0.0.1:${app.address().port}`;
});
test.after(()=>new Promise(resolve=>app.close(resolve)));
async function get(path){ const response=await fetch(base+path); return {response, body:await response.json()}; }

test('serves actual V7 UI and host script',async()=>{
 const html=await fetch(base+'/');assert.equal(html.status,200);const body=await html.text();
 assert.match(body,/Election Signal/);assert.match(body,/app-host\.js/);
 const script=await fetch(base+'/assets/app-host.js');assert.equal(script.status,200);
});
test('health advertises honest demo status',async()=>{
 const {response,body}=await get('/api/v1/health');assert.equal(response.status,200);
 assert.equal(body.dataMode,'DEMO');assert.equal(body.connected.live,false);assert.equal(body.connected.forecastModel,false);
});
test('demo races are filtered by exact office and state',async()=>{
 const {body}=await get('/api/v1/demo/races?office=house&state=TX');assert.equal(body.dataMode,'DEMO');
 assert.ok(body.data.length>0);assert.ok(body.data.every(x=>x.office==='house'&&x.state==='TX'));
 assert.ok(body.data.every(x=>!x.call));
});
test('history from 2000 supports correct empty-state semantics',async()=>{
 const {body}=await get('/api/v1/history/results?year=2000');assert.equal(body.status,'NOT_IMPORTED');assert.deepEqual(body.data,[]);
});
test('year cannot leak impossible values',async()=>{
 const {response}=await get('/api/v1/history/results?year=2019');assert.equal(response.status,400);
});
test('polling and exit polling are not falsely published',async()=>{
 for(const p of ['/api/v1/polls','/api/v1/exit-polls','/api/v1/forecasts/published']){
  const {body}=await get(p);assert.equal(body.isTestData,true);assert.deepEqual(body.data,[]);
  assert.notEqual(body.status,'AVAILABLE_LIVE');
 }
});
test('models abstain until trained and validated',async()=>{
 const {body}=await get('/api/v1/models/races/TX-SEN-2026-DEMO');assert.equal(body.status,'UNAVAILABLE_NOT_TRAINED');assert.equal(body.probabilities,null);
});
test('state and county geometry is selectable, never claimed 2026 district data',async()=>{
 const {body}=await get('/api/v1/geography/states/TX/counties');assert.equal(body.data.state,'TX');
 assert.equal(body.data.countyCount,254);assert.equal(body.status,'REFERENCE_ONLY');
 const manifest=(await get('/api/v1/geography/manifest')).body;assert.equal(manifest.data.states,51);
 assert.equal(manifest.data.eligibleFor2026HouseDistrictAssignments,false);
});
test('query validation rejects unknown state, office and modes',async()=>{
 for(const path of ['/api/v1/context?mode=imaginary','/api/v1/context?office=president','/api/v1/demo/races?state=ZZ']){
  const {response}=await get(path);assert.equal(response.status,400);
 }
});
test('only allows safe files',async()=>{
 const response=await fetch(base+'/not-a-file.exe');assert.equal(response.status,404);
 const api=await fetch(base+'/api/unknown');assert.equal(api.status,404);
});
test('SSE handshake clearly says no live results',async()=>{
 const controller=new AbortController();
 const response=await fetch(base+'/api/v1/events',{signal:controller.signal});
 assert.equal(response.status,200);const {value}=await response.body.getReader().read();
 const start=new TextDecoder().decode(value);assert.match(start,/server_status/);assert.match(start,/"resultsStream":false/);
 controller.abort();
});
