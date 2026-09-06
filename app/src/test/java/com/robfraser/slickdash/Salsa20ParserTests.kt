package com.robfraser.slickdash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Salsa20ParserTests {
    @Test
    fun salsa20IsInvolution() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        val original = ByteArray(200) { it.toByte() }
        val buf = original.copyOf()
        Salsa20.xorInPlace(key, nonce, buf)
        assertFalse(buf.contentEquals(original))
        Salsa20.xorInPlace(key, nonce, buf)
        assertArrayEquals(original, buf)
    }

    @Test
    fun salsa20FirstBlockKnownVector() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = byteArrayOf(9, 8, 7, 6, 5, 4, 3, 2)
        val block = Salsa20.firstBlock(key, nonce)
        val expectedHex =
            "9a550e1ba1528b269e3473558c4aeea28ace1c870be826aedebc789f60a9da410eb808928b2d8c81e6dd680f1a4e2e4f231333cf6d08efe20ff470279ccad55a"
        assertEquals(64, block.size)
        val actual = block.joinToString("") { "%02x".format(it) }
        assertEquals(expectedHex, actual)
    }

    @Test
    fun packetRoundTripSmokeTest() {
        val plaintext = ByteArray(0x140)
        writeU32(plaintext, 0, Gt7Crypto.MAGIC)
        writeF32(plaintext, 0x3C, 7250.5f)
        writeF32(plaintext, 0x4C, 55.55f)
        writeF32(plaintext, 0x44, 87.5f)
        writeF32(plaintext, 0x48, 100f)
        writeF32(plaintext, 0x60, 90.0f)
        writeF32(plaintext, 0x64, 91.5f)
        writeF32(plaintext, 0x68, 92.0f)
        writeF32(plaintext, 0x6C, 93.0f)
        writeI16(plaintext, 0x74, 3)
        writeI16(plaintext, 0x8A, 8500)
        plaintext[0x90] = 0x04
        plaintext[0x91] = 200.toByte()
        plaintext[0x92] = 40

        val cipher = Gt7Crypto.encryptForTest(plaintext, 0x12345678)
        assertEquals(0x12345678, ByteBuffer.wrap(cipher, 0x40, 4).order(ByteOrder.LITTLE_ENDIAN).int)

        val decoded = Gt7Crypto.tryDecode(cipher)
        assertNull(decoded.reason)
        assertNotNull(decoded.packet)
        val packet = decoded.packet!!
        assertEquals(7250.5f, packet.rpm, 0.01f)
        assertEquals(55.55f, packet.speedMps, 0.01f)
        assertEquals(87.5f, packet.fuelLevel, 0.01f)
        assertEquals(90.0f, packet.tireFL, 0.01f)
        assertEquals(91.5f, packet.tireFR, 0.01f)
        assertEquals(92.0f, packet.tireRL, 0.01f)
        assertEquals(93.0f, packet.tireRR, 0.01f)
        assertEquals(4, packet.gear)
        assertEquals(200, packet.throttle)
        assertEquals(40, packet.brake)
        assertEquals(3, packet.currentLap)
    }

    @Test
    fun badMagicIsDropped() {
        val plaintext = ByteArray(0x140)
        writeU32(plaintext, 0, 0xDEADBEEF.toInt())
        writeF32(plaintext, 0x3C, 1000f)
        val cipher = Gt7Crypto.encryptForTest(plaintext, 0x11111111)
        val decoded = Gt7Crypto.tryDecode(cipher)
        assertNull(decoded.packet)
        assertNotNull(decoded.reason)
        assertTrue(decoded.reason!!.contains("bad magic", ignoreCase = true))
    }

    @Test
    fun shortPacketIsDropped() {
        val decoded = Gt7Crypto.tryDecode(ByteArray(20) { 1 })
        assertNull(decoded.packet)
        assertTrue(decoded.reason!!.contains("short packet"))
    }

    @Test
    fun gearDisplay() {
        assertEquals("N", pkt(gear = 15).gearDisplay)
        assertEquals("R", pkt(gear = 0).gearDisplay)
        assertEquals("4", pkt(gear = 4).gearDisplay)
    }

    @Test
    fun keyIsFirst32BytesOfSeed() {
        val seed = "Simulator Interface Packet GT7 ver 0.0".toByteArray(Charsets.UTF_8)
        assertArrayEquals(seed.copyOf(32), Gt7Crypto.key)
        assertEquals(32, Gt7Crypto.key.size)
    }

    @Test
    fun nonceLayout() {
        val nonce = ByteArray(8)
        val oiv = 0x12345678
        Gt7Crypto.buildNonce(oiv, nonce)
        val le = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(oiv xor 0xDEADBEAF.toInt(), le.int)
        assertEquals(oiv, le.int)
    }

    private fun pkt(gear: Int) = TelemetryPacket(
        posX = 0f, posZ = 0f, rpm = 0f, fuelLevel = 0f, fuelCapacity = 100f,
        speedMps = 0f, tireFL = 0f, tireFR = 0f, tireRL = 0f, tireRR = 0f,
        currentLap = 0, bestLapMs = 0, lastLapMs = 0,
        alertMaxRpm = 8000, flags = 0, gear = gear, throttle = 0, brake = 0,
    )

    private fun writeF32(buf: ByteArray, offset: Int, value: Float) {
        ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putFloat(value)
    }

    private fun writeU32(buf: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(buf, offset, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(value)
    }

    private fun writeI16(buf: ByteArray, offset: Int, value: Int) {
        ByteBuffer.wrap(buf, offset, 2).order(ByteOrder.LITTLE_ENDIAN).putShort(value.toShort())
    }
}
