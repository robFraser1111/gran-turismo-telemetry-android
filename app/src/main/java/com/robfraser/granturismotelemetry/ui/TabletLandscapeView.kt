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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.Formatters
import com.robfraser.granturismotelemetry.models.SampleSessions
import com.robfraser.granturismotelemetry.models.SessionRecord
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry
import com.robfraser.granturismotelemetry.ui.components.CapsLabel
import com.robfraser.granturismotelemetry.ui.components.CardShape
import com.robfraser.granturismotelemetry.ui.components.Chip
import com.robfraser.granturismotelemetry.ui.components.DeltaSparklineCard
import com.robfraser.granturismotelemetry.ui.components.GTCard
import com.robfraser.granturismotelemetry.ui.components.LivePill
import com.robfraser.granturismotelemetry.ui.components.LockedOverlay
import com.robfraser.granturismotelemetry.ui.components.ThrottleBrakeCard
import com.robfraser.granturismotelemetry.ui.components.TireTempCard
import com.robfraser.granturismotelemetry.ui.components.TrackMapView
import com.robfraser.granturismotelemetry.ui.components.WellShape

@Composable
fun TabletLandscapeView() {
    val settings = LocalSettings.current
    val telemetry = LocalTelemetry.current
    val pkt = telemetry.packet
    val session = SampleSessions.all.first()
    val pro = settings.isPro

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GTColors.page),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(GTColors.header)
                .padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            LivePill(telemetry.state.isLive)
            Text(
                "Pit Wall - ${pkt.trackName}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GTColors.text,
                modifier = Modifier.weight(1f),
            )
            Chip(pkt.carClass)
            Chip("Lap ${maxOf(pkt.currentLap, 1)}", tint = GTColors.text)
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                TrackMapView(
                    progress = pkt.lapProgress,
                    locked = !pro,
                    showSectors = true,
                    sectors = session.sectors,
                    footer = "SECTOR / TRACK LIMITS OK",
                    modifier = Modifier.weight(1f),
                )
                DeltaSparklineCard(
                    delta = pkt.liveDeltaSeconds,
                    values = telemetry.deltaTrace.toList(),
                    locked = !pro,
                    modifier = Modifier.height(220.dp),
                )
            }

            Column(
                modifier = Modifier.width(520.dp).fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(140.dp)
                            .clip(CardShape)
                            .background(GTColors.card)
                            .padding(horizontal = 20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pkt.gearDisplay, style = GTType.numeric(84.sp), color = GTColors.text)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("${pkt.speedKph.toInt()}", style = GTType.numeric(34.sp), color = GTColors.cyan)
                            Text("km/h", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = GTColors.muted)
                        }
                    }
                    GTCard(modifier = Modifier.weight(1f).height(140.dp)) {
                        CapsLabel("FUEL")
                        Spacer(Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GTColors.inset),
                        ) {
                            val frac = (pkt.fuelPercent / 100.0).coerceIn(0.0, 1.0).toFloat()
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(frac)
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(GTColors.amber),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            String.format(java.util.Locale.US, "%.0f%%  ·  %.1f laps", pkt.fuelPercent, pkt.fuelLapsRemaining),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = GTColors.green,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "WINDOW OPEN",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = GTColors.green,
                            modifier = Modifier
                                .clip(WellShape)
                                .background(GTColors.windowFill)
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
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

                LapsTable(session, pro, modifier = Modifier.weight(1f))

                ThrottleBrakeCard(
                    throttle = telemetry.throttleTrace.toList(),
                    brake = telemetry.brakeTrace.toList(),
                    compact = true,
                    modifier = Modifier.height(140.dp),
                )
            }
        }
    }
}

@Composable
private fun LapsTable(session: SessionRecord, pro: Boolean, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShape)
                .background(GTColors.card)
                .then(if (pro) Modifier else Modifier.blur(2.dp))
                .padding(16.dp),
        ) {
            CapsLabel("LAPS")
            Spacer(Modifier.height(6.dp))
            session.lapRows.forEach { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (row.number % 2 == 1) GTColors.well else androidx.compose.ui.graphics.Color.Transparent)
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Lap ${row.number}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GTColors.text,
                        modifier = Modifier.width(64.dp),
                    )
                    Text(
                        Formatters.lapTime(row.timeMs),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = GTColors.text,
                    )
                    Spacer(Modifier.weight(1f))
                    if (row.isBest) {
                        Text("BEST", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GTColors.cyan)
                    } else if (row.deltaSeconds != null) {
                        Text(
                            Formatters.delta(row.deltaSeconds),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = GTColors.deltaColor(row.deltaSeconds),
                        )
                    }
                }
            }
        }
        if (!pro) {
            LockedOverlay("Session history is Pro", modifier = Modifier.fillMaxSize())
        }
    }
}
