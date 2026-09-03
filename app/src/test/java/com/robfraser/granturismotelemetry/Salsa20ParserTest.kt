package com.robfraser.granturismotelemetry

import com.robfraser.granturismotelemetry.telemetry.Binary
import com.robfraser.granturismotelemetry.telemetry.Gt7Crypto
import com.robfraser.granturismotelemetry.telemetry.Salsa20
import com.robfraser.granturismotelemetry.telemetry.TelemetryPacket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Salsa20ParserTest {

    @Test
    fun salsa20IsInvolution() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val original = ByteArray(200) { it.toByte() }
        val buf = original.copyOf()
        Salsa20.xorInPlace(key, nonce, buf)
        assertTrue(buf.contentEquals(original).not())
        Salsa20.xorInPlace(key, nonce, buf)
        assertTrue(buf.contentEquals(original))
    }

    @Test
    fun salsa20FirstBlockKnownVector() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2)
        val block = Salsa20.firstBlock(key, nonce)
        val expectedHex = "9a550e1ba1528b269e3473558c4aeea28ace1c870be826aedebc789f60a9da410eb808928b2d8c81e6dd680f1a4e2e4f231333cf6d08efe20ff470279ccad55a"
        assertEquals(64, block.size)
        val actual = block.joinToString("") { String.format("%02x", it.toInt() and 0xFF) }
        assertEquals(expectedHex, actual)
    }

    @Test
    fun packetRoundTripSmokeTest() {
        val plaintext = ByteArray(0x140)
        Binary.writeU32(plaintext, 0, TelemetryPacket.MAGIC)
        Binary.writeF32(plaintext, 0x3C, 7250.5f)
        Binary.writeF32(plaintext, 0x4C, 55.55f)
        Binary.writeF32(plaintext, 0x50, 160.0f)
        Binary.writeF32(plaintext, 0x44, 87.5f)
        Binary.writeF32(plaintext, 0x48, 100f)
        Binary.writeF32(plaintext, 0x60, 90.0f)
        Binary.writeF32(plaintext, 0x64, 91.5f)
        Binary.writeF32(plaintext, 0x68, 92.0f)
        Binary.writeF32(plaintext, 0x6C, 93.0f)
        Binary.writeI32(plaintext, 0x70, 12345)
        Binary.writeI16(plaintext, 0x74, 3)
        Binary.writeI16(plaintext, 0x76, 10)
        Binary.writeI16(plaintext, 0x8A, 8500)
        plaintext[0x90] = 0x04
        plaintext[0x91] = 200.toByte()
        plaintext[0x92] = 40
        Binary.writeF32(plaintext, 0xF4, 0.85f)
        Binary.writeF32(plaintext, 0x08, 512.5f)
        Binary.writeF32(plaintext, 0x14, 3.25f)
        Binary.writeF32(plaintext, 0x30, 0.42f)
        Binary.writeF32(plaintext, 0x38, 0.085f)
        Binary.writeF32(plaintext, 0x54, 4.75f)
        Binary.writeF32(plaintext, 0x58, 88.0f)
        Binary.writeF32(plaintext, 0x5C, 108.0f)
        Binary.writeF32(plaintext, 0xB4, 168.3f)
        Binary.writeF32(plaintext, 0xC4, 0.33f)

        val cipher = Gt7Crypto.encryptForTest(plaintext, 0x12345678u)
        assertEquals(0x12345678u, Binary.readU32(cipher, 0x40))

        val decoded = Gt7Crypto.tryDecode(cipher)
        assertNull(decoded.reason)
        val packet = decoded.packet
        assertNotNull(packet)
        packet!!
        assertEquals(7250.5f, packet.engineRpm, 0.01f)
        assertEquals(55.55f, packet.speedMps, 0.01f)
        assertEquals(160.0f, packet.boostKpa, 0.01f)
        assertEquals(87.5f, packet.fuelLevel, 0.01f)
        assertEquals(90.0f, packet.tireTempFL, 0.01f)
        assertEquals(4, packet.currentGear)
        assertEquals(200, packet.throttle)
        assertEquals(40, packet.brake)
        assertEquals(0.85f, packet.clutchPedal, 0.01f)
        assertEquals(512.5f, packet.positionY, 0.01f)
        assertEquals(3.25f, packet.velocityY, 0.01f)
        assertEquals(0.42f, packet.angularVelocityY, 0.01f)
        assertEquals(0.085f, packet.rideHeight, 0.001f)
        assertEquals(4.75f, packet.oilPressure, 0.01f)
        assertEquals(88.0f, packet.waterTemp, 0.01f)
        assertEquals(108.0f, packet.oilTemp, 0.01f)
        assertEquals(168.3f, packet.wheelSpeedFL, 0.01f)
        assertEquals(0.33f, packet.tireRadiusFL, 0.001f)
        assertEquals(12345, packet.packetId)
        assertEquals(3, packet.currentLap)
        assertEquals(10, packet.totalLaps)
    }

    @Test
    fun badMagicIsDropped() {
        val plaintext = ByteArray(0x140)
        Binary.writeU32(plaintext, 0, 0xDEADBEEFu)
        Binary.writeF32(plaintext, 0x3C, 1000f)
        val cipher = Gt7Crypto.encryptForTest(plaintext, 0x11111111u)
        val decoded = Gt7Crypto.tryDecode(cipher)
        assertNull(decoded.packet)
        assertNotNull(decoded.reason)
        assertTrue(decoded.reason!!.contains("bad magic"))
    }

    @Test
    fun shortPacketIsDropped() {
        val decoded = Gt7Crypto.tryDecode(ByteArray(20) { 1 })
        assertNull(decoded.packet)
        assertTrue(decoded.reason!!.contains("short packet"))
    }

    @Test
    fun gearDisplay() {
        assertEquals("N", TelemetryPacket(currentGear = 15).gearDisplay)
        assertEquals("R", TelemetryPacket(currentGear = 0).gearDisplay)
        assertEquals("4", TelemetryPacket(currentGear = 4).gearDisplay)
    }

    @Test
    fun keyIsFirst32BytesOfSeed() {
        val seed = "Simulator Interface Packet GT7 ver 0.0".encodeToByteArray()
        assertTrue(Gt7Crypto.key.contentEquals(seed.copyOf(32)))
        assertEquals(32, Gt7Crypto.key.size)
    }

    @Test
    fun nonceLayout() {
        val nonce = ByteArray(8)
        val oiv = 0x12345678u
        Gt7Crypto.buildNonce(oiv, nonce)
        assertEquals(oiv xor 0xDEADBEAFu, Binary.readU32(nonce, 0))
        assertEquals(oiv, Binary.readU32(nonce, 4))
    }
}
