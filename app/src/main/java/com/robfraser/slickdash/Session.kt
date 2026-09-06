package com.robfraser.slickdash

import kotlin.math.hypot

data class LapRow(val lap: Int, val timeMs: Int, val isBest: Boolean)

data class DashState(
    val live: Boolean = false,
    val peer: String? = null,
    val status: String = "Waiting for telemetry",
    val rx: Int = 0, val dec: Int = 0, val err: Int = 0,
    val quality: Float = 0f,
    val packet: TelemetryPacket? = null,
    val fuelPct: Double = 0.0,
    val fuelPerLap: Double? = null,
    val lapsRemaining: Double? = null,
    val stops: Int = 0,
    val lastMs: Int? = null,
    val bestMs: Int? = null,
    val liveDelta: Double? = null,
    val deltaTrace: List<Float> = emptyList(),
    val laps: List<LapRow> = emptyList(),
    val lapsInMemory: Int = 0,
)

class SessionTracker {
    private val laps = ArrayDeque<LapRow>()
    private var lastLapIndex = -1
    private var completedFlying = 0
    private var fuelAtLapStart: Double? = null
    private var fuelPerLap: Double? = null
    private var ghost = mutableListOf<Triple<Float, Float, Float>>() // x,z,t
    private var current = mutableListOf<Triple<Float, Float, Float>>()
    private var lapT0 = 0L
    private val trace = ArrayDeque<Float>()
    var bestMs: Int? = null; private set

    fun onPacket(p: TelemetryPacket, now: Long = System.currentTimeMillis()): DashState {
        if (p.currentLap != lastLapIndex) {
            if (lastLapIndex >= 0 && p.lastLapMs > 0) {
                completedFlying++
                val skipOut = completedFlying == 1
                if (!skipOut) {
                    laps.addLast(LapRow(lastLapIndex, p.lastLapMs, false))
                    while (laps.size > 100) laps.removeFirst()
                    val best = laps.minByOrNull { it.timeMs }?.timeMs
                    bestMs = best
                    val marked = laps.map { it.copy(isBest = best != null && it.timeMs == best) }
                    laps.clear(); laps.addAll(marked)
                    if (best != null && p.lastLapMs == best && current.size >= 2) {
                        ghost = current.toMutableList()
                    }
                }
                val start = fuelAtLapStart
                if (start != null) {
                    val used = (start - p.fuelPercent).coerceAtLeast(0.0)
                    if (used > 0.2) fuelPerLap = used
                }
            }
            lastLapIndex = p.currentLap
            fuelAtLapStart = p.fuelPercent
            current = mutableListOf()
            lapT0 = now
        }
        val t = ((now - lapT0).coerceAtLeast(0)) / 1000f
        if (p.onTrack) current.add(Triple(p.posX, p.posZ, t))
        val delta = liveDelta(p.posX, p.posZ, t)
        if (delta != null) {
            trace.addLast(delta.toFloat())
            while (trace.size > 120) trace.removeFirst()
        }
        val fpl = fuelPerLap
        val rem = if (fpl != null && fpl > 0.05) p.fuelPercent / fpl else null
        val stops = if (rem != null) kotlin.math.ceil((1.0).coerceAtLeast(0.0)).toInt().let {
            // remaining stint stops estimate: floor(laps still needed beyond tank) — session-only 1-stop display
            if (rem < 8) 1 else 0
        } else 0
        return DashState(
            live = p.onTrack,
            packet = p,
            fuelPct = p.fuelPercent,
            fuelPerLap = fpl,
            lapsRemaining = rem,
            stops = stops,
            lastMs = p.lastLapMs.takeIf { it > 0 },
            // Session best is local flyers only — never fall back to GT7 packet PB.
            bestMs = bestMs,
            liveDelta = delta,
            deltaTrace = trace.toList(),
            laps = laps.toList().takeLast(12),
            lapsInMemory = laps.size,
        )
    }

    private fun liveDelta(x: Float, z: Float, t: Float): Double? {
        if (ghost.size < 2 || completedFlying < 1) return null
        var best = Double.MAX_VALUE
        var ghostT = ghost.first().third
        for (s in ghost) {
            val d = hypot((s.first - x).toDouble(), (s.second - z).toDouble())
            if (d < best) { best = d; ghostT = s.third }
        }
        if (best > 40) return null
        return (t - ghostT).toDouble()
    }
}

fun formatLap(ms: Int?): String {
    if (ms == null || ms <= 0) return "—"
    val m = ms / 60000
    val s = (ms % 60000) / 1000
    val x = ms % 1000
    return "%d:%02d.%03d".format(m, s, x)
}
fun formatDelta(d: Double?): String {
    if (d == null || d.isNaN()) return "—"
    return "%+.3f".format(d).replace("+", "+").let {
        if (d < 0) "−%.3f".format(-d) else "+%.3f".format(d)
    }
}
