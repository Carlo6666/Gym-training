package it.kore.training.data

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Stats {

    fun day(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).toLocalDate()

    fun weekStart(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    fun workSets(e: SessionExercise) = e.sets.filter { it.done && it.type != SetType.WARMUP }

    fun volume(s: Session): Double = s.exercises.sumOf { e -> workSets(e).sumOf { it.weight * it.reps } }

    fun setCount(s: Session): Int = s.exercises.sumOf { workSets(it).size }

    fun bestE1rm(e: SessionExercise): Double = workSets(e).maxOfOrNull { Calc.e1rm(it.weight, it.reps, it.rir) } ?: 0.0

    data class Point(val date: LocalDate, val value: Double)

    /** Best estimated 1RM per session for one exercise. */
    fun e1rmSeries(history: List<Session>, exerciseId: String): List<Point> =
        history.sortedBy { it.startedAt }.mapNotNull { s ->
            val best = s.exercises.filter { it.exerciseId == exerciseId }.maxOfOrNull { bestE1rm(it) } ?: 0.0
            if (best > 0) Point(day(s.startedAt), best) else null
        }

    fun volumeSeries(history: List<Session>, exerciseId: String): List<Point> =
        history.sortedBy { it.startedAt }.mapNotNull { s ->
            val v = s.exercises.filter { it.exerciseId == exerciseId }.sumOf { e -> workSets(e).sumOf { it.weight * it.reps } }
            if (v > 0) Point(day(s.startedAt), v) else null
        }

    data class WeekVolume(val start: LocalDate, val tonnage: Double, val sets: Int, val sessions: Int)

    fun weekly(history: List<Session>): List<WeekVolume> =
        history.groupBy { weekStart(day(it.startedAt)) }
            .map { (w, list) -> WeekVolume(w, list.sumOf { volume(it) }, list.sumOf { setCount(it) }, list.size) }
            .sortedBy { it.start }

    /** Hard sets per muscle logged in the current calendar week (secondary muscles count half). */
    fun setsPerMuscleThisWeek(history: List<Session>): Map<Muscle, Double> {
        val start = weekStart(LocalDate.now())
        val out = mutableMapOf<Muscle, Double>()
        history.filter { !day(it.startedAt).isBefore(start) }.forEach { s ->
            s.exercises.forEach { e ->
                val ex = Exercises.find(e.exerciseId) ?: return@forEach
                val n = workSets(e).size.toDouble()
                out[ex.muscle] = (out[ex.muscle] ?: 0.0) + n
                ex.secondary.forEach { m -> out[m] = (out[m] ?: 0.0) + n * 0.5 }
            }
        }
        return out
    }

    data class Record(val exercise: Exercise, val bestE1rm: Double, val heaviest: Double, val heaviestReps: Int, val date: LocalDate)

    fun records(history: List<Session>): List<Record> {
        val map = mutableMapOf<String, Record>()
        history.forEach { s ->
            s.exercises.forEach { e ->
                val ex = Exercises.find(e.exerciseId) ?: return@forEach
                workSets(e).forEach { set ->
                    val est = Calc.e1rm(set.weight, set.reps, set.rir)
                    val cur = map[ex.id]
                    if (cur == null || est > cur.bestE1rm) {
                        map[ex.id] = Record(ex, est, maxOf(set.weight, cur?.heaviest ?: 0.0),
                            if (set.weight >= (cur?.heaviest ?: 0.0)) set.reps else cur?.heaviestReps ?: 0, day(s.startedAt))
                    } else if (set.weight > cur!!.heaviest) {
                        map[ex.id] = cur.copy(heaviest = set.weight, heaviestReps = set.reps)
                    }
                }
            }
        }
        return map.values.sortedByDescending { it.bestE1rm }
    }

    fun streakWeeks(history: List<Session>): Int {
        val weeks = history.map { weekStart(day(it.startedAt)) }.toSet()
        var w = weekStart(LocalDate.now())
        if (w !in weeks) w = w.minusWeeks(1)
        var n = 0
        while (w in weeks) { n++; w = w.minusWeeks(1) }
        return n
    }

    /** Volume landmarks (hard sets per week per muscle). */
    data class Landmarks(val mv: Int = 8, val mev: Int = 10, val mavLow: Int = 15, val mavHigh: Int = 25)

    fun landmarks(m: Muscle): Landmarks = when (m) {
        Muscle.BICIPITI, Muscle.TRICIPITI, Muscle.POLPACCI, Muscle.ADDOME -> Landmarks(4, 6, 8, 18)
        else -> Landmarks()
    }

    fun zone(m: Muscle, sets: Double): String {
        val l = landmarks(m)
        return when {
            sets < l.mv -> "Sotto mantenimento"
            sets < l.mev -> "Mantenimento"
            sets < l.mavLow -> "Minimo efficace"
            sets <= l.mavHigh -> "Zona ottimale"
            else -> "Oltre il recuperabile"
        }
    }
}

/** Session-to-session improvement of one exercise, based on the history. */
data class Improvement(
    val curE1rm: Double,
    val prevE1rm: Double?,
    val e1rmPct: Double?,
    val curVolume: Double,
    val prevVolume: Double?,
    val volumePct: Double?,
    /** expected % gain per session from the trend of previous sessions. */
    val expectedPct: Double?,
    /** expected 1RM for the session being evaluated (trend projection). */
    val expectedE1rm: Double?,
    val samples: Int,
    /** reps needed at the current top weight to beat the previous best estimate. */
    val repsToBeat: Pair<Double, Int>?
)

object Progression {

    /**
     * @param before only sessions started before this instant are "previous".
     * @param current the performance being evaluated (live session or a saved one).
     */
    fun improvement(history: List<Session>, exerciseId: String, current: SessionExercise?, before: Long): Improvement {
        val prevSessions = history.filter { it.startedAt < before }.sortedBy { it.startedAt }
        val series = prevSessions.mapNotNull { s ->
            val ex = s.exercises.filter { it.exerciseId == exerciseId }
            val e1 = ex.maxOfOrNull { Stats.bestE1rm(it) } ?: 0.0
            val vol = ex.sumOf { e -> Stats.workSets(e).sumOf { it.weight * it.reps } }
            if (e1 > 0) e1 to vol else null
        }.takeLast(8)

        val curSets = current?.let { Stats.workSets(it) } ?: emptyList()
        val curE1 = curSets.maxOfOrNull { Calc.e1rm(it.weight, it.reps, it.rir) } ?: 0.0
        val curVol = curSets.sumOf { it.weight * it.reps }

        val prev = series.lastOrNull()
        val e1Pct = if (prev != null && curE1 > 0) (curE1 - prev.first) / prev.first * 100 else null
        val volPct = if (prev != null && prev.second > 0 && curVol > 0) (curVol - prev.second) / prev.second * 100 else null

        // linear trend on the previous estimated 1RMs
        var expPct: Double? = null
        var expE1: Double? = null
        if (series.size >= 2) {
            val ys = series.map { it.first }
            val n = ys.size
            val xMean = (n - 1) / 2.0
            val yMean = ys.average()
            var num = 0.0
            var den = 0.0
            ys.forEachIndexed { i, y -> num += (i - xMean) * (y - yMean); den += (i - xMean) * (i - xMean) }
            val slope = if (den > 0) num / den else 0.0
            val fitLast = yMean + slope * (n - 1 - xMean)
            expE1 = fitLast + slope
            expPct = if (fitLast > 0) slope / fitLast * 100 else null
        } else if (series.size == 1) {
            // a single previous session: default expectation of a small progression
            expPct = 1.0
            expE1 = series.first().first * 1.01
        }

        val repsToBeat = prev?.let { (best, _) ->
            val w = curSets.maxOfOrNull { it.weight }
                ?: current?.sets?.firstOrNull { it.type != SetType.WARMUP }?.weight ?: 0.0
            if (w <= 0) null else {
                val r = (1..30).firstOrNull { Calc.e1rm(w, it) > best + 0.01 }
                r?.let { w to it }
            }
        }
        return Improvement(curE1, prev?.first, e1Pct, curVol, prev?.second, volPct, expPct, expE1, series.size, repsToBeat)
    }

    fun pct(v: Double?): String = v?.let { (if (it >= 0) "+" else "") + String.format("%.1f", it) + "%" } ?: "—"
}
