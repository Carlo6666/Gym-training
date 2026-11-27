package it.kore.training.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.data.*
import it.kore.training.ui.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val shortDate = DateTimeFormatter.ofPattern("d/M", Locale.ITALIAN)

@Composable
fun ProgressScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        Text("Progressi", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(16.dp))
        Segmented(listOf("Panoramica", "Esercizi", "Storico"), tab, { tab = it }, Modifier.padding(horizontal = 16.dp))
        when (tab) {
            0 -> Overview(data, nav)
            1 -> ExerciseProgress(data)
            else -> HistoryList(data, nav)
        }
    }
}

@Composable
private fun Overview(data: AppData, nav: NavHostController) {
    val weeks = remember(data.history) { Stats.weekly(data.history).takeLast(8) }
    val perMuscle = remember(data.history) { Stats.setsPerMuscleThisWeek(data.history) }
    val records = remember(data.history) { Stats.records(data.history) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        if (data.history.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            KCard {
                Text("Ancora nessuna seduta registrata", fontWeight = FontWeight.Bold)
                Text("Completa il tuo primo allenamento per vedere grafici, record e volume.", color = K.Muted)
            }
        }
        SectionTitle("Tonnellaggio settimanale (kg)")
        KCard {
            if (weeks.isEmpty()) Text("Nessun dato", color = K.Muted)
            else {
                BarChart(weeks.map { it.tonnage }, weeks.map { it.start.format(shortDate) })
                Spacer(Modifier.height(8.dp))
                weeks.lastOrNull()?.let {
                    KeyValue("Questa settimana", "${Calc.fmt(Math.round(it.tonnage).toDouble())} kg · ${it.sets} serie · ${it.sessions} sedute")
                }
            }
        }
        SectionTitle("Serie per muscolo · settimana in corso")
        KCard {
            if (perMuscle.isEmpty()) Text("Nessuna serie questa settimana", color = K.Muted)
            Muscle.entries.filter { (perMuscle[it] ?: 0.0) > 0 }.forEach { m ->
                val l = Stats.landmarks(m)
                VolumeBar(m.label, perMuscle[m] ?: 0.0, l.mev, l.mavHigh, Stats.zone(m, perMuscle[m] ?: 0.0))
            }
        }
        SectionTitle("Record personali")
        KCard {
            if (records.isEmpty()) Text("Nessun record ancora", color = K.Muted)
            records.take(12).forEach { r ->
                Row(Modifier.fillMaxWidth().clickable { nav.navigate("exercise/${r.exercise.id}") }.padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.EmojiEvents, null, tint = K.Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(r.exercise.name, fontWeight = FontWeight.SemiBold)
                        Text("Top: ${Calc.fmt(r.heaviest)} kg × ${r.heaviestReps}", color = K.Muted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${Calc.fmt(Calc.round(r.bestE1rm, 0.5))} kg", fontWeight = FontWeight.ExtraBold, color = K.Accent)
                        Text("1RM stimato", color = K.Muted, fontSize = 10.sp)
                    }
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun ExerciseProgress(data: AppData) {
    val done = remember(data.history) {
        data.history.flatMap { s -> s.exercises.map { it.exerciseId } }.distinct().mapNotNull { Exercises.find(it) }
    }
    var selected by remember { mutableStateOf(done.firstOrNull()?.id) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        if (done.isEmpty()) {
            Spacer(Modifier.height(24.dp))
            Text("Registra qualche seduta per vedere l'andamento dei tuoi esercizi.", color = K.Muted)
            return@Column
        }
        SectionTitle("Esercizio")
        var open by remember { mutableStateOf(false) }
        val sel = selected?.let { Exercises.find(it) } ?: done.first()
        Box {
            OutlinedButton(onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
                Text(sel.name, color = K.Text, modifier = Modifier.weight(1f)); Icon(Icons.Filled.ArrowDropDown, null)
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                done.forEach { ex -> DropdownMenuItem(text = { Text(ex.name) }, onClick = { selected = ex.id; open = false }) }
            }
        }
        ExerciseCharts(data, sel.id)
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
fun ExerciseCharts(data: AppData, exerciseId: String) {
    val e1 = remember(data.history, exerciseId) { Stats.e1rmSeries(data.history, exerciseId) }
    val vol = remember(data.history, exerciseId) { Stats.volumeSeries(data.history, exerciseId) }
    SectionTitle("1RM stimato (kg)")
    KCard {
        LineChart(e1.map { it.value })
        if (e1.isNotEmpty()) {
            val first = e1.first().value
            val last = e1.last().value
            val diff = last - first
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${Calc.fmt(Calc.round(last, 0.5))}", "Attuale", accent = true)
                StatTile("${Calc.fmt(Calc.round(e1.maxOf { it.value }, 0.5))}", "Migliore")
                StatTile((if (diff >= 0) "+" else "") + Calc.fmt(Calc.round(diff, 0.5)), "Dall'inizio")
            }
            val trend = Progression.improvement(data.history, exerciseId, null, Long.MAX_VALUE)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(Progression.pct(if (first > 0) diff / first * 100 else null), "Totale %")
                StatTile(Progression.pct(trend.expectedPct), "Atteso a seduta", accent = true)
                StatTile(trend.expectedE1rm?.let { Calc.fmt(Calc.round(it, 0.5)) } ?: "—", "Prossimo 1RM")
            }
        }
    }
    SectionTitle("Volume per seduta (kg × rip)")
    KCard { LineChart(vol.map { it.value }, color = K.Blue) }
}

@Composable
private fun HistoryList(data: AppData, nav: NavHostController) {
    val list = remember(data.history) { data.history.sortedByDescending { it.startedAt } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        if (list.isEmpty()) Text("Nessuna seduta salvata.", color = K.Muted)
        list.forEach { s ->
            KCard(Modifier.padding(bottom = 8.dp), onClick = { nav.navigate("session/${s.id}") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(s.title, fontWeight = FontWeight.Bold)
                        Text(Stats.day(s.startedAt).format(itDate) + " · Fase ${s.phase + 1} · Sett. ${s.week}", color = K.Muted, fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${Stats.setCount(s)} serie", fontWeight = FontWeight.SemiBold)
                        Text("${Calc.fmt(Math.round(Stats.volume(s)).toDouble())} kg", color = K.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun SessionDetailScreen(vm: AppViewModel, nav: NavHostController, id: Long) {
    val data by vm.data.collectAsState()
    val s = data.history.firstOrNull { it.id == id }
    var confirm by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        KTopBar(s?.title ?: "Seduta", onBack = { nav.popBackStack() }) {
            if (s != null) IconButton(onClick = { confirm = true }) { Icon(Icons.Filled.Delete, "Elimina", tint = K.Muted) }
        }
        if (s == null) { Text("Seduta non trovata", Modifier.padding(16.dp)); return@Column }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(Stats.day(s.startedAt).format(itDate), color = K.Muted)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${Stats.setCount(s)}", "Serie", accent = true)
                StatTile(Calc.fmt(Math.round(Stats.volume(s)).toDouble()), "kg totali")
                StatTile(s.endedAt?.let { "${(it - s.startedAt) / 60000}'" } ?: "-", "Durata")
            }
            if (s.notes.isNotBlank()) { SectionTitle("Note"); Text(s.notes) }
            s.exercises.forEach { e ->
                val ex = Exercises.get(e.exerciseId)
                SectionTitle(ex.name)
                KCard {
                    e.sets.filter { it.done }.forEachIndexed { i, set ->
                        Row(Modifier.padding(vertical = 2.dp)) {
                            Text(if (set.type == SetType.WARMUP) "Risc." else "Serie ${i + 1}", color = K.Muted, modifier = Modifier.weight(1f))
                            Text("${Calc.fmt(set.weight)} kg × ${set.reps}" + (set.rir?.let { "  @RIR $it" } ?: ""), fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (e.sets.none { it.done }) Text("Nessuna serie completata", color = K.Muted)
                    val best = Stats.bestE1rm(e)
                    if (best > 0) {
                        Text("1RM stimato: ${Calc.fmt(Calc.round(best, 0.5))} kg", color = K.Accent, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                        ImprovementBox(Progression.improvement(data.history, e.exerciseId, e, s.startedAt))
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = K.Surface,
            title = { Text("Eliminare la seduta?") },
            confirmButton = { TextButton(onClick = { confirm = false; vm.deleteSession(id); nav.popBackStack() }) { Text("Elimina", color = K.Bad) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Annulla") } }
        )
    }
}
