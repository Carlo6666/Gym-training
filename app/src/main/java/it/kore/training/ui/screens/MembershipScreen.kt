package it.kore.training.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import it.kore.training.R
import it.kore.training.ui.*

const val SL_EMAIL = "info@secretluxuryconcierge.com"
const val SL_PHONE = "+393522054063"

private data class Tier(val name: String, val price: String, val tag: String, val items: List<String>, val featured: Boolean = false)

private val tiers = listOf(
    Tier("KORE Private", "39", "L'essenziale, senza compromessi", listOf(
        "App completa, nessun account",
        "Nessuna raccolta, conservazione o vendita di dati",
        "Programma annuale firmato dal tuo coach",
        "Timer, progressioni, grafici e strumenti"
    )),
    Tier("KORE Signature", "190", "Il programma costruito su di te", listOf(
        "Tutto di KORE Private",
        "Programma personalizzato sui tuoi obiettivi",
        "Check mensile riservato con il coach",
        "Revisione dei carichi e dei progressi"
    ), featured = true),
    Tier("KORE Concierge", "da 690", "Il coach al tuo fianco, ovunque", listOf(
        "Tutto di KORE Signature",
        "Coaching settimanale, anche in presenza",
        "Sessioni in hotel, in viaggio o a domicilio",
        "Linea diretta e riservata con il coach"
    ))
)

private fun Context.open(intent: Intent) {
    runCatching { startActivity(intent) }
        .onFailure { Toast.makeText(this, "Nessuna app disponibile per questa azione", Toast.LENGTH_SHORT).show() }
}

private fun Context.mailTo(tier: String? = null) {
    val subject = "Richiesta Secret Luxury Wellness" + (tier?.let { " - $it" } ?: "")
    open(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SL_EMAIL?subject=${Uri.encode(subject)}")))
}

@Composable
fun MembershipScreen(nav: NavHostController) {
    val ctx = LocalContext.current
    Column(Modifier.fillMaxSize()) {
        KTopBar("Membership", onBack = { nav.popBackStack() })
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp)) {
            // ---------- manifesto
            Column(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.brand_logo), "Secret Luxury Wellness", Modifier.width(170.dp))
                Spacer(Modifier.height(14.dp))
                Text("Il tuo allenamento resta tuo.", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Nessun profilo, nessun tracciamento, nessuno sponsor. I tuoi dati non lasciano mai il telefono: non li raccogliamo, non li conserviamo, non li vendiamo. Paghi l'app, non con la tua privacy.",
                    color = K.Muted, textAlign = TextAlign.Center, fontSize = 14.sp
                )
                Spacer(Modifier.height(12.dp))
                Text("SECRET LUXURY WELLNESS", color = K.Accent, fontSize = 11.sp, letterSpacing = 3.sp, fontWeight = FontWeight.Bold)
            }

            // ---------- tiers
            tiers.forEach { t ->
                Spacer(Modifier.height(12.dp))
                KCard(highlight = t.featured) {
                    if (t.featured) Pill("LA SCELTA PIÙ RICHIESTA", filled = true)
                    if (t.featured) Spacer(Modifier.height(10.dp))
                    Text(t.name, style = MaterialTheme.typography.headlineSmall, color = K.Accent)
                    Text(t.tag, color = K.Muted, fontSize = 13.sp)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(t.price, fontSize = 34.sp, fontWeight = FontWeight.Bold, fontFamily = Serif)
                        Text("  € / mese", color = K.Muted, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(6.dp))
                    t.items.forEach { BulletText(it) }
                    Spacer(Modifier.height(14.dp))
                    if (t.featured) GradientButton("Richiedi l'accesso") { ctx.mailTo(t.name) }
                    else OutlinedButton(onClick = { ctx.mailTo(t.name) }, modifier = Modifier.fillMaxWidth()) { Text("Richiedi l'accesso", color = K.Accent) }
                }
            }

            // ---------- contacts
            Spacer(Modifier.height(16.dp))
            val shape = RoundedCornerShape(18.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(K.Surface).border(1.dp, K.Accent.copy(alpha = 0.35f), shape).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Secret Luxury Wellness", style = MaterialTheme.typography.headlineSmall, color = K.Accent)
                Spacer(Modifier.height(6.dp))
                Text("Scrivici o chiamaci: rispondiamo personalmente, nella massima riservatezza.", color = K.Muted, fontSize = 13.sp, textAlign = TextAlign.Center)
                Spacer(Modifier.height(14.dp))
                GradientButton("Secret Luxury Wellness") { ctx.mailTo() }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { ctx.open(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$SL_PHONE"))) }, Modifier.weight(1f)) { Text("Chiama", color = K.Text) }
                    OutlinedButton(onClick = { ctx.open(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/${SL_PHONE.drop(1)}"))) }, Modifier.weight(1f)) { Text("WhatsApp", color = K.Text) }
                }
                Spacer(Modifier.height(10.dp))
                Text("$SL_EMAIL · +39 352 205 4063", color = K.Muted, fontSize = 12.sp)
            }
            Spacer(Modifier.height(14.dp))
            Text("Prezzi indicativi IVA inclusa. Ogni membership è riservata e su richiesta.", color = K.Muted, fontSize = 12.sp)
            Spacer(Modifier.height(32.dp))
        }
    }
}
