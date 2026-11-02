'use client';
import { useEffect, useState } from 'react';
export default function EasternClock(){
  const [time,setTime]=useState('');
  useEffect(()=>{const update=()=>setTime(new Intl.DateTimeFormat('en-US',{timeZone:'America/New_York',hour:'numeric',minute:'2-digit',second:'2-digit'}).format(new Date()));update();const id=window.setInterval(update,1000);return()=>window.clearInterval(id);},[]);
  return <span className="eastern-clock" aria-label={time?`Current Eastern Time ${time}`:'Current Eastern Time'}><strong>{time||'—'}</strong><small>LIVE CLOCK · ET</small></span>;
}
