import type { DemoRace } from '../api';

export function CandidateVotes({ race,onOpen }:{race:DemoRace;onOpen?:()=>void}) {
  const label=race.office==='governor'?'Governors':race.office==='senate'?'Senate':'House';
  return <div className="race"><div className="race-title">{label} {race.district?`District ${race.district}`:''}<span>FICTIONAL EXAMPLE</span></div>{race.candidates.map(c=><div className="candidate" key={c.id}><span>{c.displayName} · {c.party}</span><strong>{c.votes===null?'Not reporting':c.votes.toLocaleString()+' demo votes'}</strong></div>)}<small>Estimated vote counted: {race.reporting.percent===null?'unavailable':race.reporting.percent+'%'} · fictional</small>{onOpen && <button className="secondary" onClick={onOpen}>View race →</button>}</div>;
}
export function RaceModelPanel() {
  return <div className="model"><strong>Model unavailable</strong><p>No validated model is connected. A reported vote share is not a win probability.</p></div>;
}
