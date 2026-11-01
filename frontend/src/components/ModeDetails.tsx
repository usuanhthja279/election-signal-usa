import type { ElectionContext } from '../context';

export function HistoricalResultsDashboard({context}:{context:ElectionContext}) {
  return <section className="panel mode-details"><div className="panel-head"><strong>Historical comparisons</strong><span>{context.year} · {context.office}</span></div><div className="pad"><div className="notice">No verified historical results have been imported. County and district comparisons require matching boundary versions.</div><p className="muted">Primary analysis baselines are 2022 and 2024 where office and geography match. This prototype contains no certified historical returns.</p></div></section>;
}

export function PollingDashboard({context}:{context:ElectionContext}) {
  const forecasts=context.pollView==='forecasts';
  return <section className="panel mode-details"><div className="panel-head"><strong>{forecasts?'Published forecasts':'Individual surveys'}</strong><span>Provider: {context.provider}</span></div><div className="pad"><div className="notice">{forecasts?'No licensed publisher forecast is connected. Forecast probabilities are separate from survey support and reported votes.':'No licensed opinion surveys are connected. Pollster, sponsor, field dates, population and sample size must accompany a published record.'}</div><p className="muted">District views require records for the exact district; statewide surveys are not substituted. No polling figures are fabricated.</p></div></section>;
}

export function ExitSurveyDashboard({context}:{context:ElectionContext}) {
  return <section className="panel mode-details"><div className="panel-head"><strong>Election-day voter surveys</strong><span>Survey year: {context.year}</span></div><div className="pad"><div className="notice">No authorized voter survey is available. Methodology, population, sample size and release time must accompany a published survey.</div><p className="muted">Survey responses do not represent county vote totals. Findings remain unavailable until an authorized source publishes them.</p></div></section>;
}

export default function ModeDetails({ context }:{context:ElectionContext}) {
  if(context.mode==='live') return null;
  if(context.mode==='history') return <HistoricalResultsDashboard context={context}/>;
  if(context.mode==='polls') return <PollingDashboard context={context}/>;
  return <ExitSurveyDashboard context={context}/>;
}
