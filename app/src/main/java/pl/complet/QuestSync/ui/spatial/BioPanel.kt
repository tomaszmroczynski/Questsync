package pl.complet.QuestSync.ui.spatial

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import pl.complet.QuestSync.data.DataModule
import java.net.HttpURLConnection
import java.net.URL

// Ripperdoc Design System palette.
private val InkBlack = Color(0xFF050608)
private val InkPanel = Color(0xFF0B0E12)
private val Ember = Color(0xFFEA9A3E)
private val EmberHot = Color(0xFFFFD9A0)
private val EmberDim = Color(0xFF7A5A30)
private val Hair = Color(0x22EA9A3E)
private val TextDim = Color(0xFF8A8F96)

private const val MCP_BASE = "https://ripper-mcp.complet-ai.no"

private fun panelBg() = Brush.verticalGradient(
    listOf(InkPanel.copy(alpha = 0.62f), InkBlack.copy(alpha = 0.74f))
)

private suspend fun fetchHistory(source: String): List<Float> = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL("$MCP_BASE/api/history/$source").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000; readTimeout = 5000
        }
        if (conn.responseCode != 200) return@runCatching emptyList<Float>()
        val txt = conn.inputStream.bufferedReader().readText()
        val arr = JSONArray(txt)
        (0 until arr.length()).map { arr.getDouble(it).toFloat() }
    }.getOrDefault(emptyList())
}

/* ----------------------------- TAGLINE ----------------------------- */

@Composable
fun TaglinePanel() {
    Column(
        modifier = Modifier.fillMaxSize().padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Light)) { append("AI ") }
                withStyle(SpanStyle(color = Ember)) { append("×") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Light)) { append(" SOFTWARE ") }
                withStyle(SpanStyle(color = Ember)) { append("×") }
                withStyle(SpanStyle(color = Color.White, fontWeight = FontWeight.Light)) { append(" HARDWARE") }
            },
            fontSize = 46.sp, letterSpacing = 4.sp
        )
        Spacer(Modifier.height(14.dp))
        Text("ROZSZERZAMY LUDZKIE MOŻLIWOŚCI", color = Ember, letterSpacing = 8.sp, fontSize = 18.sp)
    }
}

/* ----------------------------- COACH ----------------------------- */

private const val COACH_FALLBACK =
    "Twoje tętno spoczynkowe spada trzeci dzień z rzędu, a regeneracja rośnie. " +
        "Ciało jest gotowe na więcej, niż dajesz mu dziś."

@Composable
fun CoachPanel() {
    val ctx = LocalContext.current
    var message by remember { mutableStateOf(COACH_FALLBACK) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val insight = runCatching {
            val agg = DataModule.provideAIAggregator(ctx)
            val mcp = DataModule.provideMcpClientManager()
            val data = agg.getAggregatedDataString()
            mcp.getInsights(data)
        }.getOrNull()
        if (!insight.isNullOrBlank() && !insight.startsWith("Error") && !insight.contains("timed out")) {
            message = insight.trim()
        }
        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(panelBg(), RoundedCornerShape(26.dp))
            .border(1.dp, Hair, RoundedCornerShape(26.dp))
            .verticalScroll(rememberScrollState())
            .padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        SectionHeader("COACH")

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).border(1.5.dp, Ember, CircleShape).padding(11.dp).background(Ember, CircleShape))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("Ripperdoc", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                Text(
                    if (loading) "ANALIZUJĘ…" else "INTELLIGENCE AMPLIFIED",
                    color = EmberDim, letterSpacing = 2.sp, fontSize = 11.sp
                )
            }
        }

        Text(message, color = Color(0xFFE6E8EA), fontSize = 16.sp, lineHeight = 24.sp)

        Box(
            Modifier.fillMaxWidth().background(Ember, RoundedCornerShape(12.dp)).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("ZAPLANUJ SESJĘ", color = InkBlack, fontWeight = FontWeight.Black, letterSpacing = 1.sp, fontSize = 15.sp)
        }
    }
}

/* --------------------------- TELEMETRY --------------------------- */

private data class Metric(val label: String, val source: String, val unit: String, val fmt: (Float) -> String)

private val METRICS = listOf(
    Metric("READINESS · OURA", "oura", "/100") { it.toInt().toString() },
    Metric("KROKI · SAMSUNG", "samsung_health", "") { "%,d".format(it.toInt()).replace(',', ' ') },
    Metric("MASA · WITHINGS", "withings", "kg") { "%.1f".format(it) },
    Metric("VR · QUEST", "quest", "min") { it.toInt().toString() },
)

@Composable
fun TelemetryPanel() {
    var data by remember { mutableStateOf<Map<String, List<Float>>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val m = mutableMapOf<String, List<Float>>()
        for (metric in METRICS) m[metric.source] = fetchHistory(metric.source)
        data = m
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(panelBg(), RoundedCornerShape(26.dp))
            .border(1.dp, Hair, RoundedCornerShape(26.dp))
            .verticalScroll(rememberScrollState())
            .padding(34.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SectionHeader("TELEMETRIA - 7 DNI")
        for (metric in METRICS) {
            val series = data[metric.source].orEmpty()
            val value = if (series.isEmpty()) "—" else metric.fmt(series.last())
            MetricRow(metric.label, value, metric.unit, series)
        }
    }
}

/* ---------------------------- SHARED ----------------------------- */

@Composable
private fun SectionHeader(title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(18.dp).height(2.dp).background(Ember))
        Spacer(Modifier.width(10.dp))
        Text(title, color = Ember, letterSpacing = 3.sp, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun MetricRow(label: String, value: String, unit: String, spark: List<Float>) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = EmberDim, letterSpacing = 1.5.sp, fontSize = 12.sp)
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(value, color = Color.White, fontWeight = FontWeight.Black, fontSize = 30.sp)
                if (unit.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Text(unit, color = TextDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
        }
        Sparkline(spark, Modifier.width(96.dp).height(40.dp))
    }
}

@Composable
private fun Sparkline(data: List<Float>, modifier: Modifier) {
    Canvas(modifier) {
        if (data.size < 2) return@Canvas
        val mn = data.min(); val mx = data.max(); val range = (mx - mn).takeIf { it > 1e-6f } ?: 1f
        val w = size.width; val h = size.height; val dx = w / (data.size - 1)
        val path = Path()
        data.forEachIndexed { i, v ->
            val x = i * dx
            val y = h - ((v - mn) / range) * h
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, Ember, style = Stroke(width = 3f))
        val lastY = h - ((data.last() - mn) / range) * h
        drawCircle(EmberHot, radius = 5f, center = Offset((data.size - 1) * dx, lastY))
    }
}
