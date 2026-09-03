package com.robfraser.granturismotelemetry.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.LocalSettings
import com.robfraser.granturismotelemetry.theme.LocalTelemetry

@Composable
fun DebugSheet(onDone: () -> Unit) {
    val settings = LocalSettings.current
    val telemetry = LocalTelemetry.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .navigationBarsPadding()
            .padding(bottom = 24.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Debug", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = GTColors.text)
            Spacer(Modifier.weight(1f))
            TextButton(onClick = onDone) {
                Text("Done", color = GTColors.cyan, fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text("SOURCE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GTColors.muted, letterSpacing = 1.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Simulator telemetry", color = GTColors.text, fontWeight = FontWeight.Medium)
                Text(
                    "Simulator is the default. Turn it off and tap Connect on the home screen to talk to a PS5.",
                    fontSize = 12.sp,
                    color = GTColors.muted,
                )
            }
            Switch(
                checked = settings.useSimulator,
                onCheckedChange = {
                    settings.updateUseSimulator(it)
                    telemetry.connect(settings)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GTColors.connectInk,
                    checkedTrackColor = GTColors.cyan,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("PRO (DEBUG)", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GTColors.muted, letterSpacing = 1.5.sp)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Unlock Pro features", color = GTColors.text, fontWeight = FontWeight.Medium)
                Text(
                    "Free always: tire temps, fuel. Pro: custom layouts, live track map, lap delta, session history. No tire wear.",
                    fontSize = 12.sp,
                    color = GTColors.muted,
                )
            }
            Switch(
                checked = settings.isPro,
                onCheckedChange = { settings.updateIsPro(it) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = GTColors.connectInk,
                    checkedTrackColor = GTColors.amber,
                ),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text("DIAGNOSTICS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = GTColors.muted, letterSpacing = 1.5.sp)
        Spacer(Modifier.height(8.dp))
        DiagRow("Decoded", "${telemetry.decodedPackets}")
        DiagRow("Raw UDP", "${telemetry.rawPackets}")
        DiagRow("Decode fails", "${telemetry.decodeFailures}")
        telemetry.lastDecodeError?.let {
            Text(it, fontSize = 12.sp, color = GTColors.muted, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
private fun DiagRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(label, color = GTColors.muted, fontSize = 13.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = GTColors.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}
