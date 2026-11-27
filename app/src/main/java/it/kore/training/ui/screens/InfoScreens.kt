package it.kore.training.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.AppViewModel
import it.kore.training.data.*
import it.kore.training.ui.*

// --------------------------------------------------------------------------- Library

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(nav: NavHostController) {
    var query by remember { mutableStateOf("") }
    var muscle by remember { mutableStateOf<Muscle?>(null) }
    val list = Exercises.all.filter {
        (muscle == null || it.muscle == muscle) && (query.isBlank() || it.name.contains(query, ignoreCase = true))
    }
    Column(Modifier.fillMaxSize()) {
        KTopBar("Libreria esercizi", onBack = { nav.popBackStack() })
        OutlinedTextField(
            query, { query = it }, placeholder = { Text("Cerca esercizio") }, singleLine = true,
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        )
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(muscle == null, { muscle = null }, label = { Text("Tutti") })
            Muscle.entries.forEach { m -> FilterChip(muscle == m, { muscle = if (muscle == m) null else m }, label = { Text(m.label) }) }
        }
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            list.forEach { ex ->
                Row(
                    Modifier.fillMaxWidth().clickable { nav.navigate("exercise/${ex.id}") }.padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Pill(ex.category.short, when (ex.category) { Category.BASE -> K.Accent; Category.COMPLEMENTARE -> K.Blue; else -> K.Good })
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, fontWeight = FontWeight.SemiBold)
                        Text("${ex.muscle.label} · ${ex.equipment.label}", color = K.Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = K.Muted)
                }
                HorizontalDivider(color = K.Line)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun ExerciseDetailScreen(vm: AppViewModel, nav: NavHostController, id: String) {
    val data by vm.data.collectAsState()
    val ex = Exercises.find(id)
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        KTopBar(ex?.name ?: "Esercizio", onBack = { nav.popBackStack() })
        if (ex == null) return@Column
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pill(ex.category.label)
                Pill(ex.muscle.label, K.Blue)
                Pill(ex.equipment.label, K.Muted)
            }
            if (ex.secondary.isNotEmpty()) Text("Sinergici: " + ex.secondary.joinToString { it.label }, color = K.Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
            SectionTitle("Esecuzione")
            KCard { ex.cues.forEach { BulletText(it) } }
            SectionTitle("Come usarlo")
            KCard {
                Text(
                    when (ex.category) {
                        Category.BASE -> "Esercizio fondamentale: va all'inizio della seduta, con carichi elevati e poche ripetizioni. Mantieni sempre un buffer di 1-2 ripetizioni per preservare la tecnica."
                        Category.COMPLEMENTARE -> "Complementare: carichi medi (6-12 ripetizioni). Tieni 1-2 ripetizioni di margine nelle prime serie e arriva vicino al cedimento nell'ultima."
                        Category.ISOLAMENTO -> "Isolamento: carichi leggeri e ripetizioni alte, a fine seduta. Qui il cedimento su tutte le serie è la regola, perché aumentare il carico è difficile."
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    val q = Uri.encode("${ex.name} esecuzione corretta")
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$q")))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.PlayCircle, null, tint = K.Accent); Spacer(Modifier.width(6.dp)); Text("Guarda video su YouTube", color = K.Accent)
            }
            if (data.history.any { s -> s.exercises.any { it.exerciseId == ex.id } }) {
                ExerciseCharts(data, ex.id)
            }
            data.profile.oneRm[ex.id]?.let {
                SectionTitle("Massimale")
                KCard { KeyValue("1RM registrato", "${Calc.fmt(it)} kg", K.Accent) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --------------------------------------------------------------------------- Profile

@Composable
fun ProfileScreen(vm: AppViewModel, nav: NavHostController) {
    val data by vm.data.collectAsState()
    val ctx = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                ctx.contentResolver.openOutputStream(uri)?.use { it.write(vm.repo.exportJson().toByteArray()) }
            }.onSuccess { Toast.makeText(ctx, "Backup salvato", Toast.LENGTH_SHORT).show() }
                .onFailure { Toast.makeText(ctx, "Errore nel salvataggio", Toast.LENGTH_SHORT).show() }
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val text = runCatching { ctx.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            val ok = text != null && vm.repo.importJson(text)
            Toast.makeText(ctx, if (ok) "Backup ripristinato" else "File non valido", Toast.LENGTH_SHORT).show()
        }
    }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Text("Profilo", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(start = 16.dp, top = 16.dp))
        key(data.profile) {
            ProfileForm(data.profile, buttonText = "Salva modifiche") { p ->
                vm.saveProfile(p.copy(onboarded = true))
                Toast.makeText(ctx, "Profilo aggiornato", Toast.LENGTH_SHORT).show()
            }
        }
        Column(Modifier.padding(horizontal = 16.dp)) {
            SectionTitle("Membership")
            KCard(onClick = { nav.navigate("membership") }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WorkspacePremium, null, tint = K.Accent)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("KORE Private · Signature · Concierge", fontWeight = FontWeight.Bold)
                        Text("Scopri i livelli e la nostra promessa sulla privacy", color = K.Muted, fontSize = 12.sp)
                    }
                    Icon(Icons.Filled.ChevronRight, null, tint = K.Muted)
                }
            }
            SectionTitle("Backup dei dati")
            KCard {
                Text("Tutti i dati restano sul telefono. Esporta un backup per non perderli cambiando dispositivo.", color = K.Muted, fontSize = 13.sp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { exporter.launch("kore-backup.json") }, Modifier.weight(1f)) { Text("Esporta", color = K.Accent) }
                    OutlinedButton(onClick = { importer.launch(arrayOf("application/json", "*/*")) }, Modifier.weight(1f)) { Text("Importa", color = K.Accent) }
                }
            }
            SectionTitle("Zona pericolosa")
            OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) { Text("Ricomincia il programma dalla fase 1", color = K.Bad) }
            Spacer(Modifier.height(32.dp))
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = K.Surface,
            title = { Text("Ricominciare il programma?") },
            text = { Text("Torni alla fase 1, settimana 1. Lo storico delle sedute e i massimali restano.") },
            confirmButton = { TextButton(onClick = { vm.setProgram(ProgramState()); confirmReset = false }) { Text("Ricomincia", color = K.Bad) } },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Annulla") } }
        )
    }
}

// --------------------------------------------------------------------------- Guide

private data class GuideSection(val title: String, val body: List<String>)

private val guide = listOf(
    GuideSection("La piramide delle priorità", listOf(
        "Alla base c'è la costanza: un programma discreto seguito per mesi batte un programma perfetto abbandonato dopo tre settimane. Allenarsi con piacere aiuta a restare costanti.",
        "Sopra viene l'organizzazione: si parte dall'obiettivo a lungo termine e si costruisce a ritroso il percorso, fase per fase.",
        "Poi la prestazione: ogni seduta cerca di aggiungere qualcosa (un chilo, una ripetizione, una serie).",
        "Infine l'equilibrio tra stimolo e recupero: stimoli troppo forti o ravvicinati accumulano fatica, stimoli troppo blandi non producono adattamento."
    )),
    GuideSection("I tre stimoli dell'ipertrofia", listOf(
        "Tensione meccanica: serve un carico adeguato e ripetizioni vicine al cedimento.",
        "Stress metabolico: si ottiene con serie lunghe (oltre 15-20 secondi sotto tensione), più serie e recuperi brevi.",
        "Danno muscolare: è una conseguenza dell'allenamento intenso, non va cercato di proposito."
    )),
    GuideSection("Categorie di esercizi", listOf(
        "Fondamentali (BASE): squat, stacco, panca, lat machine, military press, dip. Carichi alti, poche ripetizioni, all'inizio della seduta.",
        "Complementari (COMP): multiarticolari o monoarticolari a carico medio, progressione più lenta.",
        "Isolamento (ISO): carichi leggeri e ripetizioni alte, a fine seduta, spesso a cedimento.",
        "Mantieni almeno due esercizi \"bersaglio\" per muscolo a lungo per misurarne i progressi; gli altri possono ruotare tra le fasi."
    )),
    GuideSection("Intensità: le fasce di ripetizioni", listOf(
        "Carico alto: meno di 6 ripetizioni. Recupero 2'30\"-5'.",
        "Carico medio: 6-12 ripetizioni. Recupero 1'30\"-2'30\".",
        "Carico basso: oltre 12 ripetizioni. Recupero 30\"-1'30\".",
        "Per la massa circa il 65-75% delle serie settimanali sta nella fascia media, il resto si divide tra carichi alti e bassi."
    )),
    GuideSection("Volume settimanale", listOf(
        "Mantenimento: circa 8-10 serie a settimana per gruppo muscolare.",
        "Minimo efficace: 10-15 serie.",
        "Zona ottimale: 15-25 serie (spingi verso l'alto per i muscoli carenti, resta verso il basso per quelli già sviluppati).",
        "Oltre 25-30 serie il recupero non regge e il volume diventa controproducente.",
        "Meglio distribuire il volume su 2 sedute (multifrequenza): le ultime serie restano di qualità."
    )),
    GuideSection("RIR, RPE e cedimento", listOf(
        "RIR (o buffer) = ripetizioni che avresti ancora potuto fare. RPE = 10 − RIR.",
        "Fondamentali pesanti: sempre con buffer, mai a cedimento, per tecnica e sicurezza.",
        "Complementari: 1-2 di buffer nelle prime serie, l'ultima arriva naturalmente al cedimento.",
        "Isolamento: cedimento su tutte le serie.",
        "Cedimento tecnico → concentrico → eccentrico: l'obiettivo è avvicinarsi al cedimento concentrico mantenendo una tecnica pulita."
    )),
    GuideSection("Il tempo di esecuzione", listOf(
        "Si scrive con quattro numeri: eccentrica · pausa in basso · concentrica · pausa in alto.",
        "Standard 2-0-1-0: discesa controllata in due secondi, salita esplosiva, nessuna pausa."
    )),
    GuideSection("Modelli di progressione", listOf(
        "Carico: stesse serie e ripetizioni, il peso sale ogni settimana (es. 4×6 con +2,5 kg).",
        "Carico con riduzione delle ripetizioni: 4×6 → 4×5 → 4×4… mentre il peso sale.",
        "Volume: a carico fisso aumentano le ripetizioni (3×8 → 3×10) o le serie (3×6 → 6×6).",
        "Doppia progressione: prima porti tutte le serie al limite alto del range, poi aumenti il carico e riparti dal basso.",
        "Densità: stesso lavoro con recuperi più brevi (es. da 120\" a 80\").",
        "Intensità percepita: stesso carico, buffer che scende da 2 a 0 e poi tecniche come parziali, stripping o isometria.",
        "Frequenza: lo stesso volume viene spezzato su più sedute."
    )),
    GuideSection("Periodizzazione", listOf(
        "Seduta → microciclo (circa una settimana) → mesociclo (5-6 settimane) → macrociclo (un anno).",
        "Ogni mesociclo si chiude con una settimana di scarico per smaltire la fatica accumulata.",
        "Fase 1 Forza & Ipertrofia: prima sale il volume a carico costante, poi sale il carico. Massimali testati a inizio e fine fase.",
        "Fase 2 Trasformazione Ipertrofica: più complementari e isolamento, più serie a cedimento, tecniche da bodybuilding.",
        "Fase 3 Qualità Muscolare: pompaggio, cedimento frequente, stripping, rest-pause e back-off."
    )),
    GuideSection("Alimentazione in massa", listOf(
        "Parti dal fabbisogno giornaliero (TDEE) e aggiungi +200/300 kcal (+400/600 se fatichi a crescere, +150/200 se ingrassi facilmente).",
        "Proteine 1,4-2 g per kg, grassi 0,8-1,2 g per kg, il resto carboidrati.",
        "Calorie cicliche: circa +10% nei giorni di allenamento, meno nei giorni di riposo, stesso totale settimanale.",
        "Pesati ogni mattina e confronta le medie settimanali: l'obiettivo è circa +0,5% a settimana."
    ))
)

@Composable
fun GuideScreen(nav: NavHostController) {
    var open by remember { mutableIntStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        KTopBar("Il metodo", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            Text(
                "Sintesi dei principi su cui si basa il programma dell'app: come vengono scelti esercizi, serie, ripetizioni, carichi e recuperi.",
                color = K.Muted, fontSize = 13.sp
            )
            Spacer(Modifier.height(10.dp))
            guide.forEachIndexed { i, g ->
                KCard(Modifier.padding(bottom = 8.dp), onClick = { open = if (open == i) -1 else i }, highlight = open == i) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("%02d".format(i + 1), color = K.Accent, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(12.dp))
                        Text(g.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Icon(if (open == i) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null, tint = K.Muted)
                    }
                    if (open == i) {
                        Spacer(Modifier.height(8.dp))
                        g.body.forEach { BulletText(it) }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
