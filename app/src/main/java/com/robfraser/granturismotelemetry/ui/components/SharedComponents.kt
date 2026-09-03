package com.robfraser.granturismotelemetry.ui.components

import android.os.Build

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.telemetry.ConnectionState
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry

val CardShape = RoundedCornerShape(12.dp)
val WellShape = RoundedCornerShape(8.dp)

@Composable
fun GTCard(
    modifier: Modifier = Modifier,
    padding: Dp = 16.dp,
    fill: Color = GTColors.card,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(fill)
            .padding(padding),
        content = content,
    )
}

@Composable
fun CapsLabel(
    text: String,
    color: Color = GTColors.muted,
    size: androidx.compose.ui.unit.TextUnit = 9.sp,
) {
    Text(
        text = text.uppercase(),
        style = GTType.caps(size).copy(color = color),
    )
}

@Composable
fun LivePill(live: Boolean, modifier: Modifier = Modifier) {
    Text(
        text = "LIVE",
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(if (live) GTColors.green else GTColors.inset)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        fontSize = 9.sp,
        fontWeight = FontWeight.Bold,
        color = if (live) GTColors.liveInk else GTColors.muted,
    )
}

@Composable
fun ProBadge(locked: Boolean = true, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(WellShape)
            .background(GTColors.proFill)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (locked) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = GTColors.amber,
                modifier = Modifier.size(10.dp),
            )
        }
        Text(
            text = "PRO",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.6.sp,
            color = GTColors.amber,
        )
    }
}

@Composable
fun StatusDot(state: ConnectionState) {
    val color = when (state) {
        ConnectionState.Live -> GTColors.green
        ConnectionState.Waiting -> GTColors.amber
        is ConnectionState.Error -> GTColors.red
        ConnectionState.Idle -> GTColors.muted
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(text = state.label, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

@Composable
fun FuelBar(
    percent: Double,
    laps: Double,
    compact: Boolean = false,
    barColor: Color = GTColors.amber,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CapsLabel("FUEL")
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (compact) 10.dp else 12.dp)
                .clip(RoundedCornerShape(50))
                .background(GTColors.inset),
        ) {
            val frac = (percent / 100.0).coerceIn(0.0, 1.0).toFloat()
            Box(
                modifier = Modifier
                    .fillMaxWidth(frac.coerceAtLeast(0.04f))
                    .height(if (compact) 10.dp else 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(barColor),
            )
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "${percent.toInt()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GTColors.text,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = String.format(java.util.Locale.US, "%.1f laps", laps),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
    }
}

@Composable
fun RPMStrip(fraction: Double, count: Int = 10, modifier: Modifier = Modifier) {
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        repeat(count) { i ->
            val on = fraction > i.toDouble() / count.toDouble()
            val color = when {
                i >= count - 1 -> GTColors.red
                i >= count - 4 -> GTColors.green
                else -> GTColors.cyan
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (on) color else GTColors.inset),
            )
        }
    }
}

@Composable
fun TireTempCard(
    label: String,
    temp: Float,
    showBar: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val color = GTColors.tireColor(temp)
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(GTColors.card)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CapsLabel(label)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${temp.toInt()}°",
                style = GTType.numeric(if (showBar) 26.sp else 22.sp),
                color = color,
            )
            if (showBar) {
                Spacer(Modifier.width(8.dp))
                val frac = ((temp - 60f) / 60f).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(GTColors.inset),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(frac)
                            .height(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
fun LockedOverlay(title: String = "Pro", modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(CardShape)
            .background(GTColors.page.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ProBadge(locked = true)
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = GTColors.muted)
        }
    }
}

@Composable
fun Chip(text: String, tint: Color = GTColors.cyan, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(GTColors.chip)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = tint,
    )
}

@Composable
fun ConnectPanel(modifier: Modifier = Modifier) {
    val settings = LocalSettings.current
    val telemetry = LocalTelemetry.current
    GTCard(modifier = modifier) {
        Text("Connect to PS5", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
        Spacer(Modifier.height(10.dp))
        CapsLabel("CONSOLE IP")
        Spacer(Modifier.height(6.dp))
        BasicTextField(
            value = settings.ps5IP,
            onValueChange = { settings.updatePs5IP(it) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.text,
            ),
            cursorBrush = SolidColor(GTColors.cyan),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .clip(WellShape)
                .background(GTColors.page)
                .border(1.dp, GTColors.fieldBorder, WellShape)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        )
        Spacer(Modifier.height(10.dp))
        if (settings.useSimulator) {
            Text(
                "Simulator is on. Connect switches to live UDP against this IP.",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.amber,
            )
            Spacer(Modifier.height(8.dp))
        }
        TextButton(
            onClick = {
                settings.updateUseSimulator(false)
                telemetry.connect(settings)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .clip(WellShape)
                .background(GTColors.cyan),
        ) {
            Text(
                text = when {
                    telemetry.state.isLive && !settings.useSimulator -> "Connected"
                    telemetry.state is ConnectionState.Waiting && !settings.useSimulator -> "Connecting…"
                    else -> "Connect"
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = GTColors.connectInk,
            )
        }
        Spacer(Modifier.height(10.dp))
        StatusDot(telemetry.state)
        if (!settings.useSimulator && telemetry.state is ConnectionState.Waiting) {
            Spacer(Modifier.height(6.dp))
            val onEmulator = Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                Build.MODEL.contains("sdk", ignoreCase = true)
            Text(
                if (onEmulator)
                    "This emulator cannot see your PS5. Run on a real phone on the same Wi‑Fi, with GT7 in a car."
                else
                    "Waiting for packets. Sit in a car in GT7 (menus do not stream).",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
    }
}
