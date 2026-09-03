package com.robfraser.granturismotelemetry.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.AppSettings
import com.robfraser.granturismotelemetry.telemetry.TelemetryService

object GTColors {
    val page = Color(0xFF0B1220)
    val header = Color(0xFF0E1626)
    val card = Color(0xFF111A2B)
    val cardAlt = Color(0xFF121A2B)
    val well = Color(0xFF0F172A)
    val inset = Color(0xFF1B2436)
    val cyan = Color(0xFF22D3EE)
    val green = Color(0xFF22C55E)
    val red = Color(0xFFEF4444)
    val amber = Color(0xFFF59E0B)
    val text = Color(0xFFF4F7FB)
    val muted = Color(0xFF8B97AB)
    val track = Color(0xFF2A3550)
    val chip = Color(0xFF16243A)
    val proFill = Color(0xFF3A2A0E)
    val connectInk = Color(0xFF052330)
    val liveInk = Color(0xFF06210F)
    val windowFill = Color(0xFF0E3A25)
    val hairline = Color.White.copy(alpha = 0.06f)
    val fieldBorder = Color(0xFF243149)
    val presetOff = Color(0xFF151D2E)

    fun tireColor(temp: Float): Color = when {
        temp < 90f -> green
        temp < 105f -> amber
        else -> red
    }

    fun deltaColor(seconds: Double): Color = when {
        kotlin.math.abs(seconds) < 0.0005 -> muted
        seconds < 0 -> green
        else -> red
    }
}

object GTType {
    fun title(size: TextUnit) = androidx.compose.ui.text.TextStyle(
        fontSize = size,
        fontWeight = FontWeight.Bold,
        fontFamily = FontFamily.SansSerif,
        color = GTColors.text,
    )

    fun numeric(size: TextUnit, weight: FontWeight = FontWeight.Bold) = androidx.compose.ui.text.TextStyle(
        fontSize = size,
        fontWeight = weight,
        fontFamily = FontFamily.SansSerif,
        color = GTColors.text,
    )

    fun caps(size: TextUnit = 9.sp) = androidx.compose.ui.text.TextStyle(
        fontSize = size,
        fontWeight = FontWeight.SemiBold,
        fontFamily = FontFamily.SansSerif,
        letterSpacing = 1.5.sp,
        color = GTColors.muted,
    )
}

val LocalSettings = staticCompositionLocalOf<AppSettings> {
    error("AppSettings not provided")
}
val LocalTelemetry = staticCompositionLocalOf<TelemetryService> {
    error("TelemetryService not provided")
}

private val DarkScheme = darkColorScheme(
    primary = GTColors.cyan,
    onPrimary = GTColors.connectInk,
    background = GTColors.page,
    onBackground = GTColors.text,
    surface = GTColors.card,
    onSurface = GTColors.text,
    error = GTColors.red,
)

@Composable
fun GTTheme(
    settings: AppSettings,
    telemetry: TelemetryService,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalSettings provides settings,
        LocalTelemetry provides telemetry,
    ) {
        MaterialTheme(colorScheme = DarkScheme, content = content)
    }
}
