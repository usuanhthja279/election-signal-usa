import type { DemoRace } from '../api';
import type { Mode } from '../context';
export default function StateSnapshot({ state,name,mode,races,demo }:{ state:string; name:string; mode:Mode; races:DemoRace[]; demo:boolean }) {
  return <div className="snapshot"><div className="eyebrow">STATE SNAPSHOT · {demo?'DEMO':'SOURCE STATUS'}</div><h2>{name}</h2>{mode!=='live'||!demo?<p className="muted">No connected source for this context.</p>:(['senate','governor','house'] as const).map(office=>{
    const matches=races.filter(r=>r.state===state&&r.office===office);
    return <div className="snapshot-line" key={office}><strong>{office==='governor'?'Governor':office==='house'?'House':'Senate'}</strong>{matches.length ? matches.map(r=><span key={r.id}>{r.district?`District ${r.district} · `:''}{r.reporting.percent===null?'Not reporting':`${r.reporting.percent}% estimated vote counted`} · fictional<br/>{r.candidates.map(c=>`${c.party}: ${c.votes===null?'not reporting':c.votes.toLocaleString()+' demo votes'}`).join(' · ')}</span>):<span>No sample data in this view</span>}</div>;
  })}{demo && mode==='live' && <p className="muted">Source: fictional fixture · Source timestamp unavailable</p>}</div>;
}
