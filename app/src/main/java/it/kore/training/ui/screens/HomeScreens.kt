package it.kore.training.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.R
import it.kore.training.data.*
import it.kore.training.ui.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val itDate: DateTimeFormatter = DateTimeFormatter.ofPattern("EEE d MMM", Locale.ITALIAN)

@Composable
fun HomeScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val summary by vm.summary.collectAsState()
    val st = data.program
    val plan = remember(data) { Program.plan(data) }
    val phase = Program.phases[st.phase]
    val thisWeek = remember(data.history) {
        val ws = Stats.weekStart(LocalDate.now())
        data.history.count { !Stats.day(it.startedAt).isBefore(ws) }
    }
    val streak = remember(data.history) { Stats.streakWeeks(data.history) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        // ---------- hero
        Box(Modifier.fillMaxWidth().background(K.heroGradient).padding(20.dp)) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(painterResource(R.drawable.brand_symbol), null, Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("KORE", fontWeight = FontWeight.Black, fontSize = 22.sp, color = K.Accent, letterSpacing = 4.sp)
                    Spacer(Modifier.weight(1f))
                    Pill("${phase.short} · SETT. ${st.week}/${Program.WEEKS_PER_MESO}")
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    if (data.profile.name.isBlank()) "Pronto ad allenarti?" else "Ciao ${data.profile.name},",
                    color = K.Muted
                )
                Text(
                    if (plan.deload) "Settimana di scarico" else phase.name,
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Mesociclo ${st.meso + 1}/${Program.MESO_PER_PHASE} · Microciclo ${Program.absoluteWeek(st)} di ${Program.totalWeeks}",
                    color = K.Muted, style = MaterialTheme.typography.bodySmall
                )
                Spacer(Modifier.height(10.dp))
                LinearProgressIndicator(
                    progress = { Program.absoluteWeek(st).toFloat() / Program.totalWeeks },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = K.Accent, trackColor = K.Surface2
                )
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            // ---------- next workout
            SectionTitle(if (data.active != null) "Allenamento in corso" else "Prossimo allenamento")
            KCard(highlight = true) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(data.active?.title ?: plan.template.name, style = MaterialTheme.typography.headlineSmall)
                        Text(plan.template.muscles, color = K.Muted)
                    }
                    Box(
                        Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(K.accentGradient),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.FitnessCenter, null, tint = Color.Black) }
                }
                Spacer(Modifier.height(12.dp))
                plan.items.forEach { pe ->
                    Row(Modifier.padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Pill(pe.slot.category.short, if (pe.slot.category == Category.BASE) K.Accent else if (pe.slot.category == Category.COMPLEMENTARE) K.Blue else K.Good)
                        Spacer(Modifier.width(8.dp))
                        Text(pe.exercise.name, Modifier.weight(1f), maxLines = 1)
                        Text("${pe.rx.sets}×${pe.rx.repsLabel}", color = K.Muted, fontSize = 13.sp)
                    }
                }
                Spacer(Modifier.height(14.dp))
                GradientButton(if (data.active != null) "Riprendi allenamento" else "Inizia allenamento") {
                    if (data.active == null) vm.startSession(st.dayIndex)
                    nav.navigate("workout")
                }
            }

            // ---------- stats
            SectionTitle("Questa settimana")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("$thisWeek/${data.profile.daysPerWeek}", "Sedute", accent = true)
                StatTile("$streak", "Settimane di fila")
                StatTile("${data.history.size}", "Totale sedute")
            }

            // ---------- shortcuts
            SectionTitle("Scorciatoie")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Shortcut(Icons.Filled.MonitorWeight, "Peso", Modifier.weight(1f)) { nav.navigate("bodyweight") }
                Shortcut(Icons.Filled.Restaurant, "Dieta", Modifier.weight(1f)) { nav.navigate("nutrition") }
                Shortcut(Icons.Filled.Calculate, "1RM", Modifier.weight(1f)) { nav.navigate("onerm") }
                Shortcut(Icons.AutoMirrored.Filled.MenuBook, "Metodo", Modifier.weight(1f)) { nav.navigate("guide") }
            }

            // ---------- last session
            data.history.maxByOrNull { it.startedAt }?.let { last ->
                SectionTitle("Ultima seduta")
                KCard(onClick = { nav.navigate("session/${last.id}") }) {
                    Text(last.title, style = MaterialTheme.typography.titleMedium)
                    Text(Stats.day(last.startedAt).format(itDate), color = K.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        KeyInline("${Stats.setCount(last)}", "serie")
                        KeyInline(Calc.fmt(Stats.volume(last).let { Math.round(it).toDouble() }), "kg sollevati")
                        last.endedAt?.let { KeyInline("${(it - last.startedAt) / 60000}", "min") }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    summary?.let { s ->
        AlertDialog(
            onDismissRequest = { vm.clearSummary() },
            confirmButton = { TextButton(onClick = { vm.clearSummary() }) { Text("Grande!", color = K.Accent) } },
            icon = { Icon(Icons.Filled.EmojiEvents, null, tint = K.Accent) },
            title = { Text("Allenamento completato") },
            text = {
                Column {
                    KeyValue("Seduta", s.title)
                    KeyValue("Durata", "${s.minutes} min")
                    KeyValue("Serie allenanti", "${s.sets}")
                    KeyValue("Tonnellaggio", "${Calc.fmt(Math.round(s.tonnage).toDouble())} kg")
                    if (s.records.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Record", color = K.Accent, fontWeight = FontWeight.Bold)
                        s.records.forEach { BulletText(it) }
                    }
                }
            },
            containerColor = K.Surface
        )
    }
}

@Composable
private fun RowScope.KeyInline(v: String, l: String) {
    Column(Modifier.weight(1f)) {
        Text(v, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
        Text(l, color = K.Muted, fontSize = 12.sp)
    }
}

@Composable
fun Shortcut(icon: ImageVector, label: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(K.Surface).border(1.dp, K.Line, RoundedCornerShape(16.dp))
            .clickable { onClick() }.padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, null, tint = K.Accent)
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

// =====================================================================================
// Onboarding / profile editor
// =====================================================================================

@Composable
fun OnboardingScreen(vm: AppViewModel, onDone: () -> Unit) {
    val data by vm.data.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().background(K.heroGradient).padding(24.dp)) {
            Column {
                Image(painterResource(R.drawable.brand_logo), "Secret Luxury Wellness", Modifier.width(150.dp))
                Spacer(Modifier.height(12.dp))
                Text("KORE", fontWeight = FontWeight.Black, fontSize = 40.sp, color = K.Accent, letterSpacing = 8.sp)
                Text("Allenati con metodo. Misura. Progredisci.", color = K.Muted)
                Spacer(Modifier.height(14.dp))
                Text(
                    "Un programma annuale in 3 fasi che adatta carichi, serie e recuperi al tuo livello e ai tuoi massimali.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        ProfileForm(data.profile, buttonText = "Crea il mio programma") { p ->
            vm.saveProfile(p.copy(onboarded = true))
            onDone()
        }
    }
}

@Composable
fun ProfileForm(initial: Profile, buttonText: String, onSave: (Profile) -> Unit) {
    var name by remember { mutableStateOf(initial.name) }
    var sex by remember { mutableStateOf(initial.sex) }
    var age by remember { mutableStateOf(initial.age.toString()) }
    var height by remember { mutableStateOf(initial.heightCm.toString()) }
    var weight by remember { mutableStateOf(Calc.fmt(initial.weightKg)) }
    var bf by remember { mutableStateOf(Calc.fmt(initial.bodyFatPct)) }
    var level by remember { mutableStateOf(initial.level) }
    var split by remember { mutableStateOf(initial.split) }
    var days by remember { mutableStateOf(initial.daysPerWeek) }
    var responder by remember { mutableStateOf(initial.responder) }
    var activity by remember { mutableStateOf(initial.activityFactor) }
    val oneRm = remember { mutableStateMapOf<String, String>().apply { initial.oneRm.forEach { (k, v) -> put(k, Calc.fmt(v)) } } }

    Column(Modifier.padding(16.dp)) {
        SectionTitle("Chi sei")
        OutlinedTextField(name, { name = it }, label = { Text("Nome") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        Segmented(Sex.entries.map { it.label }, Sex.entries.indexOf(sex), { sex = Sex.entries[it] })
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(age, { age = it }, "Età", Modifier.weight(1f), decimal = false)
            NumberField(height, { height = it }, "Altezza", Modifier.weight(1f), decimal = false, suffix = "cm")
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(weight, { weight = it }, "Peso", Modifier.weight(1f), suffix = "kg")
            NumberField(bf, { bf = it }, "Massa grassa", Modifier.weight(1f), suffix = "%")
        }

        SectionTitle("Esperienza")
        Segmented(Level.entries.map { it.label }, Level.entries.indexOf(level), { level = Level.entries[it] })
        Spacer(Modifier.height(6.dp))
        Text(
            when (level) {
                Level.PRINCIPIANTE -> "Meno serie sui complementari, sempre con 1 ripetizione di margine, niente tecniche di intensità."
                Level.INTERMEDIO -> "Programma completo come previsto dal metodo."
                Level.AVANZATO -> "Una serie in più sull'isolamento nelle fasi 2 e 3, tecniche di intensità complete."
            }, color = K.Muted, fontSize = 13.sp
        )

        SectionTitle("Organizzazione settimanale")
        SplitType.entries.forEach { s ->
            KCard(Modifier.padding(bottom = 8.dp), onClick = { split = s }, highlight = split == s) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = split == s, onClick = { split = s }, colors = RadioButtonDefaults.colors(selectedColor = K.Accent))
                    Column {
                        Text(s.label, fontWeight = FontWeight.Bold)
                        Text(s.description, color = K.Muted, fontSize = 12.sp)
                    }
                }
            }
        }
        Text("Allenamenti a settimana: $days", fontWeight = FontWeight.SemiBold)
        Slider(
            value = days.toFloat(), onValueChange = { days = it.toInt() }, valueRange = 3f..6f, steps = 2,
            colors = SliderDefaults.colors(thumbColor = K.Accent, activeTrackColor = K.Accent)
        )

        SectionTitle("Metabolismo")
        Responder.entries.forEach { r ->
            Row(Modifier.fillMaxWidth().clickable { responder = r }, verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = responder == r, onClick = { responder = r }, colors = RadioButtonDefaults.colors(selectedColor = K.Accent))
                Text(r.label)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Livello di attività (fuori dalla palestra)", fontWeight = FontWeight.SemiBold)
        val acts = listOf(1.375 to "Sedentario", 1.55 to "Moderato", 1.725 to "Attivo")
        Segmented(acts.map { it.second }, acts.indexOfFirst { it.first == activity }.coerceAtLeast(0), { activity = acts[it].first })

        SectionTitle("Massimali (1RM)")
        Text(
            "Servono per calcolare i carichi dei fondamentali in percentuale. Se non li conosci lasciali vuoti: " +
                "l'app li stimerà dalle tue prime sedute, oppure usa il calcolatore 1RM.",
            color = K.Muted, fontSize = 13.sp
        )
        Spacer(Modifier.height(8.dp))
        Exercises.oneRmLifts.forEach { ex ->
            NumberField(oneRm[ex.id] ?: "", { oneRm[ex.id] = it }, ex.name, Modifier.fillMaxWidth().padding(bottom = 6.dp), suffix = "kg")
        }
        Spacer(Modifier.height(16.dp))
        GradientButton(buttonText) {
            onSave(
                initial.copy(
                    name = name.trim(), sex = sex,
                    age = age.toIntOrNull() ?: initial.age,
                    heightCm = height.toIntOrNull() ?: initial.heightCm,
                    weightKg = weight.toDoubleOrNull() ?: initial.weightKg,
                    bodyFatPct = bf.toDoubleOrNull() ?: initial.bodyFatPct,
                    level = level, split = split, daysPerWeek = days, responder = responder, activityFactor = activity,
                    oneRm = oneRm.mapNotNull { (k, v) -> v.toDoubleOrNull()?.takeIf { it > 0 }?.let { k to it } }.toMap()
                )
            )
        }
        Spacer(Modifier.height(32.dp))
    }
}
