'use client';
import { useEffect, useState } from 'react';
import { checkEnvelope, type Envelope } from '../api';

type Source = { id:string; name:string; connection:string; licenseVerified:boolean; coverageVerified:boolean };
export default function SourceStatus({ onClose, demo }:{ onClose:()=>void; demo:boolean }) {
  const [sources,setSources] = useState<Source[]>([]);
  const [message,setMessage] = useState('Loading source status…');
  useEffect(()=>{
    const controller=new AbortController();
    fetch('/api/v1/sources/health',{signal:controller.signal,cache:'no-store'}).then(async response=>{
      if(!response.ok) throw new Error('Source status unavailable');
      const body=checkEnvelope(await response.json() as Envelope<Source[]>);
      setSources(body.data); setMessage(demo?'No external provider is connected in demo mode.':'No verified external provider is connected.');
    }).catch(error=>{ if(error.name!=='AbortError') setMessage('Source status unavailable.'); });
    return ()=>controller.abort();
  },[demo]);
  return <section className="source-panel" aria-labelledby="source-title"><div className="panel-head"><strong id="source-title">Sources & methodology</strong><button onClick={onClose}>Close</button></div><div className="pad"><p>{message}</p>{sources.map(s=><div className="source-line" key={s.id}><strong>{s.name}</strong><span>{s.connection} · License {s.licenseVerified?'verified':'unverified'} · Coverage {s.coverageVerified?'verified':'unverified'}</span></div>)}</div></section>;
}
