package com.robfraser.granturismotelemetry.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.Formatters
import com.robfraser.granturismotelemetry.models.LayoutPreset
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry
import com.robfraser.granturismotelemetry.ui.components.CapsLabel
import com.robfraser.granturismotelemetry.ui.components.CardShape
import com.robfraser.granturismotelemetry.ui.components.Chip
import com.robfraser.granturismotelemetry.ui.components.FuelBar
import com.robfraser.granturismotelemetry.ui.components.GTCard
import com.robfraser.granturismotelemetry.ui.components.LivePill
import com.robfraser.granturismotelemetry.ui.components.LockedOverlay
import com.robfraser.granturismotelemetry.ui.components.RPMStrip
import com.robfraser.granturismotelemetry.ui.components.ThrottleBrakeCard
import com.robfraser.granturismotelemetry.ui.components.TireTempCard
import com.robfraser.granturismotelemetry.ui.components.TrackMapView

@Composable
fun PhoneLandscapeView() {
    val settings = LocalSettings.current
    val telemetry = LocalTelemetry.current
    val pkt = telemetry.packet
    val pro = settings.isPro

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GTColors.page)
            .padding(top = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GTColors.header)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LivePill(live = telemetry.state.isLive)
            Text(
                text = pkt.trackName,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = GTColors.text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Chip("${settings.preset.label} - ${if (pro) "Pro" else "Free"}")
        }

        RPMStrip(fraction = pkt.rpmFraction, modifier = Modifier.padding(horizontal = 20.dp))

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 20.dp)
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(CardShape)
                        .background(GTColors.card)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = pkt.gearDisplay,
                        style = GTType.numeric(if (settings.preset == LayoutPreset.Minimal) 110.sp else 88.sp),
                        color = GTColors.text,
                        maxLines = 1,
                    )
                    Text(
                        text = "GEAR",
                        style = GTType.caps(10.sp),
                        letterSpacing = 2.sp,
                        color = GTColors.muted,
                    )
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${pkt.speedKph.toInt()}",
                            style = GTType.numeric(28.sp),
                            color = GTColors.cyan,
                        )
                        Text(
                            text = " km/h",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = GTColors.muted,
                            modifier = Modifier.padding(bottom = 4.dp),
                        )
                    }
                }
                GTCard(padding = 14.dp) {
                    FuelBar(percent = pkt.fuelPercent, laps = pkt.fuelLapsRemaining, compact = true)
                }
            }

            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Box(modifier = Modifier.height(150.dp).fillMaxWidth()) {
                    GTCard(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(if (pro) Modifier else Modifier.blur(2.dp)),
                    ) {
                        CapsLabel("DELTA")
                        Text(
                            text = Formatters.delta(pkt.liveDeltaSeconds, showPlus = false),
                            style = GTType.numeric(44.sp),
                            color = GTColors.deltaColor(pkt.liveDeltaSeconds),
                            maxLines = 1,
                        )
                        Text(
                            "LAST ${Formatters.lapTime(pkt.lastLapMs)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GTColors.muted,
                        )
                        Text(
                            "BEST ${Formatters.lapTime(pkt.bestLapMs)}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = GTColors.cyan,
                        )
                    }
                    if (!pro) {
                        LockedOverlay("Lap delta is Pro", modifier = Modifier.fillMaxSize())
                    }
                }
                if (settings.preset != LayoutPreset.Minimal) {
                    ThrottleBrakeCard(
                        throttle = telemetry.throttleTrace.toList(),
                        brake = telemetry.brakeTrace.toList(),
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    GTCard(modifier = Modifier.weight(1f)) {
                        CapsLabel("RPM")
                        Text(
                            "${pkt.engineRpm.toInt()}",
                            style = GTType.numeric(36.sp),
                            color = GTColors.text,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.width(200.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TireTempCard("FL", pkt.tireTempFL, modifier = Modifier.weight(1f))
                    TireTempCard("FR", pkt.tireTempFR, modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TireTempCard("RL", pkt.tireTempRL, modifier = Modifier.weight(1f))
                    TireTempCard("RR", pkt.tireTempRR, modifier = Modifier.weight(1f))
                }
                if (settings.preset != LayoutPreset.Minimal) {
                    TrackMapView(
                        progress = pkt.lapProgress,
                        locked = !pro,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
