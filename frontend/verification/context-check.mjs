import test from 'node:test';
import assert from 'node:assert/strict';
import { parseContext, contextQuery, changeContext, houseCountyPath, parseHouseCountyRoute } from '../src/context.ts';
import { checkEnvelope } from '../src/api.ts';
import { statusLabel } from '../src/components/DataStatus.tsx';
import { demoSnapshot } from '../src/demoReplay.ts';

test('year 2000 and poll settings survive navigation and URL round trips', () => {
  const initial = parseContext(new URLSearchParams('mode=history&year=2000&provider=ddhq&pollView=forecasts'));
  const state = changeContext(initial, { state: 'TX', office: 'house' });
  const district = changeContext(state, { district: '07' });
  assert.deepEqual(parseContext(new URLSearchParams(contextQuery(district))), district);
  assert.equal(district.year, 2000);
  assert.equal(district.provider, 'ddhq');
  assert.equal(district.pollView, 'forecasts');
});

test('changing state or office clears a stale district', () => {
  const ctx = parseContext(new URLSearchParams('state=TX&office=house&district=07'));
  assert.equal(changeContext(ctx, { state: 'MI' }).district, null);
  assert.equal(changeContext(ctx, { office: 'senate' }).district, null);
});

test('invalid context is rejected and governors alias is canonicalized', () => {
  for (const value of ['mode=fictional', 'year=nope', 'state=ZZ', 'state=TX&office=senate&district=07', 'pollView=other']) {
    assert.throws(() => parseContext(new URLSearchParams(value)));
  }
  assert.equal(parseContext(new URLSearchParams('office=governors')).office, 'governor');
  assert.equal(parseContext(new URLSearchParams('mode=live&year=2019')).year, 2026);
});

test('live cycle is accepted and switching to history chooses a valid year', () => {
  const live = parseContext(new URLSearchParams('mode=live&year=2026'));
  assert.equal(changeContext(live, { mode: 'history' }).year, 2024);
  assert.throws(() => parseContext(new URLSearchParams('mode=history&year=2026')));
});

test('live and polls normalize stale history years before API requests', () => {
  const history = parseContext(new URLSearchParams('mode=history&year=2000&state=TX&office=house&district=07'));
  const live = changeContext(history, { mode: 'live' });
  assert.equal(live.year, 2026);
  assert.equal(live.district, '07');
  assert.match(contextQuery(live), /year=2026/);
  assert.equal(changeContext(history, { mode: 'polls' }).year, 2026);
  assert.equal(parseContext(new URLSearchParams('mode=live&year=2000')).year, 2026);
  assert.equal(parseContext(new URLSearchParams('mode=polls&year=2024')).year, 2026);
});

test('exit survey cycles are selected explicitly and invalid cycles are rejected', () => {
  const exit = parseContext(new URLSearchParams('mode=exit&year=2022'));
  assert.equal(exit.year, 2022);
  assert.equal(changeContext(exit, { mode: 'live' }).year, 2026);
  assert.throws(() => parseContext(new URLSearchParams('mode=exit&year=2025')));
  assert.throws(() => parseContext(new URLSearchParams('mode=exit&year=2028')));
});

test('every supported historical year round trips with scoped filters', () => {
  for (const year of Array.from({ length: 13 }, (_, i) => 2000 + i * 2)) {
    const context = parseContext(new URLSearchParams(`mode=history&year=${year}&state=ca&office=house&district=01&provider=all&pollView=surveys`));
    assert.deepEqual(parseContext(new URLSearchParams(contextQuery(context))), { ...context, state: 'CA' });
  }
});

test('House county routes preserve the election cycle and reject invalid route shapes', () => {
  const context = parseContext(new URLSearchParams('mode=history&year=2022&office=house&state=TX&provider=ddhq&pollView=forecasts'));
  const pathname = houseCountyPath(context, '48201');
  assert.equal(pathname, '/elections/2022/states/TX/house/counties/48201/districts');
  assert.deepEqual(parseHouseCountyRoute(pathname), { year: 2022, state: 'TX', countyFips: '48201' });
  assert.equal(parseHouseCountyRoute('/elections/2026/states/ZZ/house/counties/48201/districts'), null);
  assert.equal(parseHouseCountyRoute('/elections/2026/states/TX/house/counties/4821/districts'), null);
  assert.throws(() => houseCountyPath({ ...context, office: 'senate' }, '48201'));
});

test('fixture data cannot masquerade as real observations', () => {
  const fixture = { dataMode: 'DEMO', isTestData: true, status: 'AVAILABLE_DEMO', data: [] };
  assert.equal(checkEnvelope(fixture), fixture);
  assert.throws(() => checkEnvelope({ ...fixture, dataMode: 'LIVE_REPORTED' }));
  assert.throws(() => checkEnvelope({ ...fixture, isTestData: false }));
});

test('not on ballot, not reporting, unavailable and stale are distinct', () => {
  const messages=['NOT_ON_BALLOT','NOT_REPORTING','NOT_CONNECTED','STALE'].map(statusLabel);
  assert.equal(new Set(messages).size,4);
  assert.ok(messages.every(message=>!message.includes('0 votes')));
});


test('fictional replay snapshots preserve nulls, zeroes and original fixtures', () => {
  const fixture={id:'fictional',office:'senate',state:'TX',district:null,status:'COUNTING',votesReported:null,reporting:{type:'ESTIMATED_VOTE',percent:null},candidates:[{id:'a',displayName:'A',party:'DEM',votes:0},{id:'b',displayName:'B',party:'REP',votes:null}]};
  const earlier=demoSnapshot(fixture,0); assert.equal(earlier.candidates[0].votes,0); assert.equal(earlier.candidates[1].votes,null); assert.equal(earlier.votesReported,null); assert.equal(earlier.reporting.percent,null); assert.equal(fixture.candidates[0].votes,0);
  const complete={...fixture,votesReported:300,reporting:{type:'ESTIMATED_VOTE',percent:80},candidates:[{...fixture.candidates[0],votes:100},{...fixture.candidates[1],votes:200}]};
  assert.deepEqual(demoSnapshot(complete,5),complete); assert.notEqual(demoSnapshot(complete,0).votesReported,complete.votesReported);
});
test('published average view survives URL round trips', () => {
  const ctx=parseContext(new URLSearchParams('mode=polls&year=2026&office=senate&state=TX&provider=pollingforecast&pollView=averages'));
  assert.equal(ctx.pollView,'averages'); assert.deepEqual(parseContext(new URLSearchParams(contextQuery(ctx))),ctx);
});
