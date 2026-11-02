import type { DemoRace } from './api';

/** Synthetic snapshots derived only from demo fixtures. Never pass reported observations here. */
export function demoSnapshot(race: DemoRace, step: number): DemoRace {
  const index=Math.max(0,Math.min(5,Math.trunc(step)));
  const progress=[0.15,0.32,0.50,0.68,0.84,1][index];
  const candidates=race.candidates.map((candidate,i)=>({ ...candidate, votes:candidate.votes===null?null:Math.round(candidate.votes*progress*(1+(5-index)*(i%2===0?0.008:-0.008))) }));
  const votesReported=candidates.every(c=>c.votes!==null)?candidates.reduce((sum,c)=>sum+(c.votes??0),0):null;
  return {...race,candidates,votesReported,reporting:{...race.reporting,percent:race.reporting.percent===null?null:Math.min(100,Math.round(race.reporting.percent*progress))}};
}
