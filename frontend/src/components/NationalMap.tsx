'use client';
import { useState } from 'react';
export default function NationalMap({ states,names,onSelect,onHighlight }:{ states:Record<string,{map:string}>, names:Record<string,string>, onSelect:(state:string)=>void, onHighlight:(state:string|null)=>void }) {
  const [zoom,setZoom]=useState(1);
  const width=980/zoom, height=568/zoom;
  return <div className="national-map-content"><div className="map-zoom" aria-label="Map zoom controls"><button type="button" aria-label="Zoom map in" onClick={()=>setZoom(z=>Math.min(2,z+.25))}>+</button><button type="button" aria-label="Zoom map out" onClick={()=>setZoom(z=>Math.max(1,z-.25))}>−</button><button type="button" onClick={()=>setZoom(1)}>Reset map zoom</button></div><svg viewBox={`${(980-width)/2} ${(568-height)/2} ${width} ${height}`} role="group" aria-label="Select a state" onMouseLeave={()=>onHighlight(null)}>{Object.entries(states).map(([id,shape])=><path key={id} d={shape.map} tabIndex={0} role="button" aria-label={names[id]||id} onMouseEnter={()=>onHighlight(id)} onFocus={()=>onHighlight(id)} onBlur={()=>onHighlight(null)} onClick={()=>onSelect(id)} onKeyDown={e=>{if(e.key==='Enter'||e.key===' '){e.preventDefault();onSelect(id);}}}><title>{names[id]||id} · select to open state dashboard</title></path>)}</svg></div>;
}
