package it.kore.training.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.data.*
import it.kore.training.ui.*

@Composable
fun ProgramScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val st = data.program
    var viewWeek by remember(st.week) { mutableIntStateOf(st.week) }
    var expanded by remember { mutableStateOf(st.dayIndex) }
    var editPos by remember { mutableStateOf(false) }
    val templates = Program.templates(data.profile.split)
    val volume = remember(data) { Program.volumePerMuscle(data) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Programma", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(onClick = { editPos = true }) { Icon(Icons.Filled.Tune, "Cambia posizione", tint = K.Accent) }
        }
        Text(data.profile.split.label + " · " + data.profile.level.label, color = K.Muted)

        // ---------- phases timeline
        SectionTitle("Macrociclo annuale")
        Program.phases.forEachIndexed { i, ph ->
            val current = i == st.phase
            KCard(Modifier.padding(bottom = 8.dp), highlight = current) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(34.dp).clip(RoundedCornerShape(10.dp))
                            .background(if (current) K.Accent else if (i < st.phase) K.Good.copy(alpha = 0.3f) else K.Surface2),
                        contentAlignment = Alignment.Center
                    ) {
                        if (i < st.phase) Icon(Icons.Filled.Check, null, tint = K.Good)
                        else Text("${i + 1}", fontWeight = FontWeight.Black, color = if (current) Color.Black else K.Muted)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ph.name, fontWeight = FontWeight.ExtraBold)
                        Text(ph.goal, color = K.Muted, fontSize = 12.sp)
                    }
                    if (current) Pill("IN CORSO")
                }
                if (current) {
                    Spacer(Modifier.height(8.dp))
                    ph.details.forEach { BulletText(it) }
                }
            }
        }

        // ---------- weeks of the mesocycle
        SectionTitle("Mesociclo ${st.meso + 1} di ${Program.MESO_PER_PHASE}")
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            (1..Program.WEEKS_PER_MESO).forEach { w ->
                val sel = w == viewWeek
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .background(if (sel) K.Accent else K.Surface)
                        .border(1.dp, if (w == st.week) K.Accent else K.Line, RoundedCornerShape(12.dp))
                        .clickable { viewWeek = w }.padding(vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(if (w == Program.WEEKS_PER_MESO) "S" else "$w", fontWeight = FontWeight.Black, color = if (sel) Color.Black else K.Text)
                    Text(if (w == Program.WEEKS_PER_MESO) "scarico" else "sett.", fontSize = 9.sp, color = if (sel) Color.Black else K.Muted)
                }
            }
        }
        if (viewWeek != st.week) {
            Text("Anteprima della settimana $viewWeek (i carichi sono stimati)", color = K.Accent2, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
        }

        // ---------- sessions
        SectionTitle("Sedute del microciclo")
        templates.forEachIndexed { di, t ->
            val plan = remember(data, viewWeek, di) { Program.plan(data, di, viewWeek) }
            val isNext = di == st.dayIndex && viewWeek == st.week
            val open = expanded == di
            KCard(Modifier.padding(bottom = 8.dp).animateContentSize(), onClick = { expanded = if (open) -1 else di }, highlight = isNext) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G${di + 1}", fontWeight = FontWeight.Black, color = K.Accent, fontSize = 18.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(t.name, fontWeight = FontWeight.ExtraBold)
                        Text(t.muscles, color = K.Muted, fontSize = 12.sp)
                    }
                    if (isNext) Pill("PROSSIMA")
                    Icon(if (open) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = K.Muted)
                }
                if (open) {
                    Spacer(Modifier.height(10.dp))
                    plan.items.forEach { pe -> PlannedRow(pe, data.swaps.containsKey(pe.slot.key)) { vm.resetSwap(pe.slot.key) } }
                    Spacer(Modifier.height(10.dp))
                    if (data.active == null && viewWeek == st.week) {
                        OutlinedButton(onClick = { vm.startSession(di); nav.navigate("workout") }, modifier = Modifier.fillMaxWidth()) {
                            Icon(Icons.Filled.PlayArrow, null, tint = K.Accent)
                            Spacer(Modifier.width(6.dp))
                            Text(if (isNext) "Inizia questa seduta" else "Fai questa seduta oggi", color = K.Accent)
                        }
                    }
                }
            }
        }
        TextButton(onClick = { vm.skipScheduled() }) {
            Icon(Icons.Filled.SkipNext, null, tint = K.Muted); Spacer(Modifier.width(4.dp)); Text("Salta la prossima seduta", color = K.Muted)
        }

        // ---------- weekly volume
        SectionTitle("Volume settimanale pianificato")
        KCard {
            Text("Serie allenanti per gruppo muscolare (i muscoli sinergici contano metà).", color = K.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            Muscle.entries.filter { (volume[it] ?: 0.0) > 0 }.forEach { m ->
                val l = Stats.landmarks(m)
                VolumeBar(m.label, volume[m] ?: 0.0, l.mev, l.mavHigh, Stats.zone(m, volume[m] ?: 0.0))
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (editPos) PositionDialog(st, onDismiss = { editPos = false }) { vm.setProgram(it); editPos = false }
}

@Composable
private fun PlannedRow(pe: PlannedExercise, swapped: Boolean, onReset: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Pill(pe.slot.category.short, when (pe.slot.category) { Category.BASE -> K.Accent; Category.COMPLEMENTARE -> K.Blue; else -> K.Good })
            Spacer(Modifier.width(8.dp))
            Text(pe.exercise.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (swapped) Text("ripristina", color = K.Accent, fontSize = 11.sp, modifier = Modifier.clickable { onReset() })
        }
        Text(
            buildString {
                append("${pe.rx.sets} × ${pe.rx.repsLabel}  ·  RIR ${pe.rx.rir}  ·  rec ${mmss(pe.rx.restSec.toLong())}")
                pe.suggestedKg?.let { append("  ·  ${Calc.fmt(it)} kg") }
            },
            color = K.Text.copy(alpha = 0.85f), fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp)
        )
        Text(pe.suggestionWhy, color = K.Muted, fontSize = 11.sp)
        if (pe.rx.technique != Technique.NESSUNA) Text("Tecnica: ${pe.rx.technique.label}", color = K.Accent2, fontSize = 11.sp)
    }
}

@Composable
private fun PositionDialog(st: ProgramState, onDismiss: () -> Unit, onSave: (ProgramState) -> Unit) {
    var phase by remember { mutableIntStateOf(st.phase) }
    var meso by remember { mutableIntStateOf(st.meso) }
    var week by remember { mutableIntStateOf(st.week) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = K.Surface,
        title = { Text("Posizione nel programma") },
        text = {
            Column {
                Text("Fase", fontWeight = FontWeight.SemiBold)
                Segmented(listOf("1", "2", "3"), phase, { phase = it })
                Spacer(Modifier.height(10.dp))
                Text("Mesociclo", fontWeight = FontWeight.SemiBold)
                Segmented(listOf("1", "2", "3"), meso, { meso = it })
                Spacer(Modifier.height(10.dp))
                Text("Settimana", fontWeight = FontWeight.SemiBold)
                Segmented(listOf("1", "2", "3", "4", "5", "S"), week - 1, { week = it + 1 })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(st.copy(phase = phase, meso = meso, week = week, dayIndex = 0)) }) { Text("Salva", color = K.Accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } }
    )
}
