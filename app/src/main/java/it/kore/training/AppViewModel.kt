package it.kore.training

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import it.kore.training.data.AppData
import it.kore.training.data.BodyWeightEntry
import it.kore.training.data.Calc
import it.kore.training.data.Category
import it.kore.training.data.Exercises
import it.kore.training.data.Profile
import it.kore.training.data.Program
import it.kore.training.data.ProgramState
import it.kore.training.data.Repository
import it.kore.training.data.Session
import it.kore.training.data.SessionExercise
import it.kore.training.data.SetLog
import it.kore.training.data.SetType
import it.kore.training.data.Stats
import it.kore.training.data.Technique
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

data class RestTimer(val endAt: Long, val total: Int, val label: String)

data class FinishSummary(
    val title: String,
    val minutes: Int,
    val sets: Int,
    val tonnage: Double,
    val records: List<String>
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    val repo = Repository(app)
    val data: StateFlow<AppData> = repo.data

    private val _timer = MutableStateFlow<RestTimer?>(null)
    val timer: StateFlow<RestTimer?> = _timer
    private var timerJob: Job? = null

    private val _summary = MutableStateFlow<FinishSummary?>(null)
    val summary: StateFlow<FinishSummary?> = _summary

    // ------------------------------------------------------------ profile

    fun saveProfile(p: Profile) = repo.update { it.copy(profile = p) }

    fun setOneRm(exerciseId: String, kg: Double?) = repo.update {
        val m = it.profile.oneRm.toMutableMap()
        if (kg == null || kg <= 0) m.remove(exerciseId) else m[exerciseId] = kg
        it.copy(profile = it.profile.copy(oneRm = m))
    }

    fun setProgram(st: ProgramState) = repo.update { it.copy(program = st) }

    fun resetSwap(slotKey: String) = repo.update { it.copy(swaps = it.swaps - slotKey) }

    fun setSwap(slotKey: String, exerciseId: String) = repo.update { it.copy(swaps = it.swaps + (slotKey to exerciseId)) }

    // ------------------------------------------------------------ session

    fun startSession(dayIndex: Int) = repo.update { d ->
        if (d.active != null) return@update d
        val plan = Program.plan(d, dayIndex)
        val exercises = plan.items.map { pe ->
            val sets = mutableListOf<SetLog>()
            val kg = pe.suggestedKg ?: 0.0
            if (pe.slot.category == Category.BASE && kg > 0 && !plan.deload) {
                Calc.warmup(kg, d.profile.barKg, pe.exercise.equipment.step).forEach { (w, r) ->
                    sets += SetLog(SetType.WARMUP, w, r)
                }
            }
            repeat(pe.rx.sets) { sets += SetLog(SetType.WORK, kg, pe.rx.repMax, pe.rx.rir) }
            if (pe.rx.technique == Technique.BACK_OFF && kg > 0) {
                sets += SetLog(SetType.EXTRA, Calc.round(kg * 0.8, pe.exercise.equipment.step), pe.rx.repMax + 4, 1)
            }
            SessionExercise(
                slotKey = pe.slot.key,
                exerciseId = pe.exercise.id,
                repMin = pe.rx.repMin,
                repMax = pe.rx.repMax,
                rir = pe.rx.rir,
                restSec = pe.rx.restSec,
                tempo = pe.rx.tempo,
                technique = pe.rx.technique,
                note = pe.rx.note,
                sets = sets
            )
        }
        val st = d.program
        d.copy(
            active = Session(
                id = System.currentTimeMillis(),
                title = plan.template.name + if (plan.deload) " · Scarico" else "",
                startedAt = System.currentTimeMillis(),
                phase = st.phase, meso = st.meso, week = st.week, dayIndex = dayIndex,
                exercises = exercises
            )
        )
    }

    private fun updateActive(f: (Session) -> Session) = repo.update { d -> d.active?.let { d.copy(active = f(it)) } ?: d }

    private fun updateExercise(ei: Int, f: (SessionExercise) -> SessionExercise) = updateActive { s ->
        s.copy(exercises = s.exercises.mapIndexed { i, e -> if (i == ei) f(e) else e })
    }

    fun updateSet(ei: Int, si: Int, set: SetLog) = updateExercise(ei) { e ->
        e.copy(sets = e.sets.mapIndexed { i, x -> if (i == si) set else x })
    }

    fun toggleDone(ei: Int, si: Int) {
        val s = data.value.active ?: return
        val e = s.exercises.getOrNull(ei) ?: return
        val set = e.sets.getOrNull(si) ?: return
        val nowDone = !set.done
        updateSet(ei, si, set.copy(done = nowDone))
        if (nowDone) {
            // propagate the weight to the next undone working sets if they are still empty or equal
            val isLast = e.sets.drop(si + 1).none { !it.done }
            val rest = if (set.type == SetType.WARMUP) 60 else e.restSec
            if (!isLast || ei < s.exercises.lastIndex) {
                startRest(rest, Exercises.get(e.exerciseId).name)
            }
        }
    }

    fun addSet(ei: Int) = updateExercise(ei) { e ->
        val last = e.sets.lastOrNull { it.type != SetType.WARMUP } ?: e.sets.lastOrNull()
        e.copy(sets = e.sets + SetLog(SetType.WORK, last?.weight ?: 0.0, last?.reps ?: e.repMax, e.rir))
    }

    fun removeSet(ei: Int, si: Int) = updateExercise(ei) { e ->
        e.copy(sets = e.sets.filterIndexed { i, _ -> i != si })
    }

    fun swapInSession(ei: Int, exerciseId: String, permanent: Boolean) {
        val slot = data.value.active?.exercises?.getOrNull(ei)?.slotKey
        updateExercise(ei) { e ->
            e.copy(exerciseId = exerciseId, sets = e.sets.filter { it.type != SetType.WARMUP }
                .map { it.copy(weight = 0.0, done = false) })
        }
        if (permanent && slot != null) setSwap(slot, exerciseId)
    }

    fun setSessionNotes(notes: String, feeling: Int) = updateActive { it.copy(notes = notes, feeling = feeling) }

    fun discardSession() {
        skipRest()
        repo.update { it.copy(active = null) }
    }

    fun finishSession() {
        skipRest()
        val d = data.value
        val s = d.active ?: return
        val finished = s.copy(endedAt = System.currentTimeMillis())
        val records = mutableListOf<String>()
        val newOneRm = d.profile.oneRm.toMutableMap()
        val previous = Stats.records(d.history).associateBy { it.exercise.id }
        finished.exercises.forEach { e ->
            val best = Stats.bestE1rm(e)
            if (best <= 0) return@forEach
            val ex = Exercises.get(e.exerciseId)
            val prev = previous[ex.id]?.bestE1rm ?: 0.0
            if (prev > 0 && best > prev + 0.01) records += "${ex.name}: nuovo record stimato ${Calc.fmt(Calc.round(best, 0.5))} kg"
            if (ex.tracksOneRm) {
                val cur = newOneRm[ex.id] ?: 0.0
                if (best > cur) {
                    newOneRm[ex.id] = Calc.round(best, 0.5)
                    if (cur > 0) records += "${ex.name}: massimale aggiornato a ${Calc.fmt(Calc.round(best, 0.5))} kg"
                }
            }
        }
        repo.update {
            it.copy(
                active = null,
                history = it.history + finished,
                program = Program.advance(it.program, it.profile.split).let { np ->
                    // only advance if the session done was the scheduled one
                    if (s.dayIndex == it.program.dayIndex) np else it.program
                },
                profile = it.profile.copy(oneRm = newOneRm)
            )
        }
        _summary.value = FinishSummary(
            s.title,
            ((finished.endedAt!! - s.startedAt) / 60000).toInt(),
            Stats.setCount(finished),
            Stats.volume(finished),
            records.distinct()
        )
    }

    fun clearSummary() { _summary.value = null }

    fun skipScheduled() = repo.update { it.copy(program = Program.advance(it.program, it.profile.split)) }

    fun deleteSession(id: Long) = repo.update { d -> d.copy(history = d.history.filter { it.id != id }) }

    // ------------------------------------------------------------ body weight

    fun addBodyWeight(kg: Double, day: LocalDate = LocalDate.now()) = repo.update { d ->
        val list = d.bodyWeight.filter { it.epochDay != day.toEpochDay() } + BodyWeightEntry(day.toEpochDay(), kg)
        d.copy(bodyWeight = list.sortedBy { it.epochDay }, profile = d.profile.copy(weightKg = kg))
    }

    fun deleteBodyWeight(epochDay: Long) = repo.update { d -> d.copy(bodyWeight = d.bodyWeight.filter { it.epochDay != epochDay }) }

    // ------------------------------------------------------------ rest timer

    fun startRest(seconds: Int, label: String = "Recupero") {
        _timer.value = RestTimer(System.currentTimeMillis() + seconds * 1000L, seconds, label)
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                val t = _timer.value ?: break
                if (System.currentTimeMillis() >= t.endAt) {
                    _timer.value = null
                    alertRestOver()
                    break
                }
                delay(250)
            }
        }
    }

    fun addRest(delta: Int) {
        val t = _timer.value ?: return
        val end = t.endAt + delta * 1000L
        if (end <= System.currentTimeMillis()) skipRest() else _timer.value = t.copy(endAt = end, total = (t.total + delta).coerceAtLeast(1))
    }

    fun skipRest() {
        timerJob?.cancel()
        _timer.value = null
    }

    private fun alertRestOver() {
        val ctx = getApplication<Application>()
        try {
            val vib: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            vib?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 300, 150, 300, 150, 500), -1))
        } catch (_: Exception) {
        }
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL) == null) {
                nm.createNotificationChannel(NotificationChannel(CHANNEL, "Timer di recupero", NotificationManager.IMPORTANCE_HIGH))
            }
            val granted = Build.VERSION.SDK_INT < 33 ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            if (granted) {
                val n = NotificationCompat.Builder(ctx, CHANNEL)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Recupero finito")
                    .setContentText("Prossima serie: si parte!")
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                NotificationManagerCompat.from(ctx).notify(1001, n)
            }
        } catch (_: SecurityException) {
        }
    }

    companion object {
        const val CHANNEL = "rest_timer"
    }
}
