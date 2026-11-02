/** Phase 1 client contracts; availability is never represented by a numeric default. */
export type Availability = 'AVAILABLE' | 'AVAILABLE_DEMO' | 'NOT_CONNECTED' | 'NOT_LICENSED' | 'NOT_IMPORTED' | 'NOT_PUBLISHED' | 'NOT_ON_BALLOT' | 'NOT_REPORTING' | 'REFERENCE_ONLY' | 'STALE' | 'UNAVAILABLE_NOT_TRAINED' | 'BLOCKED' | 'PARTIAL';
export interface Envelope<T> {
  schemaVersion: string;
  dataMode: 'DEMO' | 'LIVE_REPORTED' | 'HISTORICAL_CERTIFIED' | 'HISTORICAL_COMPILED' | 'POLL' | 'PUBLISHED_FORECAST' | 'EXIT_SURVEY' | 'INTERNAL_MODEL' | 'REFERENCE_GEOGRAPHY' | 'ELECTION_REGISTRY';
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
export interface HistoricalResult {
  contestId: string;
  year: number;
  office: string;
  state: string;
  district: string | null;
  seatClass: string | null;
  subtype: string;
  resultStatus: string;
  certifyingAuthority: string | null;
  certifiedAt: string | null;
  sourceProvider: string;
  sourceTitle: string;
  sourceUrl: string;
  sourceSha256: string;
  sourceAsOf: string | null;
  geoId: string | null;
  geoType: string | null;
  candidateId: string | null;
  candidateName: string | null;
  party: string | null;
  voteMethod: string | null;
  votes: number | null;
  geographyVersion: string | null;
}
export type ApiResult<T> =
  | { kind: 'data'; value: Envelope<T> }
  | { kind: 'error'; message: string };

/** Keep test fixtures out of any reported or certified data context. */
export function checkEnvelope<T>(value: Envelope<T>): Envelope<T> {
  const modes = ['DEMO','LIVE_REPORTED','HISTORICAL_CERTIFIED','HISTORICAL_COMPILED','POLL','PUBLISHED_FORECAST','EXIT_SURVEY','INTERNAL_MODEL','REFERENCE_GEOGRAPHY','ELECTION_REGISTRY'];
  if (!value || typeof value !== 'object' || typeof value.isTestData !== 'boolean' || !value.status || !modes.includes(value.dataMode)) {
    throw new Error('Invalid API response');
  }
  if (value.isTestData && value.dataMode !== 'DEMO') throw new Error('Test data cannot be represented as production data');
  if (value.dataMode === 'DEMO' && !value.isTestData) throw new Error('Demo data must be marked as test data');
  return value;
}
export type PollingRecord = {
  citations?:{provider:string;url:string;attribution:string}[]; pollId?:string; forecastId?:string; seriesId?:string; averageShare?:number|null; includedPollCount?:number|null; pollster?:string; sponsor?:string|null; office?:string; state?:string|null; district?:string|null; raceId?:string|null;
  fieldStart?:string|null; fieldEnd?:string|null; sampleSize?:number|null; population?:string|null; surveyMode?:string|null; marginOfError?:number|null;
  provider?:string; publicationUrl?:string; methodologyUrl?:string|null; license?:string; attribution?:string; publishedAt?:string|null; fetchedAt?:string|null;
  sourceDate?:string|null; candidateName?:string|null; sourceParty?:string|null; providerRaceKey?:string|null; modelName?:string; modelVersion?:string|null; candidateId?:string|null; expectedShare?:number|null; winProbability?:number|null; intervalLow?:number|null; intervalHigh?:number|null; rating?:string|null;
  answers?:Array<{choiceId:string;label:string;candidateId:string|null;percent:number|null}>;
};
