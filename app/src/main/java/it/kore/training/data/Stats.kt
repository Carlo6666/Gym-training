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
