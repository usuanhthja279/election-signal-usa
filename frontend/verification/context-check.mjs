import test from 'node:test';
import assert from 'node:assert/strict';
import { parseContext, contextQuery, changeContext } from '../src/context.ts';
import { checkEnvelope } from '../src/api.ts';
import { statusLabel } from '../src/components/DataStatus.tsx';

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
  for (const value of ['mode=fictional', 'year=2019', 'state=ZZ', 'state=TX&office=senate&district=07', 'pollView=other']) {
    assert.throws(() => parseContext(new URLSearchParams(value)));
  }
  assert.equal(parseContext(new URLSearchParams('office=governors')).office, 'governor');
});

test('live cycle is accepted and switching to history chooses a valid year', () => {
  const live = parseContext(new URLSearchParams('mode=live&year=2026'));
  assert.equal(changeContext(live, { mode: 'history' }).year, 2024);
  assert.throws(() => parseContext(new URLSearchParams('mode=history&year=2026')));
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

