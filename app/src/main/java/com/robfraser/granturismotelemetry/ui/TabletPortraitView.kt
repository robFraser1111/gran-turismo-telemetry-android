package com.robfraser.granturismotelemetry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.Formatters
import com.robfraser.granturismotelemetry.models.LayoutPreset
import com.robfraser.granturismotelemetry.models.SampleSessions
import com.robfraser.granturismotelemetry.models.SessionRecord
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry
import com.robfraser.granturismotelemetry.ui.components.CapsLabel
import com.robfraser.granturismotelemetry.ui.components.CardShape
import com.robfraser.granturismotelemetry.ui.components.Chip
import com.robfraser.granturismotelemetry.ui.components.ConnectPanel
import com.robfraser.granturismotelemetry.ui.components.FuelBar
import com.robfraser.granturismotelemetry.ui.components.GTCard
import com.robfraser.granturismotelemetry.ui.components.LivePill
import com.robfraser.granturismotelemetry.ui.components.LockedOverlay
import com.robfraser.granturismotelemetry.ui.components.ProBadge
import com.robfraser.granturismotelemetry.ui.components.RPMStrip
import com.robfraser.granturismotelemetry.ui.components.SectorBar
import com.robfraser.granturismotelemetry.ui.components.TireTempCard
import com.robfraser.granturismotelemetry.ui.components.TraceLine
import com.robfraser.granturismotelemetry.ui.components.WellShape
import com.robfraser.granturismotelemetry.ui.components.ZeroLine

enum class TabletTab(val label: String) {
    Live("Live"),
    Sessions("Sessions"),
    Layouts("Layouts"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TabletPortraitView() {
    val settings = LocalSettings.current
    var tab by remember { mutableStateOf(TabletTab.Sessions) }
    var trackFilter by remember { mutableStateOf("Track") }
    var carFilter by remember { mutableStateOf("Car") }
    var selected by remember { mutableStateOf(SampleSessions.all.first()) }
    var showDebug by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GTColors.page)
            .statusBarsPadding(),
    ) {
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            when (tab) {
                TabletTab.Live -> LiveTab(onDebug = { showDebug = true })
                TabletTab.Sessions -> SessionsTab(
                    isPro = settings.isPro,
                    trackFilter = trackFilter,
                    carFilter = carFilter,
                    selected = selected,
                    onTrack = { trackFilter = it },
                    onCar = { carFilter = it },
                    onSelect = { selected = it },
                )
                TabletTab.Layouts -> LayoutsTab()
            }
        }
        TabBar(tab = tab, onTab = { tab = it })
    }

    if (showDebug) {
        ModalBottomSheet(
            onDismissRequest = { showDebug = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GTColors.card,
        ) {
            DebugSheet(onDone = { showDebug = false })
        }
    }
}

@Composable
private fun TabBar(tab: TabletTab, onTab: (TabletTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(GTColors.header)
            .navigationBarsPadding()
            .padding(top = 12.dp, bottom = 16.dp),
    ) {
        TabletTab.entries.forEach { item ->
            val on = tab == item
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onTab(item) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = item.label,
                    fontSize = 13.sp,
                    fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (on) GTColors.cyan else GTColors.muted,
                )
                Box(
                    modifier = Modifier
                        .width(114.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (on) GTColors.cyan else androidx.compose.ui.graphics.Color.Transparent),
                )
            }
        }
    }
}

@Composable
private fun SessionsTab(
    isPro: Boolean,
    trackFilter: String,
    carFilter: String,
    selected: SessionRecord,
    onTrack: (String) -> Unit,
    onCar: (String) -> Unit,
    onSelect: (SessionRecord) -> Unit,
) {
    val tracks = listOf("Track") + SampleSessions.all.map { it.track }.distinct().sorted()
    val cars = listOf("Car") + SampleSessions.all.map { it.carClass }.distinct().sorted()
    val filtered = SampleSessions.all.filter { s ->
        (trackFilter == "Track" || s.track == trackFilter) &&
            (carFilter == "Car" || s.carClass == carFilter)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GTColors.header)
                .padding(horizontal = 32.dp, vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Sessions", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
            Spacer(Modifier.weight(1f))
            FilterChip(trackFilter, tracks, onTrack)
            Spacer(Modifier.width(8.dp))
            FilterChip(carFilter, cars, onCar)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!isPro) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 8.dp)) {
                    Text(
                        "Session history is a Pro feature.",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = GTColors.muted,
                        modifier = Modifier.weight(1f),
                    )
                    ProBadge()
                }
            }
            filtered.forEach { session ->
                SessionRow(session, enabled = isPro, onCompare = { onSelect(session) })
            }
            Box {
                CompareCard(
                    session = selected,
                    modifier = Modifier.then(if (isPro) Modifier else Modifier),
                )
                if (!isPro) {
                    LockedOverlay("Compare is Pro", modifier = Modifier.matchParentSize())
                }
            }
        }
    }
}

@Composable
private fun FilterChip(title: String, options: List<String>, onPick: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val label = if (title == "Track" || title == "Car") "$title ▾" else title
    Box {
        Text(
            text = label,
            modifier = Modifier
                .clip(WellShape)
                .background(GTColors.chip)
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 8.dp),
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = GTColors.text,
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onPick(opt)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun SessionRow(session: SessionRecord, enabled: Boolean, onCompare: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(CardShape)
            .background(GTColors.card)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.track, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
            Text(
                "${session.carClass} · ${session.laps} laps",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(session.bestLapLabel, style = GTType.numeric(20.sp), color = GTColors.cyan)
            Text(session.whenLabel, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = GTColors.muted)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = "Compare",
            modifier = Modifier
                .clip(WellShape)
                .background(GTColors.chip)
                .clickable(enabled = enabled, onClick = onCompare)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = GTColors.cyan,
        )
    }
}

@Composable
fun CompareCard(session: SessionRecord, modifier: Modifier = Modifier) {
    val delta = (session.lastLapMs - session.bestLapMs) / 1000.0
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(GTColors.card)
            .border(1.dp, GTColors.cyan.copy(alpha = 0.35f), CardShape)
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${session.track} — compare vs Best",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = GTColors.text,
                )
                Text(
                    "Lap ${session.laps} (${Formatters.lapTime(session.lastLapMs)}) vs Best (${session.bestLapLabel})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = GTColors.muted,
                )
            }
            Text(
                Formatters.delta(delta),
                style = GTType.numeric(26.sp),
                color = GTColors.deltaColor(delta),
            )
        }
        session.sectors.forEach { SectorBar(it) }
        CapsLabel("DELTA TRACE")
        Box(modifier = Modifier.fillMaxWidth().height(72.dp)) {
            ZeroLine()
            val mapped = (if (session.deltaTrace.isEmpty()) listOf(0.0, 0.0) else session.deltaTrace).map { 0.5 - it }
            TraceLine(values = mapped, color = GTColors.red, lineWidth = 3f, range = 0.0..1.0, modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun LiveTab(onDebug: () -> Unit) {
    val telemetry = LocalTelemetry.current
    val pkt = telemetry.packet
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("GRAN TELEMETRY", fontSize = 22.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = GTColors.text)
                Text("PS5 telemetry companion", fontSize = 12.sp, color = GTColors.muted)
            }
            LivePill(telemetry.state.isLive)
            Spacer(Modifier.width(12.dp))
            Icon(
                Icons.Filled.Settings,
                contentDescription = "Debug settings",
                tint = GTColors.muted,
                modifier = Modifier.clickable(onClick = onDebug),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.width(420.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConnectPanel()
                GTCard { FuelBar(pkt.fuelPercent, pkt.fuelLapsRemaining) }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(GTColors.card)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(pkt.gearDisplay, style = GTType.numeric(96.sp), color = GTColors.text)
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("${pkt.speedKph.toInt()} km/h", style = GTType.numeric(28.sp), color = GTColors.cyan)
                        Text(pkt.trackName, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = GTColors.muted)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TireTempCard("FL", pkt.tireTempFL, showBar = true, modifier = Modifier.weight(1f))
                    TireTempCard("FR", pkt.tireTempFR, showBar = true, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TireTempCard("RL", pkt.tireTempRL, showBar = true, modifier = Modifier.weight(1f))
                    TireTempCard("RR", pkt.tireTempRR, showBar = true, modifier = Modifier.weight(1f))
                }
                RPMStrip(pkt.rpmFraction)
            }
        }
    }
}

@Composable
private fun LayoutsTab() {
    val settings = LocalSettings.current
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Layouts", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
        CapsLabel("PRESETS")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LayoutPreset.entries.forEach { preset ->
                val on = settings.preset == preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (on) GTColors.cyan else GTColors.card)
                        .clickable { settings.updatePreset(preset) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        preset.label,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) GTColors.connectInk else GTColors.text,
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(GTColors.card)
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom layout", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = GTColors.text)
                Text("Reorder widgets, save profiles", fontSize = 12.sp, color = GTColors.muted)
            }
            if (settings.isPro) Chip("Unlocked") else ProBadge()
        }
    }
}
