package com.robfraser.granturismotelemetry.telemetry

import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Emits synthetic telemetry resembling a lap of Deep Forest Raceway so the UI
 * works without a PS5. Values are plausible and live-looking (~60 Hz).
 */
class TelemetrySimulator {
    var onPacket: ((TelemetryPacket) -> Unit)? = null

    @Volatile private var running = false
    private var thread: Thread? = null

    fun start() {
        if (running) return
        running = true
        thread = Thread({ runLoop() }, "gt7-simulator").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        thread = null
    }

    private fun runLoop() {
        var packetId = 0
        var lap = 12
        val bestLapMs = 84_539
        var lastLapMs = 84_881
        var lapStart = System.nanoTime()
        val lapSeconds = 84.539
        val gearMax = doubleArrayOf(0.0, 60.0, 100.0, 140.0, 180.0, 220.0, 260.0, 300.0)
        val rng = Random.Default

        while (running) {
            var elapsed = (System.nanoTime() - lapStart) / 1_000_000_000.0
            if (elapsed >= lapSeconds) {
                lastLapMs = (elapsed * 1000).toInt()
                lapStart = System.nanoTime()
                elapsed = 0.0
                lap += 1
            }

            val t = elapsed / lapSeconds
            val throttle: Double
            val brake: Double
            val clutch: Double
            val targetSpeedKph: Double
            when {
                t < 0.35 -> { throttle = 1.0; brake = 0.0; clutch = 0.0; targetSpeedKph = 260.0 }
                t < 0.45 -> { throttle = 0.0; brake = 0.9; clutch = 0.9; targetSpeedKph = 90.0 }
                t < 0.65 -> { throttle = 0.75; brake = 0.0; clutch = 0.0; targetSpeedKph = 180.0 }
                t < 0.75 -> { throttle = 0.1; brake = 0.6; clutch = 0.6; targetSpeedKph = 120.0 }
                else -> { throttle = 1.0; brake = 0.0; clutch = 0.0; targetSpeedKph = 280.0 }
            }

            val jitter = rng.nextDouble() * 2.0
            val speedKph = targetSpeedKph + sin(elapsed * 4) * 4 + jitter
            val speedMps = speedKph / 3.6

            val altitudeM = 140 + 70 * sin(t * 2 * Math.PI)
            val slopeRad = atan(0.10 * cos(t * 2 * Math.PI))
            val targetLatG = 1.4 * sin(t * 4 * Math.PI)
            val yawRate = targetLatG * 9.80665 / max(speedMps, 1.0)
            val rideHeightM = 0.078 + 0.006 * sin(elapsed * 3)
            val tireRadiusM = 0.33
            val lockSlip = if (brake > 0.5) -0.10 else 0.0
            val spinSlip = if (throttle > 0.9) 0.08 else 0.0
            val frontOmega = speedMps * (1 + lockSlip) / tireRadiusM
            val rearOmega = speedMps * (1 + spinSlip) / tireRadiusM

            var gear = 1
            for (g in 1 until gearMax.size) {
                gear = g
                if (speedKph <= gearMax[g]) break
            }
            var rpm = 1500 + (speedKph / gearMax[gear]) * 7000 + rng.nextDouble() * 100
            rpm = min(8800.0, max(900.0, rpm))

            val liveDelta = -0.342 + 0.22 * sin(t * 2 * Math.PI) + 0.05 * sin(t * 14 * Math.PI)

            val angle = t * 2 * Math.PI
            val posX = (420 * cos(angle)).toFloat()
            val posZ = (260 * sin(angle)).toFloat()

            val fuelFrac = 0.42 - elapsed * 0.0004
            val fuelLevel = (max(0.08, fuelFrac) * 100).toFloat()

            val pkt = TelemetryPacket()
            pkt.packetId = packetId
            pkt.engineRpm = rpm.toFloat()
            pkt.speedMps = speedMps.toFloat()
            pkt.positionX = posX
            pkt.positionY = altitudeM.toFloat()
            pkt.positionZ = posZ
            pkt.velocityX = (speedMps * cos(slopeRad)).toFloat()
            pkt.velocityY = (speedMps * sin(slopeRad)).toFloat()
            pkt.angularVelocityY = yawRate.toFloat()
            pkt.rideHeight = rideHeightM.toFloat()
            pkt.throttle = min(255, max(0, (throttle * 255).toInt()))
            pkt.brake = min(255, max(0, (brake * 255).toInt()))
            pkt.clutchPedal = min(1.0, max(0.0, clutch)).toFloat()
            pkt.currentGear = gear
            pkt.suggestedGear = 15
            pkt.fuelCapacity = 100f
            pkt.fuelLevel = fuelLevel
            pkt.boostKpa = (100 + throttle * 60 + rng.nextDouble() * 3).toFloat()
            pkt.oilPressure = (4.5 + throttle * 0.8 + rng.nextDouble() * 0.1).toFloat()
            pkt.waterTemp = 88f
            pkt.oilTemp = 108f
            pkt.tireTempFL = (81 + brake * 8 + rng.nextDouble() * 1.2).toFloat()
            pkt.tireTempFR = (83 + brake * 8 + rng.nextDouble() * 1.2).toFloat()
            pkt.tireTempRL = (101 + throttle * 6 + rng.nextDouble() * 1.2).toFloat()
            pkt.tireTempRR = (102 + throttle * 6 + rng.nextDouble() * 1.2).toFloat()
            pkt.wheelSpeedFL = frontOmega.toFloat()
            pkt.wheelSpeedFR = frontOmega.toFloat()
            pkt.wheelSpeedRL = rearOmega.toFloat()
            pkt.wheelSpeedRR = rearOmega.toFloat()
            pkt.tireRadiusFL = tireRadiusM.toFloat()
            pkt.tireRadiusFR = tireRadiusM.toFloat()
            pkt.tireRadiusRL = tireRadiusM.toFloat()
            pkt.tireRadiusRR = tireRadiusM.toFloat()
            pkt.currentLap = lap
            pkt.totalLaps = 15
            pkt.bestLapMs = bestLapMs
            pkt.lastLapMs = lastLapMs
            pkt.alertMinRpm = 6800
            pkt.alertMaxRpm = 8500
            pkt.calcMaxSpeedKph = 320
            pkt.flags = SimulatorFlags(
                SimulatorFlags.CAR_ON_TRACK or SimulatorFlags.IN_GEAR or SimulatorFlags.HAS_TURBO,
            )
            pkt.gearRatios = floatArrayOf(3.5f, 2.4f, 1.8f, 1.4f, 1.1f, 0.9f, 0.7f, 0f)
            pkt.trackName = "Deep Forest Raceway"
            pkt.carClass = "Gr.3"
            pkt.lapProgress = t
            pkt.liveDeltaSeconds = liveDelta
            pkt.fuelLapsRemaining = max(1.5, fuelLevel.toDouble() / 6.5)
            pkt.pitWindowOpen = fuelFrac < 0.5 && fuelFrac > 0.18

            packetId += 1
            onPacket?.invoke(pkt)

            try {
                Thread.sleep(1000L / 60L)
            } catch (_: InterruptedException) {
                break
            }
        }
    }
}
