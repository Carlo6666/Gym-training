package it.kore.training.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.data.*
import it.kore.training.goTab
import it.kore.training.ui.*
import kotlinx.coroutines.delay

fun mmss(sec: Long): String = "%d:%02d".format(sec / 60, sec % 60)

@Composable
fun WorkoutScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val timer by vm.timer.collectAsState()
    val session = data.active
    if (session == null) {
        LaunchedEffect(Unit) { nav.goTab("home") }
        return
    }

    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }

    var menu by remember { mutableStateOf(false) }
    var confirmFinish by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var swapFor by remember { mutableStateOf<Int?>(null) }

    val totalWork = session.exercises.sumOf { e -> e.sets.count { it.type != SetType.WARMUP } }
    val doneWork = session.exercises.sumOf { e -> e.sets.count { it.type != SetType.WARMUP && it.done } }

    Column(Modifier.fillMaxSize()) {
        KTopBar(session.title, onBack = { nav.popBackStack() }) {
            Text(mmss((now - session.startedAt) / 1000), style = Mono, color = K.Accent, modifier = Modifier.padding(end = 4.dp))
            IconButton(onClick = { menu = true }) { Icon(Icons.Filled.MoreVert, "Menu") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                DropdownMenuItem(text = { Text("Timer libero 90\"") }, onClick = { menu = false; vm.startRest(90) })
                DropdownMenuItem(text = { Text("Annulla allenamento") }, onClick = { menu = false; confirmDiscard = true })
            }
        }
        LinearProgressIndicator(
            progress = { if (totalWork == 0) 0f else doneWork.toFloat() / totalWork },
            modifier = Modifier.fillMaxWidth().height(3.dp), color = K.Accent, trackColor = K.Surface2
        )

        LazyColumn(
            Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            itemsIndexed(session.exercises, key = { i, e -> "$i-${e.exerciseId}" }) { ei, e ->
                ExerciseCard(vm, nav, data, ei, e, onSwap = { swapFor = ei })
            }
            item {
                Spacer(Modifier.height(4.dp))
                GradientButton("Termina allenamento") { confirmFinish = true }
                Spacer(Modifier.height(if (timer != null) 120.dp else 24.dp))
            }
        }

        timer?.let { t -> RestBar(t, now, vm) }
    }

    if (confirmFinish) {
        var notes by remember { mutableStateOf(session.notes) }
        var feeling by remember { mutableIntStateOf(session.feeling) }
        AlertDialog(
            onDismissRequest = { confirmFinish = false },
            containerColor = K.Surface,
            title = { Text("Concludere la seduta?") },
            text = {
                Column {
                    Text("Serie completate: $doneWork di $totalWork", color = K.Muted)
                    Spacer(Modifier.height(12.dp))
                    Text("Come ti sei sentito?", fontWeight = FontWeight.SemiBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("😫", "😕", "😐", "🙂", "🔥").forEachIndexed { i, emo ->
                            Box(
                                Modifier.size(42.dp).clip(CircleShape)
                                    .background(if (feeling == i + 1) K.Accent.copy(alpha = 0.3f) else K.Surface2)
                                    .clickable { feeling = i + 1 },
                                contentAlignment = Alignment.Center
                            ) { Text(emo, fontSize = 20.sp) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(notes, { notes = it }, label = { Text("Note") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmFinish = false
                    vm.setSessionNotes(notes, feeling)
                    vm.finishSession()
                    nav.goTab("home")
                }) { Text("Salva", color = K.Accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmFinish = false }) { Text("Continua") } }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            containerColor = K.Surface,
            title = { Text("Annullare l'allenamento?") },
            text = { Text("La seduta non verrà salvata e il programma non avanzerà.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; vm.discardSession(); nav.goTab("home") }) {
                    Text("Annulla seduta", color = K.Bad)
                }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Indietro") } }
        )
    }

    swapFor?.let { ei ->
        val current = Exercises.get(session.exercises[ei].exerciseId)
        var keep by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { swapFor = null },
            containerColor = K.Surface,
            title = { Text("Sostituisci ${current.name}") },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(keep, { keep = it }, colors = CheckboxDefaults.colors(checkedColor = K.Accent))
                        Text("Usa anche nelle prossime sedute", fontSize = 13.sp)
                    }
                    Exercises.alternatives(current).forEach { alt ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                vm.swapInSession(ei, alt.id, keep); swapFor = null
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Pill(alt.category.short, K.Blue)
                            Spacer(Modifier.width(8.dp))
                            Text(alt.name)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { swapFor = null }) { Text("Chiudi") } }
        )
    }
}

@Composable
private fun RestBar(t: it.kore.training.RestTimer, now: Long, vm: AppViewModel) {
    val left = ((t.endAt - now) / 1000).coerceAtLeast(0)
    val progress = (t.endAt - now).toFloat() / (t.total * 1000f)
    Row(
        Modifier.fillMaxWidth().background(Color(0xFF121212)).border(1.dp, K.Line)
            .navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
            ProgressRing(progress, Modifier.fillMaxSize())
            Icon(Icons.Filled.Timer, null, tint = K.Accent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("RECUPERO", style = MaterialTheme.typography.labelSmall, color = K.Muted)
            Text(mmss(left), style = Mono.copy(fontSize = 30.sp), color = K.Text)
            Text(t.label, color = K.Muted, fontSize = 12.sp, maxLines = 1)
        }
        FilledTonalButton(onClick = { vm.addRest(-15) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("-15") }
        Spacer(Modifier.width(6.dp))
        FilledTonalButton(onClick = { vm.addRest(15) }, contentPadding = PaddingValues(horizontal = 10.dp)) { Text("+15") }
        Spacer(Modifier.width(6.dp))
        IconButton(onClick = { vm.skipRest() }) { Icon(Icons.Filled.SkipNext, "Salta", tint = K.Accent) }
    }
}

@Composable
private fun ExerciseCard(vm: AppViewModel, nav: NavHostController, data: AppData, ei: Int, e: SessionExercise, onSwap: () -> Unit) {
    val ex = Exercises.get(e.exerciseId)
    val slotCat = Program.templates(data.profile.split).flatMap { it.slots }.firstOrNull { it.key == e.slotKey }?.category ?: ex.category
    val last = remember(data.history, e.exerciseId) { lastPerformance(data.history, e.exerciseId) }
    val allDone = e.sets.isNotEmpty() && e.sets.all { it.done }

    KCard(highlight = !allDone && e.sets.any { it.done }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(slotCat.short, when (slotCat) { Category.BASE -> K.Accent; Category.COMPLEMENTARE -> K.Blue; else -> K.Good })
            Spacer(Modifier.width(8.dp))
            Text(ex.muscle.label, color = K.Muted, fontSize = 12.sp, modifier = Modifier.weight(1f))
            IconButton(onClick = { nav.navigate("exercise/${ex.id}") }, Modifier.size(32.dp)) { Icon(Icons.Filled.Info, "Dettagli", tint = K.Muted) }
            IconButton(onClick = onSwap, Modifier.size(32.dp)) { Icon(Icons.Filled.SwapHoriz, "Sostituisci", tint = K.Muted) }
        }
        Text(ex.name, style = MaterialTheme.typography.titleLarge, color = if (allDone) K.Muted else K.Text)
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill("${e.sets.count { it.type == SetType.WORK }}×${if (e.repMin == e.repMax) "${e.repMin}" else "${e.repMin}-${e.repMax}"}", K.Text)
            Pill("RIR ${e.rir}", K.Warn)
            Pill("Rec ${mmss(e.restSec.toLong())}", K.Blue)
            Pill("Tempo ${e.tempo}", K.Muted)
        }
        if (e.technique != Technique.NESSUNA) {
            Spacer(Modifier.height(8.dp))
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(K.Accent.copy(alpha = 0.1f)).padding(10.dp)) {
                Text("Tecnica: ${e.technique.label} (ultima serie)", color = K.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(e.technique.howTo, fontSize = 12.sp, color = K.Text.copy(alpha = 0.85f))
            }
        }
        if (e.note.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(e.note, color = K.Muted, fontSize = 12.sp)
        }
        last?.let {
            Spacer(Modifier.height(4.dp))
            Text("Ultima volta: $it", color = K.Accent2, fontSize = 12.sp)
        }
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            Text("SERIE", Modifier.width(52.dp), fontSize = 10.sp, color = K.Muted, fontWeight = FontWeight.Bold)
            Text("KG", Modifier.weight(1f), fontSize = 10.sp, color = K.Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("RIP", Modifier.weight(1f), fontSize = 10.sp, color = K.Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("RIR", Modifier.weight(0.7f), fontSize = 10.sp, color = K.Muted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(Modifier.width(48.dp))
        }
        var workN = 0
        e.sets.forEachIndexed { si, set ->
            val label = when (set.type) {
                SetType.WARMUP -> "R"
                SetType.EXTRA -> "B"
                SetType.WORK -> { workN++; "$workN" }
            }
            SetRow(ei, si, set, label, onChange = { vm.updateSet(ei, si, it) }, onDone = { vm.toggleDone(ei, si) }, onDelete = { vm.removeSet(ei, si) })
        }
        TextButton(onClick = { vm.addSet(ei) }) {
            Icon(Icons.Filled.Add, null, tint = K.Accent); Spacer(Modifier.width(4.dp)); Text("Aggiungi serie", color = K.Accent)
        }
    }
}

@Composable
private fun SetRow(ei: Int, si: Int, set: SetLog, label: String, onChange: (SetLog) -> Unit, onDone: () -> Unit, onDelete: () -> Unit) {
    var kg by remember(ei, si) { mutableStateOf(if (set.weight > 0) Calc.fmt(set.weight).replace(',', '.') else "") }
    var reps by remember(ei, si) { mutableStateOf(if (set.reps > 0) set.reps.toString() else "") }
    var rir by remember(ei, si) { mutableStateOf(set.rir?.toString() ?: "") }
    LaunchedEffect(set.weight) { if ((kg.toDoubleOrNull() ?: 0.0) != set.weight) kg = if (set.weight > 0) Calc.fmt(set.weight).replace(',', '.') else "" }
    LaunchedEffect(set.reps) { if ((reps.toIntOrNull() ?: 0) != set.reps) reps = if (set.reps > 0) set.reps.toString() else "" }

    var confirmDelete by remember { mutableStateOf(false) }
    val bg = when {
        set.done -> K.Good.copy(alpha = 0.12f)
        set.type == SetType.WARMUP -> K.Surface2.copy(alpha = 0.5f)
        else -> Color.Transparent
    }
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(bg).padding(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.width(44.dp).height(36.dp).clip(RoundedCornerShape(10.dp))
                .background(if (set.type == SetType.WARMUP) K.Surface2 else K.Accent.copy(alpha = 0.15f))
                .clickable { confirmDelete = true },
            contentAlignment = Alignment.Center
        ) {
            Text(label, fontWeight = FontWeight.Black, color = if (set.type == SetType.WARMUP) K.Muted else K.Accent)
        }
        Spacer(Modifier.width(8.dp))
        CellField(kg, Modifier.weight(1f), decimal = true) { v -> kg = v; onChange(set.copy(weight = v.toDoubleOrNull() ?: 0.0)) }
        Spacer(Modifier.width(6.dp))
        CellField(reps, Modifier.weight(1f)) { v -> reps = v; onChange(set.copy(reps = v.toIntOrNull() ?: 0)) }
        Spacer(Modifier.width(6.dp))
        CellField(rir, Modifier.weight(0.7f)) { v -> rir = v; onChange(set.copy(rir = v.toIntOrNull())) }
        Spacer(Modifier.width(6.dp))
        Box(
            Modifier.size(42.dp).clip(RoundedCornerShape(12.dp))
                .background(if (set.done) K.Good else K.Surface2)
                .clickable { onDone() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, "Fatto", tint = if (set.done) Color.Black else K.Muted)
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = K.Surface,
            title = { Text("Eliminare questa serie?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Elimina", color = K.Bad) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Annulla") } }
        )
    }
}

@Composable
private fun CellField(value: String, modifier: Modifier, decimal: Boolean = false, onValue: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { v -> onValue(v.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }.take(6)) },
        singleLine = true,
        textStyle = TextStyle(color = K.Text, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(K.Accent),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).background(K.Surface2),
        decorationBox = { inner -> Box(Modifier.fillMaxSize().padding(horizontal = 4.dp), contentAlignment = Alignment.Center) { inner() } }
    )
}

fun lastPerformance(history: List<Session>, exerciseId: String): String? {
    val s = history.filter { h -> h.exercises.any { it.exerciseId == exerciseId } }.maxByOrNull { it.startedAt } ?: return null
    val sets = s.exercises.filter { it.exerciseId == exerciseId }.flatMap { Stats.workSets(it) }
    if (sets.isEmpty()) return null
    return sets.joinToString("  ") { "${Calc.fmt(it.weight)}×${it.reps}" }
}
