package it.kore.training.data

import kotlin.math.max

data class Slot(val key: String, val category: Category, val options: List<String>)

data class DayTemplate(val name: String, val muscles: String, val slots: List<Slot>)

data class Prescription(
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val restSec: Int,
    val tempo: String = "2-0-1-0",
    /** fraction of 1RM, when the scheme is percentage based. */
    val pct: Double? = null,
    val technique: Technique = Technique.NESSUNA,
    val note: String = ""
) {
    val repsLabel: String get() = if (repMin == repMax) "$repMin" else "$repMin-$repMax"
}

data class PlannedExercise(
    val slot: Slot,
    val exercise: Exercise,
    val rx: Prescription,
    val suggestedKg: Double?,
    val suggestionWhy: String
)

data class PlannedDay(val template: DayTemplate, val items: List<PlannedExercise>, val deload: Boolean)

data class PhaseInfo(val name: String, val short: String, val goal: String, val details: List<String>)

object Program {

    const val WEEKS_PER_MESO = 6
    const val MESO_PER_PHASE = 3

    val phases = listOf(
        PhaseInfo(
            "Forza & Ipertrofia", "FASE 1",
            "Costruire le basi di forza sui fondamentali e alzare i massimali.",
            listOf(
                "Fondamentali a percentuale del massimale: prima sale il volume a carico costante, poi sale il carico e scendono le ripetizioni.",
                "Complementari in doppia progressione (prima le ripetizioni, poi il carico).",
                "Massimali da testare all'inizio e alla fine della fase.",
                "Ogni mesociclo dura 5 settimane di carico + 1 di scarico."
            )
        ),
        PhaseInfo(
            "Trasformazione Ipertrofica", "FASE 2",
            "Più volume su complementari e isolamento, serie portate al cedimento.",
            listOf(
                "Fondamentali in range 6-8 con buffer 2 → 1.",
                "Più serie per i complementari, ultima serie a cedimento concentrico.",
                "Isolamento a cedimento con rest-pause nelle settimane più dure.",
                "Rotazione degli esercizi complementari rispetto alla fase 1, i fondamentali restano."
            )
        ),
        PhaseInfo(
            "Qualità Muscolare", "FASE 3",
            "Pompaggio, densità e tecniche di intensità per rifinire il fisico.",
            listOf(
                "Recuperi che si accorciano settimana dopo settimana (progressione di densità).",
                "Back-off set dopo i fondamentali, stripping e tenute isometriche sull'isolamento.",
                "Cedimento frequente su complementari e isolamento.",
                "Fase adatta anche a un periodo di definizione."
            )
        )
    )

    // -------------------------------------------------------------- templates

    private fun s(key: String, c: Category, vararg ids: String) = Slot(key, c, ids.toList())
    private val B = Category.BASE
    private val C = Category.COMPLEMENTARE
    private val I = Category.ISOLAMENTO

    private val splitA = listOf(
        DayTemplate("Petto", "Petto · Spalle · Bicipiti", listOf(
            s("A1_base", B, "panca_piana"),
            s("A1_comp", C, "panca_inclinata_man", "panca_inclinata_bil", "dip"),
            s("A1_iso", I, "croci_cavi", "pec_deck", "croci_man"),
            s("A1_spalle", I, "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("A1_bic", C, "curl_bil", "curl_man", "curl_bil"),
            s("A1_bic2", I, "curl_inclinata", "curl_scott", "curl_concentrato")
        )),
        DayTemplate("Dorso", "Dorso · Petto · Tricipiti · Femorali", listOf(
            s("A2_base", B, "lat_machine"),
            s("A2_comp", C, "rematore_bil", "rematore_man", "pulley"),
            s("A2_comp2", C, "pulley", "lat_stretta", "rematore_man"),
            s("A2_petto", I, "pec_deck", "croci_cavi", "croci_man"),
            s("A2_tri", C, "french_press", "panca_stretta", "french_press"),
            s("A2_tri2", I, "pushdown", "estensioni_sopra", "pushdown"),
            s("A2_fem", I, "leg_curl")
        )),
        DayTemplate("Spalle", "Spalle · Petto · Bicipiti", listOf(
            s("A3_base", B, "military"),
            s("A3_comp", C, "lento_man"),
            s("A3_lat", I, "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("A3_post", I, "alzate_post", "face_pull", "alzate_post"),
            s("A3_petto", C, "dip", "chest_press", "panca_piana_man"),
            s("A3_bic", I, "hammer", "curl_concentrato", "hammer")
        )),
        DayTemplate("Gambe", "Gambe · Dorso · Tricipiti", listOf(
            s("A4_base", B, "squat"),
            s("A4_post", C, "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("A4_quad", C, "pressa", "hack_squat", "affondi"),
            s("A4_ext", I, "leg_extension", "leg_extension", "sissy"),
            s("A4_calf", I, "calf_piedi", "calf_seduto", "calf_piedi"),
            s("A4_dorso", C, "lat_stretta", "trazioni", "pulley"),
            s("A4_tri", I, "estensioni_sopra", "pushdown", "estensioni_sopra")
        ))
    )

    private val splitB = listOf(
        DayTemplate("Petto & Dorso", "Petto · Dorso · Tricipiti", listOf(
            s("B1_base", B, "panca_piana"),
            s("B1_dorso", C, "rematore_bil", "rematore_man", "pulley"),
            s("B1_petto", C, "panca_inclinata_man", "dip", "panca_inclinata_bil"),
            s("B1_dorso2", C, "pulley", "lat_stretta", "rematore_man"),
            s("B1_iso", I, "croci_cavi", "pec_deck", "croci_man"),
            s("B1_tri", I, "pushdown", "estensioni_sopra", "pushdown")
        )),
        DayTemplate("Spalle & Gambe", "Spalle · Bicipiti · Gambe", listOf(
            s("B2_base", B, "military"),
            s("B2_lat", I, "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("B2_quad", C, "pressa", "hack_squat", "affondi"),
            s("B2_fem", I, "leg_curl"),
            s("B2_bic", C, "curl_bil", "curl_man", "curl_bil"),
            s("B2_calf", I, "calf_piedi", "calf_seduto", "calf_piedi")
        )),
        DayTemplate("Dorso & Petto", "Dorso · Petto · Tricipiti", listOf(
            s("B3_base", B, "lat_machine"),
            s("B3_petto", C, "panca_inclinata_bil", "panca_inclinata_man", "chest_press"),
            s("B3_dorso", C, "rematore_man", "pulley", "rematore_bil"),
            s("B3_petto2", C, "dip", "panca_piana_man", "dip"),
            s("B3_tri", C, "french_press", "panca_stretta", "french_press"),
            s("B3_abs", I, "crunch_cavo", "leg_raise", "crunch_cavo")
        )),
        DayTemplate("Gambe & Spalle", "Gambe · Spalle · Bicipiti", listOf(
            s("B4_base", B, "squat"),
            s("B4_post", C, "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("B4_ext", I, "leg_extension", "sissy", "leg_extension"),
            s("B4_spalle", C, "lento_man"),
            s("B4_post_d", I, "alzate_post", "face_pull", "alzate_post"),
            s("B4_bic", I, "curl_inclinata", "curl_scott", "hammer"),
            s("B4_calf", I, "calf_seduto", "calf_piedi", "calf_seduto")
        ))
    )

    private val ppl = listOf(
        DayTemplate("Push 1", "Petto · Spalle · Tricipiti", listOf(
            s("P1_base", B, "panca_piana"),
            s("P1_petto", C, "panca_inclinata_man", "panca_inclinata_bil", "dip"),
            s("P1_spalle", C, "lento_man"),
            s("P1_lat", I, "alzate_laterali", "alzate_cavo", "alzate_laterali"),
            s("P1_iso", I, "croci_cavi", "pec_deck", "croci_man"),
            s("P1_tri", I, "pushdown", "estensioni_sopra", "pushdown")
        )),
        DayTemplate("Legs 1", "Quadricipiti · Femorali · Polpacci", listOf(
            s("L1_base", B, "squat"),
            s("L1_quad", C, "pressa", "hack_squat", "pressa"),
            s("L1_fem", C, "stacco_rumeno", "hip_thrust", "stacco_rumeno"),
            s("L1_ext", I, "leg_extension", "sissy", "leg_extension"),
            s("L1_calf", I, "calf_piedi", "calf_seduto", "calf_piedi")
        )),
        DayTemplate("Pull 1", "Dorso · Bicipiti", listOf(
            s("U1_base", B, "lat_machine"),
            s("U1_row", C, "rematore_bil", "rematore_man", "rematore_bil"),
            s("U1_row2", C, "pulley", "lat_stretta", "pulley"),
            s("U1_post", I, "face_pull", "alzate_post", "face_pull"),
            s("U1_bic", C, "curl_bil", "curl_man", "curl_bil"),
            s("U1_bic2", I, "hammer", "curl_concentrato", "hammer")
        )),
        DayTemplate("Push 2", "Spalle · Petto · Tricipiti", listOf(
            s("P2_base", B, "military"),
            s("P2_petto", C, "panca_inclinata_bil", "chest_press", "panca_piana_man"),
            s("P2_dip", C, "dip"),
            s("P2_lat", I, "alzate_cavo", "alzate_laterali", "alzate_cavo"),
            s("P2_iso", I, "pec_deck", "croci_man", "croci_cavi"),
            s("P2_tri", C, "french_press", "panca_stretta", "french_press")
        )),
        DayTemplate("Legs 2", "Femorali · Quadricipiti · Addome", listOf(
            s("L2_base", B, "stacco"),
            s("L2_quad", C, "hack_squat", "affondi", "hack_squat"),
            s("L2_fem", I, "leg_curl"),
            s("L2_aff", C, "affondi", "pressa", "affondi"),
            s("L2_calf", I, "calf_seduto", "calf_piedi", "calf_seduto"),
            s("L2_abs", I, "crunch_cavo", "leg_raise", "crunch_cavo")
        )),
        DayTemplate("Pull 2", "Dorso · Bicipiti · Spalle post.", listOf(
            s("U2_base", B, "trazioni"),
            s("U2_row", C, "rematore_man", "pulley", "rematore_man"),
            s("U2_lat", C, "lat_stretta", "lat_machine", "lat_stretta"),
            s("U2_post", I, "alzate_post", "face_pull", "alzate_post"),
            s("U2_bic", I, "curl_inclinata", "curl_scott", "curl_inclinata"),
            s("U2_bic2", I, "curl_concentrato", "hammer", "curl_concentrato")
        ))
    )

    fun templates(split: SplitType): List<DayTemplate> = when (split) {
        SplitType.SPLIT_A -> splitA
        SplitType.SPLIT_B -> splitB
        SplitType.PPL -> ppl
    }

    fun exerciseFor(slot: Slot, phase: Int, swaps: Map<String, String>): Exercise =
        swaps[slot.key]?.let { Exercises.find(it) } ?: Exercises.get(slot.options[phase % slot.options.size])

    // ------------------------------------------------------------ prescription

    fun prescription(category: Category, phase: Int, meso: Int, week: Int, level: Level, hasOneRm: Boolean): Prescription {
        val deload = week >= WEEKS_PER_MESO
        val w = week.coerceIn(1, 5)
        var rx = when (phase) {
            0 -> when (category) {
                Category.BASE -> {
                    // volume first at constant load, then load up / reps down
                    val table = listOf(3 to 6, 4 to 6, 5 to 6, 5 to 5, 5 to 4)
                    val pcts = listOf(0.70, 0.70, 0.70, 0.75, 0.80)
                    val (sets, reps) = table[w - 1]
                    Prescription(sets, reps, reps, 2, 180, "2-0-1-0",
                        pct = if (hasOneRm) pcts[w - 1] + 0.025 * meso else null,
                        note = if (w <= 3) "Volume in crescita a carico costante" else "Carico in salita, ripetizioni in calo")
                }
                Category.COMPLEMENTARE -> Prescription(if (w <= 2) 3 else 4, 8, 10, if (w <= 2) 2 else 1, 120,
                    note = "Doppia progressione: quando chiudi tutte le serie a 10, aumenta il carico")
                Category.ISOLAMENTO -> Prescription(if (w <= 2) 2 else 3, 12, 15, if (w <= 2) 1 else 0, 75)
            }
            1 -> when (category) {
                Category.BASE -> Prescription(listOf(3, 4, 4, 4, 5)[w - 1], 6, 8, if (w <= 2) 2 else 1, 165,
                    note = "Buffer sulle prime serie, l'ultima vicina al cedimento tecnico")
                Category.COMPLEMENTARE -> Prescription(if (w <= 2) 3 else 4, 8, 12, if (w <= 2) 2 else 1, 105,
                    note = "Ultima serie a cedimento concentrico")
                Category.ISOLAMENTO -> Prescription(3, 12, 15, 0, 60,
                    technique = if (w >= 4) Technique.REST_PAUSE else Technique.NESSUNA,
                    note = "Tutte le serie a cedimento")
            }
            else -> when (category) {
                Category.BASE -> Prescription(3, 8, 10, 1, listOf(150, 135, 120, 105, 90)[w - 1],
                    technique = if (w >= 3) Technique.BACK_OFF else Technique.NESSUNA,
                    note = "Progressione di densità: stesso lavoro, recuperi più brevi")
                Category.COMPLEMENTARE -> Prescription(3, 10, 12, if (w <= 2) 1 else 0, listOf(90, 80, 75, 70, 60)[w - 1],
                    tempo = "3-0-1-1", note = "Cerca il pompaggio, contrazione di picco")
                Category.ISOLAMENTO -> Prescription(3, 15, 20, 0, listOf(60, 55, 50, 45, 45)[w - 1],
                    tempo = "2-0-1-1",
                    technique = when (w) { 3, 4 -> Technique.STRIPPING; 5 -> Technique.ISOMETRIA; else -> Technique.NESSUNA },
                    note = "Cedimento su tutte le serie")
            }
        }
        // level adjustments
        rx = when (level) {
            Level.PRINCIPIANTE -> rx.copy(
                sets = if (category == Category.BASE) rx.sets else max(2, rx.sets - 1),
                rir = max(1, rx.rir),
                technique = Technique.NESSUNA
            )
            Level.AVANZATO -> if (category == Category.ISOLAMENTO && phase > 0) rx.copy(sets = rx.sets + 1) else rx
            else -> rx
        }
        if (deload) {
            rx = rx.copy(
                sets = max(2, rx.sets / 2),
                rir = 3,
                pct = rx.pct?.let { it - 0.12 },
                technique = Technique.NESSUNA,
                note = "Settimana di scarico: carichi -15%, metà delle serie, lontano dal cedimento"
            )
        }
        return rx
    }

    // ------------------------------------------------------------- planning

    fun plan(data: AppData, dayIndex: Int = data.program.dayIndex, week: Int = data.program.week): PlannedDay {
        val p = data.profile
        val st = data.program
        val templates = templates(p.split)
        val t = templates[dayIndex.coerceIn(0, templates.size - 1)]
        val deload = week >= WEEKS_PER_MESO
        val items = t.slots.map { slot ->
            val ex = exerciseFor(slot, st.phase, data.swaps)
            // only real 1RM lifts in their main slot use percentages
            val oneRm = if (ex.tracksOneRm && slot.category == Category.BASE) p.oneRm[ex.id] else null
            val rx = prescription(slot.category, st.phase, st.meso, week, p.level, oneRm != null)
            val (kg, why) = suggest(data, ex, rx, oneRm, deload)
            PlannedExercise(slot, ex, rx, kg, why)
        }
        return PlannedDay(t, items, deload)
    }

    private fun suggest(data: AppData, ex: Exercise, rx: Prescription, oneRm: Double?, deload: Boolean): Pair<Double?, String> {
        val step = ex.equipment.step
        if (oneRm != null && rx.pct != null) {
            return Calc.round(oneRm * rx.pct, step) to "${(rx.pct * 100).toInt()}% del massimale (${Calc.fmt(oneRm)} kg)"
        }
        val last = data.history.asSequence()
            .flatMap { s -> s.exercises.asSequence().map { s to it } }
            .filter { it.second.exerciseId == ex.id && it.second.sets.any { st -> st.done && st.type == SetType.WORK } }
            .maxByOrNull { it.first.startedAt }
        if (last == null) {
            if (ex.tracksOneRm) data.profile.oneRm[ex.id]?.let { orm ->
                val pct = Calc.pctFor((rx.repMin + rx.repMax) / 2, rx.rir)
                return Calc.round(orm * pct * if (deload) 0.85 else 1.0, step) to "Stimato dal massimale"
            }
            return null to "Scegli un carico che ti lasci ${rx.rir} ripetizioni di margine"
        }
        val prev = last.second
        val work = prev.sets.filter { it.done && it.type == SetType.WORK }
        val top = work.maxOf { it.weight }
        val factor = if (deload) 0.85 else 1.0
        if (prev.repMin == rx.repMin && prev.repMax == rx.repMax) {
            val allTop = work.all { it.reps >= rx.repMax }
            return if (allTop && !deload) Calc.round(top + step, step) to "Hai chiuso tutte le serie a ${rx.repMax}: +${Calc.fmt(step)} kg"
            else Calc.round(top * factor, step) to if (deload) "Scarico: -15% rispetto all'ultima volta" else "Stesso carico, punta a più ripetizioni"
        }
        val best = work.maxOf { Calc.e1rm(it.weight, it.reps, it.rir) }
        val pct = Calc.pctFor(rx.repMax, rx.rir)
        return Calc.round(best * pct * factor, step) to "Ricalcolato sul nuovo range di ripetizioni"
    }

    /** Weekly (per microcycle) hard sets per muscle. Secondary muscles count half. */
    fun volumePerMuscle(data: AppData): Map<Muscle, Double> {
        val templates = templates(data.profile.split)
        val out = mutableMapOf<Muscle, Double>()
        templates.indices.forEach { i ->
            plan(data, i).items.forEach { pe ->
                out[pe.exercise.muscle] = (out[pe.exercise.muscle] ?: 0.0) + pe.rx.sets
                pe.exercise.secondary.forEach { m -> out[m] = (out[m] ?: 0.0) + pe.rx.sets * 0.5 }
            }
        }
        // PPL is 6 sessions spread over ~8.5 days: normalise to 7 days
        if (data.profile.split == SplitType.PPL) out.replaceAll { _, v -> v * 7.0 / 8.5 }
        return out
    }

    fun advance(st: ProgramState, split: SplitType): ProgramState {
        val n = templates(split).size
        var d = st.dayIndex + 1
        var w = st.week
        var m = st.meso
        var ph = st.phase
        if (d >= n) { d = 0; w++ }
        if (w > WEEKS_PER_MESO) { w = 1; m++ }
        if (m >= MESO_PER_PHASE) { m = 0; ph = (ph + 1) % phases.size }
        return st.copy(phase = ph, meso = m, week = w, dayIndex = d)
    }

    /** Microcycle number from the start of the program (1-based). */
    fun absoluteWeek(st: ProgramState): Int = (st.phase * MESO_PER_PHASE + st.meso) * WEEKS_PER_MESO + st.week

    val totalWeeks = phases.size * MESO_PER_PHASE * WEEKS_PER_MESO
}
