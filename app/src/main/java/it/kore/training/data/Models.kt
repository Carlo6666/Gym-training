package it.kore.training.data

import kotlinx.serialization.Serializable

@Serializable
enum class Sex(val label: String) { M("Uomo"), F("Donna") }

@Serializable
enum class Level(val label: String) {
    PRINCIPIANTE("Principiante"), INTERMEDIO("Intermedio"), AVANZATO("Avanzato")
}

@Serializable
enum class SplitType(val label: String, val description: String) {
    SPLIT_A(
        "Split 4 giorni · A",
        "Ogni giorno ha un distretto principale (petto, dorso, spalle, gambe) e due secondari. Multifrequenza: ogni muscolo lavora 2 volte."
    ),
    SPLIT_B(
        "Split 4 giorni · B",
        "Accoppiamenti antagonisti: petto+dorso, spalle+gambe. Ogni distretto principale torna due volte nella settimana."
    ),
    PPL(
        "Push · Legs · Pull ×2",
        "6 sedute a rotazione (spinta, gambe, tirata) distribuite su 8-9 giorni. Ideale per chi si allena 4-5 volte."
    )
}

@Serializable
enum class Responder(val label: String, val surplusMin: Int, val surplusMax: Int) {
    NORMALE("Normale", 200, 300),
    HARD_GAINER("Hard gainer (fatico a crescere)", 400, 600),
    INGRASSO_FACILE("Tendo ad ingrassare", 150, 200)
}

enum class Muscle(val label: String) {
    PETTO("Petto"), DORSO("Dorso"), SPALLE("Spalle"), BICIPITI("Bicipiti"), TRICIPITI("Tricipiti"),
    QUADRICIPITI("Quadricipiti"), FEMORALI("Femorali e glutei"), POLPACCI("Polpacci"), ADDOME("Addome")
}

enum class Category(val label: String, val short: String) {
    BASE("Fondamentale", "BASE"),
    COMPLEMENTARE("Complementare", "COMP"),
    ISOLAMENTO("Isolamento", "ISO")
}

enum class Equipment(val label: String, val step: Double) {
    BILANCIERE("Bilanciere", 2.5),
    MANUBRI("Manubri", 2.0),
    MACCHINA("Macchina", 2.5),
    CAVI("Cavi", 2.5),
    CORPO_LIBERO("Corpo libero", 1.25)
}

/** Intensity technique used on the last set(s) of an exercise. */
@Serializable
enum class Technique(val label: String, val howTo: String) {
    NESSUNA("", ""),
    REST_PAUSE(
        "Rest-pause",
        "Arriva a cedimento, riposa 15-20 secondi e strappa altre ripetizioni con lo stesso carico. Ripeti per 2 mini-serie."
    ),
    STRIPPING(
        "Stripping",
        "Arriva a cedimento, scala subito il carico del 20-25% e continua fino a nuovo cedimento. Ripeti una seconda volta."
    ),
    BACK_OFF(
        "Back-off",
        "Dopo le serie pesanti, togli circa il 15-20% del carico ed esegui una serie extra a buffer 1-0 con più ripetizioni."
    ),
    PARZIALI(
        "Ripetizioni parziali",
        "Al cedimento completo aggiungi 3-5 ripetizioni parziali nella porzione di movimento che riesci ancora a completare."
    ),
    ISOMETRIA(
        "Tenuta isometrica",
        "All'ultima ripetizione blocca il carico nel punto di massima contrazione per 10-15 secondi."
    )
}

@Serializable
enum class SetType(val label: String) { WARMUP("Risc."), WORK("Serie"), EXTRA("Extra") }

@Serializable
data class Profile(
    val name: String = "",
    val sex: Sex = Sex.M,
    val age: Int = 30,
    val heightCm: Int = 175,
    val weightKg: Double = 75.0,
    val bodyFatPct: Double = 15.0,
    val level: Level = Level.INTERMEDIO,
    val split: SplitType = SplitType.SPLIT_A,
    val daysPerWeek: Int = 4,
    val responder: Responder = Responder.NORMALE,
    val activityFactor: Double = 1.55,
    /** exerciseId -> 1RM in kg (massimali). */
    val oneRm: Map<String, Double> = emptyMap(),
    val barKg: Double = 20.0,
    val plates: List<Double> = listOf(25.0, 20.0, 15.0, 10.0, 5.0, 2.5, 1.25),
    val onboarded: Boolean = false
)

/** Where the athlete is in the periodisation. */
@Serializable
data class ProgramState(
    val phase: Int = 0,       // 0..2
    val meso: Int = 0,        // 0..2 mesocycles per phase
    val week: Int = 1,        // 1..6 (6 = scarico)
    val dayIndex: Int = 0,    // session inside the microcycle
    val startedAt: Long = System.currentTimeMillis()
)

@Serializable
data class SetLog(
    val type: SetType = SetType.WORK,
    val weight: Double = 0.0,
    val reps: Int = 0,
    val rir: Int? = null,
    val done: Boolean = false
)

@Serializable
data class SessionExercise(
    val slotKey: String,
    val exerciseId: String,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val restSec: Int,
    val tempo: String,
    val technique: Technique = Technique.NESSUNA,
    val note: String = "",
    val sets: List<SetLog>
)

@Serializable
data class Session(
    val id: Long,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val phase: Int,
    val meso: Int,
    val week: Int,
    val dayIndex: Int,
    val exercises: List<SessionExercise>,
    val notes: String = "",
    val feeling: Int = 3
)

@Serializable
data class BodyWeightEntry(val epochDay: Long, val kg: Double)

@Serializable
data class AppData(
    val profile: Profile = Profile(),
    val program: ProgramState = ProgramState(),
    /** slotKey -> exerciseId chosen by the user instead of the default. */
    val swaps: Map<String, String> = emptyMap(),
    val active: Session? = null,
    val history: List<Session> = emptyList(),
    val bodyWeight: List<BodyWeightEntry> = emptyList(),
    val version: Int = 1
)
