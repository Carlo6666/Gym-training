package it.kore.training.data

import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.roundToInt

object Calc {

    /** Epley estimate of the 1RM. */
    fun epley(weight: Double, reps: Int): Double = if (reps <= 1) weight else weight * (1 + reps / 30.0)

    /** Brzycki estimate of the 1RM (valid up to ~12 reps). */
    fun brzycki(weight: Double, reps: Int): Double =
        if (reps <= 1) weight else weight * 36.0 / (37.0 - reps.coerceAtMost(36))

    /**
     * 1RM estimate that also counts the reps left in reserve:
     * 8 reps at RIR 2 are treated like a 10RM.
     */
    fun e1rm(weight: Double, reps: Int, rir: Int? = null): Double {
        if (weight <= 0 || reps <= 0) return 0.0
        val r = reps + (rir ?: 0)
        return if (r <= 10) (epley(weight, r) + brzycki(weight, r)) / 2 else epley(weight, r)
    }

    /** Fraction of 1RM that allows [reps] reps leaving [rir] in reserve. */
    fun pctFor(reps: Int, rir: Int = 0): Double {
        val r = (reps + rir).coerceAtLeast(1)
        return if (r == 1) 1.0 else 1.0 / (1 + r / 30.0)
    }

    /** Max reps doable at [pct] of 1RM (inverse of Epley). */
    fun repsAt(pct: Double): Int = if (pct >= 1.0) 1 else ((1 / pct - 1) * 30).roundToInt().coerceAtLeast(1)

    fun round(weight: Double, step: Double): Double {
        if (step <= 0) return weight
        return (weight / step).roundToInt() * step
    }

    fun fmt(kg: Double): String =
        if (kg % 1.0 == 0.0) kg.toInt().toString() else String.format("%.2f", kg).trimEnd('0').trimEnd(',', '.')

    /** Plates to load on EACH side to reach [target]. Returns plates and the leftover not reachable. */
    fun plates(target: Double, bar: Double, available: List<Double>): Pair<List<Double>, Double> {
        var perSide = (target - bar) / 2.0
        if (perSide <= 0) return emptyList<Double>() to 0.0
        val out = mutableListOf<Double>()
        for (p in available.sortedDescending()) {
            while (perSide + 1e-6 >= p) {
                out += p; perSide -= p
            }
        }
        return out to perSide * 2
    }

    /** Warm-up ramp toward the first working set. */
    fun warmup(work: Double, bar: Double, step: Double): List<Pair<Double, Int>> {
        if (work <= 0) return emptyList()
        val ramp = listOf(0.4 to 8, 0.6 to 5, 0.75 to 3, 0.87 to 1)
        val sets = mutableListOf<Pair<Double, Int>>()
        if (step == 2.5 && bar > 0 && work > bar + 15) sets += bar to 10
        for ((pct, reps) in ramp) {
            val w = round(work * pct, step)
            if (w <= 0) continue
            if (sets.isNotEmpty() && w <= sets.last().first) continue
            if (w >= work) continue
            sets += w to reps
        }
        // light lifts need fewer ramps
        return if (work < 40) sets.takeLast(2) else sets
    }

    // ------------------- Nutrition -------------------

    data class Nutrition(
        val bmr: Int, val tdee: Int, val target: Int,
        val proteinG: Int, val fatG: Int, val carbG: Int,
        val trainingDay: Macro, val restDay: Macro, val note: String
    )

    data class Macro(val kcal: Int, val p: Int, val f: Int, val c: Int)

    fun nutrition(p: Profile, trainingDays: Int, surplusOverride: Int? = null): Nutrition {
        val bmr = 10 * p.weightKg + 6.25 * p.heightCm - 5 * p.age + if (p.sex == Sex.M) 5 else -161
        val tdee = bmr * p.activityFactor
        val surplus = surplusOverride ?: ((p.responder.surplusMin + p.responder.surplusMax) / 2)
        val target = tdee + surplus
        val protPerKg = when (p.responder) { Responder.HARD_GAINER -> 2.0; else -> 1.8 }
        val fatPerKg = when (p.responder) { Responder.HARD_GAINER -> 1.2; Responder.INGRASSO_FACILE -> 0.9; else -> 1.0 }
        val prot = protPerKg * p.weightKg
        val fat = fatPerKg * p.weightKg
        val carbs = ((target - prot * 4 - fat * 9) / 4).coerceAtLeast(0.0)

        // Calorie cycling: +10% on training days, the rest of the week balances it out.
        val week = target * 7
        val tKcal = target * 1.10
        val restDays = (7 - trainingDays).coerceAtLeast(1)
        val rKcal = (week - tKcal * trainingDays) / restDays
        fun macro(kcal: Double) = Macro(
            kcal.roundToInt(), prot.roundToInt(), fat.roundToInt(),
            ((kcal - prot * 4 - fat * 9) / 4).coerceAtLeast(0.0).roundToInt()
        )
        val note = when {
            p.bodyFatPct > 15 && p.sex == Sex.M -> "Con una massa grassa sopra il 15% conviene una fase di definizione prima di iniziare il surplus."
            p.bodyFatPct > 25 && p.sex == Sex.F -> "Con una massa grassa sopra il 25% conviene una fase di definizione prima di iniziare il surplus."
            else -> "Puoi partire con la fase di massa. Pesati ogni mattina e confronta le medie settimanali."
        }
        return Nutrition(
            bmr.roundToInt(), tdee.roundToInt(), target.roundToInt(),
            prot.roundToInt(), fat.roundToInt(), carbs.roundToInt(),
            macro(tKcal), macro(rKcal), note
        )
    }

    // ------------------- Body weight -------------------

    data class WeekAvg(val weekStart: LocalDate, val avg: Double, val count: Int)

    fun weeklyAverages(entries: List<BodyWeightEntry>): List<WeekAvg> =
        entries.groupBy { LocalDate.ofEpochDay(it.epochDay).let { d -> d.minusDays((d.dayOfWeek.value - 1).toLong()) } }
            .map { (start, list) -> WeekAvg(start, list.map { it.kg }.average(), list.size) }
            .sortedBy { it.weekStart }

    /** Advice based on the % change between the last two weekly averages (target ~0.5%/week in bulk). */
    fun weightAdvice(weeks: List<WeekAvg>): Pair<Double?, String> {
        if (weeks.size < 2) return null to "Servono almeno due settimane di pesate per valutare l'andamento."
        val a = weeks[weeks.size - 2].avg
        val b = weeks.last().avg
        val pct = (b - a) / a * 100
        val msg = when {
            pct > 1.0 -> "Stai crescendo troppo in fretta (oltre 1% a settimana): riduci leggermente le calorie (100-150 kcal)."
            pct >= 0.25 -> "Andamento ideale: mantieni le calorie attuali."
            pct > -0.1 -> "Peso fermo: aumenta le calorie di 100-200 kcal al giorno."
            else -> "Peso in calo: se sei in fase di massa aumenta le calorie di circa 200 kcal."
        }
        return pct to msg
    }

    fun floorStep(v: Double, step: Double) = floor(v / step) * step
}
