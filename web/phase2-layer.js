/* Phase 2 prototype layer: preserves V7's visual system and replaces demo-only
 * lens content with truthful registry/source availability and URL state. */
(() => {
  const p2 = {
    registry: 'NOT_IMPORTED', results: 'NOT_CONNECTED', history: 'NOT_IMPORTED',
    polls: 'NOT_CONNECTED', exit: 'NOT_CONNECTED', districts: 'NOT_IMPORTED',
    geography: 'REFERENCE_ONLY', restoring: false,
  };
  const style = document.createElement('style');
  style.textContent = `.phase2-global-status{margin:0 22px 14px;padding:10px 14px;border:1px solid rgba(230,185,120,.34);border-radius:8px;background:linear-gradient(90deg,rgba(119,81,39,.2),rgba(16,29,48,.92));color:#d8e3f0;font-size:12px;line-height:1.55;display:flex;align-items:flex-start;gap:10px}.phase2-global-status strong{color:#f2c781;letter-spacing:.06em}.phase2-global-status .p2-dot{color:#efbd72;font-size:15px}.phase2-data-grid{display:grid;grid-template-columns:repeat(auto-fit,minmax(195px,1fr));gap:10px;margin:14px 0}.phase2-data-card{border:1px solid #293d59;border-radius:8px;background:#111f32;padding:13px}.phase2-data-card b{display:block;color:#e4b978;font-size:11px;letter-spacing:.08em}.phase2-data-card strong{display:block;color:#eaf1fb;font-size:15px;margin:7px 0 4px}.phase2-data-card small{color:#99abc3;line-height:1.45}.phase2-unavailable{border-left:3px solid #e4b978;padding:11px 13px;background:#18263a;color:#c3d0e2;font-size:12px;line-height:1.55}.phase2-neutral-map path[data-lens-state]{fill:#334760!important}.phase2-source-state{display:inline-flex;align-items:center;gap:7px;color:#e5bd80;font-size:11px;letter-spacing:.05em}.phase2-source-state:before{content:"";width:7px;height:7px;border-radius:50%;background:#d6a85e}.phase2-detail-list{display:grid;gap:8px}.phase2-detail-row{display:flex;justify-content:space-between;gap:12px;padding:10px 0;border-bottom:1px solid #263a55;color:#dce6f3;font-size:12px}.phase2-detail-row span:last-child{color:#e6bd80;font-weight:700;text-align:right}.lens-phase2-note{margin-top:12px}@media(max-width:640px){.phase2-global-status{margin:0 12px 10px;padding:9px 10px;font-size:11px}.phase2-data-grid{grid-template-columns:1fr 1fr}.phase2-data-card{padding:10px}.phase2-detail-row{align-items:flex-start}}`;
  document.head.append(style);
  const years = Array.from({ length: 13 }, (_, i) => 2000 + i * 2);
  const originalNav = nav;
  const originalRenderLens = renderLens;
  const escape = value => lensEsc(String(value ?? ''));
  const currentYear = () => LENS.mode === 'history' ? LENS.year :
    LENS.mode === 'exit' ? LENS.exitYear : 2026;

  function syncUrl(replace = false) {
    if (p2.restoring) return;
    const query = new URLSearchParams({
      mode: LENS.mode, year: String(currentYear()), office: activeOffice || 'all',
      provider: LENS.pollProvider || 'all', pollView: LENS.pollView || 'surveys',
    });
    if (activeState) query.set('state', activeState);
    if (pageName && pageName !== 'overview') query.set('page', pageName);
    // There is no verified district registry to support a valid district ID.
    const next = `${location.pathname}?${query.toString()}`;
    if (next === `${location.pathname}${location.search}`) return;
    history[replace ? 'replaceState' : 'pushState']({ phase2: true }, '', next);
  }

  function readUrl() {
    const q = new URLSearchParams(location.search);
    const modes = ['live', 'history', 'polls', 'exit'];
    LENS.mode = modes.includes(q.get('mode')) ? q.get('mode') : 'live';
    const requestedYear = Number(q.get('year'));
    LENS.year = years.includes(requestedYear) ? requestedYear : 2024;
    LENS.exitYear = [2026, 2024, 2022, 2020, 2018].includes(requestedYear) ? requestedYear : 2026;
    if (LENS.mode === 'live' || LENS.mode === 'polls') LENS.year = 2026;
    const offices = ['all', 'house', 'senate', 'governors'];
    activeOffice = offices.includes(q.get('office')) ? q.get('office') : 'all';
    LENS.pollProvider = Object.hasOwn(LENS_PROVIDER_INFO, q.get('provider')) ? q.get('provider') : 'all';
    LENS.pollView = ['surveys', 'forecasts'].includes(q.get('pollView')) ? q.get('pollView') : 'surveys';
    const state = (q.get('state') || '').toUpperCase();
    activeState = Object.hasOwn(STATES, state) ? state : null;
    const pages = ['overview', 'office', 'state', 'county', 'race', 'history', 'sources'];
    pageName = pages.includes(q.get('page')) ? q.get('page') : 'overview';
    if (['state', 'county', 'race'].includes(pageName) && !activeState) pageName = 'overview';
    if (pageName === 'county' && LENS.mode !== 'history') pageName = 'state';
    stateFocus = activeOffice === 'all' ? 'all' : activeOffice;
    raceOffice = activeOffice === 'all' ? 'senate' : activeOffice;
    selectedDistrict = null;
    if (LENS.mode === 'exit') p2.exit = LENS.exitYear === 2026 ? 'NOT_PUBLISHED' : 'NOT_IMPORTED';
  }

  function statusBar() {
    const status = LENS.mode === 'live' ? `Registry ${p2.registry} · results ${p2.results}` :
      LENS.mode === 'history' ? `historical results ${p2.history} · geography ${p2.geography}` :
      LENS.mode === 'polls' ? `survey source ${p2.polls} · forecast source NOT_CONNECTED` :
      `exit survey ${p2.exit} · findings NOT_IMPORTED`;
    return `<div class="phase2-global-status" role="status"><span class="p2-dot">●</span><span><strong>PHASE 2 DATA STATUS</strong> · ${status}. No verified results or authorized survey provider is connected. Any V7 sample figures remain fictional.</span></div>`;
  }

  function statusCards() {
    const sets = {
      live: [['2026 race registry', 'NOT_IMPORTED', 'No verified state ballot contests loaded.'], ['Results feed', 'NOT_CONNECTED', 'No authorized live results provider enabled.'], ['House districts', 'NOT_IMPORTED', 'No reviewed, versioned district registry loaded.'], ['Reporting measure', 'UNAVAILABLE', 'No source supplied a reporting measure.']],
      history: [['Selected cycle', String(LENS.year), 'A selectable cycle does not mean results are imported.'], ['Historical results', 'NOT_IMPORTED', 'No certified, rights-reviewed result rows loaded.'], ['Map boundaries', 'REFERENCE_ONLY', 'Bundled outlines are not verified result geography.'], ['Comparisons', 'UNAVAILABLE', 'No office and geography matched comparison is loaded.']],
      polls: [['Election cycle', '2026', 'Surveys and publisher forecasts remain separate.'], ['Survey source', 'NOT_CONNECTED', 'No licensed survey provider is enabled.'], ['Published forecasts', 'NOT_CONNECTED', 'No publisher forecast is connected.'], ['District coverage', 'UNAVAILABLE', 'No district-specific survey records imported.']],
      exit: [['Survey year', String(LENS.exitYear), 'Selected cycle only; source coverage is not established.'], ['Survey release', p2.exit, 'No authorized survey findings are loaded.'], ['Survey records', 'NOT_IMPORTED', 'No supported observations available.'], ['County estimates', 'UNAVAILABLE', 'Survey samples are not county vote totals.']],
    }[LENS.mode];
    return `<div class="phase2-data-grid">${sets.map(([label, value, note]) => `<div class="phase2-data-card"><b>${escape(label)}</b><strong>${escape(value)}</strong><small>${escape(note)}</small></div>`).join('')}</div>`;
  }

  function neutralMapLegend() {
    return '<span><i class="lens-dot lens-none"></i>No verified result coverage</span><span>Neutral geography · boundaries are reference-only</span>';
  }

  // The V7 map silhouette and navy panel system remain; fabricated colors do not.
  lensStateColor = () => '#334760';
  lensMapLegend = neutralMapLegend;

  lensProviderCards = () => `<div class="lens-stat-alert">Provider candidates are disabled in this prototype. No endpoint, license, coverage, or source health has been verified for an active integration.</div><div class="phase2-detail-list"><div class="phase2-detail-row"><span>Live election results</span><span>NOT_CONNECTED</span></div><div class="phase2-detail-row"><span>Historical result files</span><span>NOT_IMPORTED</span></div><div class="phase2-detail-row"><span>Polls and forecasts</span><span>NOT_CONNECTED</span></div><div class="phase2-detail-row"><span>Exit surveys</span><span>NOT_CONNECTED</span></div></div>`;

  // With no source-approved survey cycles or active polling provider, controls
  // explain the unavailable state instead of implying a supported feed.
  lensContext = () => `<div class="lens-contextbar">
    ${LENS.mode === 'history' ? `<label for="historyYear">Election year</label><select id="historyYear">${LENS_YEARS.map(y => `<option value="${y}" ${LENS.year === y ? 'selected' : ''}>${y}</option>`).join('')}</select>` : ''}
    ${LENS.mode === 'polls' ? `<label for="pollView">Display</label><select id="pollView"><option value="surveys" ${LENS.pollView === 'surveys' ? 'selected' : ''}>Opinion polls</option><option value="forecasts" ${LENS.pollView === 'forecasts' ? 'selected' : ''}>Published forecasts</option></select><label>Provider</label><span class="lens-indicator">No provider enabled · NOT_CONNECTED</span>` : ''}
    ${LENS.mode === 'exit' ? '<label>Survey year</label><span class="lens-indicator">No source-supported survey year imported · NOT_CONNECTED</span>' : ''}
    <div class="lens-indicator">● <strong>${LENS.mode === 'history' ? 'HISTORICAL DATA' : LENS.mode === 'exit' ? 'EXIT SURVEYS' : LENS.mode === 'polls' ? 'POLL / FORECAST' : 'LIVE RESULTS'}</strong> · No records are asserted without a verified source.</div>
  </div>`;

  // Phase 2 has no validated statistical model. Keep the individual-race panel
  // in V7's layout, with an unavailable state and no illustrative percentages.
  modelExperience = () => `<div class="model-status-card" role="status"><div><div class="model-kicker">INDIVIDUAL RACE MODEL</div><div class="m-head">MODEL_UNAVAILABLE</div><div class="m-sub">No validated model is connected. A win probability is not derived from reported share, vote lead, or provider projections.</div></div></div>`;

  lensNational = () => `<div class="lens-hero"><div><div class="lens-eyebrow">${escape(LENS_MODES[LENS.mode])} · ELECTION DATA</div><h1>Report what sources can verify.</h1><p>The V7 national layout stays intact while each view reflects current registry and source availability.</p></div><span class="lens-chip">${LENS.mode === 'history' ? LENS.year : 2026} · PHASE 2 PREVIEW</span></div>${lensOfficeNav()}${statusCards()}<div class="lens-layout"><div class="lens-panel"><div class="lp-head"><div><strong>United States · ${escape(OFFICE[activeOffice]?.name || 'All offices')}</strong><br><span>Choose a state to inspect registry and source status</span></div><span class="lens-chip">Reference geography</span></div><div class="phase2-neutral-map">${lensMap()}</div><div class="lens-legend">${neutralMapLegend()}</div><div class="lens-stat-alert lens-phase2-note">No race is inferred from geography. House results require a registered district; no county-to-district allocation is shown.</div></div><div class="lens-panel"><div class="lp-head"><strong>${LENS.mode === 'live' ? '2026 registry coverage' : LENS.mode === 'history' ? 'Historical availability' : LENS.mode === 'polls' ? 'Polling and forecast availability' : 'Exit survey availability'}</strong><span class="phase2-source-state">${LENS.mode === 'live' || LENS.mode === 'history' ? 'NOT_IMPORTED' : 'NOT_CONNECTED'}</span></div><div class="lp-pad"><div class="phase2-detail-list"><div class="phase2-detail-row"><span>Registry races</span><span>NOT_IMPORTED</span></div><div class="phase2-detail-row"><span>Source attribution</span><span>Unavailable</span></div><div class="phase2-detail-row"><span>Source as of</span><span>Unavailable</span></div><div class="phase2-detail-row"><span>National win probabilities</span><span>Not displayed</span></div></div><div class="lens-stat-alert lens-phase2-note">No source-grounded contest coverage is loaded. This view makes no claim about whether a specific office is on the ballot.</div></div></div></div>`;

  lensState = () => {
    const name = STATES[activeState] || activeState;
    const rows = [['house', 'House', 'NOT_IMPORTED'], ['senate', 'Senate', 'NOT_IMPORTED'], ['governors', 'Governor', 'NOT_IMPORTED']];
    return `<div class="lens-hero"><div><div class="lens-eyebrow"><button class="lens-button ghost" data-lens-back="overview">← National ${escape(LENS_MODES[LENS.mode])}</button></div><h1>${escape(name)} elections.</h1><p>Ballot eligibility cannot be inferred until a verified contest registry is loaded.</p></div><span class="lens-chip">${escape(activeState)} · ${currentYear()}</span></div>${lensOfficeNav()}${statusCards()}<div class="lens-layout"><div class="lens-panel"><div class="lp-head"><strong>${escape(name)} · office registry status</strong><span>Source status</span></div><div class="lp-pad">${rows.map(([id, label, status]) => `<div class="lens-line"><div><b>${label}</b><small>${id === 'house' ? 'District identity requires reviewed registry metadata' : 'Contest subtype and ballot status require an authoritative source'}</small></div><span class="value">${status}</span><button class="lens-button ghost" data-lens-go="${id}">Inspect ↗</button></div>`).join('')}</div></div><div class="lens-panel"><div class="lp-head"><strong>Geography and provenance</strong><span>Reference-only</span></div><div class="lp-pad"><div class="phase2-detail-row"><span>County shapes</span><span>REFERENCE_ONLY</span></div><div class="phase2-detail-row"><span>2026 House districts</span><span>NOT_IMPORTED</span></div><div class="phase2-detail-row"><span>County reporting</span><span>NOT_CONNECTED</span></div><div class="lens-stat-alert lens-phase2-note">County boundaries are not House district boundaries. No unsupported result coloring or allocation is displayed.</div></div></div></div>`;
  };

  lensCounty = () => `<div class="lens-hero"><div><div class="lens-eyebrow"><button class="lens-button ghost" data-lens-back="state">← ${escape(STATES[activeState])}</button></div><h1>${escape(STATES[activeState])} geography.</h1><p>County and district records are separate geographic units.</p></div></div><div class="lens-panel"><div class="lp-head"><strong>County result coverage</strong><span>NOT_IMPORTED</span></div><div class="lp-pad"><div class="phase2-data-grid"><div class="phase2-data-card"><b>County results</b><strong>NOT_IMPORTED</strong><small>No verified observation or county-level result is loaded.</small></div><div class="phase2-data-card"><b>Boundary vintage</b><strong>REFERENCE_ONLY</strong><small>Bundled prototype geography is not validated for this election.</small></div><div class="phase2-data-card"><b>House districts</b><strong>SEPARATE GEOGRAPHY</strong><small>Counties can overlap multiple congressional districts.</small></div></div><div class="lens-stat-alert">No county coloring is shown because no verified county result is available for this context.</div></div></div>`;

  lensRace = () => {
    const title = raceOffice === 'house' ? `House district ${selectedDistrict || '—'}` : OFFICE[raceOffice]?.name || OFFICE[activeOffice].name;
    return `<div class="lens-hero"><div><div class="lens-eyebrow"><button class="lens-button ghost" data-lens-back="state">← ${escape(STATES[activeState])}</button></div><h1>${escape(STATES[activeState])} · ${escape(title)}.</h1><p>Reported votes, provider calls, certification, polling and model estimates remain distinct.</p></div><span class="lens-chip">${currentYear()} · DATA UNAVAILABLE</span></div>${raceOffice === 'house' ? '<div class="lens-contextbar"><span class="phase2-source-state">2026 HOUSE DISTRICT REGISTRY · NOT_IMPORTED</span><span class="lens-indicator">No district is selected because no verified district registry is loaded.</span></div>' : ''}<div class="lens-layout"><div class="lens-panel"><div class="lp-head"><strong>${LENS.mode === 'history' ? 'Historical result' : LENS.mode === 'polls' ? 'Poll record' : 'Reported result'}</strong><span>NOT_IMPORTED</span></div><div class="lp-pad"><div class="lens-stat-alert">No verified record is available for this exact race and geography. A missing record does not mean zero votes or NOT_ON_BALLOT.</div><div class="phase2-detail-row"><span>Source and source-as-of</span><span>Unavailable</span></div><div class="phase2-detail-row"><span>Reporting measure</span><span>Unavailable</span></div><div class="phase2-detail-row"><span>Provider projection / certification</span><span>Not established</span></div></div></div><div class="lens-panel"><div class="lp-head"><strong>Individual race model</strong><span>MODEL_UNAVAILABLE</span></div><div class="lp-pad"><div class="lens-stat-alert">No validated model is connected. No win probability is calculated from vote share or current leads.</div></div></div></div>`;
  };

  renderLens = function () {
    originalRenderLens();
    const root = $('page-lens');
    if (!root) return;
    root.classList.add('phase2-prototype');
    root.querySelector('.phase2-local-status')?.remove();
    const footer = root.querySelector('.lens-bottom');
    if (footer) footer.textContent = 'PHASE 2 PREVIEW · Static prototype · No verified live results, historical records, polls, forecasts, or exit-survey findings are connected.';
    root.querySelectorAll('select').forEach(select => select.addEventListener('change', () => {
      if (select.id === 'historyYear' && years.includes(Number(select.value))) LENS.year = Number(select.value);
      if (select.id === 'exitYear') LENS.exitYear = Number(select.value);
      syncUrl();
    }, { once: true }));
    syncUrl();
  };

  nav = function (name, office = null) {
    if (office && office !== activeOffice) selectedDistrict = null;
    originalNav(name, office);
    syncUrl();
  };

  const oldSetMode = setGlobalMode;
  setGlobalMode = function (mode) {
    oldSetMode(mode);
    if (mode === 'live' || mode === 'polls') LENS.year = 2026;
    if (mode === 'history' && !years.includes(LENS.year)) LENS.year = 2024;
    syncUrl();
  };

  function showFromUrl() {
    p2.restoring = true;
    readUrl();
    document.querySelectorAll('.lens-tab').forEach(tab => tab.setAttribute('aria-selected', String(tab.dataset.lens === LENS.mode)));
    document.body.dataset.lens = LENS.mode;
    if (LENS.mode === 'live') {
      document.querySelector('#page-lens').classList.remove('active');
      originalNav(pageName, activeOffice === 'all' ? null : activeOffice);
    } else {
      const target = $('page-lens');
      document.querySelectorAll('.page').forEach(page => page.classList.toggle('active', page === target));
      renderLens();
    }
    p2.restoring = false;
  }

  window.addEventListener('popstate', showFromUrl);
  const notice = document.createElement('div');
  notice.className = 'phase2-global-status';
  notice.setAttribute('role', 'status');
  notice.innerHTML = '<span class="p2-dot">●</span><span><strong>PHASE 2 PREVIEW</strong> · No verified 2026 race registry, historical result imports, or authorized results/survey provider is loaded. V7 sample figures remain fictional.</span>';
  document.querySelector('.demo-alert')?.insertAdjacentElement('afterend', notice);

  readUrl();
  document.querySelectorAll('.lens-tab').forEach(tab => tab.setAttribute('aria-selected', String(tab.dataset.lens === LENS.mode)));
  document.body.dataset.lens = LENS.mode;
  if (LENS.mode !== 'live') showFromUrl();
  else if (pageName !== 'overview') originalNav(pageName, activeOffice === 'all' ? null : activeOffice);
  syncUrl(true);
})();
