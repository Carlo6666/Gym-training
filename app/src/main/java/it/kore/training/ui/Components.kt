package it.kore.training.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun KCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    highlight: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(K.Surface)
            .border(1.dp, if (highlight) K.Accent.copy(alpha = 0.6f) else K.Line, shape)
            .let { if (onClick != null) it.clickable { onClick() } else it }
            .padding(16.dp),
        content = content
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 20.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = K.Muted, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun Pill(text: String, color: Color = K.Accent, filled: Boolean = false, modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (filled) color else color.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(text, color = if (filled) Color.Black else color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun RowScope.StatTile(value: String, label: String, accent: Boolean = false) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .background(if (accent) K.Accent.copy(alpha = 0.12f) else K.Surface)
            .border(1.dp, if (accent) K.Accent.copy(alpha = 0.4f) else K.Line, RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Text(value, style = MaterialTheme.typography.titleLarge, color = if (accent) K.Accent else K.Text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = MaterialTheme.typography.bodySmall, color = K.Muted, maxLines = 1)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KTopBar(title: String, onBack: (() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(
        title = { Text(title, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        navigationIcon = {
            if (onBack != null) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
        },
        actions = actions,
        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = K.Bg, titleContentColor = K.Text)
    )
}

@Composable
fun GradientButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) K.accentGradient else Brush.linearGradient(listOf(K.Surface2, K.Surface2)))
            .clickable(enabled = enabled) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text.uppercase(), color = if (enabled) Color.Black else K.Muted, fontWeight = FontWeight.Black, letterSpacing = 1.sp)
    }
}

@Composable
fun NumberField(
    value: String,
    onValue: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    decimal: Boolean = true,
    suffix: String? = null
) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onValue(v.replace(',', '.').filter { it.isDigit() || (decimal && it == '.') }) },
        label = { Text(label) },
        singleLine = true,
        suffix = if (suffix != null) { { Text(suffix) } } else null,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = K.Accent, unfocusedBorderColor = K.Line, focusedLabelColor = K.Accent
        )
    )
}

/** Simple line chart with gradient fill. */
@Composable
fun LineChart(values: List<Double>, modifier: Modifier = Modifier, color: Color = K.Accent, height: Dp = 160.dp) {
    if (values.size < 2) {
        Box(modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
            Text("Servono almeno due rilevazioni", color = K.Muted, style = MaterialTheme.typography.bodySmall)
        }
        return
    }
    val min = values.min()
    val max = values.max()
    val span = (max - min).takeIf { it > 0 } ?: 1.0
    Canvas(modifier.fillMaxWidth().height(height)) {
        val pad = 8.dp.toPx()
        val w = size.width - pad * 2
        val h = size.height - pad * 2
        val pts = values.mapIndexed { i, v ->
            Offset(pad + w * i / (values.size - 1), pad + h - (h * ((v - min) / span)).toFloat())
        }
        // grid
        for (g in 0..3) {
            val y = pad + h * g / 3
            drawLine(K.Line, Offset(pad, y), Offset(pad + w, y), 1f)
        }
        val line = Path().apply {
            moveTo(pts.first().x, pts.first().y)
            pts.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val fill = Path().apply {
            addPath(line)
            lineTo(pts.last().x, pad + h)
            lineTo(pts.first().x, pad + h)
            close()
        }
        drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.35f), Color.Transparent)))
        drawPath(line, color, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))
        pts.forEach { drawCircle(K.Bg, 4.dp.toPx(), it); drawCircle(color, 2.5.dp.toPx(), it) }
    }
}

@Composable
fun BarChart(values: List<Double>, labels: List<String>, modifier: Modifier = Modifier, height: Dp = 140.dp) {
    if (values.isEmpty()) return
    val max = values.max().takeIf { it > 0 } ?: 1.0
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(height)) {
            val n = values.size
            val gap = 6.dp.toPx()
            val bw = (size.width - gap * (n - 1)) / n
            values.forEachIndexed { i, v ->
                val bh = (size.height * (v / max)).toFloat().coerceAtLeast(2f)
                drawRoundRect(
                    brush = if (i == n - 1) K.accentGradient else Brush.linearGradient(listOf(K.Surface2, Color(0xFF3A3A3A))),
                    topLeft = Offset(i * (bw + gap), size.height - bh),
                    size = Size(bw, bh),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx())
                )
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            labels.forEach { Text(it, Modifier.weight(1f), fontSize = 9.sp, color = K.Muted, maxLines = 1) }
        }
    }
}

/** Horizontal bar showing weekly sets against volume landmarks. */
@Composable
fun VolumeBar(label: String, sets: Double, mev: Int, mavHigh: Int, zone: String) {
    val maxScale = (mavHigh + 8).toFloat()
    val frac = (sets.toFloat() / maxScale).coerceIn(0f, 1f)
    val color = when {
        sets < mev -> K.Warn
        sets <= mavHigh -> K.Good
        else -> K.Bad
    }
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
            Text(String.format("%.1f serie", sets), color = color, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(K.Surface2)) {
            Box(Modifier.fillMaxWidth(frac).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text(zone, fontSize = 11.sp, color = K.Muted)
    }
}

@Composable
fun ProgressRing(progress: Float, modifier: Modifier = Modifier, stroke: Dp = 6.dp, color: Color = K.Accent) {
    Canvas(modifier) {
        val s = stroke.toPx()
        drawArc(K.Surface2, -90f, 360f, false, style = Stroke(s), topLeft = Offset(s / 2, s / 2), size = Size(size.width - s, size.height - s))
        drawArc(color, -90f, 360f * progress.coerceIn(0f, 1f), false, style = Stroke(s, cap = StrokeCap.Round),
            topLeft = Offset(s / 2, s / 2), size = Size(size.width - s, size.height - s))
    }
}

@Composable
fun KeyValue(key: String, value: String, valueColor: Color = K.Text) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(key, color = K.Muted, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(K.Surface).border(1.dp, K.Line, RoundedCornerShape(14.dp)).padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        options.forEachIndexed { i, o ->
            Box(
                Modifier.weight(1f).clip(RoundedCornerShape(10.dp))
                    .background(if (i == selected) K.Accent else Color.Transparent)
                    .clickable { onSelect(i) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(o, color = if (i == selected) Color.Black else K.Text, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1)
            }
        }
    }
}

@Composable
fun BulletText(text: String) {
    Row(Modifier.padding(vertical = 3.dp)) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(RoundedCornerShape(3.dp)).background(K.Accent))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = K.Text.copy(alpha = 0.9f))
    }
}

@Composable
fun rememberText(initial: String) = remember { mutableStateOf(initial) }
