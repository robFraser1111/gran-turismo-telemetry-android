package com.robfraser.slickdash

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.cos
import kotlin.math.sin

class SessionTrackerTests {
    @Test
    fun recordsFlyingLapAfterOutLapSkip() {
        val s = SessionTracker()
        // Attach on lap 1
        var st = s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0), now = 1_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(s.bestMs)

        // Out-lap completion skipped
        st = s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 80_000, fuel = 47.0), now = 91_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.bestMs)
        assertNull(s.bestMs)

        // First flying lap recorded
        st = s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0), now = 176_000)
        assertEquals(1, st.lapsInMemory)
        assertEquals(85_000, st.bestMs)
        assertEquals(85_000, s.bestMs)
        assertTrue(st.laps[0].isBest)
        assertEquals(85_000, st.laps[0].timeMs)
    }

    @Test
    fun sessionBestIsLocalFlyersOnly() {
        val s = SessionTracker()
        // Game PB present before any recorded flyer must not set session bestMs
        var st = s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 70_000, fuel = 50.0), now = 1_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.bestMs)
        assertNull(s.bestMs)

        st = s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 70_000, fuel = 49.5), now = 2_000)
        assertNull(st.bestMs)
        assertNull(s.bestMs)

        // Still no flyer after out-lap skip
        st = s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 70_000, fuel = 47.0), now = 91_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.bestMs)
    }

    @Test
    fun onlyOneLapMarkedIsBest() {
        val s = SessionTracker()
        s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0), now = 1_000)
        s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 80_000, fuel = 47.0), now = 91_000)
        s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0), now = 176_000)
        val st = s.onPacket(racing(lap = 4, lastMs = 85_000, bestMs = 80_000, fuel = 41.0), now = 261_000)
        assertEquals(2, st.lapsInMemory)
        assertEquals(1, st.laps.count { it.isBest })
        assertEquals(85_000, st.bestMs)
    }

    @Test
    fun fuelPerLapDerived() {
        val s = SessionTracker()
        s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0), now = 1_000)
        // First transition (out-lap skip) still updates fuel/lap
        var st = s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 80_000, fuel = 47.0), now = 91_000)
        assertTrue(st.fuelPerLap != null)
        assertTrue(st.fuelPerLap!! in 2.5..3.5)

        st = s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0), now = 176_000)
        assertTrue(st.fuelPerLap != null)
        assertTrue(st.fuelPerLap!! in 2.5..3.5)
    }

    @Test
    fun currentLapDropResetsStint() {
        val s = SessionTracker()
        s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0), now = 1_000)
        s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 80_000, fuel = 47.0), now = 91_000)
        s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0), now = 176_000)
        assertEquals(85_000, s.bestMs)

        val st = s.onPacket(racing(lap = 1, lastMs = 85_000, bestMs = 80_000, fuel = 80.0), now = 200_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.bestMs)
        assertNull(s.bestMs)
    }

    @Test
    fun carCodeChangeResetsStint() {
        val s = SessionTracker()
        s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0, carCode = 10), now = 1_000)
        s.onPacket(racing(lap = 2, lastMs = 90_000, bestMs = 80_000, fuel = 47.0, carCode = 10), now = 91_000)
        s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0, carCode = 10), now = 176_000)
        assertEquals(1, s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0, carCode = 10), now = 177_000).lapsInMemory)

        val st = s.onPacket(racing(lap = 3, lastMs = 85_000, bestMs = 80_000, fuel = 44.0, carCode = 20), now = 178_000)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.bestMs)
    }

    @Test
    fun notRacingReturnsNullDeltaAndLiveFalse() {
        val s = SessionTracker()
        s.onPacket(racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0), now = 1_000)
        val paused = racing(lap = 1, lastMs = 0, bestMs = 80_000, fuel = 50.0).copy(flags = 1 or 2)
        val st = s.onPacket(paused, now = 2_000)
        assertFalse(st.live)
        assertNull(st.liveDelta)
        assertTrue(st.deltaTrace.isEmpty())
    }

    @Test
    fun formatLapAndFormatDeltaBasics() {
        assertEquals("—", formatLap(null))
        assertEquals("—", formatLap(0))
        assertEquals("1:30.000", formatLap(90_000))
        assertEquals("—", formatDelta(null))
        assertEquals("—", formatDelta(Double.NaN))
        assertEquals("+1.250", formatDelta(1.25))
        assertEquals("−0.500", formatDelta(-0.5))
    }

    @Test
    fun noGhostYetLiveDeltaNullUntilSecondFlyer() {
        val s = SessionTracker()
        var clock = 1_000_000L

        // Drive out-lap path then complete (skipped)
        clock = driveArc(s, clock, lap = 1, lastMs = 0, bestMs = 80_000, durationMs = 80_000, points = 16)
        var st = s.onPacket(pathPkt(lap = 2, lastMs = 80_000, bestMs = 80_000, x = 80f, z = 0f), now = clock)
        assertEquals(0, st.lapsInMemory)
        assertNull(st.liveDelta)

        // Drive first flying lap — still no ghost until it completes
        clock = driveArc(s, clock, lap = 2, lastMs = 80_000, bestMs = 80_000, durationMs = 80_000, points = 9)
        st = s.onPacket(pathPkt(lap = 2, lastMs = 80_000, bestMs = 80_000, x = 80f, z = 0f), now = clock)
        assertNull(st.liveDelta)

        // Complete flying lap (installs ghost when path was long enough)
        clock += 1
        st = s.onPacket(pathPkt(lap = 3, lastMs = 80_000, bestMs = 80_000, x = 80f, z = 0f), now = clock)
        assertEquals(1, st.lapsInMemory)
        assertEquals(80_000, st.bestMs)

        // After first eligible flyer install, subsequent samples can produce a delta.
        clock = driveArc(s, clock, lap = 3, lastMs = 80_000, bestMs = 80_000, durationMs = 96_000, points = 9)
        st = s.onPacket(pathPkt(lap = 3, lastMs = 80_000, bestMs = 80_000, x = 0f, z = 80f), now = clock)
        if (st.liveDelta != null) {
            assertTrue(st.liveDelta!!.isFinite())
        }
    }

    private fun racing(
        lap: Int,
        lastMs: Int,
        bestMs: Int,
        fuel: Double,
        carCode: Int = 0,
        totalLaps: Int = 0,
    ) = TelemetryPacket(
        posX = 0f, posZ = 0f, rpm = 3000f, fuelLevel = fuel.toFloat(), fuelCapacity = 100f,
        speedMps = 10f, tireFL = 80f, tireFR = 80f, tireRL = 80f, tireRR = 80f,
        currentLap = lap, totalLaps = totalLaps, bestLapMs = bestMs, lastLapMs = lastMs,
        alertMaxRpm = 8000, flags = 1, gear = 3, throttle = 100, brake = 0, carCode = carCode,
    )

    private fun pathPkt(lap: Int, lastMs: Int, bestMs: Int, x: Float, z: Float) = TelemetryPacket(
        posX = x, posZ = z, rpm = 3000f, fuelLevel = 50f, fuelCapacity = 100f,
        speedMps = 10f, tireFL = 80f, tireFR = 80f, tireRL = 80f, tireRR = 80f,
        currentLap = lap, totalLaps = 0, bestLapMs = bestMs, lastLapMs = lastMs,
        alertMaxRpm = 8000, flags = 1, gear = 3, throttle = 100, brake = 0, carCode = 0,
    )

    /** Drive an elliptical arc; returns clock after last sample. */
    private fun driveArc(
        s: SessionTracker,
        startTick: Long,
        lap: Int,
        lastMs: Int,
        bestMs: Int,
        durationMs: Int,
        points: Int,
        radius: Double = 80.0,
    ): Long {
        var clock = startTick
        for (i in 0 until points) {
            val t = i / points.toDouble()
            clock = startTick + (t * durationMs).toLong()
            val ang = t * 2 * Math.PI
            val x = (radius * cos(ang)).toFloat()
            val z = (radius * sin(ang)).toFloat()
            s.onPacket(pathPkt(lap, lastMs, bestMs, x, z), now = clock)
        }
        return startTick + durationMs
    }
}
