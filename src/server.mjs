/**
 * Election Signal Phase 0 - deliberately minimal local HTTP server.
 * Built entirely with Node.js standard library. No Docker, DB, network ingestion,
 * credentials, or external dependencies. NEVER represent demo data as live results.
 * Codex Phase 1 will replace these interfaces with Spring Boot implementation.
 */
import http from 'node:http';
import { readFile } from 'node:fs/promises';
import path from 'node:path';
import { fileURLToPath, pathToFileURL } from 'node:url';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const WEB = path.join(ROOT, 'web');
const DEMO = path.join(ROOT, 'data', 'demo');
const GEO = path.join(ROOT, 'data', 'geography');
const ALLOWED_MODES = new Set(['live', 'history', 'polls', 'exit']);
const ALLOWED_OFFICES = new Set(['all', 'house', 'senate', 'governor']);
const YEAR_SET = new Set(Array.from({length:13}, (_,i) => 2000 + i * 2));
const STATES = new Set('AL AK AZ AR CA CO CT DE DC FL GA HI ID IL IN IA KS KY LA ME MD MA MI MN MS MO MT NE NV NH NJ NM NY NC ND OH OK OR PA RI SC SD TN TX UT VT VA WA WV WI WY'.split(' '));
const MODES = {
  live: { label:'Live Results', status:'DEMO_ONLY', category:'reported_ballots' },
  history:{ label:'Past Results', status:'NOT_IMPORTED', category:'certified_historical_results' },
  polls:{ label:'Polls & Forecasts', status:'NOT_CONNECTED', category:'pre_election_surveys' },
  exit:{ label:'Exit Polls', status:'NOT_PUBLISHED', category:'election_day_voter_surveys' }
};
const MIME = { '.html':'text/html; charset=utf-8', '.js':'application/javascript; charset=utf-8', '.css':'text/css; charset=utf-8', '.json':'application/json; charset=utf-8', '.png':'image/png', '.svg':'image/svg+xml', '.ico':'image/x-icon' };

let fixtures;
async function loadFixtures() {
  if (!fixtures) {
    const [races,meta,sources,geometry] = await Promise.all([
      readFile(path.join(DEMO,'races.json'),'utf8'),
      readFile(path.join(DEMO,'meta.json'),'utf8'),
      readFile(path.join(DEMO,'sources.json'),'utf8'),
      readFile(path.join(GEO,'reference-boundaries.json'),'utf8')
    ]);
    fixtures = { races:JSON.parse(races), meta:JSON.parse(meta), sources:JSON.parse(sources), geography:JSON.parse(geometry) };
  }
  return fixtures;
}
function reply(res,status,payload) {
  res.writeHead(status, {
    'content-type':'application/json; charset=utf-8',
    'cache-control':'no-store, max-age=0',
    'x-content-type-options':'nosniff',
    'x-election-signal-mode':'DEMO',
  });
  res.end(JSON.stringify(payload));
}
function envelope(status, data, extra={}) {
  return {
    schemaVersion:'1.0',
    dataMode:'DEMO',
    isTestData:true,
    status,
    source:{ provider:'phase0-fixture', sourceAsOf:null, attribution:'Fictional fixture data - not an election data provider' },
    data,
    ...extra
  };
}
function paramError(res, message) { reply(res,400,{error:'INVALID_PARAMETER',message}); }
function serverError(res) { reply(res,500,{error:'INTERNAL_ERROR',message:'Local Phase 0 server error'}); }
function queryChoice(url,name,set,defaultValue,res){
  const v=(url.searchParams.get(name)||defaultValue).toLowerCase();
  if(!set.has(v)) { paramError(res,`Invalid ${name}. Allowed: ${[...set].join(', ')}`); return null; }
  return v;
}
function safeState(state,res){
  if (!state) return null;
  const s=state.toUpperCase();
  if (!STATES.has(s)) {paramError(res,'Unknown state abbreviation'); return false;}
  return s;
}
async function api(req,res,url) {
  const {races,meta,sources,geography}=await loadFixtures();
  const endpoint=url.pathname;
  if(endpoint==='/api/v1/health') return reply(res,200,{
    service:'election-signal-phase0', status:'ok', phase:0, dataMode:'DEMO', isTestData:true,
    connected:{live:false,history:false,polls:false,exitPolls:false,forecastModel:false},
    serverTimeUtc:new Date().toISOString(), uptimeSeconds:Math.floor(process.uptime())
  });
  if(endpoint==='/api/v1/meta') return reply(res,200,envelope('AVAILABLE_DEMO',meta));
  if(endpoint==='/api/v1/sources/health') return reply(res,200,envelope('NOT_CONNECTED',sources));
  if(endpoint==='/api/v1/context') {
    const mode=queryChoice(url,'mode',ALLOWED_MODES,'live',res);if(!mode)return;
    const office=queryChoice(url,'office',ALLOWED_OFFICES,'all',res);if(!office)return;
    let state=safeState(url.searchParams.get('state'),res);if(state===false)return;
    const yearRaw=url.searchParams.get('year');
    const year=mode==='history'? Number(yearRaw||2024):2026;
    if(mode==='history'&&!YEAR_SET.has(year))return paramError(res,'Past Results supports election years 2000, 2002, ... 2024');
    const context={mode,office,state,year,status:MODES[mode].status,category:MODES[mode].category,availableModes:Object.keys(MODES),forecastModelAvailable:false};
    return reply(res,200,envelope(MODES[mode].status,context));
  }
  if(endpoint==='/api/v1/demo/races') {
    const office=queryChoice(url,'office',ALLOWED_OFFICES,'all',res);if(!office)return;
    const state=safeState(url.searchParams.get('state'),res);if(state===false)return;
    let filtered=races.filter(r=>(office==='all'||r.office===office)&&(!state||r.state===state));
    return reply(res,200,envelope('AVAILABLE_DEMO',filtered,{total:filtered.length}));
  }
  if(endpoint.startsWith('/api/v1/demo/races/')) {
    const id=decodeURIComponent(endpoint.slice('/api/v1/demo/races/'.length));
    if(!/^[A-Za-z0-9_-]{1,80}$/.test(id))return paramError(res,'Invalid race ID');
    const race=races.find(r=>r.id===id);
    return reply(res,race?200:404,envelope(race?'AVAILABLE_DEMO':'NOT_FOUND',race||null));
  }
  const stateMatch=endpoint.match(/^\/api\/v1\/demo\/states\/([A-Z]{2})\/summary$/);
  if(stateMatch){
    const st=safeState(stateMatch[1],res);if(st===false)return;
    const stateRaces=races.filter(r=>r.state===st);
    return reply(res,200,envelope('AVAILABLE_DEMO',{state:st,contests:stateRaces,offices:['senate','governor','house'],hasSampleData:stateRaces.length>0}));
  }
  if(endpoint==='/api/v1/geography/manifest') {
    return reply(res,200,envelope('REFERENCE_ONLY',{
      states:Object.keys(geography.states).length,
      counties:Object.values(geography.counties).reduce((n,c)=>n+c.length,0),
      boundaryVersion:'V3 prototype reference; not verified 2026 legal district map',
      eligibleFor2026HouseDistrictAssignments:false
    }));
  }
  const geoMatch=endpoint.match(/^\/api\/v1\/geography\/states\/([A-Z]{2})\/counties$/);
  if(geoMatch){
    const st=safeState(geoMatch[1],res);if(st===false)return;
    const countyShapes=geography.counties[st]||[];
    return reply(res,200,envelope('REFERENCE_ONLY',{state:st,countyCount:countyShapes.length,countyShapes}));
  }
  if(endpoint==='/api/v1/history/results') {
    const year=Number(url.searchParams.get('year')||2024);
    if(!YEAR_SET.has(year))return paramError(res,'year must be one of the even election years 2000–2024');
    return reply(res,200,envelope('NOT_IMPORTED',[],{year,notice:'No verified historical returns have been imported in Phase 0.'}));
  }
  if(endpoint==='/api/v1/polls') return reply(res,200,envelope('NOT_CONNECTED',[],{notice:'No licensed poll provider connected. Do not treat UI example surveys as published polls.'}));
  if(endpoint==='/api/v1/forecasts/published') return reply(res,200,envelope('NOT_CONNECTED',[],{notice:'Published forecast provider is not connected.'}));
  if(endpoint==='/api/v1/exit-polls')return reply(res,200,envelope('NOT_PUBLISHED',[],{notice:'No authorized exit-poll feed. 2026 voter surveys may not have been published yet.'}));
  const modelMatch=endpoint.match(/^\/api\/v1\/models\/races\/([A-Za-z0-9_-]{1,80})$/);
  if(modelMatch)return reply(res,200,envelope('UNAVAILABLE_NOT_TRAINED',null,{
    raceId:modelMatch[1], modelVersion:null, probabilities:null,
    notice:'No validated live winning-probability model. Live Overview must never show simulated forecasts.'
  }));
  if(endpoint==='/api/v1/events'){
    res.writeHead(200,{
      'content-type':'text/event-stream; charset=utf-8',
      'cache-control':'no-cache, no-transform',
      'connection':'keep-alive',
      'x-accel-buffering':'no',
      'x-election-signal-mode':'DEMO'
    });
    res.write(`event: server_status\ndata: ${JSON.stringify({phase:0,dataMode:'DEMO',resultsStream:false,notice:'No live vote events connected'})}\n\n`);
    const timer=setInterval(()=>res.write(': keepalive\n\n'),25000);
    res.on('close',()=>clearInterval(timer));
    return;
  }
  return reply(res,404,{error:'NOT_FOUND',message:'No such Phase 0 API endpoint'});
}
async function staticFile(res, pathname){
  const relative= pathname==='/' ? 'index.html':decodeURIComponent(pathname).replace(/^\/+/, '');
  const file=path.resolve(WEB,relative);
  if(!(file===WEB||file.startsWith(WEB+path.sep))) return reply(res,403,{error:'FORBIDDEN'});
  const ext=path.extname(file);
  if(!MIME[ext])return reply(res,404,{error:'NOT_FOUND'});
  try {
    const bytes=await readFile(file);
    res.writeHead(200,{'content-type':MIME[ext],'cache-control':'no-cache','x-content-type-options':'nosniff'});
    return res.end(bytes);
  }catch(e){
    if(e.code==='ENOENT'||e.code==='EISDIR')return reply(res,404,{error:'NOT_FOUND'});
    return serverError(res);
  }
}
export function createApp(){
  return http.createServer(async (req,res)=>{
    try{
      const url=new URL(req.url||'/',`http://${req.headers.host||'localhost'}`);
      if(req.method!=='GET'&&req.method!=='HEAD')return reply(res,405,{error:'METHOD_NOT_ALLOWED'});
      if(url.pathname.startsWith('/api/v1/'))return api(req,res,url);
      if(url.pathname.startsWith('/api/'))return reply(res,404,{error:'NOT_FOUND'});
      return staticFile(res,url.pathname);
    }catch(e){
      // Do not expose stack traces or file paths to browsers.
      if(e instanceof URIError)return paramError(res,'Malformed URL encoding');
      console.error('[Phase 0 server]',e);
      return serverError(res);
    }
  });
}
const entry=process.argv[1]&&pathToFileURL(path.resolve(process.argv[1])).href;
if(entry===import.meta.url){
  const host=process.env.HOST||'127.0.0.1';
  const port=Number(process.env.PORT||4173);
  const app=createApp();
  app.listen(port,host,()=>{
    console.log(`Election Signal Phase 0 running at http://${host}:${app.address().port}`);
    console.log('DATA MODE: DEMO. No real election feeds, historical results, polling providers or forecast model are connected.');
    console.log('Health: /api/v1/health | Source status: /api/v1/sources/health');
  });
}
