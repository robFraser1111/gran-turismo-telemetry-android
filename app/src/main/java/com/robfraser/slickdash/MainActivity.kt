package com.robfraser.slickdash

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.Executors

val Page = Color(0xFF0B1220)
val Card = Color(0xFF111A2B)
val Line = Color(0xFF1A2740)
val Cyan = Color(0xFF22D3EE)
val Green = Color(0xFF22C55E)
val Red = Color(0xFFEF4444)
val Amber = Color(0xFFF59E0B)
val Text = Color(0xFFF8FAFC)
val Muted = Color(0xFF8B9BB4)
val LiveBg = Color(0xFF143528)
val TireBg = Color(0xFF0D1526)

enum class Mode { Simple, Driving, PitWall }

class DashViewModel : ViewModel() {
    private val tracker = SessionTracker()
    private val _ui = MutableStateFlow(DashState())
    val ui = _ui.asStateFlow()
    var mode by mutableStateOf(Mode.Simple)
    var settings by mutableStateOf(false)
    var manualIp by mutableStateOf("")
    var showIp by mutableStateOf(false)
    private val exec = Executors.newSingleThreadExecutor()
    private var client: Gt7UdpClient? = null
    private val throttleTrace = ArrayDeque<Float>()
    private val brakeTrace = ArrayDeque<Float>()
    private var qualityWindowStart = System.currentTimeMillis()
    private var qualityRxAtStart = 0
    private var qualityErrAtStart = 0

    init { findPs5() }

    private fun appendInputTrace(buf: ArrayDeque<Float>, v: Float) {
        buf.addLast(v)
        while (buf.size > 120) buf.removeFirst()
    }

    private fun refreshQuality(rx: Int, err: Int): QualityRating {
        val now = System.currentTimeMillis()
        val elapsed = (now - qualityWindowStart) / 1000.0
        if (elapsed >= 1.0) {
            val drx = (rx - qualityRxAtStart).toDouble()
            val derr = (err - qualityErrAtStart).toDouble()
            val pps = drx / maxOf(elapsed, 0.001)
            val ratio = when {
                drx > 0 -> derr / drx
                derr > 0 -> 1.0
                else -> 0.0
            }
            val rating = ConnectionQuality.classify(pps, ratio)
            qualityWindowStart = now
            qualityRxAtStart = rx
            qualityErrAtStart = err
            return rating
        }
        return _ui.value.quality
    }

    private fun applyPacket(p: TelemetryPacket, peerOverride: String? = null) {
        val s = tracker.onPacket(p)
        val prev = _ui.value
        val racing = p.isRacing
        if (racing) {
            appendInputTrace(throttleTrace, p.throttleNorm.toFloat())
            appendInputTrace(brakeTrace, p.brakeNorm.toFloat())
        }
        val displayPacket = if (racing) {
            p
        } else {
            (s.packet ?: prev.packet)?.copy(
                speedMps = 0f, rpm = 0f, throttle = 0, brake = 0, gear = 15,
            )
        }
        _ui.value = prev.copy(
            live = racing,
            packet = displayPacket,
            fuelPct = s.fuelPct,
            fuelPerLap = s.fuelPerLap,
            lapsRemaining = s.lapsRemaining,
            stops = s.stops,
            windowOpen = s.windowOpen,
            lastMs = s.lastMs,
            bestMs = s.bestMs,
            liveDelta = if (racing) s.liveDelta else null,
            deltaTrace = if (racing) s.deltaTrace else emptyList(),
            throttleTrace = throttleTrace.toList(),
            brakeTrace = brakeTrace.toList(),
            laps = s.laps,
            lapsInMemory = s.lapsInMemory,
            dec = prev.dec + 1,
            peer = peerOverride ?: prev.peer,
            quality = refreshQuality(prev.rx, prev.err),
        )
    }

    private fun bindClient(peerOverride: String? = null): Gt7UdpClient =
        Gt7UdpClient(
            onPacket = { p -> applyPacket(p, peerOverride) },
            onRaw = {
                val rx = _ui.value.rx + 1
                _ui.value = _ui.value.copy(rx = rx, quality = refreshQuality(rx, _ui.value.err))
            },
            onDecodeFail = {
                val err = _ui.value.err + 1
                _ui.value = _ui.value.copy(err = err, quality = refreshQuality(_ui.value.rx, err))
            },
            onPeer = { ip -> _ui.value = _ui.value.copy(peer = ip) },
            onStatus = { st -> _ui.value = _ui.value.copy(status = st) },
        )

    fun findPs5() {
        client?.stop()
        _ui.value = _ui.value.copy(status = "Looking for GT7 on this network…", peer = null, live = false)
        val c = bindClient()
        client = c
        exec.execute { c.startDiscover() }
    }

    fun connectIp(ip: String) {
        client?.stop()
        val c = bindClient(peerOverride = ip.trim())
        client = c
        exec.execute { c.startHost(ip.trim()) }
    }

    override fun onCleared() { client?.stop(); exec.shutdownNow() }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { MaterialTheme { App() } }
    }
}

@Composable
fun App(vm: DashViewModel = viewModel()) {
    val s by vm.ui.collectAsState()
    // Background stays edge-to-edge; chrome uses safeDrawing (status, nav, cutout, side nav in landscape).
    Box(Modifier.fillMaxSize().background(Page)) {
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Header(s.live, vm.mode, { vm.mode = it }, { vm.settings = true })
            when (vm.mode) {
                Mode.Simple -> SimpleView(s)
                Mode.Driving -> DrivingView(s)
                Mode.PitWall -> PitWallView(s)
            }
        }
        if (vm.settings) SettingsSheet(s, vm)
    }
}

@Composable
fun Header(live: Boolean, mode: Mode, onMode: (Mode) -> Unit, onCog: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)
            .border(1.dp, Cyan.copy(alpha = 0f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.s_mark), null, Modifier.size(22.dp))
        Spacer(Modifier.width(8.dp))
        Text("SlickDash", color = Text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Box(
            Modifier.clip(CircleShape).background(LiveBg).border(1.dp, Color(0xFF1D6B3A), CircleShape)
                .padding(horizontal = 8.dp, vertical = 2.dp)
        ) { Text(if (live) "LIVE" else "IDLE", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.weight(1f))
        listOf(Mode.Simple to "Simple", Mode.Driving to "Driving", Mode.PitWall to "Pit wall").forEach { (m, label) ->
            val on = mode == m
            Text(
                label, color = if (on) Cyan else Muted, fontSize = 11.sp,
                modifier = Modifier.padding(start = 6.dp).border(1.dp, if (on) Cyan else Line, CircleShape)
                    .clickable { onMode(m) }.padding(horizontal = 9.dp, vertical = 3.dp)
            )
        }
        Box(
            Modifier.padding(start = 8.dp).size(28.dp).border(1.dp, Line, RoundedCornerShape(6.dp))
                .clickable { onCog() },
            contentAlignment = Alignment.Center
        ) { Text("⚙", color = Muted, fontSize = 14.sp) }
    }
    Box(Modifier.fillMaxWidth().height(1.dp).background(Cyan))
}

@Composable fun CardBox(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(8.dp)).background(Card).border(1.dp, Line, RoundedCornerShape(8.dp))
            .padding(10.dp),
        content = content
    )
}
@Composable fun Lbl(t: String) = Text(t.uppercase(), color = Muted, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
@Composable fun Bar(frac: Float, color: Color) {
    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color(0xFF1B2438))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(frac.coerceIn(0f, 1f)).background(color))
    }
}

@Composable fun isLandscape(): Boolean =
    LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE

@Composable fun FuelCard(s: DashState, modifier: Modifier = Modifier, big: Boolean = false) {
    CardBox(modifier) {
        Lbl("Fuel — this session")
        Text("%.0f%%".format(s.fuelPct), color = Text, fontSize = if (big) 42.sp else 22.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(8.dp)); Bar((s.fuelPct / 100.0).toFloat(), Amber)
        Text(s.fuelPerLap?.let { "%.1f%%/lap".format(it) } ?: "—%/lap", color = Text, fontSize = 13.sp)
        Text(s.lapsRemaining?.let { "%.1f laps remaining".format(it) } ?: "— laps remaining", color = Text, fontSize = 13.sp)
        Text(if (s.stops == 1) "1 stop" else "${s.stops} stops", color = Muted, fontSize = 12.sp)
    }
}

@Composable fun SimpleView(s: DashState) {
    val land = isLandscape()
    if (land) {
        Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FuelCard(s, Modifier.fillMaxHeight().weight(0.42f), big = true)
            CardBox(Modifier.fillMaxHeight().weight(1f)) {
                Lbl("Tire temps")
                TireGrid(s.packet, Modifier.weight(1f))
            }
        }
    } else {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FuelCard(s, Modifier.fillMaxWidth(), big = true)
            CardBox(Modifier.fillMaxWidth().height(280.dp)) {
                Lbl("Tire temps")
                TireGrid(s.packet, Modifier.weight(1f))
            }
        }
    }
}

@Composable fun TireGrid(p: TelemetryPacket?, modifier: Modifier = Modifier) {
    val cells = listOf("FL" to (p?.tireFL ?: 0f), "FR" to (p?.tireFR ?: 0f), "RL" to (p?.tireRL ?: 0f), "RR" to (p?.tireRR ?: 0f))
    Column(modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { row ->
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (k, v) ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(6.dp)).background(TireBg),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
                    ) {
                        Text(k, color = Muted, fontSize = 11.sp)
                        val c = if (v >= 100) Red else Green
                        Text("%.0f°C".format(v), color = c, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable fun GearSpeedCard(p: TelemetryPacket?, modifier: Modifier = Modifier) {
    CardBox(modifier) {
        Lbl("Gear / speed")
        RpmBar(p?.rpmFrac ?: 0f)
        Row(verticalAlignment = Alignment.Bottom) {
            Column { Lbl("Gear"); Text(p?.gearDisplay ?: "N", color = Text, fontSize = 34.sp, fontWeight = FontWeight.SemiBold) }
            Spacer(Modifier.width(16.dp))
            Column { Lbl("Speed"); Text("%d km/h".format((p?.speedKph ?: 0.0).toInt()), color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold) }
        }
    }
}

@Composable fun ThrottleBrakeCard(s: DashState, modifier: Modifier = Modifier) {
    val p = s.packet
    CardBox(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Lbl("Throttle"); Text("${p?.throttlePct ?: 0}%", color = Green, fontSize = 13.sp)
        }
        Bar((p?.throttlePct ?: 0) / 100f, Color(0xFF166534))
        InputTrace(s.throttleTrace, Green, Modifier.fillMaxWidth().height(28.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Lbl("Brake"); Text("${p?.brakePct ?: 0}%", color = Muted, fontSize = 13.sp)
        }
        Bar((p?.brakePct ?: 0) / 100f, Red)
        InputTrace(s.brakeTrace, Red, Modifier.fillMaxWidth().height(28.dp))
    }
}

@Composable fun DeltaCard(s: DashState, modifier: Modifier = Modifier, fill: Boolean = false) {
    CardBox(modifier) {
        Lbl("Delta vs session best")
        val d = s.liveDelta
        Text(
            formatDelta(d),
            color = if (d != null && d < 0) Green else if (d != null) Red else Text,
            fontSize = 34.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
        )
        if (fill) Spacer(Modifier.weight(1f)) else Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("LAST ${formatLap(s.lastMs)}", color = Muted, fontSize = 12.sp)
            Text("BEST ${formatLap(s.bestMs)}", color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable fun DrivingFuelCard(s: DashState, modifier: Modifier = Modifier) {
    CardBox(modifier) {
        Lbl("Fuel — this session")
        Text("%.0f%%".format(s.fuelPct), color = Text, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
        Bar((s.fuelPct / 100.0).toFloat(), Amber)
        Text(s.fuelPerLap?.let { "%.1f%%/lap".format(it) } ?: "—%/lap", color = Text, fontSize = 13.sp)
        Text(
            "%s laps · %s".format(
                s.lapsRemaining?.let { "%.1f".format(it) } ?: "—",
                if (s.stops == 1) "1 stop" else "${s.stops} stops",
            ),
            color = Muted,
            fontSize = 12.sp,
        )
    }
}

@Composable fun DrivingView(s: DashState) {
    val p = s.packet
    val land = isLandscape()
    if (land) {
        // Max 2 columns: Gear+Throttle | Delta / Fuel+Tires
        Row(Modifier.fillMaxSize().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                GearSpeedCard(p, Modifier.fillMaxWidth())
                ThrottleBrakeCard(s, Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DeltaCard(s, Modifier.fillMaxWidth())
                DrivingFuelCard(s, Modifier.fillMaxWidth())
                CardBox(Modifier.fillMaxWidth().height(200.dp)) {
                    Lbl("Tire temps")
                    TireGrid(p, Modifier.weight(1f))
                }
            }
        }
    } else {
        // Portrait: always 1 column, stacked order from design lock
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GearSpeedCard(p, Modifier.fillMaxWidth())
            ThrottleBrakeCard(s, Modifier.fillMaxWidth())
            DeltaCard(s, Modifier.fillMaxWidth())
            DrivingFuelCard(s, Modifier.fillMaxWidth())
            CardBox(Modifier.fillMaxWidth().height(240.dp)) {
                Lbl("Tire temps")
                TireGrid(p, Modifier.weight(1f))
            }
        }
    }
}

@Composable fun RpmBar(frac: Float) {
    Row(Modifier.fillMaxWidth().height(10.dp).padding(bottom = 8.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        val n = 10
        val on = (frac * n).toInt()
        repeat(n) { i ->
            val c = when {
                i >= on -> Color(0xFF1B2438)
                i < 4 -> Green
                i < 6 -> Color(0xFFEAB308)
                i < 8 -> Amber
                else -> Red
            }
            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(2.dp)).background(c))
        }
    }
}

@Composable fun PitWallView(s: DashState) {
    val p = s.packet
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CardBox(Modifier.fillMaxWidth()) {
            Lbl("Delta vs session best")
            val d = s.liveDelta
            Text(
                formatDelta(d),
                color = if (d != null && d < 0) Green else if (d != null) Red else Text,
                fontSize = 36.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(Modifier.height(6.dp))
            DeltaTrace(s.deltaTrace, Modifier.fillMaxWidth().height(80.dp))
        }
        CardBox(Modifier.fillMaxWidth()) {
            Lbl("Gear / speed")
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) { Lbl("Gear"); Text(p?.gearDisplay ?: "N", color = Text, fontSize = 36.sp, fontWeight = FontWeight.SemiBold) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Lbl("Speed"); Text("%d km/h".format((p?.speedKph ?: 0.0).toInt()), color = Text, fontSize = 36.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
            }
            RpmBar(p?.rpmFrac ?: 0f)
        }
        CardBox(Modifier.fillMaxWidth()) {
            Lbl("This session")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column { Lbl("Last"); Text(formatLap(s.lastMs), color = Cyan, fontSize = 13.sp) }
                Column { Lbl("Best"); Text(formatLap(s.bestMs), color = Green, fontSize = 13.sp) }
                Column { Lbl("Laps in memory"); Text("${s.lapsInMemory}", color = Text, fontSize = 13.sp) }
            }
        }
        CardBox(Modifier.fillMaxWidth().height(180.dp)) { Lbl("Tire temps"); TireGrid(p, Modifier.weight(1f)) }
        CardBox(Modifier.fillMaxWidth()) {
            Lbl("Fuel — this session")
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("%.0f%%".format(s.fuelPct), color = Amber, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Bar((s.fuelPct / 100.0).toFloat(), Amber)
                    Text(
                        "%s%%/lap · %s laps · %s".format(
                            s.fuelPerLap?.let { "%.1f".format(it) } ?: "—",
                            s.lapsRemaining?.let { "%.1f".format(it) } ?: "—",
                            if (s.stops == 1) "1 stop" else "${s.stops} stops",
                        ),
                        color = Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
        CardBox(Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Lbl("This session — last 100 laps")
                Text("BEST ${formatLap(s.bestMs)}", color = Green, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text("LAP", color = Muted, fontSize = 10.sp, modifier = Modifier.width(36.dp))
                Text("TIME", color = Muted, fontSize = 10.sp, modifier = Modifier.weight(1f))
                Text("DELTA", color = Muted, fontSize = 10.sp, modifier = Modifier.width(72.dp))
            }
            s.laps.forEach { row ->
                val best = s.bestMs
                val d = if (best != null) (row.timeMs - best) / 1000.0 else null
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        .then(if (row.isBest) Modifier.background(Color(0xFF10261A)).border(1.dp, Color(0xFF166534), RoundedCornerShape(4.dp)) else Modifier)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("L${row.lap}", color = Text, fontSize = 13.sp, modifier = Modifier.width(36.dp))
                    Text(formatLap(row.timeMs), color = Text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(if (row.isBest) "BEST" else formatDelta(d), color = if (row.isBest) Green else if ((d ?: 0.0) > 0.4) Red else Amber, fontSize = 13.sp, modifier = Modifier.width(72.dp))
                }
            }
        }
    }
}

@Composable fun DeltaTrace(samples: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.background(TireBg, RoundedCornerShape(4.dp))) {
        if (samples.size < 2) return@Canvas
        val mid = size.height / 2
        val maxAbs = samples.maxOf { kotlin.math.abs(it) }.coerceAtLeast(0.2f)
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
            val y = mid - (v / maxAbs) * (size.height * 0.4f)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawLine(Line, Offset(0f, mid), Offset(size.width, mid), 2f)
        drawPath(path, Cyan, style = Stroke(width = 3f, cap = StrokeCap.Round))
    }
}

@Composable fun InputTrace(samples: List<Float>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier.clip(RoundedCornerShape(4.dp)).background(TireBg, RoundedCornerShape(4.dp))) {
        if (samples.size < 2) return@Canvas
        val path = Path()
        samples.forEachIndexed { i, v ->
            val x = size.width * i / (samples.size - 1).coerceAtLeast(1)
            val y = size.height * (1f - v.coerceIn(0f, 1f))
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 1.5f, cap = StrokeCap.Round))
    }
}

@Composable fun SettingsSheet(s: DashState, vm: DashViewModel) {
    Box(Modifier.fillMaxSize().background(Color(0xB80B1220))) {
        Column(
            Modifier.fillMaxHeight().fillMaxWidth(0.42f).align(Alignment.CenterEnd)
                .safeDrawingPadding()
                .background(Page).border(1.dp, Cyan).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Lbl("Settings")
                Text("Done", color = Muted, modifier = Modifier.clickable { vm.settings = false })
            }
            Lbl("PlayStation on this Wi-Fi")
            Box(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(Cyan).clickable { vm.findPs5() }
                    .padding(vertical = 11.dp),
                contentAlignment = Alignment.Center
            ) { Text("Find PS5", color = Color(0xFF08222A), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Text("Sends a heartbeat on the LAN and connects when GT7 answers.", color = Muted, fontSize = 12.sp)
            CardBox(Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(s.peer ?: "—", color = Text, fontSize = 13.sp)
                    Box(Modifier.clip(CircleShape).background(LiveBg).padding(horizontal = 8.dp, vertical = 2.dp)) {
                        Text(if (s.peer != null) "Connected" else "Idle", color = Green, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Bar(if (s.peer != null) qualityFrac(s.quality) else 0f, Cyan)
                Text("${s.quality.name} · rx ${s.rx}   dec ${s.dec}   err ${s.err}", color = Muted, fontSize = 12.sp)
            }
            Box(
                Modifier.fillMaxWidth().border(1.dp, Cyan, RoundedCornerShape(8.dp)).clickable { vm.showIp = true }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) { Text("Enter IP manually", color = Cyan, fontSize = 13.sp) }
            if (vm.showIp) {
                OutlinedTextField(
                    vm.manualIp, { vm.manualIp = it },
                    label = { Text("PS5 IPv4") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Text, unfocusedTextColor = Text)
                )
                Text("Connect", color = Cyan, modifier = Modifier.clickable {
                    vm.connectIp(vm.manualIp); vm.showIp = false
                })
            }
            if (BuildConfig.DEBUG) {
                Text(
                    "Send Sentry test",
                    color = Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { SlickDashApp.captureTestError() }
                )
            }
        }
    }
}
