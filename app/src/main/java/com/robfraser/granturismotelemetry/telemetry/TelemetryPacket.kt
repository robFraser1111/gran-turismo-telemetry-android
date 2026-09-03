package com.robfraser.granturismotelemetry.telemetry

class PacketParseException(message: String) : Exception(message)

data class SimulatorFlags(val rawValue: Int) {
    companion object {
        const val CAR_ON_TRACK = 1 shl 0
        const val PAUSED = 1 shl 1
        const val LOADING_OR_PROCESSING = 1 shl 2
        const val IN_GEAR = 1 shl 3
        const val HAS_TURBO = 1 shl 4
        const val REV_LIMITER = 1 shl 5
        const val HAND_BRAKE_ACTIVE = 1 shl 6
        const val LIGHTS_ACTIVE = 1 shl 7
        const val HIGH_BEAM_ACTIVE = 1 shl 8
        const val LOW_BEAM_ACTIVE = 1 shl 9
        const val ASM_ACTIVE = 1 shl 10
        const val TCS_ACTIVE = 1 shl 11
    }

    operator fun contains(flag: Int): Boolean = (rawValue and flag) != 0
}

/**
 * Decoded Gran Turismo 7 telemetry packet (296-byte "A" packet).
 * Field offsets match the public reference:
 * https://github.com/robFraser1111/gran-turismo-telemetry
 */
data class TelemetryPacket(
    var packetId: Int = 0,
    var positionX: Float = 0f,
    var positionY: Float = 0f,
    var positionZ: Float = 0f,
    var velocityX: Float = 0f,
    var velocityY: Float = 0f,
    var velocityZ: Float = 0f,
    var rotationPitch: Float = 0f,
    var rotationYaw: Float = 0f,
    var rotationRoll: Float = 0f,
    var heading: Float = 0f,
    var angularVelocityX: Float = 0f,
    var angularVelocityY: Float = 0f,
    var angularVelocityZ: Float = 0f,
    var rideHeight: Float = 0f,
    var engineRpm: Float = 0f,
    var fuelLevel: Float = 0f,
    var fuelCapacity: Float = 0f,
    var speedMps: Float = 0f,
    var boostKpa: Float = 0f,
    var oilPressure: Float = 0f,
    var waterTemp: Float = 0f,
    var oilTemp: Float = 0f,
    var tireTempFL: Float = 0f,
    var tireTempFR: Float = 0f,
    var tireTempRL: Float = 0f,
    var tireTempRR: Float = 0f,
    var currentLap: Int = 0,
    var totalLaps: Int = 0,
    var bestLapMs: Int = 0,
    var lastLapMs: Int = 0,
    var dayProgressionMs: Int = 0,
    var preRaceStartPos: Int = 0,
    var numCarsPreRace: Int = 0,
    var alertMinRpm: Int = 0,
    var alertMaxRpm: Int = 0,
    var calcMaxSpeedKph: Int = 0,
    var flags: SimulatorFlags = SimulatorFlags(0),
    /** 0 = reverse, 15 = neutral (raw 0x0F) */
    var currentGear: Int = 15,
    /** 15 = none */
    var suggestedGear: Int = 15,
    var throttle: Int = 0,
    var brake: Int = 0,
    var wheelSpeedFL: Float = 0f,
    var wheelSpeedFR: Float = 0f,
    var wheelSpeedRL: Float = 0f,
    var wheelSpeedRR: Float = 0f,
    var tireRadiusFL: Float = 0f,
    var tireRadiusFR: Float = 0f,
    var tireRadiusRL: Float = 0f,
    var tireRadiusRR: Float = 0f,
    var suspensionFL: Float = 0f,
    var suspensionFR: Float = 0f,
    var suspensionRL: Float = 0f,
    var suspensionRR: Float = 0f,
    var clutchPedal: Float = 0f,
    var clutchEngagement: Float = 0f,
    var rpmAfterClutch: Float = 0f,
    var transmissionTopSpeedRatio: Float = 0f,
    var gearRatios: FloatArray = FloatArray(8),
    var carCode: Int = 0,
    var trackName: String = "Deep Forest Raceway",
    var carClass: String = "Gr.3",
    var lapProgress: Double = 0.0,
    var liveDeltaSeconds: Double = 0.0,
    var fuelLapsRemaining: Double = 0.0,
    var pitWindowOpen: Boolean = false,
) {
    val speedKph: Double get() = speedMps.toDouble() * 3.6
    val speedMph: Double get() = speedMps.toDouble() * 2.2369362920544
    val boostBar: Double get() = (boostKpa.toDouble() - 100.0) / 100.0

    val fuelPercent: Double
        get() = if (fuelCapacity > 0f) {
            (fuelLevel / fuelCapacity).toDouble() * 100.0
        } else {
            fuelLevel.toDouble()
        }

    val throttleNorm: Double get() = throttle / 255.0
    val brakeNorm: Double get() = brake / 255.0

    val rpmFraction: Double
        get() {
            val maxRpm = maxOf(alertMaxRpm, 1)
            return minOf(1.0, maxOf(0.0, engineRpm.toDouble() / maxRpm.toDouble()))
        }

    val gearDisplay: String
        get() = when (currentGear) {
            0 -> "R"
            15 -> "N"
            else -> currentGear.toString()
        }

    fun serialize(size: Int = 0x140): ByteArray {
        val p = ByteArray(maxOf(size, MINIMUM_SIZE))
        Binary.writeU32(p, 0, MAGIC)
        Binary.writeF32(p, 0x04, positionX)
        Binary.writeF32(p, 0x08, positionY)
        Binary.writeF32(p, 0x0C, positionZ)
        Binary.writeF32(p, 0x10, velocityX)
        Binary.writeF32(p, 0x14, velocityY)
        Binary.writeF32(p, 0x18, velocityZ)
        Binary.writeF32(p, 0x1C, rotationPitch)
        Binary.writeF32(p, 0x20, rotationYaw)
        Binary.writeF32(p, 0x24, rotationRoll)
        Binary.writeF32(p, 0x28, heading)
        Binary.writeF32(p, 0x2C, angularVelocityX)
        Binary.writeF32(p, 0x30, angularVelocityY)
        Binary.writeF32(p, 0x34, angularVelocityZ)
        Binary.writeF32(p, 0x38, rideHeight)
        Binary.writeF32(p, 0x3C, engineRpm)
        Binary.writeF32(p, 0x44, fuelLevel)
        Binary.writeF32(p, 0x48, fuelCapacity)
        Binary.writeF32(p, 0x4C, speedMps)
        Binary.writeF32(p, 0x50, boostKpa)
        Binary.writeF32(p, 0x54, oilPressure)
        Binary.writeF32(p, 0x58, waterTemp)
        Binary.writeF32(p, 0x5C, oilTemp)
        Binary.writeF32(p, 0x60, tireTempFL)
        Binary.writeF32(p, 0x64, tireTempFR)
        Binary.writeF32(p, 0x68, tireTempRL)
        Binary.writeF32(p, 0x6C, tireTempRR)
        Binary.writeI32(p, 0x70, packetId)
        Binary.writeI16(p, 0x74, currentLap.toShort())
        Binary.writeI16(p, 0x76, totalLaps.toShort())
        Binary.writeI32(p, 0x78, bestLapMs)
        Binary.writeI32(p, 0x7C, lastLapMs)
        Binary.writeI32(p, 0x80, dayProgressionMs)
        Binary.writeI16(p, 0x84, preRaceStartPos.toShort())
        Binary.writeI16(p, 0x86, numCarsPreRace.toShort())
        Binary.writeI16(p, 0x88, alertMinRpm.toShort())
        Binary.writeI16(p, 0x8A, alertMaxRpm.toShort())
        Binary.writeI16(p, 0x8C, calcMaxSpeedKph.toShort())
        Binary.writeI16(p, 0x8E, flags.rawValue.toShort())
        p[0x90] = ((currentGear and 0x0F) or ((suggestedGear and 0x0F) shl 4)).toByte()
        p[0x91] = (throttle and 0xFF).toByte()
        p[0x92] = (brake and 0xFF).toByte()
        Binary.writeF32(p, 0xB4, wheelSpeedFL)
        Binary.writeF32(p, 0xB8, wheelSpeedFR)
        Binary.writeF32(p, 0xBC, wheelSpeedRL)
        Binary.writeF32(p, 0xC0, wheelSpeedRR)
        Binary.writeF32(p, 0xC4, tireRadiusFL)
        Binary.writeF32(p, 0xC8, tireRadiusFR)
        Binary.writeF32(p, 0xCC, tireRadiusRL)
        Binary.writeF32(p, 0xD0, tireRadiusRR)
        Binary.writeF32(p, 0xD4, suspensionFL)
        Binary.writeF32(p, 0xD8, suspensionFR)
        Binary.writeF32(p, 0xDC, suspensionRL)
        Binary.writeF32(p, 0xE0, suspensionRR)
        Binary.writeF32(p, 0xF4, clutchPedal)
        Binary.writeF32(p, 0xF8, clutchEngagement)
        Binary.writeF32(p, 0xFC, rpmAfterClutch)
        Binary.writeF32(p, 0x100, transmissionTopSpeedRatio)
        val ratios = FloatArray(8)
        for (i in 0 until minOf(8, gearRatios.size)) ratios[i] = gearRatios[i]
        for (i in 0 until 8) {
            Binary.writeF32(p, 0x104 + i * 4, ratios[i])
        }
        Binary.writeI32(p, 0x124, carCode)
        return p
    }

    companion object {
        const val MINIMUM_SIZE = 0x128
        val MAGIC: UInt = 0x47375330u
        val idle: TelemetryPacket get() = TelemetryPacket()

        fun parse(p: ByteArray): TelemetryPacket {
            if (p.size < MINIMUM_SIZE) {
                throw PacketParseException(
                    "Packet too small: ${p.size} bytes (need >= $MINIMUM_SIZE)",
                )
            }
            val pkt = TelemetryPacket()
            pkt.positionX = Binary.readF32(p, 0x04)
            pkt.positionY = Binary.readF32(p, 0x08)
            pkt.positionZ = Binary.readF32(p, 0x0C)
            pkt.velocityX = Binary.readF32(p, 0x10)
            pkt.velocityY = Binary.readF32(p, 0x14)
            pkt.velocityZ = Binary.readF32(p, 0x18)
            pkt.rotationPitch = Binary.readF32(p, 0x1C)
            pkt.rotationYaw = Binary.readF32(p, 0x20)
            pkt.rotationRoll = Binary.readF32(p, 0x24)
            pkt.heading = Binary.readF32(p, 0x28)
            pkt.angularVelocityX = Binary.readF32(p, 0x2C)
            pkt.angularVelocityY = Binary.readF32(p, 0x30)
            pkt.angularVelocityZ = Binary.readF32(p, 0x34)
            pkt.rideHeight = Binary.readF32(p, 0x38)
            pkt.engineRpm = Binary.readF32(p, 0x3C)
            pkt.fuelLevel = Binary.readF32(p, 0x44)
            pkt.fuelCapacity = Binary.readF32(p, 0x48)
            pkt.speedMps = Binary.readF32(p, 0x4C)
            pkt.boostKpa = Binary.readF32(p, 0x50)
            pkt.oilPressure = Binary.readF32(p, 0x54)
            pkt.waterTemp = Binary.readF32(p, 0x58)
            pkt.oilTemp = Binary.readF32(p, 0x5C)
            pkt.tireTempFL = Binary.readF32(p, 0x60)
            pkt.tireTempFR = Binary.readF32(p, 0x64)
            pkt.tireTempRL = Binary.readF32(p, 0x68)
            pkt.tireTempRR = Binary.readF32(p, 0x6C)
            pkt.packetId = Binary.readI32(p, 0x70)
            pkt.currentLap = Binary.readI16(p, 0x74).toInt()
            pkt.totalLaps = Binary.readI16(p, 0x76).toInt()
            pkt.bestLapMs = Binary.readI32(p, 0x78)
            pkt.lastLapMs = Binary.readI32(p, 0x7C)
            pkt.dayProgressionMs = Binary.readI32(p, 0x80)
            pkt.preRaceStartPos = Binary.readI16(p, 0x84).toInt()
            pkt.numCarsPreRace = Binary.readI16(p, 0x86).toInt()
            pkt.alertMinRpm = Binary.readI16(p, 0x88).toInt()
            pkt.alertMaxRpm = Binary.readI16(p, 0x8A).toInt()
            pkt.calcMaxSpeedKph = Binary.readI16(p, 0x8C).toInt()
            pkt.flags = SimulatorFlags(Binary.readI16(p, 0x8E).toInt() and 0xFFFF)
            val gears = p[0x90].toInt() and 0xFF
            pkt.currentGear = gears and 0x0F
            pkt.suggestedGear = (gears shr 4) and 0x0F
            pkt.throttle = p[0x91].toInt() and 0xFF
            pkt.brake = p[0x92].toInt() and 0xFF
            pkt.wheelSpeedFL = Binary.readF32(p, 0xB4)
            pkt.wheelSpeedFR = Binary.readF32(p, 0xB8)
            pkt.wheelSpeedRL = Binary.readF32(p, 0xBC)
            pkt.wheelSpeedRR = Binary.readF32(p, 0xC0)
            pkt.tireRadiusFL = Binary.readF32(p, 0xC4)
            pkt.tireRadiusFR = Binary.readF32(p, 0xC8)
            pkt.tireRadiusRL = Binary.readF32(p, 0xCC)
            pkt.tireRadiusRR = Binary.readF32(p, 0xD0)
            pkt.suspensionFL = Binary.readF32(p, 0xD4)
            pkt.suspensionFR = Binary.readF32(p, 0xD8)
            pkt.suspensionRL = Binary.readF32(p, 0xDC)
            pkt.suspensionRR = Binary.readF32(p, 0xE0)
            pkt.clutchPedal = Binary.readF32(p, 0xF4)
            pkt.clutchEngagement = Binary.readF32(p, 0xF8)
            pkt.rpmAfterClutch = Binary.readF32(p, 0xFC)
            pkt.transmissionTopSpeedRatio = Binary.readF32(p, 0x100)
            pkt.gearRatios = FloatArray(8) { i -> Binary.readF32(p, 0x104 + i * 4) }
            pkt.carCode = Binary.readI32(p, 0x124)
            return pkt
        }
    }
}
