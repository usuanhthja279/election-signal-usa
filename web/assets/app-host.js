/* Phase 0 host instrumentation. Intentionally independent of legacy V7 UI internals. */
(() => {
  'use strict';
  const CSS=`
    #phase0-host-btn{display:inline-flex;align-items:center;gap:7px;white-space:nowrap;border:1px solid #385377;background:#18283d;border-radius:9px;color:#c4d9f5;padding:8px 11px;font-size:11px;font-weight:800;cursor:pointer}
    #phase0-host-btn:hover{border-color:#6e9bd9;color:white}
    #phase0-host-btn .host-dot{display:inline-block;width:7px;height:7px;border-radius:50%;background:#d8ab6d}
    #phase0-host-btn.ok .host-dot{background:#6bd6b3}
    #phase0-host-dialog{position:fixed;inset:0;z-index:200;display:none;place-items:center;background:#030711c9;padding:20px;backdrop-filter:blur(4px)}
    #phase0-host-dialog.open{display:grid}
    #phase0-host-dialog .host-card{width:min(580px,100%);background:#111c2e;border:1px solid #38506d;border-radius:18px;color:#eff6ff;box-shadow:0 20px 80px #0009;padding:23px}
    #phase0-host-dialog .host-head{display:flex;align-items:center;justify-content:space-between;gap:8px}
    #phase0-host-dialog h2{font-size:19px;margin:0 0 12px}
    #phase0-host-dialog p{color:#a4b5cd;line-height:1.5;margin:9px 0}
    #phase0-host-dialog table{border-collapse:collapse;width:100%;font-size:12px;margin-top:15px}
    #phase0-host-dialog td{padding:9px;border-bottom:1px solid #293a50}
    #phase0-host-dialog td:last-child{text-align:right;color:#e8be78}
    #phase0-host-dialog a{color:#8fb9ff}
    #phase0-host-dialog .host-close{background:#21334c;border:1px solid #506384;border-radius:8px;color:#fff;padding:6px 10px;cursor:pointer}
    @media(max-width:790px){#phase0-host-btn{font-size:10px;padding:7px 9px}#phase0-host-dialog .host-card{padding:15px}}
  `;
  const style=document.createElement('style');style.textContent=CSS;document.head.append(style);
  const bar=document.querySelector('.topbar')||document.querySelector('header');
  if(!bar)return;
  const button=document.createElement('button');button.type='button';button.id='phase0-host-btn';button.innerHTML='<span class="host-dot"></span><span id="host-status-label">Checking local API</span>';
  const controls=bar.querySelector('.right-tools');(controls||bar).prepend(button);
  const dialog=document.createElement('div');dialog.id='phase0-host-dialog';dialog.setAttribute('role','dialog');dialog.setAttribute('aria-modal','true');dialog.setAttribute('aria-labelledby','host-modal-title');
  dialog.innerHTML=`<div class="host-card"><div class="host-head"><h2 id="host-modal-title">Phase 0 / Data connection</h2><button class="host-close" type="button" id="host-close">Close ×</button></div>
  <p><strong>Interactive V7 UI with a local API.</strong> All vote counts and poll graphics in the browser are fictional design fixtures. No provider is connected, no historical returns have been imported, and no winning-probability model has been trained.</p>
  <div id="host-detail">Checking the local server…</div>
  <p><a href="/api-explorer.html" target="_blank" rel="noopener noreferrer">Interactive API explorer</a> · <a href="/api/v1/health" target="_blank" rel="noopener noreferrer">Server health JSON</a> · <a href="/api/v1/sources/health" target="_blank" rel="noopener noreferrer">Source registry JSON</a></p>
  </div>`;
  document.body.append(dialog);
  const close=()=>{dialog.classList.remove('open');button.focus()};
  button.addEventListener('click',()=>{dialog.classList.add('open');document.getElementById('host-close').focus()});
  dialog.querySelector('#host-close').addEventListener('click',close);
  dialog.addEventListener('click',e=>{if(e.target===dialog)close()});
  document.addEventListener('keydown',e=>{if(e.key==='Escape'&&dialog.classList.contains('open'))close()});
  async function refresh(){
    const detail=document.getElementById('host-detail');
    try{
      const response=await fetch('/api/v1/health',{cache:'no-store',signal:AbortSignal.timeout(5000)});
      if(!response.ok)throw new Error('HTTP '+response.status);
      const h=await response.json();
      button.classList.add('ok');document.getElementById('host-status-label').textContent='Demo API connected';
      const data=[['Local server','Connected'],['Results feed','Not connected'],['Historical election returns','Not imported'],['Polls and exit polls','Not connected'],['Win-probability model','Not trained']];
      detail.textContent='';const table=document.createElement('table');
      for(const [k,v] of data){const row=table.insertRow();row.insertCell().textContent=k;row.insertCell().textContent=v}detail.append(table);
    }catch(err){
      button.classList.remove('ok');document.getElementById('host-status-label').textContent='Standalone UI / API offline';
      detail.textContent='The HTML UI is still usable, but no local API server was detected. Launch with Start-All.ps1 or npm start.';
    }
  }
  refresh();
})();
