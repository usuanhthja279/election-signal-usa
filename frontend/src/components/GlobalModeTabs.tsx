import type { Mode } from '../context';
export const modeLabels: Record<Mode,string> = { live:'Live Results', history:'Past Results', polls:'Polls & Forecasts', exit:'Exit Polls' };
export default function GlobalModeTabs({ mode,onSelect }:{mode:Mode;onSelect:(mode:Mode)=>void}) {
  const modes=Object.keys(modeLabels) as Mode[];
  return <div className="mode-tabs" role="tablist" aria-label="Election data context">{modes.map((m,index)=><button role="tab" aria-controls="election-view" aria-selected={mode===m} tabIndex={mode===m?0:-1} key={m} onClick={()=>onSelect(m)} onKeyDown={e=>{
    let target=index;
    if(e.key==='ArrowRight') target=(index+1)%modes.length;
    else if(e.key==='ArrowLeft') target=(index+modes.length-1)%modes.length;
    else if(e.key==='Home') target=0;
    else if(e.key==='End') target=modes.length-1;
    else return;
    e.preventDefault();onSelect(modes[target]);(e.currentTarget.parentElement?.children[target] as HTMLElement|undefined)?.focus();
  }}>{modeLabels[m]}</button>)}</div>;
}
