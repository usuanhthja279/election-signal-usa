/** Phase 1 client contracts; availability is never represented by a numeric default. */
export type Availability = 'AVAILABLE' | 'AVAILABLE_DEMO' | 'NOT_CONNECTED' | 'NOT_IMPORTED' | 'NOT_PUBLISHED' | 'NOT_ON_BALLOT' | 'NOT_REPORTING' | 'REFERENCE_ONLY' | 'STALE' | 'UNAVAILABLE_NOT_TRAINED';
export interface Envelope<T> {
  schemaVersion: string;
  dataMode: 'DEMO' | 'LIVE_REPORTED' | 'HISTORICAL_CERTIFIED' | 'POLL' | 'PUBLISHED_FORECAST' | 'EXIT_SURVEY' | 'INTERNAL_MODEL' | 'REFERENCE_GEOGRAPHY';
  isTestData: boolean;
  status: Availability;
  data: T;
  source: { provider: string | null; sourceAsOf: string | null; attribution: string };
}
export interface DemoRace {
  id: string;
  office: 'house' | 'senate' | 'governor';
  state: string;
  district: string | null;
  status: 'COUNTING' | 'NOT_REPORTING';
  votesReported: number | null;
  reporting: { type: 'ESTIMATED_VOTE'; percent: number | null };
  candidates: Array<{ id: string; displayName: string; party: string; votes: number | null }>;
  call: null;
}
export type ApiResult<T> =
  | { kind: 'data'; value: Envelope<T> }
  | { kind: 'error'; message: string };

/** Keep test fixtures out of any reported or certified data context. */
export function checkEnvelope<T>(value: Envelope<T>): Envelope<T> {
  const modes = ['DEMO','LIVE_REPORTED','HISTORICAL_CERTIFIED','POLL','PUBLISHED_FORECAST','EXIT_SURVEY','INTERNAL_MODEL','REFERENCE_GEOGRAPHY'];
  if (!value || typeof value !== 'object' || typeof value.isTestData !== 'boolean' || !value.status || !modes.includes(value.dataMode)) {
    throw new Error('Invalid API response');
  }
  if (value.isTestData && value.dataMode !== 'DEMO') throw new Error('Test data cannot be represented as production data');
  if (value.dataMode === 'DEMO' && !value.isTestData) throw new Error('Demo data must be marked as test data');
  return value;
}
