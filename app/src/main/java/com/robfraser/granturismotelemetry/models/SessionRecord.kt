package com.robfraser.granturismotelemetry.models

data class SectorCompare(
    val number: Int,
    val timeSeconds: Double,
    val deltaSeconds: Double,
)

data class LapRow(
    val number: Int,
    val timeMs: Int,
    val deltaSeconds: Double?,
    val isBest: Boolean,
)

data class SessionRecord(
    val track: String,
    val carClass: String,
    val bestLapMs: Int,
    val laps: Int,
    val whenLabel: String,
    val lastLapMs: Int,
    val sectors: List<SectorCompare>,
    val deltaTrace: List<Double>,
    val lapRows: List<LapRow>,
) {
    val id: String get() = "$track-$whenLabel"
    val bestLapLabel: String get() = Formatters.lapTime(bestLapMs)
}

object SampleSessions {
    val all: List<SessionRecord> = listOf(
        SessionRecord(
            track = "Deep Forest Raceway",
            carClass = "Gr.3",
            bestLapMs = 84_539,
            laps = 12,
            whenLabel = "Today 14:32",
            lastLapMs = 84_881,
            sectors = listOf(
                SectorCompare(1, 28.114, -0.121),
                SectorCompare(2, 31.902, 0.463),
                SectorCompare(3, 24.523, 0.000),
            ),
            deltaTrace = listOf(0.05, -0.04, 0.08, -0.12, 0.04, -0.18, 0.02, -0.22, 0.06, -0.28, 0.10),
            lapRows = listOf(
                LapRow(9, 85_102, 0.563, false),
                LapRow(10, 84_712, 0.173, false),
                LapRow(11, 84_539, null, true),
                LapRow(12, 84_881, 0.342, false),
                LapRow(13, 84_197, -0.342, false),
            ),
        ),
        SessionRecord(
            track = "Trial Mountain",
            carClass = "Gr.3",
            bestLapMs = 118_204,
            laps = 8,
            whenLabel = "Yesterday 20:17",
            lastLapMs = 118_540,
            sectors = listOf(
                SectorCompare(1, 38.2, 0.08),
                SectorCompare(2, 42.1, -0.04),
                SectorCompare(3, 37.904, 0.12),
            ),
            deltaTrace = listOf(0.1, 0.05, -0.02, 0.08, 0.14, 0.09, 0.16),
            lapRows = emptyList(),
        ),
        SessionRecord(
            track = "Suzuka Circuit",
            carClass = "Gr.3",
            bestLapMs = 125_771,
            laps = 15,
            whenLabel = "Mon 18:41",
            lastLapMs = 126_102,
            sectors = listOf(
                SectorCompare(1, 36.4, -0.05),
                SectorCompare(2, 48.9, 0.21),
                SectorCompare(3, 40.471, 0.04),
            ),
            deltaTrace = listOf(0.0, 0.04, 0.12, 0.08, 0.18, 0.22, 0.15),
            lapRows = emptyList(),
        ),
        SessionRecord(
            track = "Nürburgring GP",
            carClass = "Gr.3",
            bestLapMs = 112_318,
            laps = 9,
            whenLabel = "Sun 11:26",
            lastLapMs = 112_890,
            sectors = listOf(
                SectorCompare(1, 32.1, 0.11),
                SectorCompare(2, 41.2, -0.08),
                SectorCompare(3, 39.018, 0.05),
            ),
            deltaTrace = listOf(0.04, -0.02, 0.07, 0.12, 0.03, 0.09),
            lapRows = emptyList(),
        ),
    )
}

object Formatters {
    fun lapTime(ms: Int): String {
        if (ms <= 0) return "–.–––"
        val total = ms / 1000.0
        val minutes = total.toInt() / 60
        val seconds = total % 60.0
        return String.format(java.util.Locale.US, "%d:%06.3f", minutes, seconds)
    }

    fun delta(seconds: Double, showPlus: Boolean = true): String {
        if (kotlin.math.abs(seconds) < 0.0005) return "0.000"
        val sign = if (seconds < 0) "−" else if (showPlus) "+" else ""
        return String.format(java.util.Locale.US, "%s%.3f", sign, kotlin.math.abs(seconds))
    }

    fun sector(seconds: Double): String =
        String.format(java.util.Locale.US, "%.3f", seconds)
}
