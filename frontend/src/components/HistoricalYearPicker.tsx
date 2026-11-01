'use client';

import type { Mode } from '../context';

export default function HistoricalYearPicker({mode,year,onChange}:{mode:Extract<Mode,'history'|'exit'>;year:number;onChange:(year:number)=>void}) {
  const title=mode==='history'?'Election year':'Survey year';
  const lastYear=mode==='history'?2024:2026;
  const years=Array.from({length:(lastYear-2000)/2+1},(_,i)=>2000+i*2);
  return <label>{title}<select aria-label={title} value={year} onChange={event=>onChange(Number(event.target.value))}>{years.map(value=><option key={value} value={value}>{value}</option>)}</select></label>;
}
