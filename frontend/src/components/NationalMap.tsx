'use client';
export default function NationalMap({ states,names,onSelect,onHighlight }:{ states:Record<string,{map:string}>, names:Record<string,string>, onSelect:(state:string)=>void, onHighlight:(state:string|null)=>void }) {
  return <svg viewBox="0 0 980 568" role="group" aria-label="Select a state" onMouseLeave={()=>onHighlight(null)}>{Object.entries(states).map(([id,shape])=><path key={id} d={shape.map} tabIndex={0} role="button" aria-label={names[id]||id} onMouseEnter={()=>onHighlight(id)} onFocus={()=>onHighlight(id)} onBlur={()=>onHighlight(null)} onClick={()=>onSelect(id)} onKeyDown={e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();onSelect(id);}}}><title>{names[id]||id} · select to open state dashboard</title></path>)}</svg>;
}
