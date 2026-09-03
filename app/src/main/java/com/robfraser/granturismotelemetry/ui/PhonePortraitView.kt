package com.robfraser.granturismotelemetry.ui

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
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
import com.robfraser.granturismotelemetry.models.LayoutPreset
import com.robfraser.granturismotelemetry.models.SampleSessions
import com.robfraser.granturismotelemetry.models.SessionRecord
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry
import com.robfraser.granturismotelemetry.ui.components.CapsLabel
import com.robfraser.granturismotelemetry.ui.components.CardShape
import com.robfraser.granturismotelemetry.ui.components.ConnectPanel
import com.robfraser.granturismotelemetry.ui.components.GTCard
import com.robfraser.granturismotelemetry.ui.components.ProBadge
import com.robfraser.granturismotelemetry.ui.components.WellShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhonePortraitView() {
    val settings = LocalSettings.current
    var showDebug by remember { mutableStateOf(false) }
    var showSession by remember { mutableStateOf(false) }
    val last = SampleSessions.all.first()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GTColors.page)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 20.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(onDebug = { showDebug = true })
            ConnectPanel()
            LastSessionCard(
                session = last,
                isPro = settings.isPro,
                onOpen = {
                    if (settings.isPro) showSession = true else showDebug = true
                },
            )
            PresetsRow()
            CustomLayoutRow(isPro = settings.isPro)
            SessionHistoryCard(isPro = settings.isPro)
        }
        Text(
            text = "Hold landscape to drive",
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = GTColors.muted,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
    if (showSession) {
        ModalBottomSheet(
            onDismissRequest = { showSession = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = GTColors.page,
        ) {
            Column(Modifier.padding(16.dp).navigationBarsPadding()) {
                Text("Session", fontWeight = FontWeight.Bold, color = GTColors.text, fontSize = 18.sp)
                Spacer(Modifier.height(12.dp))
                CompareCard(session = last)
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Header(onDebug: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "GRAN TELEMETRY",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 3.sp,
                color = GTColors.text,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "PS5 telemetry companion",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = "Debug settings",
            tint = GTColors.muted,
            modifier = Modifier
                .size(32.dp)
                .clickable(onClick = onDebug)
                .padding(6.dp),
        )
    }
}

@Composable
private fun LastSessionCard(session: SessionRecord, isPro: Boolean, onOpen: () -> Unit) {
    Box {
        GTCard(modifier = Modifier.then(if (isPro) Modifier else Modifier)) {
            Text("Last session", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "${shortTrack(session.track)} - ${session.carClass}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(WellShape)
                        .background(GTColors.well)
                        .padding(12.dp),
                ) {
                    CapsLabel("BEST LAP")
                    Spacer(Modifier.height(4.dp))
                    Text(session.bestLapLabel, style = GTType.numeric(17.sp), color = GTColors.cyan)
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(WellShape)
                        .background(GTColors.well)
                        .padding(12.dp),
                ) {
                    CapsLabel("LAPS")
                    Spacer(Modifier.height(4.dp))
                    Text("${session.laps}", style = GTType.numeric(17.sp), color = GTColors.text)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.clickable(onClick = onOpen),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Open session",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isPro) GTColors.cyan else GTColors.muted,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = if (isPro) GTColors.cyan else GTColors.muted,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
        if (!isPro) {
            ProBadge(modifier = Modifier.align(Alignment.TopEnd).padding(12.dp))
        }
    }
}

@Composable
private fun PresetsRow() {
    val settings = LocalSettings.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CapsLabel("PRESETS")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LayoutPreset.entries.forEach { preset ->
                val on = settings.preset == preset
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(WellShape)
                        .background(if (on) GTColors.cyan else GTColors.presetOff)
                        .clickable { settings.updatePreset(preset) },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = preset.label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (on) GTColors.connectInk else GTColors.text,
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomLayoutRow(isPro: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(CardShape)
            .background(GTColors.card)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "Custom layout",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isPro) GTColors.text else GTColors.text.copy(alpha = 0.7f),
            )
            Text(
                "Reorder widgets, save profiles",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
        if (isPro) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = GTColors.muted,
            )
        } else {
            ProBadge()
        }
    }
}

@Composable
private fun SessionHistoryCard(isPro: Boolean) {
    GTCard(padding = 16.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Session history", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = GTColors.text)
            Spacer(Modifier.weight(1f))
            if (!isPro) ProBadge()
        }
        Spacer(Modifier.height(10.dp))
        SampleSessions.all.take(3).forEach { s ->
            Text(
                text = "${shortTrack(s.track)} - ${s.bestLapLabel} - ${s.laps} laps",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
    }
}

internal fun shortTrack(name: String): String =
    name.replace(" Raceway", "").replace(" Circuit", "")
