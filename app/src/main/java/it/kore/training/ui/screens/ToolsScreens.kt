package it.kore.training.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.data.*
import it.kore.training.ui.*
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
fun ToolsScreen(nav: NavHostController) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Strumenti", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        ToolRow(Icons.Filled.Calculate, "Calcolatore massimale", "Stima l'1RM da qualsiasi serie e tabella dei carichi") { nav.navigate("onerm") }
        ToolRow(Icons.Filled.Speed, "Tabella RIR / RPE", "Che carico usare per ripetizioni e buffer desiderati") { nav.navigate("rir") }
        ToolRow(Icons.Filled.FitnessCenter, "Calcolatore dischi", "Quali dischi caricare sul bilanciere") { nav.navigate("plates") }
        ToolRow(Icons.Filled.Restaurant, "Calorie e macro", "Fabbisogno, surplus e calorie cicliche") { nav.navigate("nutrition") }
        ToolRow(Icons.Filled.MonitorWeight, "Peso corporeo", "Pesate giornaliere, medie settimanali e andamento") { nav.navigate("bodyweight") }
        ToolRow(Icons.Filled.Search, "Libreria esercizi", "Tecnica esecutiva e alternative per ogni distretto") { nav.navigate("library") }
        ToolRow(Icons.AutoMirrored.Filled.MenuBook, "Il metodo", "Principi di allenamento, volume, intensità e progressioni") { nav.navigate("guide") }
    }
}

@Composable
private fun ToolRow(icon: ImageVector, title: String, sub: String, onClick: () -> Unit) {
    KCard(Modifier.padding(vertical = 5.dp), onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(K.Accent.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = K.Accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(sub, color = K.Muted, fontSize = 12.sp)
            }
            Icon(Icons.Filled.ChevronRight, null, tint = K.Muted)
        }
    }
}

// --------------------------------------------------------------------------- 1RM

@Composable
fun OneRmScreen(vm: AppViewModel, nav: NavHostController) {
    var w by remember { mutableStateOf("80") }
    var r by remember { mutableStateOf("8") }
    var rir by remember { mutableStateOf("0") }
    var saveOpen by remember { mutableStateOf(false) }
    val weight = w.toDoubleOrNull() ?: 0.0
    val reps = r.toIntOrNull() ?: 0
    val res = rir.toIntOrNull() ?: 0
    val est = Calc.e1rm(weight, reps, res)
    Column(Modifier.fillMaxSize()) {
        KTopBar("Calcolatore massimale", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(w, { w = it }, "Carico", Modifier.weight(1.2f), suffix = "kg")
                NumberField(r, { r = it }, "Ripetizioni", Modifier.weight(1f), decimal = false)
                NumberField(rir, { rir = it }, "RIR", Modifier.weight(0.8f), decimal = false)
            }
            Spacer(Modifier.height(16.dp))
            KCard(highlight = true) {
                Text("MASSIMALE STIMATO", style = MaterialTheme.typography.labelSmall, color = K.Muted)
                Text("${Calc.fmt(Calc.round(est, 0.5))} kg", style = MaterialTheme.typography.displaySmall, color = K.Accent)
                Text(
                    "Epley ${Calc.fmt(Calc.round(Calc.epley(weight, reps + res), 0.5))} · Brzycki ${Calc.fmt(Calc.round(Calc.brzycki(weight, reps + res), 0.5))}",
                    color = K.Muted, fontSize = 12.sp
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { saveOpen = true }, enabled = est > 0) { Text("Salva come massimale…", color = K.Accent) }
            }
            SectionTitle("Tabella percentuali")
            KCard {
                Row { TH("% 1RM"); TH("Carico"); TH("Rip. max") }
                listOf(100, 95, 90, 87, 85, 82, 80, 77, 75, 72, 70, 67, 65, 60, 55, 50).forEach { p ->
                    Row(Modifier.padding(vertical = 3.dp)) {
                        TD("$p%", K.Accent)
                        TD("${Calc.fmt(Calc.round(est * p / 100, 2.5))} kg")
                        TD("${Calc.repsAt(p / 100.0)}")
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
    if (saveOpen) {
        AlertDialog(
            onDismissRequest = { saveOpen = false },
            containerColor = K.Surface,
            title = { Text("Salva massimale di…") },
            text = {
                Column {
                    Exercises.oneRmLifts.forEach { ex ->
                        Text(ex.name, Modifier.fillMaxWidth().clickable {
                            vm.setOneRm(ex.id, Calc.round(est, 0.5)); saveOpen = false
                        }.padding(vertical = 12.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { saveOpen = false }) { Text("Chiudi") } }
        )
    }
}

@Composable
private fun RowScope.TH(t: String) = Text(t, Modifier.weight(1f), color = K.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)

@Composable
private fun RowScope.TD(t: String, c: Color = K.Text) = Text(t, Modifier.weight(1f), color = c, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)

// --------------------------------------------------------------------------- RIR / RPE

@Composable
fun RirScreen(nav: NavHostController) {
    var orm by remember { mutableStateOf("100") }
    val one = orm.toDoubleOrNull() ?: 0.0
    Column(Modifier.fillMaxSize()) {
        KTopBar("Tabella RIR / RPE", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            KCard {
                Text("Come leggerla", fontWeight = FontWeight.Bold)
                BulletText("RIR (ripetizioni di riserva) o buffer: quante ripetizioni avresti potuto ancora fare.")
                BulletText("RPE = 10 − RIR. 10 rip @RPE 8 = 10 rip con buffer 2 = un carico da 12RM.")
                BulletText("Inserisci il tuo massimale per vedere i chili; altrimenti la tabella mostra le percentuali.")
            }
            Spacer(Modifier.height(10.dp))
            NumberField(orm, { orm = it }, "Massimale (1RM)", Modifier.fillMaxWidth(), suffix = "kg")
            SectionTitle("Ripetizioni × RIR")
            KCard {
                Row(Modifier.horizontalScroll(rememberScrollState())) {
                    Column {
                        Row {
                            Cell("Rip", true)
                            (0..4).forEach { Cell("RIR $it\nRPE ${10 - it}", true) }
                        }
                        (1..15).forEach { reps ->
                            Row {
                                Cell("$reps", true)
                                (0..4).forEach { rr ->
                                    val pct = Calc.pctFor(reps, rr)
                                    Cell(if (one > 0) Calc.fmt(Calc.round(one * pct, 2.5)) else "${(pct * 100).roundToInt()}%")
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Cell(t: String, header: Boolean = false) {
    Box(Modifier.width(66.dp).height(if (header) 40.dp else 32.dp), contentAlignment = Alignment.Center) {
        Text(t, color = if (header) K.Accent else K.Text, fontSize = if (header) 11.sp else 13.sp,
            fontWeight = if (header) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center)
    }
}

// --------------------------------------------------------------------------- Plates

@Composable
fun PlatesScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    var target by remember { mutableStateOf("100") }
    var bar by remember { mutableStateOf(Calc.fmt(data.profile.barKg)) }
    val t = target.toDoubleOrNull() ?: 0.0
    val b = bar.toDoubleOrNull() ?: 20.0
    val (plates, left) = Calc.plates(t, b, data.profile.plates)
    Column(Modifier.fillMaxSize()) {
        KTopBar("Calcolatore dischi", onBack = { nav.popBackStack() })
        Column(Modifier.padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(target, { target = it }, "Carico totale", Modifier.weight(1f), suffix = "kg")
                NumberField(bar, { bar = it }, "Bilanciere", Modifier.weight(1f), suffix = "kg")
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(-5.0, -2.5, 2.5, 5.0).forEach { d ->
                    FilledTonalButton(onClick = { target = Calc.fmt((t + d).coerceAtLeast(0.0)).replace(',', '.') }, Modifier.weight(1f)) {
                        Text((if (d > 0) "+" else "") + Calc.fmt(d))
                    }
                }
            }
            SectionTitle("Per lato")
            KCard {
                // barbell drawing
                Row(Modifier.fillMaxWidth().height(120.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(40.dp).height(10.dp).background(Color(0xFF777777)))
                    Box(Modifier.width(8.dp).height(30.dp).background(Color(0xFF999999)))
                    plates.forEach { p ->
                        val h = (40 + p * 3).coerceAtMost(110.0).dp
                        Box(
                            Modifier.padding(horizontal = 1.dp).width(if (p >= 10) 16.dp else 10.dp).height(h)
                                .clip(RoundedCornerShape(3.dp)).background(plateColor(p))
                        )
                    }
                    Box(Modifier.weight(1f).height(10.dp).background(Color(0xFF777777)))
                }
                Spacer(Modifier.height(8.dp))
                if (plates.isEmpty()) Text("Solo bilanciere", color = K.Muted)
                plates.groupBy { it }.forEach { (p, l) -> KeyValue("Disco ${Calc.fmt(p)} kg", "× ${l.size}") }
                if (left > 0.01) Text("Non raggiungibile con precisione: mancano ${Calc.fmt(Calc.round(left, 0.25))} kg", color = K.Warn, fontSize = 12.sp)
            }
        }
    }
}

private fun plateColor(p: Double): Color = when {
    p >= 25 -> Color(0xFFE53935)
    p >= 20 -> Color(0xFF1E88E5)
    p >= 15 -> Color(0xFFFDD835)
    p >= 10 -> Color(0xFF43A047)
    p >= 5 -> Color(0xFFEEEEEE)
    else -> Color(0xFF9E9E9E)
}

// --------------------------------------------------------------------------- Nutrition

@Composable
fun NutritionScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val p = data.profile
    val n = remember(p) { Calc.nutrition(p, p.daysPerWeek.coerceAtMost(6)) }
    Column(Modifier.fillMaxSize()) {
        KTopBar("Calorie e macro", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                "Calcolo indicativo basato sul tuo profilo (${Calc.fmt(p.weightKg)} kg, ${p.heightCm} cm, ${p.age} anni, ${p.responder.label.lowercase()}). " +
                    "Per esigenze specifiche rivolgiti a un professionista.",
                color = K.Muted, fontSize = 12.sp
            )
            SectionTitle("Fabbisogno")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${n.bmr}", "Metabolismo basale")
                StatTile("${n.tdee}", "TDEE")
                StatTile("${n.target}", "Obiettivo kcal", accent = true)
            }
            SectionTitle("Macronutrienti giornalieri")
            KCard {
                MacroBar("Proteine", n.proteinG, n.proteinG * 4, n.target, K.Accent)
                MacroBar("Grassi", n.fatG, n.fatG * 9, n.target, K.Warn)
                MacroBar("Carboidrati", n.carbG, n.carbG * 4, n.target, K.Blue)
            }
            SectionTitle("Calorie cicliche (+10% nei giorni di allenamento)")
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                DayMacro("Allenamento", n.trainingDay, Modifier.weight(1f), true)
                DayMacro("Riposo", n.restDay, Modifier.weight(1f), false)
            }
            SectionTitle("Indicazioni")
            KCard {
                BulletText(n.note)
                BulletText("Obiettivo in massa: circa +0,5% del peso corporeo a settimana, mai oltre l'1%.")
                BulletText("Proteine e grassi restano costanti, sono i carboidrati a variare tra giorni di allenamento e riposo.")
                BulletText("Pesati ogni mattina a digiuno per almeno 3 settimane prima di modificare le calorie.")
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(onClick = { nav.navigate("bodyweight") }, modifier = Modifier.fillMaxWidth()) { Text("Apri il diario del peso", color = K.Accent) }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MacroBar(name: String, g: Int, kcal: Int, total: Int, color: Color) {
    val frac = if (total > 0) kcal.toFloat() / total else 0f
    Column(Modifier.padding(vertical = 6.dp)) {
        Row {
            Text(name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text("$g g", fontWeight = FontWeight.ExtraBold, color = color)
            Text("  ·  $kcal kcal", color = K.Muted, fontSize = 12.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(K.Surface2)) {
            Box(Modifier.fillMaxWidth(frac.coerceIn(0f, 1f)).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
    }
}

@Composable
private fun DayMacro(title: String, m: Calc.Macro, modifier: Modifier, accent: Boolean) {
    KCard(modifier, highlight = accent) {
        Text(title.uppercase(), style = MaterialTheme.typography.labelSmall, color = K.Muted)
        Text("${m.kcal} kcal", fontWeight = FontWeight.Black, fontSize = 20.sp, color = if (accent) K.Accent else K.Text)
        Text("P ${m.p} g", fontSize = 13.sp)
        Text("G ${m.f} g", fontSize = 13.sp)
        Text("C ${m.c} g", fontSize = 13.sp)
    }
}

// --------------------------------------------------------------------------- Body weight

@Composable
fun BodyWeightScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    var kg by remember { mutableStateOf(Calc.fmt(data.profile.weightKg).replace(',', '.')) }
    val weeks = remember(data.bodyWeight) { Calc.weeklyAverages(data.bodyWeight) }
    val (pct, advice) = remember(weeks) { Calc.weightAdvice(weeks) }
    Column(Modifier.fillMaxSize()) {
        KTopBar("Peso corporeo", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberField(kg, { kg = it }, "Peso di oggi", Modifier.weight(1f), suffix = "kg")
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = { kg.toDoubleOrNull()?.let { vm.addBodyWeight(it) } },
                    colors = ButtonDefaults.buttonColors(containerColor = K.Accent, contentColor = Color.Black),
                    modifier = Modifier.height(56.dp)
                ) { Text("Salva", fontWeight = FontWeight.Bold) }
            }
            SectionTitle("Andamento giornaliero")
            KCard { LineChart(data.bodyWeight.takeLast(60).map { it.kg }) }
            SectionTitle("Medie settimanali")
            KCard {
                LineChart(weeks.map { it.avg }, color = K.Blue, height = 120.dp)
                Spacer(Modifier.height(8.dp))
                pct?.let {
                    Text(
                        (if (it >= 0) "+" else "") + String.format("%.2f", it) + "% rispetto alla settimana precedente",
                        fontWeight = FontWeight.Bold, color = K.Accent
                    )
                }
                Text(advice, color = K.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
                weeks.takeLast(6).reversed().forEach {
                    KeyValue("Settimana del ${it.weekStart.format(itDate)}", "${String.format("%.1f", it.avg)} kg (${it.count})")
                }
            }
            SectionTitle("Pesate")
            data.bodyWeight.sortedByDescending { it.epochDay }.take(30).forEach { e ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalDate.ofEpochDay(e.epochDay).format(itDate), Modifier.weight(1f), color = K.Muted)
                    Text("${Calc.fmt(e.kg)} kg", fontWeight = FontWeight.Bold)
                    IconButton(onClick = { vm.deleteBodyWeight(e.epochDay) }) { Icon(Icons.Filled.Close, "Elimina", tint = K.Muted) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
