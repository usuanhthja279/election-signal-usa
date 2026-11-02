'use client';
import { useEffect, useRef, useState } from 'react';
import type { County } from './CountyMap';

const sections=[
  {id:'A',color:'#54b6c9',index:0},
  {id:'B',color:'#efbd70',index:1},
  {id:'C',color:'#c6a7ef',index:2},
] as const;
type Box={x:number;y:number;width:number;height:number};

type Props={
  state:string;stateName:string;countyFips:string;counties:County[];year:number;
  mode:string;demo:boolean;onSelectCounty:(fips:string)=>void;onBack:()=>void;onState:()=>void;
};

/** Clearly schematic navigation preview; never represents real districts or vote allocation. */
export default function HouseCountyDistrictPreview(p:Props){
  const county=p.counties.find(item=>item.f===p.countyFips)||null;
  const [selected,setSelected]=useState('B');
  const [bounds,setBounds]=useState<Box|null>(null);
  const svgRef=useRef<SVGSVGElement>(null);
  const clipId=`county-reference-${p.state}-${p.countyFips}`;
  const select=(id:string)=>setSelected(id);
  useEffect(()=>{const outline=svgRef.current?.querySelector<SVGPathElement>('.county-reference-outline:not(.overlay)');if(!outline)return;try{const box=outline.getBBox();if(box.width>0&&box.height>0)setBounds({x:box.x,y:box.y,width:box.width,height:box.height});}catch{/* Geometry may not be measurable until the browser paints the SVG. */}},[county?.f]);
  const mapBounds=bounds?.width&&bounds.height?bounds:null;
  const padX=mapBounds?mapBounds.width*.18:0;const padY=mapBounds?mapBounds.height*.18:0;
  const shapeBox=mapBounds||{x:0,y:0,width:835,height:524};
  const border=(index:number)=>Array.from({length:17},(_,i)=>{const t=i/16;const offset=Math.sin(t*Math.PI*2.5+index*.8)*.075+Math.sin(t*Math.PI*4.5+index*1.6)*.018;return [shapeBox.x+shapeBox.width*((index===1?.345:.66)+offset),shapeBox.y+shapeBox.height*t];});
  const first=border(1),second=border(2);const points=(items:number[][])=>items.map(([x,y])=>`${x} ${y}`).join(' L');
  const paths=[`M${shapeBox.x-4} ${shapeBox.y-4} L${points(first)} L${shapeBox.x-4} ${shapeBox.y+shapeBox.height+4} Z`,`M${points(first)} L${points([...second].reverse())} Z`,`M${points(second)} L${shapeBox.x+shapeBox.width+4} ${shapeBox.y+shapeBox.height+4} L${shapeBox.x+shapeBox.width+4} ${shapeBox.y-4} Z`];
  const viewBox=mapBounds?`${mapBounds.x-padX} ${mapBounds.y-padY} ${mapBounds.width+2*padX} ${mapBounds.height+2*padY}`:'0 0 835 524';
  return <>
    <div className="hero house-county-hero"><div><div className="eyebrow">HOUSE / COUNTY → DISTRICTS · {p.mode.toUpperCase()}</div><h1>{county?.n||'County'} <span>House district map.</span></h1><p>You selected this county from the House county map. Explore a schematic intersection preview. Verified district boundaries and source-grounded district results are not loaded.</p></div><span className="chip">{p.year} · REFERENCE GEOGRAPHY</span></div>
    <div className="house-county-toolbar"><span className="fips-chip">COUNTY FIPS {p.countyFips}</span><strong>{p.stateName} · {county?.n||'Selected county'}</strong><small>Click a schematic region to inspect its availability</small><label>Change county<select value={county?.f||''} onChange={event=>p.onSelectCounty(event.target.value)} aria-label="Change county"><option value="" disabled>Select a county</option>{p.counties.map(item=><option key={item.f} value={item.f}>{item.n}</option>)}</select></label></div>
    <div className="district-schematic-warning" role="note"><span aria-hidden="true">◇</span><div><strong>DEMO DISTRICT DIVISIONS · NOT VERIFIED 2026 BOUNDARIES</strong><p>The county outline is reference geography. Colored sections are intentionally schematic, not congressional district shapes or IDs. A county can intersect one or more districts. No votes are divided or allocated by polygon area.</p></div></div>
    <div className="house-county-grid">
      <section className="panel"><div className="panel-head"><strong>{county?.n||'County'} · county map</strong><span>Three illustrative sections · select to inspect</span></div><div className="district-schematic-map"><svg ref={svgRef} viewBox={viewBox} role="group" aria-label={`${county?.n||'Selected'} County reference outline with schematic sections, not real congressional districts`}>
        {county&&<defs><clipPath id={clipId} clipPathUnits="userSpaceOnUse"><path d={county.p}/></clipPath></defs>}
        {county?<><path d={county.p} className="county-reference-outline"/><g clipPath={`url(#${clipId})`}>{sections.map(section=>{return <path key={section.id} d={paths[section.index]} style={{fill:section.color}} className={`district-schematic-section ${selected===section.id?'is-selected':''}`} tabIndex={0} role="button" aria-label={`Select illustrative section ${section.id}; schematic only, no official district assignment`} aria-pressed={selected===section.id} onMouseEnter={()=>{if(!svgRef.current?.contains(document.activeElement))select(section.id)}} onFocus={()=>select(section.id)} onClick={()=>select(section.id)} onKeyDown={event=>{if(event.key==='Enter'||event.key===' '){event.preventDefault();select(section.id)}}}><title>Illustrative section {section.id}; not an official district</title></path>;})}</g><path d={county.p} className="county-reference-outline overlay"/></>:<text x="40" y="260" className="map-unavailable-label">Reference county geometry unavailable</text>}
      </svg><span className="schematic-tag">SCHEMATIC OVERLAY · COUNTY OUTLINE IS REFERENCE GEOGRAPHY</span></div><div className="district-map-help">Click, focus, or use Enter/Space on a section. The selected section only changes the inspector.</div></section>
      <section className="panel district-inspector" aria-live="polite"><div className="panel-head"><strong>District inspector</strong><span>Geographic intersection preview</span></div><div className="pad"><div className="eyebrow">CURRENTLY INSPECTING</div><h2>Segment {selected}</h2><p className="muted">This region is a schematic interaction target. It has no canonical district ID or verified district boundary.</p><div className="schematic-section-list">{sections.map(section=><button key={section.id} className={selected===section.id?'active':''} aria-pressed={selected===section.id} onClick={()=>select(section.id)}><i style={{background:section.color}}/><span><strong>Segment {section.id}</strong><small>Schematic section · no district assignment</small></span></button>)}</div><div className="intersection-status-grid"><div><small>Actual House district</small><strong>Not mapped</strong></div><div><small>Candidate votes here</small><strong>Unavailable</strong></div><div><small>Estimated counted</small><strong>Unavailable</strong></div><div><small>Latest source update</small><strong>Not connected</strong></div></div><div className="notice intersection-notice">{p.mode==='history'?'No historical county/district crosswalk or certified result is imported.':p.mode==='polls'?'No district-specific survey source is connected.':p.mode==='exit'?'No authorized exit survey is available for this district or county.':'No verified 2026 county-to-district intersection or district result is loaded.'} {p.demo?' Demo vote fixtures do not enter this geography preview.':''}</div><div className="district-inspector-actions"><button className="secondary" onClick={p.onBack}>← Back to House county map</button><button className="secondary" onClick={p.onState}>View House state desk ↗</button></div></div></section>
    </div>
    <section className="panel production-geography-note"><div className="eyebrow">PRODUCTION MAP LOGIC</div><div className="production-logic-grid"><div><small>Geometry</small><strong>County ∩ House district</strong></div><div><small>Votes</small><strong>Use race- and district-specific reporting units</strong></div></div><p className="muted">Authoritative, election-applicable district boundaries and spatial intersections are not loaded. County-wide votes cannot be allocated to House districts by area. A verified version must be sourced before real divisions appear.</p></section>
  </>;
}
