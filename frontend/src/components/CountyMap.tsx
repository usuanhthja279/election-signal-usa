'use client';
import { useState } from 'react';

export type County = { n:string; f:string; p:string };
export function CountyTooltip({county}:{county:County|null}) {
  return <div className="county-tooltip" aria-live="polite">{county?<><strong>{county.n}</strong><span>No verified result · Source unavailable · Counted percentage unavailable · No source timestamp</span></>:<span>Hover, focus, or select a county. Gray indicates no verified data.</span>}</div>;
}
export default function CountyMap({ counties,selected,onSelect }:{counties:County[];selected:string|null;onSelect:(id:string)=>void}) {
  const [highlight,setHighlight]=useState<string|null>(null);
  const active=counties.find(c=>c.f===(highlight||selected));
  return <><div className="map"><svg viewBox="0 0 835 524" role="group" aria-label="Select a county">{counties.map(c=><path key={c.f} d={c.p} tabIndex={0} role="button" aria-label={c.n} aria-pressed={selected===c.f} onMouseEnter={()=>setHighlight(c.f)} onMouseLeave={()=>setHighlight(null)} onFocus={()=>setHighlight(c.f)} onBlur={()=>setHighlight(null)} onClick={()=>onSelect(c.f)} onKeyDown={e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();onSelect(c.f);}}}><title>{c.n}: no verified results available</title></path>)}</svg></div><CountyTooltip county={active||null}/></>;
}
