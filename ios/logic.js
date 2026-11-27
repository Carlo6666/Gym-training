// KORE Training · logica (calcoli, programma, statistiche, progressione). Porting 1:1 dall'app Android.
'use strict';

const EX_BY_ID = Object.fromEntries(EXERCISES.map(e => [e.id, e]));
const Ex = {
  get: id => EX_BY_ID[id] || EXERCISES[0],
  find: id => EX_BY_ID[id] || null,
  alternatives: ex => EXERCISES.filter(e => e.muscle === ex.muscle && e.id !== ex.id),
  oneRmLifts: () => EXERCISES.filter(e => e.tracksOneRm)
};

// ------------------------------------------------------------------ date helpers
const DAY_MS = 86400000;
/** local calendar day of a timestamp, as epoch day (days since 1970-01-01). */
function epochDayOf(ms) { const d = new Date(ms); return Math.round(Date.UTC(d.getFullYear(), d.getMonth(), d.getDate()) / DAY_MS); }
function todayEpochDay() { return epochDayOf(Date.now()); }
/** Monday of the week containing epoch day [ed]. 1970-01-01 was a Thursday. */
function weekStartDay(ed) { const dow = ((ed + 3) % 7 + 7) % 7; return ed - dow; } // dow: 0 = Monday
function dateOfEpochDay(ed) { const u = new Date(ed * DAY_MS); return new Date(u.getUTCFullYear(), u.getUTCMonth(), u.getUTCDate()); }
function fmtDate(ed) { return dateOfEpochDay(ed).toLocaleDateString('it-IT', { weekday: 'short', day: 'numeric', month: 'short' }); }
function fmtShort(ed) { const d = dateOfEpochDay(ed); return d.getDate() + '/' + (d.getMonth() + 1); }

// ------------------------------------------------------------------ Calc
const Calc = {
  epley: (w, r) => r <= 1 ? w : w * (1 + r / 30),
  brzycki: (w, r) => r <= 1 ? w : w * 36 / (37 - Math.min(r, 36)),
  /** 1RM estimate counting reps in reserve: 8 reps @RIR 2 ≈ a 10RM. */
  e1rm(w, reps, rir) {
    if (!(w > 0) || !(reps > 0)) return 0;
    const r = reps + (rir ?? 0);
    return r <= 10 ? (Calc.epley(w, r) + Calc.brzycki(w, r)) / 2 : Calc.epley(w, r);
  },
  pctFor(reps, rir = 0) { const r = Math.max(1, reps + rir); return r === 1 ? 1 : 1 / (1 + r / 30); },
  repsAt: pct => pct >= 1 ? 1 : Math.max(1, Math.round((1 / pct - 1) * 30)),
  round: (w, step) => step <= 0 ? w : Math.round(w / step) * step,
  fmt(kg) {
    if (kg == null || isNaN(kg)) return '0';
    if (Math.abs(kg % 1) < 1e-9) return String(Math.round(kg));
    return kg.toFixed(2).replace(/0+$/, '').replace(/\.$/, '').replace('.', ',');
  },
  /** input-friendly number (dot decimal) */
  num(kg) { return kg > 0 ? String(+kg.toFixed(2)) : ''; },
  plates(target, bar, available) {
    let perSide = (target - bar) / 2;
    if (perSide <= 0) return [[], 0];
    const out = [];
    for (const p of [...available].sort((a, b) => b - a)) {
      while (perSide + 1e-6 >= p) { out.push(p); perSide -= p; }
    }
    return [out, perSide * 2];
  },
  warmup(work, bar, step) {
    if (work <= 0) return [];
    const ramp = [[0.4, 8], [0.6, 5], [0.75, 3], [0.87, 1]];
    const sets = [];
    if (step === 2.5 && bar > 0 && work > bar + 15) sets.push([bar, 10]);
    for (const [pct, reps] of ramp) {
      const w = Calc.round(work * pct, step);
      if (w <= 0) continue;
      if (sets.length && w <= sets[sets.length - 1][0]) continue;
      if (w >= work) continue;
      sets.push([w, reps]);
    }
    return work < 40 ? sets.slice(-2) : sets;
  },
  nutrition(p, trainingDays) {
    const bmr = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + (p.sex === 'M' ? 5 : -161);
    const tdee = bmr * p.activityFactor;
    const r = RESPONDERS[p.responder] || RESPONDERS.NORMALE;
    const surplus = (r.surplusMin + r.surplusMax) / 2;
    const target = tdee + surplus;
    const protPerKg = p.responder === 'HARD_GAINER' ? 2.0 : 1.8;
    const fatPerKg = p.responder === 'HARD_GAINER' ? 1.2 : p.responder === 'INGRASSO_FACILE' ? 0.9 : 1.0;
    const prot = protPerKg * p.weightKg, fat = fatPerKg * p.weightKg;
    const carbs = Math.max(0, (target - prot * 4 - fat * 9) / 4);
    const week = target * 7, tKcal = target * 1.10;
    const restDays = Math.max(1, 7 - trainingDays);
    const rKcal = (week - tKcal * trainingDays) / restDays;
    const macro = kcal => ({ kcal: Math.round(kcal), p: Math.round(prot), f: Math.round(fat), c: Math.round(Math.max(0, (kcal - prot * 4 - fat * 9) / 4)) });
    let note = 'Puoi partire con la fase di massa. Pesati ogni mattina e confronta le medie settimanali.';
    if (p.bodyFatPct > 15 && p.sex === 'M') note = 'Con una massa grassa sopra il 15% conviene una fase di definizione prima di iniziare il surplus.';
    else if (p.bodyFatPct > 25 && p.sex === 'F') note = 'Con una massa grassa sopra il 25% conviene una fase di definizione prima di iniziare il surplus.';
    return { bmr: Math.round(bmr), tdee: Math.round(tdee), target: Math.round(target), proteinG: Math.round(prot), fatG: Math.round(fat), carbG: Math.round(carbs), trainingDay: macro(tKcal), restDay: macro(rKcal), note };
  },
  weeklyAverages(entries) {
    const g = new Map();
    for (const e of entries) { const ws = weekStartDay(e.epochDay); if (!g.has(ws)) g.set(ws, []); g.get(ws).push(e.kg); }
    return [...g.entries()].map(([ws, l]) => ({ weekStart: ws, avg: l.reduce((a, b) => a + b, 0) / l.length, count: l.length })).sort((a, b) => a.weekStart - b.weekStart);
  },
  weightAdvice(weeks) {
    if (weeks.length < 2) return [null, "Servono almeno due settimane di pesate per valutare l'andamento."];
    const a = weeks[weeks.length - 2].avg, b = weeks[weeks.length - 1].avg;
    const pct = (b - a) / a * 100;
    const msg = pct > 1.0 ? 'Stai crescendo troppo in fretta (oltre 1% a settimana): riduci leggermente le calorie (100-150 kcal).'
      : pct >= 0.25 ? 'Andamento ideale: mantieni le calorie attuali.'
        : pct > -0.1 ? 'Peso fermo: aumenta le calorie di 100-200 kcal al giorno.'
          : 'Peso in calo: se sei in fase di massa aumenta le calorie di circa 200 kcal.';
    return [pct, msg];
  }
};

// ------------------------------------------------------------------ Program
const Program = {
  WEEKS_PER_MESO: 6,
  MESO_PER_PHASE: 3,
  get totalWeeks() { return PHASES.length * this.MESO_PER_PHASE * this.WEEKS_PER_MESO; },
  templates: split => TEMPLATES[split] || TEMPLATES.SPLIT_A,
  exerciseFor(slot, phase, swaps) {
    const sw = swaps[slot.key] && Ex.find(swaps[slot.key]);
    return sw || Ex.get(slot.options[phase % slot.options.length]);
  },
  repsLabel: rx => rx.repMin === rx.repMax ? `${rx.repMin}` : `${rx.repMin}-${rx.repMax}`,

  prescription(category, phase, meso, week, level, hasOneRm) {
    const deload = week >= this.WEEKS_PER_MESO;
    const w = Math.min(5, Math.max(1, week));
    const P = (sets, repMin, repMax, rir, restSec, o = {}) => ({ sets, repMin, repMax, rir, restSec, tempo: o.tempo || '2-0-1-0', pct: o.pct ?? null, technique: o.technique || 'NESSUNA', note: o.note || '' });
    let rx;
    if (phase === 0) {
      if (category === 'BASE') {
        const table = [[3, 6], [4, 6], [5, 6], [5, 5], [5, 4]];
        const pcts = [0.70, 0.70, 0.70, 0.75, 0.80];
        const [sets, reps] = table[w - 1];
        rx = P(sets, reps, reps, 2, 180, { pct: hasOneRm ? pcts[w - 1] + 0.025 * meso : null, note: w <= 3 ? 'Volume in crescita a carico costante' : 'Carico in salita, ripetizioni in calo' });
      } else if (category === 'COMPLEMENTARE') {
        rx = P(w <= 2 ? 3 : 4, 8, 10, w <= 2 ? 2 : 1, 120, { note: 'Doppia progressione: quando chiudi tutte le serie a 10, aumenta il carico' });
      } else rx = P(w <= 2 ? 2 : 3, 12, 15, w <= 2 ? 1 : 0, 75);
    } else if (phase === 1) {
      if (category === 'BASE') rx = P([3, 4, 4, 4, 5][w - 1], 6, 8, w <= 2 ? 2 : 1, 165, { note: "Buffer sulle prime serie, l'ultima vicina al cedimento tecnico" });
      else if (category === 'COMPLEMENTARE') rx = P(w <= 2 ? 3 : 4, 8, 12, w <= 2 ? 2 : 1, 105, { note: 'Ultima serie a cedimento concentrico' });
      else rx = P(3, 12, 15, 0, 60, { technique: w >= 4 ? 'REST_PAUSE' : 'NESSUNA', note: 'Tutte le serie a cedimento' });
    } else {
      if (category === 'BASE') rx = P(3, 8, 10, 1, [150, 135, 120, 105, 90][w - 1], { technique: w >= 3 ? 'BACK_OFF' : 'NESSUNA', note: 'Progressione di densità: stesso lavoro, recuperi più brevi' });
      else if (category === 'COMPLEMENTARE') rx = P(3, 10, 12, w <= 2 ? 1 : 0, [90, 80, 75, 70, 60][w - 1], { tempo: '3-0-1-1', note: 'Cerca il pompaggio, contrazione di picco' });
      else rx = P(3, 15, 20, 0, [60, 55, 50, 45, 45][w - 1], { tempo: '2-0-1-1', technique: (w === 3 || w === 4) ? 'STRIPPING' : w === 5 ? 'ISOMETRIA' : 'NESSUNA', note: 'Cedimento su tutte le serie' });
    }
    if (level === 'PRINCIPIANTE') rx = { ...rx, sets: category === 'BASE' ? rx.sets : Math.max(2, rx.sets - 1), rir: Math.max(1, rx.rir), technique: 'NESSUNA' };
    else if (level === 'AVANZATO' && category === 'ISOLAMENTO' && phase > 0) rx = { ...rx, sets: rx.sets + 1 };
    if (deload) {
      rx = { ...rx, sets: Math.max(2, Math.floor(rx.sets / 2)), rir: 3, pct: rx.pct != null ? rx.pct - 0.12 : null, technique: 'NESSUNA', note: 'Settimana di scarico: carichi -15%, metà delle serie, lontano dal cedimento' };
    }
    return rx;
  },

  plan(data, dayIndex = data.program.dayIndex, week = data.program.week) {
    const p = data.profile, st = data.program;
    const templates = this.templates(p.split);
    const t = templates[Math.min(templates.length - 1, Math.max(0, dayIndex))];
    const deload = week >= this.WEEKS_PER_MESO;
    const items = t.slots.map(slot => {
      const ex = this.exerciseFor(slot, st.phase, data.swaps);
      const oneRm = ex.tracksOneRm && slot.category === 'BASE' ? (p.oneRm[ex.id] ?? null) : null;
      const rx = this.prescription(slot.category, st.phase, st.meso, week, p.level, oneRm != null);
      const [kg, why] = this.suggest(data, ex, rx, oneRm, deload);
      return { slot, exercise: ex, rx, suggestedKg: kg, suggestionWhy: why };
    });
    return { template: t, items, deload };
  },

  suggest(data, ex, rx, oneRm, deload) {
    const step = EQUIPMENT[ex.equipment].step;
    if (oneRm != null && rx.pct != null) return [Calc.round(oneRm * rx.pct, step), `${Math.floor(rx.pct * 100 + 1e-9)}% del massimale (${Calc.fmt(oneRm)} kg)`];
    let last = null;
    for (const s of data.history) for (const e of s.exercises) {
      if (e.exerciseId === ex.id && e.sets.some(x => x.done && x.type === 'WORK') && (!last || s.startedAt > last.s.startedAt)) last = { s, e };
    }
    if (!last) {
      if (ex.tracksOneRm && data.profile.oneRm[ex.id] != null) {
        const pct = Calc.pctFor(Math.floor((rx.repMin + rx.repMax) / 2), rx.rir);
        return [Calc.round(data.profile.oneRm[ex.id] * pct * (deload ? 0.85 : 1), step), 'Stimato dal massimale'];
      }
      return [null, `Scegli un carico che ti lasci ${rx.rir} ripetizioni di margine`];
    }
    const prev = last.e;
    const work = prev.sets.filter(x => x.done && x.type === 'WORK');
    const top = Math.max(...work.map(x => x.weight));
    const factor = deload ? 0.85 : 1;
    if (prev.repMin === rx.repMin && prev.repMax === rx.repMax) {
      const allTop = work.every(x => x.reps >= rx.repMax);
      if (allTop && !deload) return [Calc.round(top + step, step), `Hai chiuso tutte le serie a ${rx.repMax}: +${Calc.fmt(step)} kg`];
      return [Calc.round(top * factor, step), deload ? "Scarico: -15% rispetto all'ultima volta" : 'Stesso carico, punta a più ripetizioni'];
    }
    const best = Math.max(...work.map(x => Calc.e1rm(x.weight, x.reps, x.rir)));
    return [Calc.round(best * Calc.pctFor(rx.repMax, rx.rir) * factor, step), 'Ricalcolato sul nuovo range di ripetizioni'];
  },

  volumePerMuscle(data) {
    const out = {};
    this.templates(data.profile.split).forEach((_, i) => {
      this.plan(data, i).items.forEach(pe => {
        out[pe.exercise.muscle] = (out[pe.exercise.muscle] || 0) + pe.rx.sets;
        pe.exercise.secondary.forEach(m => { out[m] = (out[m] || 0) + pe.rx.sets * 0.5; });
      });
    });
    if (data.profile.split === 'PPL') for (const k in out) out[k] = out[k] * 7 / 8.5;
    return out;
  },

  advance(st, split) {
    const n = this.templates(split).length;
    let d = st.dayIndex + 1, w = st.week, m = st.meso, ph = st.phase;
    if (d >= n) { d = 0; w++; }
    if (w > this.WEEKS_PER_MESO) { w = 1; m++; }
    if (m >= this.MESO_PER_PHASE) { m = 0; ph = (ph + 1) % PHASES.length; }
    return { ...st, phase: ph, meso: m, week: w, dayIndex: d };
  },
  absoluteWeek(st) { return (st.phase * this.MESO_PER_PHASE + st.meso) * this.WEEKS_PER_MESO + st.week; }
};

// ------------------------------------------------------------------ Stats
const Stats = {
  workSets: e => e.sets.filter(x => x.done && x.type !== 'WARMUP'),
  volume: s => s.exercises.reduce((a, e) => a + Stats.workSets(e).reduce((b, x) => b + x.weight * x.reps, 0), 0),
  setCount: s => s.exercises.reduce((a, e) => a + Stats.workSets(e).length, 0),
  bestE1rm: e => Stats.workSets(e).reduce((m, x) => Math.max(m, Calc.e1rm(x.weight, x.reps, x.rir)), 0),
  sorted: h => [...h].sort((a, b) => a.startedAt - b.startedAt),
  e1rmSeries(history, id) {
    return Stats.sorted(history).map(s => {
      const best = s.exercises.filter(e => e.exerciseId === id).reduce((m, e) => Math.max(m, Stats.bestE1rm(e)), 0);
      return best > 0 ? { day: epochDayOf(s.startedAt), value: best } : null;
    }).filter(Boolean);
  },
  volumeSeries(history, id) {
    return Stats.sorted(history).map(s => {
      const v = s.exercises.filter(e => e.exerciseId === id).reduce((a, e) => a + Stats.workSets(e).reduce((b, x) => b + x.weight * x.reps, 0), 0);
      return v > 0 ? { day: epochDayOf(s.startedAt), value: v } : null;
    }).filter(Boolean);
  },
  weekly(history) {
    const g = new Map();
    for (const s of history) { const w = weekStartDay(epochDayOf(s.startedAt)); if (!g.has(w)) g.set(w, []); g.get(w).push(s); }
    return [...g.entries()].map(([w, l]) => ({ start: w, tonnage: l.reduce((a, s) => a + Stats.volume(s), 0), sets: l.reduce((a, s) => a + Stats.setCount(s), 0), sessions: l.length })).sort((a, b) => a.start - b.start);
  },
  setsPerMuscleThisWeek(history) {
    const start = weekStartDay(todayEpochDay());
    const out = {};
    history.filter(s => epochDayOf(s.startedAt) >= start).forEach(s => s.exercises.forEach(e => {
      const ex = Ex.find(e.exerciseId); if (!ex) return;
      const n = Stats.workSets(e).length;
      out[ex.muscle] = (out[ex.muscle] || 0) + n;
      ex.secondary.forEach(m => { out[m] = (out[m] || 0) + n * 0.5; });
    }));
    return out;
  },
  records(history) {
    const map = {};
    history.forEach(s => s.exercises.forEach(e => {
      const ex = Ex.find(e.exerciseId); if (!ex) return;
      Stats.workSets(e).forEach(set => {
        const est = Calc.e1rm(set.weight, set.reps, set.rir);
        const cur = map[ex.id];
        if (!cur || est > cur.bestE1rm) {
          map[ex.id] = { exercise: ex, bestE1rm: est, heaviest: Math.max(set.weight, cur?.heaviest || 0), heaviestReps: set.weight >= (cur?.heaviest || 0) ? set.reps : (cur?.heaviestReps || 0), day: epochDayOf(s.startedAt) };
        } else if (set.weight > cur.heaviest) {
          map[ex.id] = { ...cur, heaviest: set.weight, heaviestReps: set.reps };
        }
      });
    }));
    return Object.values(map).sort((a, b) => b.bestE1rm - a.bestE1rm);
  },
  streakWeeks(history) {
    const weeks = new Set(history.map(s => weekStartDay(epochDayOf(s.startedAt))));
    let w = weekStartDay(todayEpochDay());
    if (!weeks.has(w)) w -= 7;
    let n = 0;
    while (weeks.has(w)) { n++; w -= 7; }
    return n;
  },
  landmarks: m => ['BICIPITI', 'TRICIPITI', 'POLPACCI', 'ADDOME'].includes(m) ? { mv: 4, mev: 6, mavLow: 8, mavHigh: 18 } : { mv: 8, mev: 10, mavLow: 15, mavHigh: 25 },
  zone(m, sets) {
    const l = Stats.landmarks(m);
    return sets < l.mv ? 'Sotto mantenimento' : sets < l.mev ? 'Mantenimento' : sets < l.mavLow ? 'Minimo efficace' : sets <= l.mavHigh ? 'Zona ottimale' : 'Oltre il recuperabile';
  }
};

// ------------------------------------------------------------------ Progression (miglioramento % e atteso)
const Progression = {
  /**
   * before: only sessions started before this instant count as "previous".
   * current: the performance being evaluated (live session or a saved one).
   */
  improvement(history, exerciseId, current, before) {
    const series = Stats.sorted(history.filter(s => s.startedAt < before)).map(s => {
      const ex = s.exercises.filter(e => e.exerciseId === exerciseId);
      const e1 = ex.reduce((m, e) => Math.max(m, Stats.bestE1rm(e)), 0);
      const vol = ex.reduce((a, e) => a + Stats.workSets(e).reduce((b, x) => b + x.weight * x.reps, 0), 0);
      return e1 > 0 ? [e1, vol] : null;
    }).filter(Boolean).slice(-8);

    const curSets = current ? Stats.workSets(current) : [];
    const curE1 = curSets.reduce((m, x) => Math.max(m, Calc.e1rm(x.weight, x.reps, x.rir)), 0);
    const curVol = curSets.reduce((a, x) => a + x.weight * x.reps, 0);
    const prev = series.length ? series[series.length - 1] : null;
    const e1Pct = prev && curE1 > 0 ? (curE1 - prev[0]) / prev[0] * 100 : null;
    const volPct = prev && prev[1] > 0 && curVol > 0 ? (curVol - prev[1]) / prev[1] * 100 : null;

    let expPct = null, expE1 = null;
    if (series.length >= 2) {
      const ys = series.map(x => x[0]); const n = ys.length;
      const xMean = (n - 1) / 2, yMean = ys.reduce((a, b) => a + b, 0) / n;
      let num = 0, den = 0;
      ys.forEach((y, i) => { num += (i - xMean) * (y - yMean); den += (i - xMean) * (i - xMean); });
      const slope = den > 0 ? num / den : 0;
      const fitLast = yMean + slope * (n - 1 - xMean);
      expE1 = fitLast + slope;
      expPct = fitLast > 0 ? slope / fitLast * 100 : null;
    } else if (series.length === 1) {
      expPct = 1.0; expE1 = series[0][0] * 1.01;
    }

    let repsToBeat = null;
    if (prev) {
      let w = curSets.reduce((m, x) => Math.max(m, x.weight), 0);
      if (!(w > 0)) w = current?.sets?.find(x => x.type !== 'WARMUP')?.weight || 0;
      if (w > 0) {
        for (let r = 1; r <= 30; r++) if (Calc.e1rm(w, r) > prev[0] + 0.01) { repsToBeat = [w, r]; break; }
      }
    }
    return { curE1rm: curE1, prevE1rm: prev ? prev[0] : null, e1rmPct: e1Pct, curVolume: curVol, prevVolume: prev ? prev[1] : null, volumePct: volPct, expectedPct: expPct, expectedE1rm: expE1, samples: series.length, repsToBeat };
  },
  pct: v => v == null ? '—' : (v >= 0 ? '+' : '') + v.toFixed(1).replace('.', ',') + '%'
};
