package com.robfraser.slickdash

import kotlin.math.ceil
import kotlin.math.hypot

data class LapRow(val lap: Int, val timeMs: Int, val isBest: Boolean)

data class DashState(
    val live: Boolean = false,
    val peer: String? = null,
    val status: String = "Waiting for telemetry",
    val rx: Int = 0, val dec: Int = 0, val err: Int = 0,
    val quality: QualityRating = QualityRating.Poor,
    val packet: TelemetryPacket? = null,
    val fuelPct: Double = 0.0,
    val fuelPerLap: Double? = null,
    val lapsRemaining: Double? = null,
    val stops: Int = 0,
    val windowOpen: Boolean = false,
    val lastMs: Int? = null,
    val bestMs: Int? = null,
    val liveDelta: Double? = null,
    val deltaTrace: List<Float> = emptyList(),
    val throttleTrace: List<Float> = emptyList(),
    val brakeTrace: List<Float> = emptyList(),
    val laps: List<LapRow> = emptyList(),
    val lapsInMemory: Int = 0,
)

/**
 * In-memory this-stint lap table, fuel estimates, and ghost delta — Windows SessionTracker
 * behaviour, mobile-appropriate (skip out-lap from the table; one isBest mark).
 */
class SessionTracker {
    companion object {
        const val MaxLaps = 100
        const val SampleSpacingM = 1.0
        const val MaxMatchDistanceM = 32.0
        const val FlyerPathFraction = 0.85
        private const val MaxSamplesPerLap = 4096
    }

    private data class GhostSample(val x: Float, val z: Float, val elapsedSec: Float)

    private val laps = ArrayDeque<LapRow>()
    private val fuelSamples = ArrayDeque<Double>()
    private var lastLapIndex = -1
    private var lastLapMsSeen = 0
    private var completedFlying = 0
    private var fuelAtLapStart: Double? = null
    private var ghost = mutableListOf<GhostSample>()
    private var current = mutableListOf<GhostSample>()
    private var lapT0 = 0L
    private var pauseStarted: Long? = null
    private val trace = ArrayDeque<Float>()
    private var heldDelta: Double? = null
    private var ghostMatchIndex = -1
    private var ghostBestMs = 0
    private var ghostPathM = 0.0
    private var maxPathM = 0.0
    private var lastSampleX: Float? = null
    private var lastSampleZ: Float? = null
    private var carCode = 0
    private var hasCarCode = false
    var bestMs: Int? = null; private set

    private val fuelPerLap: Double?
        get() = if (fuelSamples.isEmpty()) null else fuelSamples.average()

    fun resetStint() {
        laps.clear()
        fuelSamples.clear()
        lastLapIndex = -1
        lastLapMsSeen = 0
        completedFlying = 0
        fuelAtLapStart = null
        ghost = mutableListOf()
        current = mutableListOf()
        pauseStarted = null
        trace.clear()
        heldDelta = null
        ghostMatchIndex = -1
        ghostBestMs = 0
        ghostPathM = 0.0
        maxPathM = 0.0
        lastSampleX = null
        lastSampleZ = null
        bestMs = null
        hasCarCode = false
        carCode = 0
    }

    fun onPacket(p: TelemetryPacket, now: Long = System.currentTimeMillis()): DashState {
        if (hasCarCode && p.carCode != 0 && p.carCode != carCode) resetStint()
        if (!hasCarCode && p.carCode != 0) {
            carCode = p.carCode
            hasCarCode = true
        }
        if (lastLapIndex >= 0 && p.currentLap >= 0 && p.currentLap < lastLapIndex) resetStint()

        val fplEarly = fuelPerLap
        val remEarly = if (fplEarly != null && fplEarly > 0.05) p.fuelPercent / fplEarly else null
        val stopsEarly = predictedStops(p.fuelPercent, remEarly, fplEarly, p.totalLaps, p.currentLap)
        val windowEarly = windowOpen(p.fuelPercent, remEarly)

        if (!p.isRacing) {
            if (pauseStarted == null) pauseStarted = now
            heldDelta = null
            ghostMatchIndex = -1
            return DashState(
                live = false,
                packet = p,
                fuelPct = p.fuelPercent,
                fuelPerLap = fplEarly,
                lapsRemaining = remEarly,
                stops = stopsEarly,
                windowOpen = windowEarly,
                lastMs = p.lastLapMs.takeIf { it > 0 },
                bestMs = bestMs,
                liveDelta = null,
                deltaTrace = emptyList(),
                laps = laps.toList().takeLast(12),
                lapsInMemory = laps.size,
            )
        }

        pauseStarted?.let { ps ->
            lapT0 += now - ps
            pauseStarted = null
            ghostMatchIndex = if (ghost.isEmpty()) -1 else 0
        }

        // Lap boundary: record on CurrentLap change when lastLapMs > 0; same-index only when lastLapMsSeen changes.
        if (p.currentLap != lastLapIndex) {
            if (lastLapIndex >= 0 && p.lastLapMs > 0) {
                completedFlying++
                val startFuel = fuelAtLapStart
                if (startFuel != null) {
                    val used = (startFuel - p.fuelPercent).coerceAtLeast(0.0)
                    if (used in 0.3..25.0) {
                        fuelSamples.addLast(used)
                        while (fuelSamples.size > 12) fuelSamples.removeFirst()
                    }
                }
                // Skip first completed lap (out-lap) from the table; still counts fuel.
                if (completedFlying > 1) {
                    recordFlyer(lastLapIndex, p.lastLapMs)
                } else {
                    current = mutableListOf()
                    lastSampleX = null
                    lastSampleZ = null
                    ghostMatchIndex = if (ghost.isEmpty()) -1 else 0
                }
                lastLapMsSeen = p.lastLapMs
            } else if (lastLapIndex < 0) {
                // First attach — remember packet last so mid-session join does not invent a lap.
                lastLapMsSeen = p.lastLapMs
            }
            lastLapIndex = p.currentLap
            fuelAtLapStart = p.fuelPercent
            current = mutableListOf()
            lastSampleX = null
            lastSampleZ = null
            lapT0 = now
            heldDelta = null
        } else if (lastLapIndex >= 0 && p.lastLapMs > 0 && p.lastLapMs != lastLapMsSeen) {
            // Same lap index but a new last-lap time (Windows).
            completedFlying++
            val startFuel = fuelAtLapStart
            if (startFuel != null) {
                val used = (startFuel - p.fuelPercent).coerceAtLeast(0.0)
                if (used in 0.3..25.0) {
                    fuelSamples.addLast(used)
                    while (fuelSamples.size > 12) fuelSamples.removeFirst()
                }
            }
            if (completedFlying > 1) {
                recordFlyer(lastLapIndex, p.lastLapMs)
            } else {
                current = mutableListOf()
                lastSampleX = null
                lastSampleZ = null
                ghostMatchIndex = if (ghost.isEmpty()) -1 else 0
            }
            lastLapMsSeen = p.lastLapMs
            fuelAtLapStart = p.fuelPercent
            current = mutableListOf()
            lastSampleX = null
            lastSampleZ = null
            lapT0 = now
            heldDelta = null
        }

        val t = ((now - lapT0).coerceAtLeast(0)) / 1000f
        appendSample(p.posX, p.posZ, t)
        val delta = liveDelta(p.posX, p.posZ, t)
        val shown = delta ?: heldDelta
        if (delta != null) {
            heldDelta = delta
            trace.addLast(delta.toFloat())
            while (trace.size > 120) trace.removeFirst()
        }

        val fpl = fuelPerLap
        val rem = if (fpl != null && fpl > 0.05) p.fuelPercent / fpl else null
        return DashState(
            live = p.isRacing,
            packet = p,
            fuelPct = p.fuelPercent,
            fuelPerLap = fpl,
            lapsRemaining = rem,
            stops = predictedStops(p.fuelPercent, rem, fpl, p.totalLaps, p.currentLap),
            windowOpen = windowOpen(p.fuelPercent, rem),
            lastMs = p.lastLapMs.takeIf { it > 0 },
            bestMs = bestMs,
            liveDelta = shown,
            deltaTrace = trace.toList(),
            laps = laps.toList().takeLast(12),
            lapsInMemory = laps.size,
        )
    }

    private fun predictedStops(
        fuelPct: Double,
        rem: Double?,
        fpl: Double?,
        totalLaps: Int,
        currentLap: Int,
    ): Int {
        val rate = fpl ?: 2.1
        val raceLeft = if (totalLaps > 0) maxOf(0, totalLaps - maxOf(currentLap, 0)) else 0
        return if (raceLeft > 0) {
            val need = raceLeft * rate
            val extra = need - fuelPct
            if (extra <= 0.5) 0 else ceil(extra / 100.0).toInt()
        } else {
            if ((rem ?: 99.0) < 8.0) 1 else 0
        }
    }

    private fun windowOpen(fuelPct: Double, rem: Double?): Boolean =
        (fuelPct > 18.0 && fuelPct < 50.0) || (rem != null && rem > 1.5 && rem < 8.0)

    private fun recordFlyer(lap: Int, timeMs: Int) {
        if (timeMs <= 0) return
        if (current.isNotEmpty()) {
            val last = current.last()
            current[current.lastIndex] = last.copy(elapsedSec = timeMs / 1000f)
        }
        val path = pathLengthM(current)
        laps.addLast(LapRow(lap, timeMs, false))
        while (laps.size > MaxLaps) laps.removeFirst()
        relabelBest()

        val eligible = current.size >= 2 && timeMs > 0
        if (eligible) {
            if (path > maxPathM) maxPathM = path
            val install = ghost.size < 2 ||
                (timeMs < ghostBestMs && path >= FlyerPathFraction * ghostPathM)
            if (install) {
                ghost = current.toMutableList()
                ghostBestMs = timeMs
                ghostPathM = path
            }
        }
        ghostMatchIndex = if (ghost.isEmpty()) -1 else 0
        current = mutableListOf()
        lastSampleX = null
        lastSampleZ = null
    }

    private fun relabelBest() {
        val best = laps.minOfOrNull { it.timeMs }
        bestMs = best
        var marked = false
        val markedRows = laps.map { row ->
            val isBest = !marked && best != null && row.timeMs == best
            if (isBest) marked = true
            row.copy(isBest = isBest)
        }
        laps.clear()
        laps.addAll(markedRows)
    }

    private fun appendSample(x: Float, z: Float, t: Float) {
        val lx = lastSampleX
        val lz = lastSampleZ
        if (lx != null && lz != null) {
            if (hypot((x - lx).toDouble(), (z - lz).toDouble()) < SampleSpacingM) return
        }
        if (current.size >= MaxSamplesPerLap) {
            current[current.lastIndex] = GhostSample(x, z, t)
        } else {
            current.add(GhostSample(x, z, t))
        }
        lastSampleX = x
        lastSampleZ = z
    }

    private fun pathLengthM(samples: List<GhostSample>): Double {
        if (samples.size < 2) return 0.0
        var sum = 0.0
        for (i in 1 until samples.size) {
            sum += hypot(
                (samples[i].x - samples[i - 1].x).toDouble(),
                (samples[i].z - samples[i - 1].z).toDouble(),
            )
        }
        return sum
    }

    private fun liveDelta(x: Float, z: Float, t: Float): Double? {
        if (ghost.size < 2) return null
        val idx = findGhostMatch(x, z, t) ?: return null
        val g = ghost[idx]
        if (hypot((g.x - x).toDouble(), (g.z - z).toDouble()) > MaxMatchDistanceM) return null
        ghostMatchIndex = idx
        val d = (t - g.elapsedSec).toDouble()
        // Reject absurd early unlock deltas (start/finish XZ ambiguity).
        if (kotlin.math.abs(d) > 30 && idx < maxOf(2, ghost.size / 10)) {
            ghostMatchIndex = -1
            return null
        }
        return d
    }

    private fun findGhostMatch(x: Float, z: Float, elapsed: Float): Int? {
        val n = ghost.size
        if (n == 0) return null
        val start: Int
        val count: Int
        if (ghostMatchIndex < 0) {
            start = 0
            count = n
        } else {
            val fwd = minOf(n, maxOf(32, n / 8))
            val back = minOf(n, maxOf(8, n / 32))
            start = (ghostMatchIndex - back + n) % n
            count = minOf(n, back + fwd + 1)
        }
        var bestIdx = -1
        var bestDistSq = Double.MAX_VALUE
        for (i in 0 until count) {
            val idx = (start + i) % n
            val g = ghost[idx]
            val dx = (g.x - x).toDouble()
            val dz = (g.z - z).toDouble()
            val dsq = dx * dx + dz * dz
            if (dsq < bestDistSq) {
                bestDistSq = dsq
                bestIdx = idx
            }
        }
        if (bestIdx < 0) return null
        val near = bestDistSq + 16.0
        var chosen = bestIdx
        var bestElapsedErr = kotlin.math.abs(elapsed - ghost[bestIdx].elapsedSec).toDouble()
        for (i in 0 until count) {
            val idx = (start + i) % n
            val g = ghost[idx]
            val dx = (g.x - x).toDouble()
            val dz = (g.z - z).toDouble()
            val dsq = dx * dx + dz * dz
            if (dsq > near) continue
            val err = kotlin.math.abs(elapsed - g.elapsedSec).toDouble()
            if (err < bestElapsedErr) {
                bestElapsedErr = err
                chosen = idx
            }
        }
        return chosen
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
    return if (d < 0) "−%.3f".format(-d) else "+%.3f".format(d)
}

fun qualityFrac(q: QualityRating): Float = when (q) {
    QualityRating.Good -> 0.92f
    QualityRating.Fair -> 0.55f
    QualityRating.Poor -> 0.2f
}
