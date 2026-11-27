// KORE Training · web app per iPhone (PWA). Interfaccia, stato e navigazione.
'use strict';

// =============================================================================== store
const STORE_KEY = 'kore_data';
const TIMER_KEY = 'kore_timer';

function defaultData() {
  return {
    profile: {
      name: '', sex: 'M', age: 30, heightCm: 175, weightKg: 75.0, bodyFatPct: 15.0, level: 'INTERMEDIO', split: 'SPLIT_A',
      daysPerWeek: 4, responder: 'NORMALE', activityFactor: 1.55, oneRm: {}, barKg: 20.0,
      plates: [25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25], onboarded: false
    },
    program: { phase: 0, meso: 0, week: 1, dayIndex: 0, startedAt: Date.now() },
    swaps: {}, active: null, history: [], bodyWeight: [], version: 1
  };
}

/** Merge a (possibly partial or Android-exported) object with the defaults. */
function normalize(raw) {
  const d = defaultData();
  if (!raw || typeof raw !== 'object') return d;
  const out = { ...d, ...raw };
  out.profile = { ...d.profile, ...(raw.profile || {}) };
  out.profile.oneRm = { ...(out.profile.oneRm || {}) };
  out.program = { ...d.program, ...(raw.program || {}) };
  out.swaps = { ...(raw.swaps || {}) };
  out.history = Array.isArray(raw.history) ? raw.history.map(normSession) : [];
  out.active = raw.active ? normSession(raw.active) : null;
  out.bodyWeight = Array.isArray(raw.bodyWeight) ? raw.bodyWeight.filter(e => e && e.epochDay != null && e.kg > 0) : [];
  if (!SPLITS[out.profile.split]) out.profile.split = 'SPLIT_A';
  return out;
}
function normSession(s) {
  return {
    id: s.id, title: s.title || 'Seduta', startedAt: s.startedAt, endedAt: s.endedAt ?? null,
    phase: s.phase || 0, meso: s.meso || 0, week: s.week || 1, dayIndex: s.dayIndex || 0,
    notes: s.notes || '', feeling: s.feeling || 3,
    exercises: (s.exercises || []).map(e => ({
      slotKey: e.slotKey, exerciseId: e.exerciseId, repMin: e.repMin, repMax: e.repMax, rir: e.rir, restSec: e.restSec,
      tempo: e.tempo || '2-0-1-0', technique: e.technique || 'NESSUNA', note: e.note || '',
      sets: (e.sets || []).map(x => ({ type: x.type || 'WORK', weight: +x.weight || 0, reps: +x.reps || 0, rir: x.rir ?? null, done: !!x.done }))
    }))
  };
}

let data;
try { data = normalize(JSON.parse(localStorage.getItem(STORE_KEY))); } catch (e) { data = defaultData(); }

function save() {
  try { localStorage.setItem(STORE_KEY, JSON.stringify(data)); } catch (e) { toast('Spazio di archiviazione non disponibile'); }
}
function update(fn) { fn(data); save(); }

// =============================================================================== utils
const $ = sel => document.querySelector(sel);
const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const mmss = sec => `${Math.floor(sec / 60)}:${String(Math.floor(sec % 60)).padStart(2, '0')}`;
const fmtKgInt = v => Calc.fmt(Math.round(v));
const catColor = c => c === 'BASE' ? 'var(--accent)' : c === 'COMPLEMENTARE' ? 'var(--blue)' : 'var(--good)';
const parseNum = v => { const n = parseFloat(String(v).replace(',', '.')); return isNaN(n) ? null : n; };

let toastTimer;
function toast(msg) {
  const t = $('#toast'); t.textContent = msg; t.classList.add('show');
  clearTimeout(toastTimer); toastTimer = setTimeout(() => t.classList.remove('show'), 2200);
}

// =============================================================================== icons
const ICONS = {
  home: '<path d="M3 10l9-7 9 7v10a2 2 0 0 1-2 2h-4v-7H9v7H5a2 2 0 0 1-2-2z"/>',
  calendar: '<rect x="3" y="4" width="18" height="18" rx="3"/><path d="M16 2v4M8 2v4M3 10h18"/>',
  chart: '<polyline points="22 6 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 6 22 6 22 12"/>',
  tools: '<path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z"/>',
  user: '<path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>',
  dumbbell: '<path d="M6.5 6.5v11M17.5 6.5v11M3 9v6M21 9v6M6.5 12h11"/>',
  scale: '<rect x="3" y="3" width="18" height="18" rx="5"/><path d="M7.5 10a6 6 0 0 1 9 0M12 10.5l1.5-2"/>',
  food: '<path d="M7 2v20M4 2v6a3 3 0 0 0 6 0V2M18 22V2c-2.5 1.5-4 4.5-4 9h4"/>',
  calc: '<rect x="4" y="2" width="16" height="20" rx="3"/><path d="M8 6h8M8 11h.01M12 11h.01M16 11h.01M8 15h.01M12 15h.01M16 15h.01M8 19h.01M12 19h.01M16 19h.01"/>',
  book: '<path d="M2 4h6a4 4 0 0 1 4 4v13a3 3 0 0 0-3-3H2z"/><path d="M22 4h-6a4 4 0 0 0-4 4v13a3 3 0 0 1 3-3h7z"/>',
  info: '<circle cx="12" cy="12" r="10"/><path d="M12 16v-4M12 8h.01"/>',
  swap: '<polyline points="17 1 21 5 17 9"/><path d="M3 11V9a4 4 0 0 1 4-4h14"/><polyline points="7 23 3 19 7 15"/><path d="M21 13v2a4 4 0 0 1-4 4H3"/>',
  check: '<polyline points="20 6 9 17 4 12"/>',
  plus: '<path d="M12 5v14M5 12h14"/>',
  timer: '<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2M9 2h6"/>',
  skip: '<polygon points="5 4 15 12 5 20 5 4"/><path d="M19 5v14"/>',
  more: '<circle cx="12" cy="5" r="1.3"/><circle cx="12" cy="12" r="1.3"/><circle cx="12" cy="19" r="1.3"/>',
  back: '<polyline points="15 18 9 12 15 6"/>',
  trophy: '<path d="M8 21h8M12 17v4M7 4h10v5a5 5 0 0 1-10 0z"/><path d="M17 5h3v2a3 3 0 0 1-3 3M7 5H4v2a3 3 0 0 0 3 3"/>',
  right: '<polyline points="9 18 15 12 9 6"/>',
  down: '<polyline points="6 9 12 15 18 9"/>',
  up: '<polyline points="18 15 12 9 6 15"/>',
  close: '<path d="M18 6L6 18M6 6l12 12"/>',
  play: '<polygon points="6 3 20 12 6 21 6 3"/>',
  trash: '<polyline points="3 6 5 6 21 6"/><path d="M19 6l-1 14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2L5 6M10 11v6M14 11v6M9 6V4h6v2"/>',
  tune: '<path d="M4 21v-7M4 10V3M12 21v-9M12 8V3M20 21v-5M20 12V3M1 14h6M9 8h6M17 16h6"/>',
  search: '<circle cx="11" cy="11" r="8"/><path d="M21 21l-4.35-4.35"/>',
  speed: '<path d="M12 14l4-4"/><path d="M3.3 19a10 10 0 1 1 17.4 0"/>',
  crown: '<path d="M3 18h18M4 15l-1-9 5 4 4-6 4 6 5-4-1 9z"/>',
  lock: '<rect x="4" y="11" width="16" height="10" rx="2"/><path d="M8 11V7a4 4 0 0 1 8 0v4"/>',
  share: '<path d="M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8"/><polyline points="16 6 12 2 8 6"/><path d="M12 2v13"/>'
};
const icon = (n, cls = '') => `<svg class="ic ${cls}" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">${ICONS[n] || ''}</svg>`;

// =============================================================================== components
const pill = (text, color = 'var(--accent)', filled = false) =>
  `<span class="pill${filled ? ' filled' : ''}" style="--c:${color}">${esc(text)}</span>`;
const section = (text, action = '') => `<div class="section"><span>${esc(text)}</span>${action}</div>`;
const card = (inner, { highlight = false, action = '', cls = '' } = {}) =>
  `<div class="card${highlight ? ' hl' : ''}${action ? ' tap' : ''} ${cls}" ${action}>${inner}</div>`;
const stat = (value, label, accent = false) => `<div class="stat${accent ? ' accent' : ''}"><b>${esc(value)}</b><span>${esc(label)}</span></div>`;
const kv = (k, v, color = '') => `<div class="kv"><span>${esc(k)}</span><b ${color ? `style="color:${color}"` : ''}>${esc(v)}</b></div>`;
const bullet = t => `<div class="bullet"><i></i><span>${esc(t)}</span></div>`;
const gradBtn = (text, action) => `<button class="grad-btn" ${action}>${esc(text)}</button>`;
const outBtn = (text, action, color = 'var(--accent)', extra = '') => `<button class="out-btn" style="color:${color}" ${action} ${extra}>${text}</button>`;
const segmented = (opts, sel, action) => `<div class="seg">${opts.map((o, i) =>
  `<button class="${i === sel ? 'on' : ''}" ${action(i)}>${esc(o)}</button>`).join('')}</div>`;
const topBar = (title, right = '') => `<header class="topbar"><button class="icon-btn" data-a="back" aria-label="Indietro">${icon('back')}</button><h1>${esc(title)}</h1><div class="tb-right">${right}</div></header>`;
const field = (label, value, attrs, suffix = '', decimal = true) => `<label class="field"><span>${esc(label)}</span><div class="field-in">
  <input type="text" inputmode="${decimal ? 'decimal' : 'numeric'}" value="${esc(value)}" ${attrs} autocomplete="off">${suffix ? `<em>${esc(suffix)}</em>` : ''}</div></label>`;

function lineChart(values, color = 'var(--accent)', height = 160) {
  if (values.length < 2) return `<div class="empty-chart" style="height:${height}px">Servono almeno due rilevazioni</div>`;
  const W = 320, H = height, pad = 8;
  const min = Math.min(...values), max = Math.max(...values), span = (max - min) || 1;
  const pts = values.map((v, i) => [pad + (W - pad * 2) * i / (values.length - 1), pad + (H - pad * 2) - (H - pad * 2) * ((v - min) / span)]);
  const line = pts.map((p, i) => `${i ? 'L' : 'M'}${p[0].toFixed(1)},${p[1].toFixed(1)}`).join('');
  const fill = `${line}L${pts[pts.length - 1][0].toFixed(1)},${H - pad}L${pts[0][0].toFixed(1)},${H - pad}Z`;
  const gid = 'g' + Math.random().toString(36).slice(2, 8);
  let grid = '';
  for (let g = 0; g <= 3; g++) { const y = pad + (H - pad * 2) * g / 3; grid += `<line x1="${pad}" y1="${y}" x2="${W - pad}" y2="${y}" stroke="var(--line)" stroke-width="1"/>`; }
  return `<svg class="chart" viewBox="0 0 ${W} ${H}" style="color:${color}"><defs><linearGradient id="${gid}" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="currentColor" stop-opacity=".35"/><stop offset="1" stop-color="currentColor" stop-opacity="0"/></linearGradient></defs>
    ${grid}<path d="${fill}" fill="url(#${gid})"/><path d="${line}" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
    ${pts.map(p => `<circle cx="${p[0].toFixed(1)}" cy="${p[1].toFixed(1)}" r="4" fill="var(--bg)"/><circle cx="${p[0].toFixed(1)}" cy="${p[1].toFixed(1)}" r="2.5" fill="currentColor"/>`).join('')}</svg>
    <div class="chart-range"><span>min ${Calc.fmt(Calc.round(min, 0.5))}</span><span>max ${Calc.fmt(Calc.round(max, 0.5))}</span></div>`;
}

function barChart(values, labels, height = 140) {
  if (!values.length) return '';
  const max = Math.max(...values) || 1;
  return `<div class="bars" style="height:${height}px">${values.map((v, i) =>
    `<div class="bar${i === values.length - 1 ? ' last' : ''}" style="height:${Math.max(2, 100 * v / max)}%"></div>`).join('')}</div>
    <div class="bar-labels">${labels.map(l => `<span>${esc(l)}</span>`).join('')}</div>`;
}

function volumeBar(label, sets, mev, mavHigh, zone) {
  const frac = Math.min(1, Math.max(0, sets / (mavHigh + 8)));
  const color = sets < mev ? 'var(--warn)' : sets <= mavHigh ? 'var(--good)' : 'var(--bad)';
  return `<div class="vbar"><div class="vbar-top"><span>${esc(label)}</span><b style="color:${color}">${sets.toFixed(1).replace('.', ',')} serie</b></div>
    <div class="track"><div style="width:${frac * 100}%;background:${color}"></div></div><small>${esc(zone)}</small></div>`;
}

// ---------------------------------------------- improvement (miglioramento % e atteso)
function improvementBox(imp) {
  const c = imp.e1rmPct == null ? 'var(--muted)' : imp.e1rmPct >= 0 ? 'var(--good)' : 'var(--bad)';
  const first = imp.prevE1rm == null;
  let left = `<small class="lbl">MIGLIORAMENTO</small>
    <b class="big" style="color:${c};font-size:${first ? 16 : 22}px">${first ? 'Prima volta' : imp.curE1rm <= 0 ? '—' : Progression.pct(imp.e1rmPct)}</b>
    <small>1RM stimato vs ultima seduta</small>`;
  if (imp.curE1rm > 0) left += `<span class="t11">${Calc.fmt(Calc.round(imp.curE1rm, 0.5))} kg${imp.prevE1rm != null ? ` (prima ${Calc.fmt(Calc.round(imp.prevE1rm, 0.5))})` : ''}</span>`;
  if (imp.volumePct != null) left += `<span class="t11 sb" style="color:${imp.volumePct >= 0 ? 'var(--good)' : 'var(--bad)'}">Volume ${Progression.pct(imp.volumePct)}</span>`;

  let right = `<small class="lbl">ATTESO A SEDUTA</small>
    <b class="big" style="color:var(--accent)">${imp.expectedPct == null ? '—' : Progression.pct(imp.expectedPct)}</b>
    <small>${imp.samples === 0 ? 'Servono dati delle sedute precedenti' : imp.samples === 1 ? 'Stima base (1 seduta nello storico)' : `Tendenza delle ultime ${imp.samples} sedute`}</small>`;
  if (imp.expectedE1rm != null) right += `<span class="t11">Obiettivo 1RM ~${Calc.fmt(Calc.round(imp.expectedE1rm, 0.5))} kg</span>`;
  if (imp.expectedPct != null && imp.e1rmPct != null && imp.curE1rm > 0) {
    const ok = imp.e1rmPct >= imp.expectedPct;
    right += `<span class="t11 sb" style="color:${ok ? 'var(--good)' : 'var(--warn)'}">${ok ? 'Sopra le attese ✓' : 'Sotto le attese'}</span>`;
  }
  let out = `<div class="imp"><div class="imp-l">${left}</div><div class="imp-r">${right}</div></div>`;
  if (imp.repsToBeat) out += `<div class="t11 beat">Per superare l'ultima seduta: ${Calc.fmt(imp.repsToBeat[0])} kg × ${imp.repsToBeat[1]} ripetizioni</div>`;
  return out;
}

// =============================================================================== router & ui state
const TABS = [['home', 'Oggi', 'home'], ['program', 'Programma', 'calendar'], ['progress', 'Progressi', 'chart'], ['tools', 'Strumenti', 'tools'], ['profile', 'Profilo', 'user']];
const ui = {
  programWeek: null, programOpen: null, progressTab: 0, progressEx: null, libQuery: '', libMuscle: null, guideOpen: 0,
  onerm: { w: '80', r: '8', rir: '0' }, plates: { target: '100', bar: null }, rirOrm: '100', bw: null, form: null,
  summary: null, modal: null, installHidden: false
};
try { ui.installHidden = localStorage.getItem('kore_install_hidden') === '1'; } catch (e) { /* ignore */ }

function route() {
  const h = location.hash.replace(/^#\/?/, '');
  const [name, arg] = h.split('/');
  return { name: name || (data.profile.onboarded ? 'home' : 'onboarding'), arg: arg ? decodeURIComponent(arg) : null };
}
function go(path, replace = false) {
  if (replace) location.replace('#/' + path); else location.hash = '#/' + path;
}
let lastRouteKey = '';
window.addEventListener('hashchange', () => render(true));

const VIEWS = {};

function render(routeChanged = false) {
  let r = route();
  if (!data.profile.onboarded && r.name !== 'onboarding') { go('onboarding', true); return; }
  if (r.name === 'workout' && !data.active) { go('home', true); return; }
  const key = r.name + '/' + (r.arg || '');
  const changed = key !== lastRouteKey;
  if (changed) {
    lastRouteKey = key;
    if (r.name === 'profile' || r.name === 'onboarding') ui.form = null;
  }
  const view = VIEWS[r.name] || VIEWS.home;
  const isTab = TABS.some(t => t[0] === r.name);
  $('#app').innerHTML = view(r.arg);
  $('#app').classList.toggle('with-tabs', isTab);
  $('#tabs').innerHTML = isTab ? TABS.map(([k, l, ic]) =>
    `<button class="${k === r.name ? 'on' : ''}" data-a="tab" data-v="${k}">${icon(ic)}<span>${l}</span></button>`).join('') : '';
  $('#tabs').style.display = isTab ? '' : 'none';
  renderOverlay();
  renderRestBar();
  if (changed && routeChanged) window.scrollTo(0, 0);
  tick();
}

// =============================================================================== HOME
VIEWS.home = () => {
  const st = data.program;
  const plan = Program.plan(data);
  const phase = PHASES[st.phase];
  const ws = weekStartDay(todayEpochDay());
  const thisWeek = data.history.filter(s => epochDayOf(s.startedAt) >= ws).length;
  const streak = Stats.streakWeeks(data.history);
  const abs = Program.absoluteWeek(st);
  const last = [...data.history].sort((a, b) => b.startedAt - a.startedAt)[0];
  const standalone = window.navigator.standalone === true || matchMedia('(display-mode: standalone)').matches;

  let h = `<div class="hero"><div class="row"><span class="logo">KORE</span><span class="grow"></span>${pill(`${phase.short} · SETT. ${st.week}/${Program.WEEKS_PER_MESO}`)}</div>
    <p class="muted mt18">${data.profile.name.trim() ? `Ciao ${esc(data.profile.name.trim())},` : 'Pronto ad allenarti?'}</p>
    <h2 class="headline">${plan.deload ? 'Settimana di scarico' : esc(phase.name)}</h2>
    <p class="muted small mt6">Mesociclo ${st.meso + 1}/${Program.MESO_PER_PHASE} · Microciclo ${abs} di ${Program.totalWeeks}</p>
    <div class="progress mt10"><div style="width:${abs / Program.totalWeeks * 100}%"></div></div></div><div class="pad">`;

  if (!standalone && !ui.installHidden) {
    h += `<div class="install">${icon('share')}<div><b>Installa KORE sull'iPhone</b><span>In Safari tocca <b>Condividi</b> e poi <b>Aggiungi alla schermata Home</b>. Così l'app funziona anche offline e i dati restano al sicuro sul telefono.</span></div><button class="icon-btn" data-a="hideInstall" aria-label="Chiudi">${icon('close')}</button></div>`;
  }

  h += section(data.active ? 'Allenamento in corso' : 'Prossimo allenamento');
  h += card(`<div class="row"><div class="grow"><h3 class="h-sm">${esc(data.active ? data.active.title : plan.template.name)}</h3><p class="muted">${esc(plan.template.muscles)}</p></div>
      <div class="badge-grad">${icon('dumbbell')}</div></div><div class="mt12">
      ${plan.items.map(pe => `<div class="row plan-li">${pill(CATEGORIES[pe.slot.category].short, catColor(pe.slot.category))}<span class="grow ell">${esc(pe.exercise.name)}</span><span class="muted t13">${pe.rx.sets}×${Program.repsLabel(pe.rx)}</span></div>`).join('')}
      </div><div class="mt14">${gradBtn(data.active ? 'Riprendi allenamento' : 'Inizia allenamento', `data-a="startWorkout" data-v="${st.dayIndex}"`)}</div>`, { highlight: true });

  h += section('Questa settimana');
  h += `<div class="stats">${stat(`${thisWeek}/${data.profile.daysPerWeek}`, 'Sedute', true)}${stat(streak, 'Settimane di fila')}${stat(data.history.length, 'Totale sedute')}</div>`;

  h += section('Scorciatoie');
  h += `<div class="shortcuts">${[['scale', 'Peso', 'bodyweight'], ['food', 'Dieta', 'nutrition'], ['calc', '1RM', 'onerm'], ['book', 'Metodo', 'guide']]
    .map(([ic, l, r]) => `<button data-a="go" data-v="${r}">${icon(ic)}<span>${l}</span></button>`).join('')}</div>`;

  if (last) {
    h += section('Ultima seduta');
    h += card(`<div class="t-md">${esc(last.title)}</div><div class="muted t13">${fmtDate(epochDayOf(last.startedAt))}</div>
      <div class="inline-kv mt8"><div><b>${Stats.setCount(last)}</b><span>serie</span></div><div><b>${fmtKgInt(Stats.volume(last))}</b><span>kg sollevati</span></div>
      ${last.endedAt ? `<div><b>${Math.floor((last.endedAt - last.startedAt) / 60000)}</b><span>min</span></div>` : ''}</div>`, { action: `data-a="go" data-v="session/${last.id}"` });
  }
  return h + '<div class="sp24"></div></div>';
};

// =============================================================================== ONBOARDING / PROFILE FORM
function formDraft() {
  if (!ui.form) {
    const p = data.profile;
    ui.form = {
      name: p.name, sex: p.sex, age: String(p.age), heightCm: String(p.heightCm), weightKg: Calc.num(p.weightKg), bodyFatPct: Calc.num(p.bodyFatPct),
      level: p.level, split: p.split, daysPerWeek: p.daysPerWeek, responder: p.responder, activityFactor: p.activityFactor,
      oneRm: Object.fromEntries(Object.entries(p.oneRm).map(([k, v]) => [k, Calc.num(v)]))
    };
  }
  return ui.form;
}

function profileForm(buttonText) {
  const f = formDraft();
  const sexKeys = Object.keys(SEX), lvKeys = Object.keys(LEVELS);
  const acts = [[1.375, 'Sedentario'], [1.55, 'Moderato'], [1.725, 'Attivo']];
  const lvNote = { PRINCIPIANTE: 'Meno serie sui complementari, sempre con 1 ripetizione di margine, niente tecniche di intensità.', INTERMEDIO: 'Programma completo come previsto dal metodo.', AVANZATO: "Una serie in più sull'isolamento nelle fasi 2 e 3, tecniche di intensità complete." };
  let h = `<div class="pad">` + section('Chi sei');
  h += `<label class="field"><span>Nome</span><div class="field-in"><input type="text" value="${esc(f.name)}" data-in="form" data-k="name" autocomplete="given-name"></div></label>`;
  h += `<div class="mt10">${segmented(sexKeys.map(k => SEX[k]), sexKeys.indexOf(f.sex), i => `data-a="formSet" data-k="sex" data-v="${sexKeys[i]}"`)}</div>`;
  h += `<div class="grid2 mt10">${field('Età', f.age, 'data-in="form" data-k="age"', '', false)}${field('Altezza', f.heightCm, 'data-in="form" data-k="heightCm"', 'cm', false)}</div>`;
  h += `<div class="grid2">${field('Peso', f.weightKg, 'data-in="form" data-k="weightKg"', 'kg')}${field('Massa grassa', f.bodyFatPct, 'data-in="form" data-k="bodyFatPct"', '%')}</div>`;
  h += section('Esperienza') + segmented(lvKeys.map(k => LEVELS[k]), lvKeys.indexOf(f.level), i => `data-a="formSet" data-k="level" data-v="${lvKeys[i]}"`);
  h += `<p class="muted t13 mt6">${lvNote[f.level]}</p>`;
  h += section('Organizzazione settimanale');
  h += Object.entries(SPLITS).map(([k, s]) => card(`<div class="row"><span class="radio${f.split === k ? ' on' : ''}"></span><div class="grow"><b>${esc(s.label)}</b><p class="muted t12">${esc(s.description)}</p></div></div>`,
    { highlight: f.split === k, action: `data-a="formSet" data-k="split" data-v="${k}"`, cls: 'mb8' })).join('');
  h += `<div class="sb mt6">Allenamenti a settimana: <span id="days-l">${f.daysPerWeek}</span></div>
    <input type="range" class="range" min="3" max="6" step="1" value="${f.daysPerWeek}" data-in="formDays">`;
  h += section('Metabolismo');
  h += Object.entries(RESPONDERS).map(([k, r]) => `<button class="radio-row" data-a="formSet" data-k="responder" data-v="${k}"><span class="radio${f.responder === k ? ' on' : ''}"></span>${esc(r.label)}</button>`).join('');
  h += `<div class="sb mt10 mb6">Livello di attività (fuori dalla palestra)</div>`;
  h += segmented(acts.map(a => a[1]), Math.max(0, acts.findIndex(a => a[0] === f.activityFactor)), i => `data-a="formSet" data-k="activityFactor" data-v="${acts[i][0]}"`);
  h += section('Massimali (1RM)');
  h += `<p class="muted t13 mb8">Servono per calcolare i carichi dei fondamentali in percentuale. Se non li conosci lasciali vuoti: l'app li stimerà dalle tue prime sedute, oppure usa il calcolatore 1RM.</p>`;
  h += Ex.oneRmLifts().map(ex => field(ex.name, f.oneRm[ex.id] || '', `data-in="formRm" data-k="${ex.id}"`, 'kg')).join('');
  h += `<div class="mt16">${gradBtn(buttonText, 'data-a="saveProfile"')}</div><div class="sp32"></div></div>`;
  return h;
}

function readForm() {
  const f = formDraft(), p = data.profile;
  const int = (v, d) => { const n = parseInt(v, 10); return isNaN(n) ? d : n; };
  const dbl = (v, d) => parseNum(v) ?? d;
  const oneRm = {};
  for (const [k, v] of Object.entries(f.oneRm)) { const n = parseNum(v); if (n > 0) oneRm[k] = n; }
  return {
    ...p, name: f.name.trim(), sex: f.sex, age: int(f.age, p.age), heightCm: int(f.heightCm, p.heightCm), weightKg: dbl(f.weightKg, p.weightKg),
    bodyFatPct: dbl(f.bodyFatPct, p.bodyFatPct), level: f.level, split: f.split, daysPerWeek: f.daysPerWeek, responder: f.responder,
    activityFactor: f.activityFactor, oneRm, onboarded: true
  };
}

VIEWS.onboarding = () => `<div class="hero big-hero"><span class="logo xl">KORE</span><p class="muted">Allenati con metodo. Misura. Progredisci.</p>
  <p class="mt14">Un programma annuale in 3 fasi che adatta carichi, serie e recuperi al tuo livello e ai tuoi massimali.</p>
  <button class="out-btn mt14" data-a="importBackup">Hai un backup dell'app Android? Importalo</button></div>` + profileForm('Crea il mio programma');

VIEWS.profile = () => {
  let h = `<h1 class="page-title pad">Profilo</h1>` + profileForm('Salva modifiche');
  h += `<div class="pad">` + section('Membership') + card(`<div class="row"><div class="tool-ic">${icon('crown')}</div><div class="grow ml14"><b>KORE Private · Signature · Concierge</b><p class="muted t12">Scopri i livelli e la nostra promessa sulla privacy</p></div><span class="muted">${icon('right')}</span></div>`, { action: 'data-a="go" data-v="membership"' });
  h += section('Backup dei dati');
  h += card(`<p class="muted t13">Tutti i dati restano sul telefono. Esporta un backup per non perderli cambiando dispositivo. Il file è compatibile con l'app Android: puoi spostare i dati da un telefono all'altro.</p>
    <div class="grid2 mt10">${outBtn('Esporta', 'data-a="exportBackup"')}${outBtn('Importa', 'data-a="importBackup"')}</div>`);
  h += section('Zona pericolosa') + outBtn('Ricomincia il programma dalla fase 1', 'data-a="confirmReset"', 'var(--bad)', 'style="width:100%;color:var(--bad)"');
  return h + '<div class="sp32"></div></div>';
};

// =============================================================================== WORKOUT
function lastPerformance(exerciseId) {
  const s = data.history.filter(h => h.exercises.some(e => e.exerciseId === exerciseId)).sort((a, b) => b.startedAt - a.startedAt)[0];
  if (!s) return null;
  const sets = s.exercises.filter(e => e.exerciseId === exerciseId).flatMap(Stats.workSets);
  return sets.length ? sets.map(x => `${Calc.fmt(x.weight)}×${x.reps}`).join('  ') : null;
}

function workoutCounts(s) {
  let total = 0, done = 0;
  s.exercises.forEach(e => e.sets.forEach(x => { if (x.type !== 'WARMUP') { total++; if (x.done) done++; } }));
  return [done, total];
}

VIEWS.workout = () => {
  const s = data.active;
  const [done, total] = workoutCounts(s);
  let h = topBar(s.title, `<span class="mono accent" data-clock="session">${mmss((Date.now() - s.startedAt) / 1000)}</span><button class="icon-btn" data-a="workoutMenu" aria-label="Menu">${icon('more')}</button>`);
  h += `<div class="thin-progress"><div style="width:${total ? done / total * 100 : 0}%"></div></div><div class="pad wk">`;
  h += s.exercises.map((e, ei) => exerciseCard(e, ei, s.startedAt)).join('');
  h += `<div class="mt4">${gradBtn('Termina allenamento', 'data-a="finishAsk"')}</div><div style="height:${timer ? 140 : 32}px"></div></div>`;
  return h;
};

function exerciseCard(e, ei, startedAt) {
  const ex = Ex.get(e.exerciseId);
  const slotCat = Program.templates(data.profile.split).flatMap(t => t.slots).find(sl => sl.key === e.slotKey)?.category || ex.category;
  const last = lastPerformance(e.exerciseId);
  const allDone = e.sets.length > 0 && e.sets.every(x => x.done);
  const workCount = e.sets.filter(x => x.type === 'WORK').length;
  let h = `<div class="row">${pill(CATEGORIES[slotCat].short, catColor(slotCat))}<span class="muted t12 grow ml8">${esc(MUSCLES[ex.muscle])}</span>
    <button class="icon-btn sm" data-a="go" data-v="exercise/${ex.id}" aria-label="Dettagli">${icon('info')}</button>
    <button class="icon-btn sm" data-a="swapAsk" data-v="${ei}" aria-label="Sostituisci">${icon('swap')}</button></div>
    <h3 class="t-lg${allDone ? ' muted' : ''}">${esc(ex.name)}</h3>
    <div class="pills mt6">${pill(`${workCount}×${e.repMin === e.repMax ? e.repMin : `${e.repMin}-${e.repMax}`}`, 'var(--text)')}${pill(`RIR ${e.rir}`, 'var(--warn)')}${pill(`Rec ${mmss(e.restSec)}`, 'var(--blue)')}${pill(`Tempo ${e.tempo}`, 'var(--muted)')}</div>`;
  if (e.technique !== 'NESSUNA' && TECHNIQUES[e.technique]) h += `<div class="tech mt8"><b>Tecnica: ${esc(TECHNIQUES[e.technique].label)} (ultima serie)</b><span>${esc(TECHNIQUES[e.technique].howTo)}</span></div>`;
  if (e.note) h += `<p class="muted t12 mt6">${esc(e.note)}</p>`;
  if (last) h += `<p class="accent2 t12 mt4">Ultima volta: ${esc(last)}</p>`;
  h += `<div class="set-head"><span class="c-lbl">SERIE</span><span>KG</span><span>RIP</span><span class="c-rir">RIR</span><span class="c-ok"></span></div>`;
  let workN = 0;
  e.sets.forEach((x, si) => {
    const label = x.type === 'WARMUP' ? 'R' : x.type === 'EXTRA' ? 'B' : String(++workN);
    h += `<div class="set${x.done ? ' done' : ''}${x.type === 'WARMUP' ? ' warm' : ''}">
      <button class="set-lbl" data-a="delSetAsk" data-e="${ei}" data-s="${si}">${label}</button>
      <input class="cell" type="text" inputmode="decimal" value="${x.weight > 0 ? Calc.num(x.weight) : ''}" data-in="set" data-f="weight" data-e="${ei}" data-s="${si}" placeholder="–">
      <input class="cell" type="text" inputmode="numeric" value="${x.reps > 0 ? x.reps : ''}" data-in="set" data-f="reps" data-e="${ei}" data-s="${si}" placeholder="–">
      <input class="cell c-rir" type="text" inputmode="numeric" value="${x.rir ?? ''}" data-in="set" data-f="rir" data-e="${ei}" data-s="${si}" placeholder="–">
      <button class="ok" data-a="toggleDone" data-e="${ei}" data-s="${si}" aria-label="Fatto">${icon('check')}</button></div>`;
  });
  h += `<button class="text-btn accent" data-a="addSet" data-e="${ei}">${icon('plus')} Aggiungi serie</button>`;
  h += `<div id="imp-${ei}">${improvementBox(Progression.improvement(data.history, e.exerciseId, e, startedAt))}</div>`;
  return card(h, { highlight: !allDone && e.sets.some(x => x.done) });
}

function startSession(dayIndex) {
  if (data.active) return;
  const plan = Program.plan(data, dayIndex);
  const exercises = plan.items.map(pe => {
    const sets = [];
    const kg = pe.suggestedKg || 0;
    const step = EQUIPMENT[pe.exercise.equipment].step;
    if (pe.slot.category === 'BASE' && kg > 0 && !plan.deload) Calc.warmup(kg, data.profile.barKg, step).forEach(([w, r]) => sets.push({ type: 'WARMUP', weight: w, reps: r, rir: null, done: false }));
    for (let i = 0; i < pe.rx.sets; i++) sets.push({ type: 'WORK', weight: kg, reps: pe.rx.repMax, rir: pe.rx.rir, done: false });
    if (pe.rx.technique === 'BACK_OFF' && kg > 0) sets.push({ type: 'EXTRA', weight: Calc.round(kg * 0.8, step), reps: pe.rx.repMax + 4, rir: 1, done: false });
    return { slotKey: pe.slot.key, exerciseId: pe.exercise.id, repMin: pe.rx.repMin, repMax: pe.rx.repMax, rir: pe.rx.rir, restSec: pe.rx.restSec, tempo: pe.rx.tempo, technique: pe.rx.technique, note: pe.rx.note, sets };
  });
  const st = data.program;
  update(d => {
    d.active = { id: Date.now(), title: plan.template.name + (plan.deload ? ' · Scarico' : ''), startedAt: Date.now(), endedAt: null, phase: st.phase, meso: st.meso, week: st.week, dayIndex, exercises, notes: '', feeling: 3 };
  });
}

function finishSession(notes, feeling) {
  skipRest();
  const s = data.active; if (!s) return;
  const finished = { ...s, notes, feeling, endedAt: Date.now() };
  const records = [];
  const newOneRm = { ...data.profile.oneRm };
  const previous = Object.fromEntries(Stats.records(data.history).map(r => [r.exercise.id, r]));
  finished.exercises.forEach(e => {
    const best = Stats.bestE1rm(e); if (best <= 0) return;
    const ex = Ex.get(e.exerciseId);
    const prev = previous[ex.id]?.bestE1rm || 0;
    if (prev > 0 && best > prev + 0.01) records.push(`${ex.name}: nuovo record stimato ${Calc.fmt(Calc.round(best, 0.5))} kg`);
    if (ex.tracksOneRm) {
      const cur = newOneRm[ex.id] || 0;
      if (best > cur) {
        newOneRm[ex.id] = Calc.round(best, 0.5);
        if (cur > 0) records.push(`${ex.name}: massimale aggiornato a ${Calc.fmt(Calc.round(best, 0.5))} kg`);
      }
    }
  });
  update(d => {
    const np = Program.advance(d.program, d.profile.split);
    if (s.dayIndex === d.program.dayIndex) d.program = np;
    d.active = null; d.history.push(finished); d.profile.oneRm = newOneRm;
  });
  ui.summary = { title: s.title, minutes: Math.floor((finished.endedAt - s.startedAt) / 60000), sets: Stats.setCount(finished), tonnage: Stats.volume(finished), records: [...new Set(records)] };
  releaseWakeLock();
}

// =============================================================================== REST TIMER
let timer = null;
try { timer = JSON.parse(localStorage.getItem(TIMER_KEY)); if (timer && timer.endAt < Date.now() - 60000) timer = null; } catch (e) { timer = null; }
function saveTimer() { try { if (timer) localStorage.setItem(TIMER_KEY, JSON.stringify(timer)); else localStorage.removeItem(TIMER_KEY); } catch (e) { /* ignore */ } }
function startRest(seconds, label = 'Recupero') { timer = { endAt: Date.now() + seconds * 1000, total: seconds, label }; saveTimer(); renderRestBar(); }
function addRest(delta) {
  if (!timer) return;
  const end = timer.endAt + delta * 1000;
  if (end <= Date.now()) skipRest(); else { timer = { ...timer, endAt: end, total: Math.max(1, timer.total + delta) }; saveTimer(); renderRestBar(); }
}
function skipRest() { timer = null; saveTimer(); renderRestBar(); }

function renderRestBar() {
  const bar = $('#restbar');
  if (!timer || route().name !== 'workout') { bar.innerHTML = ''; bar.style.display = 'none'; return; }
  bar.style.display = '';
  if (!bar.firstChild) {
    bar.innerHTML = `<div class="ring"><svg viewBox="0 0 64 64"><circle cx="32" cy="32" r="28" class="ring-bg"/><circle cx="32" cy="32" r="28" class="ring-fg" id="ring-fg"/></svg>${icon('timer')}</div>
      <div class="grow"><small class="lbl">RECUPERO</small><div class="mono big-time" id="rest-left"></div><div class="muted t12 ell" id="rest-label"></div></div>
      <button class="tonal" data-a="rest" data-v="-15">-15</button><button class="tonal" data-a="rest" data-v="15">+15</button>
      <button class="icon-btn accent" data-a="skipRest" aria-label="Salta">${icon('skip')}</button>`;
  }
  tick();
}

let alerted = null;
function tick() {
  const now = Date.now();
  document.querySelectorAll('[data-clock="session"]').forEach(el => { if (data.active) el.textContent = mmss((now - data.active.startedAt) / 1000); });
  if (timer) {
    const leftMs = timer.endAt - now;
    if (leftMs <= 0) {
      if (alerted !== timer.endAt) { alerted = timer.endAt; alertRestOver(); }
      timer = null; saveTimer(); renderRestBar(); return;
    }
    const left = $('#rest-left');
    if (left) {
      left.textContent = mmss(Math.ceil(leftMs / 1000));
      $('#rest-label').textContent = timer.label;
      const c = 2 * Math.PI * 28;
      const fg = $('#ring-fg'); fg.style.strokeDasharray = c; fg.style.strokeDashoffset = c * (1 - Math.min(1, leftMs / (timer.total * 1000)));
    }
  }
}
setInterval(tick, 250);
document.addEventListener('visibilitychange', () => { if (!document.hidden) { tick(); if (data.active && route().name === 'workout') requestWakeLock(); } });

// sound: iOS needs the AudioContext to be unlocked by a tap
let audioCtx = null;
function unlockAudio() {
  try {
    if (!audioCtx) audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    if (audioCtx.state === 'suspended') audioCtx.resume();
  } catch (e) { /* ignore */ }
}
function beep() {
  if (!audioCtx) return;
  const t0 = audioCtx.currentTime;
  [0, 0.25, 0.5].forEach((dt, i) => {
    const o = audioCtx.createOscillator(), g = audioCtx.createGain();
    o.type = 'sine'; o.frequency.value = i === 2 ? 1320 : 880;
    g.gain.setValueAtTime(0.0001, t0 + dt); g.gain.exponentialRampToValueAtTime(0.4, t0 + dt + 0.02); g.gain.exponentialRampToValueAtTime(0.0001, t0 + dt + (i === 2 ? 0.45 : 0.18));
    o.connect(g).connect(audioCtx.destination); o.start(t0 + dt); o.stop(t0 + dt + 0.5);
  });
}
function alertRestOver() {
  beep();
  try { if (navigator.vibrate) navigator.vibrate([300, 150, 300, 150, 500]); } catch (e) { /* ignore */ }
  toast('Recupero finito: prossima serie!');
  try {
    if ('Notification' in window && Notification.permission === 'granted' && navigator.serviceWorker) {
      navigator.serviceWorker.ready.then(r => r.showNotification('Recupero finito', { body: 'Prossima serie: si parte!', icon: 'icons/icon-192.png', tag: 'rest' })).catch(() => {});
    }
  } catch (e) { /* ignore */ }
}

let wakeLock = null;
async function requestWakeLock() { try { if ('wakeLock' in navigator && !wakeLock) { wakeLock = await navigator.wakeLock.request('screen'); wakeLock.addEventListener('release', () => { wakeLock = null; }); } } catch (e) { wakeLock = null; } }
function releaseWakeLock() { try { wakeLock?.release(); } catch (e) { /* ignore */ } wakeLock = null; }

// =============================================================================== PROGRAM
VIEWS.program = () => {
  const st = data.program;
  if (ui.programWeek == null) ui.programWeek = st.week;
  if (ui.programOpen == null) ui.programOpen = st.dayIndex;
  const vw = ui.programWeek;
  const volume = Program.volumePerMuscle(data);
  let h = `<div class="pad"><div class="row mt16"><h1 class="page-title grow">Programma</h1><button class="icon-btn accent" data-a="positionAsk" aria-label="Cambia posizione">${icon('tune')}</button></div>
    <p class="muted">${esc(SPLITS[data.profile.split].label)} · ${esc(LEVELS[data.profile.level])}</p>`;
  h += section('Macrociclo annuale');
  h += PHASES.map((ph, i) => {
    const cur = i === st.phase;
    return card(`<div class="row"><div class="num-box${cur ? ' cur' : i < st.phase ? ' past' : ''}">${i < st.phase ? icon('check') : i + 1}</div>
      <div class="grow ml12"><b class="xb">${esc(ph.name)}</b><p class="muted t12">${esc(ph.goal)}</p></div>${cur ? pill('IN CORSO') : ''}</div>
      ${cur ? `<div class="mt8">${ph.details.map(bullet).join('')}</div>` : ''}`, { highlight: cur, cls: 'mb8' });
  }).join('');
  h += section(`Mesociclo ${st.meso + 1} di ${Program.MESO_PER_PHASE}`);
  h += `<div class="weeks">${Array.from({ length: Program.WEEKS_PER_MESO }, (_, k) => k + 1).map(w =>
    `<button class="${w === vw ? 'sel' : ''}${w === st.week ? ' cur' : ''}" data-a="viewWeek" data-v="${w}"><b>${w === Program.WEEKS_PER_MESO ? 'S' : w}</b><small>${w === Program.WEEKS_PER_MESO ? 'scarico' : 'sett.'}</small></button>`).join('')}</div>`;
  if (vw !== st.week) h += `<p class="accent2 t12 mt6">Anteprima della settimana ${vw} (i carichi sono stimati)</p>`;
  h += section('Sedute del microciclo');
  h += Program.templates(data.profile.split).map((t, di) => {
    const plan = Program.plan(data, di, vw);
    const isNext = di === st.dayIndex && vw === st.week;
    const open = ui.programOpen === di;
    let inner = `<div class="row"><b class="g-num">G${di + 1}</b><div class="grow ml12"><b class="xb">${esc(t.name)}</b><p class="muted t12">${esc(t.muscles)}</p></div>${isNext ? pill('PROSSIMA') : ''}<span class="muted">${icon(open ? 'up' : 'down')}</span></div>`;
    if (open) {
      inner += `<div class="mt10">${plan.items.map(pe => plannedRow(pe, !!data.swaps[pe.slot.key])).join('')}</div>`;
      if (!data.active && vw === st.week) inner += `<div class="mt10">${outBtn(`${icon('play')} ${isNext ? 'Inizia questa seduta' : 'Fai questa seduta oggi'}`, `data-a="startWorkout" data-v="${di}"`, 'var(--accent)', 'style="width:100%"')}</div>`;
    }
    return card(inner, { highlight: isNext, action: `data-a="toggleDay" data-v="${di}"`, cls: 'mb8' });
  }).join('');
  h += `<button class="text-btn muted" data-a="skipScheduled">${icon('skip')} Salta la prossima seduta</button>`;
  h += section('Volume settimanale pianificato');
  h += card(`<p class="muted t12">Serie allenanti per gruppo muscolare (i muscoli sinergici contano metà).</p>
    ${Object.keys(MUSCLES).filter(m => (volume[m] || 0) > 0).map(m => { const l = Stats.landmarks(m); return volumeBar(MUSCLES[m], volume[m], l.mev, l.mavHigh, Stats.zone(m, volume[m])); }).join('')}`);
  return h + '<div class="sp24"></div></div>';
};

function plannedRow(pe, swapped) {
  return `<div class="planned"><div class="row">${pill(CATEGORIES[pe.slot.category].short, catColor(pe.slot.category))}<b class="grow ml8">${esc(pe.exercise.name)}</b>
    ${swapped ? `<button class="link t11" data-a="resetSwap" data-v="${pe.slot.key}">ripristina</button>` : ''}</div>
    <div class="t13 soft mt2">${pe.rx.sets} × ${Program.repsLabel(pe.rx)}  ·  RIR ${pe.rx.rir}  ·  rec ${mmss(pe.rx.restSec)}${pe.suggestedKg != null ? `  ·  ${Calc.fmt(pe.suggestedKg)} kg` : ''}</div>
    <div class="muted t11">${esc(pe.suggestionWhy)}</div>${pe.rx.technique !== 'NESSUNA' ? `<div class="accent2 t11">Tecnica: ${esc(TECHNIQUES[pe.rx.technique].label)}</div>` : ''}</div>`;
}

// =============================================================================== PROGRESS
VIEWS.progress = () => {
  let h = `<div class="pad"><h1 class="page-title mt16 mb12">Progressi</h1>${segmented(['Panoramica', 'Esercizi', 'Storico'], ui.progressTab, i => `data-a="progressTab" data-v="${i}"`)}`;
  if (ui.progressTab === 0) h += progressOverview();
  else if (ui.progressTab === 1) h += progressExercises();
  else h += historyList();
  return h + '<div class="sp24"></div></div>';
};

function progressOverview() {
  const weeks = Stats.weekly(data.history).slice(-8);
  const perMuscle = Stats.setsPerMuscleThisWeek(data.history);
  const records = Stats.records(data.history);
  let h = '';
  if (!data.history.length) h += `<div class="mt24">${card('<b>Ancora nessuna seduta registrata</b><p class="muted">Completa il tuo primo allenamento per vedere grafici, record e volume.</p>')}</div>`;
  h += section('Tonnellaggio settimanale (kg)');
  h += card(!weeks.length ? '<p class="muted">Nessun dato</p>' : barChart(weeks.map(w => w.tonnage), weeks.map(w => fmtShort(w.start))) +
    `<div class="mt8">${kv('Questa settimana', `${fmtKgInt(weeks[weeks.length - 1].tonnage)} kg · ${weeks[weeks.length - 1].sets} serie · ${weeks[weeks.length - 1].sessions} sedute`)}</div>`);
  h += section('Serie per muscolo · settimana in corso');
  const ms = Object.keys(MUSCLES).filter(m => (perMuscle[m] || 0) > 0);
  h += card(!ms.length ? '<p class="muted">Nessuna serie questa settimana</p>' : ms.map(m => { const l = Stats.landmarks(m); return volumeBar(MUSCLES[m], perMuscle[m], l.mev, l.mavHigh, Stats.zone(m, perMuscle[m])); }).join(''));
  h += section('Record personali');
  h += card(!records.length ? '<p class="muted">Nessun record ancora</p>' : records.slice(0, 12).map(r =>
    `<button class="rec-row" data-a="go" data-v="exercise/${r.exercise.id}"><span class="accent">${icon('trophy')}</span><div class="grow ml8"><b class="sb">${esc(r.exercise.name)}</b><small class="muted">Top: ${Calc.fmt(r.heaviest)} kg × ${r.heaviestReps}</small></div>
      <div class="right"><b class="accent xb">${Calc.fmt(Calc.round(r.bestE1rm, 0.5))} kg</b><small class="muted">1RM stimato</small></div></button>`).join(''));
  return h;
}

function doneExercises() {
  const ids = [];
  data.history.forEach(s => s.exercises.forEach(e => { if (!ids.includes(e.exerciseId)) ids.push(e.exerciseId); }));
  return ids.map(Ex.find).filter(Boolean);
}

function progressExercises() {
  const done = doneExercises();
  if (!done.length) return `<p class="muted mt24">Registra qualche seduta per vedere l'andamento dei tuoi esercizi.</p>`;
  const sel = done.find(e => e.id === ui.progressEx) || done[0];
  return section('Esercizio') + `<div class="select-wrap"><select data-in="progressEx">${done.map(e => `<option value="${e.id}" ${e.id === sel.id ? 'selected' : ''}>${esc(e.name)}</option>`).join('')}</select>${icon('down')}</div>` + exerciseCharts(sel.id);
}

function exerciseCharts(id) {
  const e1 = Stats.e1rmSeries(data.history, id);
  const vol = Stats.volumeSeries(data.history, id);
  let inner = lineChart(e1.map(p => p.value));
  if (e1.length) {
    const first = e1[0].value, last = e1[e1.length - 1].value, diff = last - first;
    const trend = Progression.improvement(data.history, id, null, Number.MAX_SAFE_INTEGER);
    inner += `<div class="stats mt8">${stat(Calc.fmt(Calc.round(last, 0.5)), 'Attuale', true)}${stat(Calc.fmt(Calc.round(Math.max(...e1.map(p => p.value)), 0.5)), 'Migliore')}${stat((diff >= 0 ? '+' : '') + Calc.fmt(Calc.round(diff, 0.5)), "Dall'inizio")}</div>
      <div class="stats mt8">${stat(Progression.pct(first > 0 ? diff / first * 100 : null), 'Totale %')}${stat(Progression.pct(trend.expectedPct), 'Atteso a seduta', true)}${stat(trend.expectedE1rm != null ? Calc.fmt(Calc.round(trend.expectedE1rm, 0.5)) : '—', 'Prossimo 1RM')}</div>`;
  }
  return section('1RM stimato (kg)') + card(inner) + section('Volume per seduta (kg × rip)') + card(lineChart(vol.map(p => p.value), 'var(--blue)'));
}

function historyList() {
  const list = [...data.history].sort((a, b) => b.startedAt - a.startedAt);
  if (!list.length) return '<p class="muted mt16">Nessuna seduta salvata.</p>';
  return '<div class="mt16">' + list.map(s => card(`<div class="row"><div class="grow"><b>${esc(s.title)}</b><p class="muted t12">${fmtDate(epochDayOf(s.startedAt))} · Fase ${s.phase + 1} · Sett. ${s.week}</p></div>
    <div class="right"><b class="sb">${Stats.setCount(s)} serie</b><small class="muted">${fmtKgInt(Stats.volume(s))} kg</small></div></div>`, { action: `data-a="go" data-v="session/${s.id}"`, cls: 'mb8' })).join('') + '</div>';
}

VIEWS.session = arg => {
  const s = data.history.find(x => String(x.id) === arg);
  if (!s) return topBar('Seduta') + '<p class="pad">Seduta non trovata</p>';
  let h = topBar(s.title, `<button class="icon-btn" data-a="deleteSessionAsk" data-v="${s.id}" aria-label="Elimina">${icon('trash')}</button>`);
  h += `<div class="pad"><p class="muted">${fmtDate(epochDayOf(s.startedAt))}</p>
    <div class="stats mt10">${stat(Stats.setCount(s), 'Serie', true)}${stat(fmtKgInt(Stats.volume(s)), 'kg totali')}${stat(s.endedAt ? `${Math.floor((s.endedAt - s.startedAt) / 60000)}'` : '-', 'Durata')}</div>`;
  if (s.notes.trim()) h += section('Note') + `<p>${esc(s.notes)}</p>`;
  s.exercises.forEach(e => {
    const ex = Ex.get(e.exerciseId);
    const doneSets = e.sets.filter(x => x.done);
    let n = 0;
    let inner = doneSets.map(x => `<div class="kv"><span>${x.type === 'WARMUP' ? 'Risc.' : `Serie ${++n}`}</span><b class="sb">${Calc.fmt(x.weight)} kg × ${x.reps}${x.rir != null ? `  @RIR ${x.rir}` : ''}</b></div>`).join('');
    if (!doneSets.length) inner += '<p class="muted">Nessuna serie completata</p>';
    const best = Stats.bestE1rm(e);
    if (best > 0) inner += `<p class="accent t12 mt6">1RM stimato: ${Calc.fmt(Calc.round(best, 0.5))} kg</p>` + improvementBox(Progression.improvement(data.history, e.exerciseId, e, s.startedAt));
    h += section(ex.name) + card(inner);
  });
  return h + '<div class="sp24"></div></div>';
};

// =============================================================================== TOOLS
VIEWS.tools = () => {
  const rows = [['crown', 'Membership', 'Private, Signature e Concierge: privacy assoluta', 'membership'], ['calc', 'Calcolatore massimale', "Stima l'1RM da qualsiasi serie e tabella dei carichi", 'onerm'], ['speed', 'Tabella RIR / RPE', 'Che carico usare per ripetizioni e buffer desiderati', 'rir'],
    ['dumbbell', 'Calcolatore dischi', 'Quali dischi caricare sul bilanciere', 'plates'], ['food', 'Calorie e macro', 'Fabbisogno, surplus e calorie cicliche', 'nutrition'],
    ['scale', 'Peso corporeo', 'Pesate giornaliere, medie settimanali e andamento', 'bodyweight'], ['search', 'Libreria esercizi', 'Tecnica esecutiva e alternative per ogni distretto', 'library'],
    ['book', 'Il metodo', 'Principi di allenamento, volume, intensità e progressioni', 'guide']];
  return `<div class="pad"><h1 class="page-title mt16 mb8">Strumenti</h1>` + rows.map(([ic, t, sub, r]) =>
    card(`<div class="row"><div class="tool-ic">${icon(ic)}</div><div class="grow ml14"><b>${esc(t)}</b><p class="muted t12">${esc(sub)}</p></div><span class="muted">${icon('right')}</span></div>`, { action: `data-a="go" data-v="${r}"`, cls: 'mv5' })).join('') + '</div>';
};

VIEWS.onerm = () => {
  const o = ui.onerm;
  return topBar('Calcolatore massimale') + `<div class="pad"><div class="grid3">${field('Carico', o.w, 'data-in="onerm" data-k="w"', 'kg')}${field('Ripetizioni', o.r, 'data-in="onerm" data-k="r"', '', false)}${field('RIR', o.rir, 'data-in="onerm" data-k="rir"', '', false)}</div>
    <div id="onerm-out">${onermOut()}</div><div class="sp24"></div></div>`;
};
function onermOut() {
  const w = parseNum(ui.onerm.w) || 0, r = parseInt(ui.onerm.r, 10) || 0, rir = parseInt(ui.onerm.rir, 10) || 0;
  const est = Calc.e1rm(w, r, rir);
  return `<div class="mt16">${card(`<small class="lbl">MASSIMALE STIMATO</small><div class="display accent">${Calc.fmt(Calc.round(est, 0.5))} kg</div>
    <p class="muted t12">Epley ${Calc.fmt(Calc.round(Calc.epley(w, r + rir), 0.5))} · Brzycki ${Calc.fmt(Calc.round(Calc.brzycki(w, r + rir), 0.5))}</p>
    <div class="mt10">${outBtn('Salva come massimale…', `data-a="saveOneRmAsk" data-v="${Calc.round(est, 0.5)}"`, 'var(--accent)', est > 0 ? '' : 'disabled')}</div>`, { highlight: true })}</div>` +
    section('Tabella percentuali') + card(`<table class="tbl"><tr><th>% 1RM</th><th>Carico</th><th>Rip. max</th></tr>${[100, 95, 90, 87, 85, 82, 80, 77, 75, 72, 70, 67, 65, 60, 55, 50].map(p =>
      `<tr><td class="accent">${p}%</td><td>${Calc.fmt(Calc.round(est * p / 100, 2.5))} kg</td><td>${Calc.repsAt(p / 100)}</td></tr>`).join('')}</table>`);
}

VIEWS.rir = () => topBar('Tabella RIR / RPE') + `<div class="pad">${card(`<b>Come leggerla</b>${bullet('RIR (ripetizioni di riserva) o buffer: quante ripetizioni avresti potuto ancora fare.')}${bullet('RPE = 10 − RIR. 10 rip @RPE 8 = 10 rip con buffer 2 = un carico da 12RM.')}${bullet('Inserisci il tuo massimale per vedere i chili; altrimenti la tabella mostra le percentuali.')}`)}
  <div class="mt10">${field('Massimale (1RM)', ui.rirOrm, 'data-in="rirOrm"', 'kg')}</div>${section('Ripetizioni × RIR')}<div id="rir-out">${rirOut()}</div><div class="sp24"></div></div>`;
function rirOut() {
  const one = parseNum(ui.rirOrm) || 0;
  let t = `<div class="hscroll"><table class="tbl rir"><tr><th>Rip</th>${[0, 1, 2, 3, 4].map(r => `<th>RIR ${r}<br>RPE ${10 - r}</th>`).join('')}</tr>`;
  for (let reps = 1; reps <= 15; reps++) {
    t += `<tr><th>${reps}</th>${[0, 1, 2, 3, 4].map(rr => { const pct = Calc.pctFor(reps, rr); return `<td>${one > 0 ? Calc.fmt(Calc.round(one * pct, 2.5)) : Math.round(pct * 100) + '%'}</td>`; }).join('')}</tr>`;
  }
  return card(t + '</table></div>');
}

VIEWS.plates = () => {
  if (ui.plates.bar == null) ui.plates.bar = Calc.num(data.profile.barKg);
  return topBar('Calcolatore dischi') + `<div class="pad"><div class="grid2">${field('Carico totale', ui.plates.target, 'data-in="plates" data-k="target" id="plates-target"', 'kg')}${field('Bilanciere', ui.plates.bar, 'data-in="plates" data-k="bar"', 'kg')}</div>
    <div class="grid4 mt8">${[-5, -2.5, 2.5, 5].map(d => `<button class="tonal" data-a="platesAdd" data-v="${d}">${d > 0 ? '+' : ''}${Calc.fmt(d)}</button>`).join('')}</div>
    ${section('Per lato')}<div id="plates-out">${platesOut()}</div></div>`;
};
function plateColor(p) { return p >= 25 ? '#E53935' : p >= 20 ? '#1E88E5' : p >= 15 ? '#FDD835' : p >= 10 ? '#43A047' : p >= 5 ? '#EEEEEE' : '#9E9E9E'; }
function platesOut() {
  const t = parseNum(ui.plates.target) || 0, b = parseNum(ui.plates.bar) ?? 20;
  const [plates, left] = Calc.plates(t, b, data.profile.plates);
  const groups = {};
  plates.forEach(p => { groups[p] = (groups[p] || 0) + 1; });
  return card(`<div class="barbell"><i class="sleeve-end"></i><i class="collar"></i>${plates.map(p => `<i class="plate" style="height:${Math.min(110, 40 + p * 3)}px;width:${p >= 10 ? 16 : 10}px;background:${plateColor(p)}"></i>`).join('')}<i class="sleeve"></i></div>
    ${!plates.length ? '<p class="muted mt8">Solo bilanciere</p>' : ''}${Object.keys(groups).map(Number).sort((a, b) => b - a).map(p => kv(`Disco ${Calc.fmt(p)} kg`, `× ${groups[p]}`)).join('')}
    ${left > 0.01 ? `<p class="t12 mt6" style="color:var(--warn)">Non raggiungibile con precisione: mancano ${Calc.fmt(Calc.round(left, 0.25))} kg</p>` : ''}`);
}

VIEWS.nutrition = () => {
  const p = data.profile;
  const n = Calc.nutrition(p, Math.min(6, p.daysPerWeek));
  const macroBar = (name, g, kcal, color) => `<div class="macro"><div class="row"><span class="sb grow">${name}</span><b style="color:${color}">${g} g</b><span class="muted t12">  ·  ${kcal} kcal</span></div>
    <div class="track"><div style="width:${Math.min(100, n.target > 0 ? kcal / n.target * 100 : 0)}%;background:${color}"></div></div></div>`;
  const dayMacro = (title, m, accent) => card(`<small class="lbl">${title.toUpperCase()}</small><div class="kcal${accent ? ' accent' : ''}">${m.kcal} kcal</div><div class="t13">P ${m.p} g</div><div class="t13">G ${m.f} g</div><div class="t13">C ${m.c} g</div>`, { highlight: accent });
  return topBar('Calorie e macro') + `<div class="pad"><p class="muted t12">Calcolo indicativo basato sul tuo profilo (${Calc.fmt(p.weightKg)} kg, ${p.heightCm} cm, ${p.age} anni, ${esc(RESPONDERS[p.responder].label.toLowerCase())}). Per esigenze specifiche rivolgiti a un professionista.</p>
    ${section('Fabbisogno')}<div class="stats">${stat(n.bmr, 'Metabolismo basale')}${stat(n.tdee, 'TDEE')}${stat(n.target, 'Obiettivo kcal', true)}</div>
    ${section('Macronutrienti giornalieri')}${card(macroBar('Proteine', n.proteinG, n.proteinG * 4, 'var(--accent)') + macroBar('Grassi', n.fatG, n.fatG * 9, 'var(--warn)') + macroBar('Carboidrati', n.carbG, n.carbG * 4, 'var(--blue)'))}
    ${section('Calorie cicliche (+10% nei giorni di allenamento)')}<div class="grid2">${dayMacro('Allenamento', n.trainingDay, true)}${dayMacro('Riposo', n.restDay, false)}</div>
    ${section('Indicazioni')}${card(bullet(n.note) + bullet("Obiettivo in massa: circa +0,5% del peso corporeo a settimana, mai oltre l'1%.") + bullet('Proteine e grassi restano costanti, sono i carboidrati a variare tra giorni di allenamento e riposo.') + bullet('Pesati ogni mattina a digiuno per almeno 3 settimane prima di modificare le calorie.'))}
    <div class="mt10">${outBtn('Apri il diario del peso', 'data-a="go" data-v="bodyweight"', 'var(--accent)', 'style="width:100%"')}</div><div class="sp24"></div></div>`;
};

VIEWS.bodyweight = () => {
  if (ui.bw == null) ui.bw = Calc.num(data.profile.weightKg);
  const weeks = Calc.weeklyAverages(data.bodyWeight);
  const [pct, advice] = Calc.weightAdvice(weeks);
  let h = topBar('Peso corporeo') + `<div class="pad"><div class="row bw-in"><div class="grow">${field('Peso di oggi', ui.bw, 'data-in="bw"', 'kg')}</div><button class="solid-btn" data-a="saveBw">Salva</button></div>`;
  h += section('Andamento giornaliero') + card(lineChart(data.bodyWeight.slice(-60).map(e => e.kg)));
  h += section('Medie settimanali') + card(lineChart(weeks.map(w => w.avg), 'var(--blue)', 120) +
    (pct != null ? `<p class="accent xb mt8">${pct >= 0 ? '+' : ''}${pct.toFixed(2).replace('.', ',')}% rispetto alla settimana precedente</p>` : '') +
    `<p class="muted t13">${esc(advice)}</p><div class="mt6">${weeks.slice(-6).reverse().map(w => kv(`Settimana del ${fmtDate(w.weekStart)}`, `${w.avg.toFixed(1).replace('.', ',')} kg (${w.count})`)).join('')}</div>`);
  h += section('Pesate') + [...data.bodyWeight].sort((a, b) => b.epochDay - a.epochDay).slice(0, 30).map(e =>
    `<div class="row bw-row"><span class="muted grow">${fmtDate(e.epochDay)}</span><b>${Calc.fmt(e.kg)} kg</b><button class="icon-btn" data-a="delBw" data-v="${e.epochDay}" aria-label="Elimina">${icon('close')}</button></div>`).join('');
  return h + '<div class="sp24"></div></div>';
};

VIEWS.library = () => topBar('Libreria esercizi') + `<div class="pad"><div class="search">${icon('search')}<input type="search" placeholder="Cerca esercizio" value="${esc(ui.libQuery)}" data-in="libQuery"></div></div>
  <div class="chips">${[[null, 'Tutti'], ...Object.entries(MUSCLES)].map(([k, l]) => `<button class="chip${ui.libMuscle === k ? ' on' : ''}" data-a="libMuscle" data-v="${k || ''}">${esc(l)}</button>`).join('')}</div>
  <div class="pad" id="lib-list">${libList()}</div>`;
function libList() {
  const q = ui.libQuery.trim().toLowerCase();
  return EXERCISES.filter(e => (!ui.libMuscle || e.muscle === ui.libMuscle) && (!q || e.name.toLowerCase().includes(q))).map(ex =>
    `<button class="lib-row" data-a="go" data-v="exercise/${ex.id}">${pill(CATEGORIES[ex.category].short, catColor(ex.category))}<div class="grow ml10"><b class="sb">${esc(ex.name)}</b><small class="muted">${esc(MUSCLES[ex.muscle])} · ${esc(EQUIPMENT[ex.equipment].label)}</small></div><span class="muted">${icon('right')}</span></button>`).join('') + '<div class="sp24"></div>';
}

VIEWS.exercise = id => {
  const ex = Ex.find(id);
  if (!ex) return topBar('Esercizio');
  const how = { BASE: "Esercizio fondamentale: va all'inizio della seduta, con carichi elevati e poche ripetizioni. Mantieni sempre un buffer di 1-2 ripetizioni per preservare la tecnica.", COMPLEMENTARE: "Complementare: carichi medi (6-12 ripetizioni). Tieni 1-2 ripetizioni di margine nelle prime serie e arriva vicino al cedimento nell'ultima.", ISOLAMENTO: 'Isolamento: carichi leggeri e ripetizioni alte, a fine seduta. Qui il cedimento su tutte le serie è la regola, perché aumentare il carico è difficile.' };
  let h = topBar(ex.name) + `<div class="pad"><div class="pills">${pill(CATEGORIES[ex.category].label)}${pill(MUSCLES[ex.muscle], 'var(--blue)')}${pill(EQUIPMENT[ex.equipment].label, 'var(--muted)')}</div>`;
  if (ex.secondary.length) h += `<p class="muted t12 mt6">Sinergici: ${ex.secondary.map(m => MUSCLES[m]).join(', ')}</p>`;
  h += section('Esecuzione') + card(ex.cues.map(bullet).join(''));
  h += section('Come usarlo') + card(`<p>${how[ex.category]}</p>`);
  h += `<div class="mt10"><a class="out-btn" style="width:100%;color:var(--accent)" href="https://www.youtube.com/results?search_query=${encodeURIComponent(ex.name + ' esecuzione corretta')}" target="_blank" rel="noopener">${icon('play')} Guarda video su YouTube</a></div>`;
  if (data.history.some(s => s.exercises.some(e => e.exerciseId === ex.id))) h += exerciseCharts(ex.id);
  if (data.profile.oneRm[ex.id] != null) h += section('Massimale') + card(kv('1RM registrato', `${Calc.fmt(data.profile.oneRm[ex.id])} kg`, 'var(--accent)'));
  return h + '<div class="sp24"></div></div>';
};

VIEWS.guide = () => topBar('Il metodo') + `<div class="pad"><p class="muted t13 mb10">Sintesi dei principi su cui si basa il programma dell'app: come vengono scelti esercizi, serie, ripetizioni, carichi e recuperi.</p>
  ${GUIDE.map((g, i) => card(`<div class="row"><b class="accent xb">${String(i + 1).padStart(2, '0')}</b><b class="grow ml12">${esc(g.title)}</b><span class="muted">${icon(ui.guideOpen === i ? 'up' : 'down')}</span></div>
    ${ui.guideOpen === i ? `<div class="mt8">${g.body.map(bullet).join('')}</div>` : ''}`, { highlight: ui.guideOpen === i, action: `data-a="guide" data-v="${i}"`, cls: 'mb8' })).join('')}<div class="sp24"></div></div>`;

// =============================================================================== MEMBERSHIP
const TIERS = [
  { name: 'KORE Private', price: '39', per: '€ / mese', tag: 'L\'essenziale, senza compromessi', items: ['App completa, nessun account', 'Nessuna raccolta, conservazione o vendita di dati', 'Programma annuale firmato dal tuo coach', 'Timer, progressioni, grafici e strumenti'] },
  { name: 'KORE Signature', price: '190', per: '€ / mese', tag: 'Il programma costruito su di te', featured: true, items: ['Tutto di KORE Private', 'Programma personalizzato sui tuoi obiettivi', 'Check mensile riservato con il coach', 'Revisione dei carichi e dei progressi'] },
  { name: 'KORE Concierge', price: 'da 690', per: '€ / mese', tag: 'Il coach al tuo fianco, ovunque', items: ['Tutto di KORE Signature', 'Coaching settimanale, anche in presenza', 'Sessioni in hotel, in viaggio o a domicilio', 'Linea diretta e riservata con il coach'] }
];
VIEWS.membership = () => topBar('Membership') + `<div class="pad">
  <div class="manifesto"><span class="accent">${icon('lock')}</span><h2 class="headline">Il tuo allenamento resta tuo.</h2>
  <p class="soft mt8">Nessun profilo, nessun tracciamento, nessuno sponsor. I tuoi dati non lasciano mai il telefono: non li raccogliamo, non li conserviamo, non li vendiamo. Paghi l'app, non con la tua privacy.</p>
  <small class="sl-tag mt14">SECRET LUXURY · WELL-BEING</small></div>
  ${TIERS.map(t => `<div class="tier${t.featured ? ' featured' : ''}">${t.featured ? '<span class="tier-badge">LA SCELTA PIÙ RICHIESTA</span>' : ''}
    <h3 class="tier-name">${esc(t.name)}</h3><p class="muted t13">${esc(t.tag)}</p>
    <div class="tier-price"><b>${esc(t.price)}</b><span>${esc(t.per)}</span></div>
    ${t.items.map(bullet).join('')}
    <button class="${t.featured ? 'grad-btn' : 'out-btn tier-btn'} mt14" data-a="tierInfo">Richiedi l'accesso</button></div>`).join('')}
  <p class="muted t12 mt16">Prezzi indicativi IVA inclusa. Ogni membership è riservata e su richiesta.</p><div class="sp32"></div></div>`;

// =============================================================================== MODALS
function openModal(m) { ui.modal = m; renderOverlay(); }
function closeModal() { ui.modal = null; renderOverlay(); }

function renderOverlay() {
  const root = $('#modal');
  let m = ui.modal;
  if (!m && ui.summary && route().name === 'home') m = { type: 'summary' };
  if (!m) { root.innerHTML = ''; root.classList.remove('open'); document.body.classList.remove('locked'); return; }
  const dlg = (title, body, buttons) => `<div class="scrim" data-a="closeModal"></div><div class="dialog" role="dialog">${title ? `<h2>${title}</h2>` : ''}<div class="dlg-body">${body}</div><div class="dlg-btns">${buttons}</div></div>`;
  const btn = (t, a, color = 'var(--accent)') => `<button style="color:${color}" ${a}>${t}</button>`;
  let html = '';
  switch (m.type) {
    case 'summary': {
      const s = ui.summary;
      html = dlg(`<span class="accent">${icon('trophy')}</span><br>Allenamento completato`, kv('Seduta', s.title) + kv('Durata', `${s.minutes} min`) + kv('Serie allenanti', String(s.sets)) + kv('Tonnellaggio', `${fmtKgInt(s.tonnage)} kg`) +
        (s.records.length ? `<p class="accent xb mt8">Record</p>${s.records.map(bullet).join('')}` : ''), btn('Grande!', 'data-a="closeSummary"'));
      break;
    }
    case 'finish': {
      const [done, total] = workoutCounts(data.active);
      html = dlg('Concludere la seduta?', `<p class="muted">Serie completate: ${done} di ${total}</p><p class="sb mt12">Come ti sei sentito?</p>
        <div class="emojis">${['😫', '😕', '😐', '🙂', '🔥'].map((e, i) => `<button class="${m.feeling === i + 1 ? 'on' : ''}" data-a="feeling" data-v="${i + 1}">${e}</button>`).join('')}</div>
        <label class="field mt10"><span>Note</span><textarea rows="3" data-in="finishNotes">${esc(m.notes)}</textarea></label>`,
      btn('Continua', 'data-a="closeModal"', 'var(--text)') + btn('<b>Salva</b>', 'data-a="finishSave"'));
      break;
    }
    case 'menu':
      html = dlg('', `<button class="menu-item" data-a="freeTimer">Timer libero 90"</button><button class="menu-item" style="color:var(--bad)" data-a="discardAsk">Annulla allenamento</button>`, btn('Chiudi', 'data-a="closeModal"', 'var(--text)'));
      break;
    case 'discard':
      html = dlg("Annullare l'allenamento?", '<p>La seduta non verrà salvata e il programma non avanzerà.</p>', btn('Indietro', 'data-a="closeModal"', 'var(--text)') + btn('Annulla seduta', 'data-a="discard"', 'var(--bad)'));
      break;
    case 'swap': {
      const cur = Ex.get(data.active.exercises[m.ei].exerciseId);
      html = dlg(`Sostituisci ${esc(cur.name)}`, `<button class="check-row" data-a="swapKeep"><span class="checkbox${m.keep ? ' on' : ''}">${m.keep ? icon('check') : ''}</span>Usa anche nelle prossime sedute</button>
        <div class="dlg-list">${Ex.alternatives(cur).map(a => `<button class="lib-row" data-a="swapDo" data-v="${a.id}">${pill(CATEGORIES[a.category].short, 'var(--blue)')}<span class="ml8">${esc(a.name)}</span></button>`).join('')}</div>`,
      btn('Chiudi', 'data-a="closeModal"', 'var(--text)'));
      break;
    }
    case 'delSet':
      html = dlg('Eliminare questa serie?', '', btn('Annulla', 'data-a="closeModal"', 'var(--text)') + btn('Elimina', 'data-a="delSet"', 'var(--bad)'));
      break;
    case 'position':
      html = dlg('Posizione nel programma', `<p class="sb">Fase</p>${segmented(['1', '2', '3'], m.phase, i => `data-a="pos" data-k="phase" data-v="${i}"`)}
        <p class="sb mt10">Mesociclo</p>${segmented(['1', '2', '3'], m.meso, i => `data-a="pos" data-k="meso" data-v="${i}"`)}
        <p class="sb mt10">Settimana</p>${segmented(['1', '2', '3', '4', '5', 'S'], m.week - 1, i => `data-a="pos" data-k="week" data-v="${i + 1}"`)}`,
      btn('Annulla', 'data-a="closeModal"', 'var(--text)') + btn('Salva', 'data-a="posSave"'));
      break;
    case 'saveOneRm':
      html = dlg('Salva massimale di…', Ex.oneRmLifts().map(ex => `<button class="menu-item" data-a="saveOneRm" data-v="${ex.id}">${esc(ex.name)}</button>`).join(''), btn('Chiudi', 'data-a="closeModal"', 'var(--text)'));
      break;
    case 'deleteSession':
      html = dlg('Eliminare la seduta?', '', btn('Annulla', 'data-a="closeModal"', 'var(--text)') + btn('Elimina', 'data-a="deleteSession"', 'var(--bad)'));
      break;
    case 'reset':
      html = dlg('Ricominciare il programma?', '<p>Torni alla fase 1, settimana 1. Lo storico delle sedute e i massimali restano.</p>', btn('Annulla', 'data-a="closeModal"', 'var(--text)') + btn('Ricomincia', 'data-a="reset"', 'var(--bad)'));
      break;
    case 'importConfirm':
      html = dlg('Ripristinare il backup?', `<p>Il backup contiene ${m.data.history.length} sedute e ${m.data.bodyWeight.length} pesate. I dati attuali su questo telefono verranno sostituiti.</p>`,
        btn('Annulla', 'data-a="closeModal"', 'var(--text)') + btn('Ripristina', 'data-a="importDo"'));
      break;
  }
  root.innerHTML = html; root.classList.add('open'); document.body.classList.add('locked');
}

// =============================================================================== backup
async function exportBackup() {
  const json = JSON.stringify(data);
  const name = 'kore-backup.json';
  try {
    const file = new File([json], name, { type: 'application/json' });
    if (navigator.canShare && navigator.canShare({ files: [file] })) { await navigator.share({ files: [file], title: 'Backup KORE' }); return; }
  } catch (e) { if (e && e.name === 'AbortError') return; }
  const a = document.createElement('a');
  a.href = URL.createObjectURL(new Blob([json], { type: 'application/json' })); a.download = name;
  document.body.appendChild(a); a.click(); setTimeout(() => { URL.revokeObjectURL(a.href); a.remove(); }, 1000);
  toast('Backup salvato');
}
function importBackup() {
  const inp = document.createElement('input');
  inp.type = 'file'; inp.accept = '.json,application/json,text/plain';
  inp.onchange = () => {
    const f = inp.files && inp.files[0]; if (!f) return;
    f.text().then(t => {
      const raw = JSON.parse(t);
      if (!raw || typeof raw !== 'object' || !raw.profile) throw new Error('bad');
      openModal({ type: 'importConfirm', data: normalize(raw) });
    }).catch(() => toast('File non valido'));
  };
  inp.click();
}

// =============================================================================== actions
const actions = {
  back: () => { if (history.length > 1) history.back(); else go('home'); },
  tab: el => go(el.dataset.v),
  go: el => go(el.dataset.v),
  hideInstall: () => { ui.installHidden = true; try { localStorage.setItem('kore_install_hidden', '1'); } catch (e) { /* ignore */ } render(); },
  startWorkout: el => {
    startSession(+el.dataset.v);
    requestWakeLock();
    try { if ('Notification' in window && Notification.permission === 'default' && window.navigator.standalone) Notification.requestPermission(); } catch (e) { /* ignore */ }
    go('workout');
  },
  // profile form
  formSet: el => { const f = formDraft(); const k = el.dataset.k; f[k] = k === 'activityFactor' ? +el.dataset.v : el.dataset.v; render(); },
  saveProfile: () => {
    const p = readForm();
    const first = !data.profile.onboarded;
    update(d => { d.profile = p; });
    ui.form = null;
    if (first) go('home', true); else { toast('Profilo aggiornato'); render(); }
  },
  exportBackup, importBackup,
  importDo: () => { const m = ui.modal; data = m.data; save(); ui.modal = null; ui.form = null; toast('Backup ripristinato'); if (data.profile.onboarded) go('home', true); render(); },
  confirmReset: () => openModal({ type: 'reset' }),
  reset: () => { update(d => { d.program = defaultData().program; }); closeModal(); toast('Programma ricominciato'); },
  // workout
  workoutMenu: () => openModal({ type: 'menu' }),
  freeTimer: () => { closeModal(); startRest(90); },
  discardAsk: () => openModal({ type: 'discard' }),
  discard: () => { skipRest(); update(d => { d.active = null; }); ui.modal = null; releaseWakeLock(); go('home'); },
  finishAsk: () => openModal({ type: 'finish', notes: data.active.notes || '', feeling: data.active.feeling || 3 }),
  feeling: el => { ui.modal.feeling = +el.dataset.v; document.querySelectorAll('.emojis button').forEach((b, i) => b.classList.toggle('on', i + 1 === ui.modal.feeling)); },
  finishSave: () => { const m = ui.modal; ui.modal = null; finishSession(m.notes, m.feeling); go('home'); render(); },
  toggleDone: el => {
    const ei = +el.dataset.e, si = +el.dataset.s;
    const s = data.active, e = s.exercises[ei], set = e.sets[si];
    const nowDone = !set.done;
    update(() => { set.done = nowDone; });
    if (nowDone) {
      const isLast = e.sets.slice(si + 1).every(x => x.done);
      const rest = set.type === 'WARMUP' ? 60 : e.restSec;
      if (!isLast || ei < s.exercises.length - 1) startRest(rest, Ex.get(e.exerciseId).name);
    }
    render();
  },
  addSet: el => {
    const e = data.active.exercises[+el.dataset.e];
    const last = [...e.sets].reverse().find(x => x.type !== 'WARMUP') || e.sets[e.sets.length - 1];
    update(() => { e.sets.push({ type: 'WORK', weight: last ? last.weight : 0, reps: last ? last.reps : e.repMax, rir: e.rir, done: false }); });
    render();
  },
  delSetAsk: el => openModal({ type: 'delSet', ei: +el.dataset.e, si: +el.dataset.s }),
  delSet: () => { const { ei, si } = ui.modal; update(d => { d.active.exercises[ei].sets.splice(si, 1); }); ui.modal = null; render(); },
  swapAsk: el => openModal({ type: 'swap', ei: +el.dataset.v, keep: true }),
  swapKeep: () => { ui.modal.keep = !ui.modal.keep; renderOverlay(); },
  swapDo: el => {
    const { ei, keep } = ui.modal; const id = el.dataset.v;
    update(d => {
      const e = d.active.exercises[ei];
      e.exerciseId = id;
      e.sets = e.sets.filter(x => x.type !== 'WARMUP').map(x => ({ ...x, weight: 0, done: false }));
      if (keep) d.swaps[e.slotKey] = id;
    });
    ui.modal = null; render();
  },
  rest: el => addRest(+el.dataset.v),
  skipRest: () => skipRest(),
  // program
  positionAsk: () => { const st = data.program; openModal({ type: 'position', phase: st.phase, meso: st.meso, week: st.week }); },
  pos: el => { ui.modal[el.dataset.k] = +el.dataset.v; renderOverlay(); },
  posSave: () => { const m = ui.modal; update(d => { d.program = { ...d.program, phase: m.phase, meso: m.meso, week: m.week, dayIndex: 0 }; }); ui.modal = null; ui.programWeek = m.week; ui.programOpen = 0; render(); },
  viewWeek: el => { ui.programWeek = +el.dataset.v; render(); },
  toggleDay: el => { const di = +el.dataset.v; ui.programOpen = ui.programOpen === di ? -1 : di; render(); },
  resetSwap: el => { update(d => { delete d.swaps[el.dataset.v]; }); render(); },
  skipScheduled: () => { update(d => { d.program = Program.advance(d.program, d.profile.split); }); ui.programWeek = data.program.week; ui.programOpen = data.program.dayIndex; toast('Seduta saltata'); render(); },
  // progress
  progressTab: el => { ui.progressTab = +el.dataset.v; render(); },
  deleteSessionAsk: el => openModal({ type: 'deleteSession', id: el.dataset.v }),
  deleteSession: () => { const id = ui.modal.id; update(d => { d.history = d.history.filter(s => String(s.id) !== id); }); ui.modal = null; actions.back(); },
  closeSummary: () => { ui.summary = null; renderOverlay(); },
  closeModal: () => { if (ui.modal) closeModal(); else if (ui.summary) actions.closeSummary(); },
  // tools
  saveOneRmAsk: el => openModal({ type: 'saveOneRm', kg: +el.dataset.v }),
  saveOneRm: el => { const kg = ui.modal.kg; update(d => { d.profile.oneRm[el.dataset.v] = kg; }); closeModal(); toast(`Massimale salvato: ${Calc.fmt(kg)} kg`); },
  platesAdd: el => {
    const t = parseNum(ui.plates.target) || 0;
    ui.plates.target = Calc.num(Math.max(0, t + +el.dataset.v)) || '0';
    const inp = $('#plates-target'); if (inp) inp.value = ui.plates.target;
    $('#plates-out').innerHTML = platesOut();
  },
  saveBw: () => {
    const kg = parseNum(ui.bw); if (!(kg > 0)) { toast('Inserisci un peso valido'); return; }
    const day = todayEpochDay();
    update(d => { d.bodyWeight = [...d.bodyWeight.filter(e => e.epochDay !== day), { epochDay: day, kg }].sort((a, b) => a.epochDay - b.epochDay); d.profile.weightKg = kg; });
    toast('Pesata salvata'); render();
  },
  delBw: el => { const day = +el.dataset.v; update(d => { d.bodyWeight = d.bodyWeight.filter(e => e.epochDay !== day); }); render(); },
  libMuscle: el => { ui.libMuscle = el.dataset.v || null; render(); },
  tierInfo: () => toast('Le richieste di accesso apriranno a breve'),
  guide: el => { const i = +el.dataset.v; ui.guideOpen = ui.guideOpen === i ? -1 : i; render(); }
};

const inputs = {
  form: el => { formDraft()[el.dataset.k] = el.value; },
  formRm: el => { formDraft().oneRm[el.dataset.k] = el.value; },
  formDays: el => { formDraft().daysPerWeek = +el.value; $('#days-l').textContent = el.value; },
  set: el => {
    const ei = +el.dataset.e, si = +el.dataset.s, f = el.dataset.f;
    const e = data.active.exercises[ei], set = e.sets[si];
    const v = el.value.replace(',', '.');
    if (f === 'weight') set.weight = parseNum(v) || 0;
    else if (f === 'reps') set.reps = parseInt(v, 10) || 0;
    else set.rir = v.trim() === '' || isNaN(parseInt(v, 10)) ? null : parseInt(v, 10);
    save();
    $(`#imp-${ei}`).innerHTML = improvementBox(Progression.improvement(data.history, e.exerciseId, e, data.active.startedAt));
  },
  finishNotes: el => { ui.modal.notes = el.value; },
  onerm: el => { ui.onerm[el.dataset.k] = el.value; $('#onerm-out').innerHTML = onermOut(); },
  rirOrm: el => { ui.rirOrm = el.value; $('#rir-out').innerHTML = rirOut(); },
  plates: el => { ui.plates[el.dataset.k] = el.value; $('#plates-out').innerHTML = platesOut(); },
  bw: el => { ui.bw = el.value; },
  libQuery: el => { ui.libQuery = el.value; $('#lib-list').innerHTML = libList(); },
  progressEx: el => { ui.progressEx = el.value; render(); }
};

document.addEventListener('click', ev => {
  unlockAudio();
  const el = ev.target.closest('[data-a]');
  if (!el || el.disabled) return;
  const fn = actions[el.dataset.a];
  if (fn) { ev.preventDefault(); fn(el); }
});
document.addEventListener('input', ev => { const el = ev.target; const fn = el.dataset && inputs[el.dataset.in]; if (fn && el.tagName !== 'SELECT') fn(el); });
document.addEventListener('change', ev => { const el = ev.target; if (el.tagName === 'SELECT' && inputs[el.dataset.in]) inputs[el.dataset.in](el); });
document.addEventListener('focusin', ev => { if (ev.target.classList && ev.target.classList.contains('cell')) setTimeout(() => ev.target.select && ev.target.select(), 0); });
document.addEventListener('touchstart', unlockAudio, { passive: true });

// =============================================================================== boot
render();
renderRestBar();
if ('serviceWorker' in navigator && location.protocol !== 'file:') navigator.serviceWorker.register('sw.js').catch(() => {});
try { if (navigator.storage && navigator.storage.persist) navigator.storage.persist(); } catch (e) { /* ignore */ }
