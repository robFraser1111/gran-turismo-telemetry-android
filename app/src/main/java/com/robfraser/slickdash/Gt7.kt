package com.robfraser.slickdash

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.min

object Salsa20 {
    private val sigma = "expand 32-byte k".toByteArray(Charsets.US_ASCII)
    fun xorInPlace(key: ByteArray, nonce: ByteArray, cipher: ByteArray) {
        require(key.size == 32 && nonce.size == 8)
        val state = IntArray(16)
        fun u32(b: ByteArray, o: Int) = ByteBuffer.wrap(b, o, 4).order(ByteOrder.LITTLE_ENDIAN).int
        state[0] = u32(sigma, 0); state[5] = u32(sigma, 4)
        state[10] = u32(sigma, 8); state[15] = u32(sigma, 12)
        for (i in 0..3) state[1 + i] = u32(key, i * 4)
        for (i in 0..3) state[11 + i] = u32(key, 16 + i * 4)
        state[6] = u32(nonce, 0); state[7] = u32(nonce, 4)
        val block = ByteArray(64)
        var offset = 0
        while (offset < cipher.size) {
            generate(state, block)
            val take = min(64, cipher.size - offset)
            for (i in 0 until take) cipher[offset + i] = (cipher[offset + i].toInt() xor block[i].toInt()).toByte()
            offset += take
            state[8]++
            if (state[8] == 0) state[9]++
        }
    }

    /** First 64 bytes of keystream for (key, nonce) with counter 0. Used by tests. */
    fun firstBlock(key: ByteArray, nonce: ByteArray): ByteArray {
        val zeros = ByteArray(64)
        xorInPlace(key, nonce, zeros)
        return zeros
    }

    private fun rotl(v: Int, c: Int) = (v shl c) or (v ushr (32 - c))
    private fun generate(input: IntArray, out: ByteArray) {
        val x = input.copyOf()
        repeat(10) {
            x[4] = x[4] xor rotl(x[0] + x[12], 7)
            x[8] = x[8] xor rotl(x[4] + x[0], 9)
            x[12] = x[12] xor rotl(x[8] + x[4], 13)
            x[0] = x[0] xor rotl(x[12] + x[8], 18)
            x[9] = x[9] xor rotl(x[5] + x[1], 7)
            x[13] = x[13] xor rotl(x[9] + x[5], 9)
            x[1] = x[1] xor rotl(x[13] + x[9], 13)
            x[5] = x[5] xor rotl(x[1] + x[13], 18)
            x[14] = x[14] xor rotl(x[10] + x[6], 7)
            x[2] = x[2] xor rotl(x[14] + x[10], 9)
            x[6] = x[6] xor rotl(x[2] + x[14], 13)
            x[10] = x[10] xor rotl(x[6] + x[2], 18)
            x[3] = x[3] xor rotl(x[15] + x[11], 7)
            x[7] = x[7] xor rotl(x[3] + x[15], 9)
            x[11] = x[11] xor rotl(x[7] + x[3], 13)
            x[15] = x[15] xor rotl(x[11] + x[7], 18)
            x[1] = x[1] xor rotl(x[0] + x[3], 7)
            x[2] = x[2] xor rotl(x[1] + x[0], 9)
            x[3] = x[3] xor rotl(x[2] + x[1], 13)
            x[0] = x[0] xor rotl(x[3] + x[2], 18)
            x[6] = x[6] xor rotl(x[5] + x[4], 7)
            x[7] = x[7] xor rotl(x[6] + x[5], 9)
            x[4] = x[4] xor rotl(x[7] + x[6], 13)
            x[5] = x[5] xor rotl(x[4] + x[7], 18)
            x[11] = x[11] xor rotl(x[10] + x[9], 7)
            x[8] = x[8] xor rotl(x[11] + x[10], 9)
            x[9] = x[9] xor rotl(x[8] + x[11], 13)
            x[10] = x[10] xor rotl(x[9] + x[8], 18)
            x[12] = x[12] xor rotl(x[15] + x[14], 7)
            x[13] = x[13] xor rotl(x[12] + x[15], 9)
            x[14] = x[14] xor rotl(x[13] + x[12], 13)
            x[15] = x[15] xor rotl(x[14] + x[13], 18)
        }
        val bb = ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN)
        for (i in 0..15) bb.putInt(x[i] + input[i])
    }
}

enum class QualityRating { Poor, Fair, Good }

/** Connection quality from packet rate and decode-error ratio. */
object ConnectionQuality {
    fun classify(packetsPerSecond: Double, errorRatio: Double): QualityRating {
        val err = errorRatio.coerceIn(0.0, 1.0)
        if (packetsPerSecond >= 40 && err < 0.08) return QualityRating.Good
        if (packetsPerSecond >= 12 && err < 0.25) return QualityRating.Fair
        return QualityRating.Poor
    }
}

data class DecodeResult(val packet: TelemetryPacket?, val reason: String?)

object Gt7Crypto {
    private const val DEAD = 0xDEADBEAF.toInt()
    const val MAGIC = 0x47375330
    const val KEY_SEED = "Simulator Interface Packet GT7 ver 0.0"
    val key: ByteArray = KEY_SEED.toByteArray(Charsets.UTF_8).copyOf(32)

    fun buildNonce(oiv: Int, nonce: ByteArray) {
        require(nonce.size >= 8)
        val le = ByteBuffer.wrap(nonce).order(ByteOrder.LITTLE_ENDIAN)
        le.putInt(oiv xor DEAD)
        le.putInt(oiv)
    }

    /**
     * Builds ciphertext that decrypts back to [plaintext] via [tryDecode].
     * IV bytes at offset 0x40 in the returned buffer are literally [ciphertextIv].
     */
    fun encryptForTest(plaintext: ByteArray, ciphertextIv: Int): ByteArray {
        val nonce = ByteArray(8)
        buildNonce(ciphertextIv, nonce)
        val cipher = plaintext.copyOf()
        Salsa20.xorInPlace(key, nonce, cipher)
        ByteBuffer.wrap(cipher, 0x40, 4).order(ByteOrder.LITTLE_ENDIAN).putInt(ciphertextIv)
        return cipher
    }

    fun tryDecode(raw: ByteArray): DecodeResult {
        if (raw.size < TelemetryPacket.MIN) {
            return DecodeResult(null, "short packet (${raw.size} bytes, need >= ${TelemetryPacket.MIN})")
        }
        return try {
            val buf = raw.copyOf()
            val oiv = ByteBuffer.wrap(buf, 0x40, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val nonce = ByteArray(8)
            buildNonce(oiv, nonce)
            Salsa20.xorInPlace(key, nonce, buf)
            val magic = ByteBuffer.wrap(buf, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
            if (magic != MAGIC) {
                return DecodeResult(
                    null,
                    "bad magic 0x${magic.toUInt().toString(16).uppercase().padStart(8, '0')} after decrypt (expected 0x47375330 'G7S0')",
                )
            }
            DecodeResult(TelemetryPacket.parse(buf), null)
        } catch (ex: Exception) {
            DecodeResult(null, ex.message)
        }
    }
}

data class TelemetryPacket(
    val posX: Float, val posZ: Float,
    val rpm: Float, val fuelLevel: Float, val fuelCapacity: Float,
    val speedMps: Float, val tireFL: Float, val tireFR: Float, val tireRL: Float, val tireRR: Float,
    val currentLap: Int, val totalLaps: Int, val bestLapMs: Int, val lastLapMs: Int,
    val alertMaxRpm: Int, val flags: Int, val gear: Int, val throttle: Int, val brake: Int,
    val carCode: Int = 0,
) {
    val speedKph get() = speedMps * 3.6
    val fuelPercent get() = if (fuelCapacity > 0f) (fuelLevel / fuelCapacity) * 100.0 else fuelLevel.toDouble()
    val throttlePct get() = (throttle / 255.0 * 100).toInt()
    val brakePct get() = (brake / 255.0 * 100).toInt()
    val throttleNorm get() = throttle / 255.0
    val brakeNorm get() = brake / 255.0
    val rpmFrac get() = (rpm / maxOf(alertMaxRpm, 1)).coerceIn(0f, 1f)
    val gearDisplay get() = when (gear) { 0 -> "R"; 15 -> "N"; else -> gear.toString() }
    /** Car on track, not paused, not loading — same as Windows IsRacing. */
    val onTrack get() = flags and 1 != 0 && flags and 2 == 0 && flags and 4 == 0
    val isPaused get() = flags and 2 != 0
    val isLoading get() = flags and 4 != 0
    val isRacing get() = onTrack
    companion object {
        const val MIN = 0x128
        fun parse(p: ByteArray): TelemetryPacket {
            fun f(o: Int) = ByteBuffer.wrap(p, o, 4).order(ByteOrder.LITTLE_ENDIAN).float
            fun i16(o: Int) = ByteBuffer.wrap(p, o, 2).order(ByteOrder.LITTLE_ENDIAN).short.toInt()
            fun i32(o: Int) = ByteBuffer.wrap(p, o, 4).order(ByteOrder.LITTLE_ENDIAN).int
            val gears = p[0x90].toInt() and 0xFF
            return TelemetryPacket(
                posX = f(0x04), posZ = f(0x0C), rpm = f(0x3C),
                fuelLevel = f(0x44), fuelCapacity = f(0x48), speedMps = f(0x4C),
                tireFL = f(0x60), tireFR = f(0x64), tireRL = f(0x68), tireRR = f(0x6C),
                currentLap = i16(0x74), totalLaps = i16(0x76),
                bestLapMs = i32(0x78), lastLapMs = i32(0x7C),
                alertMaxRpm = i16(0x8A), flags = i16(0x8E),
                gear = gears and 0x0F, throttle = p[0x91].toInt() and 0xFF, brake = p[0x92].toInt() and 0xFF,
                carCode = if (p.size >= 0x128) i32(0x124) else 0,
            )
        }
    }
}

class Gt7UdpClient(
    private val onPacket: (TelemetryPacket) -> Unit,
    private val onRaw: () -> Unit,
    private val onDecodeFail: () -> Unit,
    private val onPeer: (String) -> Unit,
    private val onStatus: (String) -> Unit,
) {
    companion object { const val SEND = 33739; const val RECV = 33740 }
    private val running = AtomicBoolean(false)
    @Volatile var peer: String? = null; private set
    @Volatile var discovering = false; private set
    private var socket: DatagramSocket? = null
    private var thread: Thread? = null

    fun startDiscover() = start(null)
    fun startHost(ip: String) = start(ip)

    private fun start(ip: String?) {
        stop()
        running.set(true)
        discovering = ip == null
        peer = null
        thread = Thread({ runLoop(ip) }, "gt7-udp").also { it.isDaemon = true; it.start() }
    }

    fun stop() {
        running.set(false)
        discovering = false
        try { socket?.close() } catch (_: Exception) {}
        socket = null
        thread = null
    }

    private fun runLoop(host: String?) {
        val sock = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                broadcast = true
                bind(InetSocketAddress(RECV))
            }
        } catch (e: Exception) {
            onStatus("bind :$RECV failed: ${e.message}"); return
        }
        socket = sock
        val targets = if (host == null) discoverTargets() else listOf(InetSocketAddress(host, SEND))
        if (host == null) onStatus("Looking for GT7 on this network…")
        else onStatus("Heartbeat → $host:$SEND")
        val hb = byteArrayOf('A'.code.toByte())
        val buf = ByteArray(4096)
        var lastHb = 0L
        while (running.get()) {
            val now = System.currentTimeMillis()
            if (now - lastHb > 250) {
                lastHb = now
                for (t in (if (peer != null) listOf(InetSocketAddress(peer, SEND)) else targets)) {
                    try { sock.send(DatagramPacket(hb, hb.size, t)) } catch (_: Exception) {}
                }
            }
            sock.soTimeout = 50
            try {
                val pkt = DatagramPacket(buf, buf.size)
                sock.receive(pkt)
                onRaw()
                val data = buf.copyOf(pkt.length)
                val decoded = Gt7Crypto.tryDecode(data)
                if (decoded.packet == null) { onDecodeFail(); continue }
                val from = pkt.address.hostAddress
                if (peer == null && from != null) {
                    peer = from
                    discovering = false
                    onPeer(from)
                    onStatus("Connected $from")
                }
                onPacket(decoded.packet)
            } catch (_: Exception) { /* timeout */ }
        }
    }

    private fun discoverTargets(): List<InetSocketAddress> {
        val out = mutableListOf(InetSocketAddress("255.255.255.255", SEND))
        try {
            NetworkInterface.getNetworkInterfaces()?.toList().orEmpty().forEach { nic ->
                if (!nic.isUp || nic.isLoopback) return@forEach
                nic.interfaceAddresses.forEach { ia ->
                    val b = ia.broadcast ?: return@forEach
                    if (b is Inet4Address) out += InetSocketAddress(b, SEND)
                }
            }
        } catch (_: Exception) {}
        return out.distinct()
    }
}
