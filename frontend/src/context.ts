/** URL state shared by all election views. No election data is stored here. */
export const MODES = ['live', 'history', 'polls', 'exit'] as const;
export const OFFICES = ['all', 'house', 'senate', 'governor'] as const;
export type Mode = typeof MODES[number];
export type Office = typeof OFFICES[number];
export type PollView = 'surveys' | 'forecasts';
export const LIVE_ELECTION_YEAR = 2026;
export const POLLS_ELECTION_YEAR = 2026;
export const HISTORICAL_YEARS = Array.from({ length: 13 }, (_, index) => 2000 + index * 2);
// No exit-survey provider is currently approved. The selector carries a cycle;
// endpoint availability remains a separate source status.
export const EXIT_SURVEY_YEARS = [...HISTORICAL_YEARS, LIVE_ELECTION_YEAR];
export type ElectionContext = {
  mode: Mode; year: number; office: Office; state: string | null;
  district: string | null; provider: string; pollView: PollView;
};

const states = new Set('AL AK AZ AR CA CO CT DE DC FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY'.split(' '));
export type HouseCountyRoute = { year:number; state:string; countyFips:string };

/** Canonical semantic route for the House county/district intersection preview. */
export function houseCountyPath(context:ElectionContext,countyFips:string):string {
  if(!context.state||context.office!=='house'||!/^\d{5}$/.test(countyFips))throw new Error('A House county route requires a state and five-digit county FIPS');
  return `/elections/${context.year}/states/${context.state}/house/counties/${countyFips}/districts`;
}

export function parseHouseCountyRoute(pathname:string):HouseCountyRoute|null {
  const match=pathname.match(/^\/elections\/(\d{4})\/states\/([A-Za-z]{2})\/house\/counties\/(\d{5})\/districts\/?$/);
  if(!match)return null;
  const state=match[2].toUpperCase();
  if(!states.has(state))return null;
  return {year:Number(match[1]),state,countyFips:match[3]};
}

const member = <T extends string>(values: readonly T[], value: string | null): value is T =>
  value !== null && values.includes(value as T);

/** Reject invalid values instead of silently changing the requested context. */
export function parseContext(query: URLSearchParams): ElectionContext {
  const mode = query.get('mode') ?? 'live';
  const rawOffice = query.get('office') ?? 'all';
  const office = rawOffice === 'governors' ? 'governor' : rawOffice;
  const yearText = query.get('year') ?? (mode === 'history' ? '2024' : String(LIVE_ELECTION_YEAR));
  const requestedYear = Number(yearText);
  const parsedYearIsValid = /^\d{4}$/.test(yearText) && Number.isInteger(requestedYear);
  // Live and pre-election polling always describe the current federal cycle.
  // Normalize stale cycle parameters before URLs or API requests are produced.
  const year = mode === 'history' || mode === 'exit' ? requestedYear : LIVE_ELECTION_YEAR;
  const state = query.get('state')?.toUpperCase() ?? null;
  const district = query.get('district');
  const provider = query.get('provider') ?? 'all';
  const pollView = query.get('pollView') ?? query.get('view') ?? 'surveys';
  if (!member(MODES, mode)) throw new Error('Invalid election mode');
  if (!member(OFFICES, office)) throw new Error('Invalid office');
  if (!parsedYearIsValid) throw new Error('Invalid election year');
  if (mode === 'history' && !HISTORICAL_YEARS.includes(year)) throw new Error('Invalid historical election year');
  if (mode === 'exit' && !EXIT_SURVEY_YEARS.includes(year)) throw new Error('Invalid exit survey year');
  if (state && !states.has(state)) throw new Error('Invalid state');
  if (district && (!state || office !== 'house' || !/^\d{2}$/.test(district))) throw new Error('Invalid district context');
  if (!/^[a-z0-9_-]{1,40}$/i.test(provider)) throw new Error('Invalid provider');
  if (pollView !== 'surveys' && pollView !== 'forecasts') throw new Error('Invalid poll view');
  return { mode, year, office, state, district, provider, pollView };
}

export function contextQuery(context: ElectionContext): string {
  const query = new URLSearchParams({mode: context.mode, year: String(context.year), office: context.office, provider: context.provider, pollView: context.pollView});
  if (context.state) query.set('state', context.state);
  if (context.district) query.set('district', context.district);
  return query.toString();
}

/** Changing office or state invalidates a district, but retains all global filters. */
export function changeContext(context: ElectionContext, patch: Partial<ElectionContext>): ElectionContext {
  const next = { ...context, ...patch };
  if (patch.mode === 'history' && patch.year === undefined && next.year === LIVE_ELECTION_YEAR) next.year = 2024;
  if (patch.mode !== undefined && patch.mode !== context.mode && patch.year === undefined && patch.mode !== 'history' && patch.mode !== 'exit') next.year = LIVE_ELECTION_YEAR;
  if (patch.mode === 'exit' && patch.year === undefined && !EXIT_SURVEY_YEARS.includes(next.year)) next.year = LIVE_ELECTION_YEAR;
  if ((patch.state !== undefined && patch.state !== context.state) || (patch.office !== undefined && patch.office !== context.office)) {
    if (patch.district === undefined) next.district = null;
  }
  return parseContext(new URLSearchParams(contextQuery(next)));
}
