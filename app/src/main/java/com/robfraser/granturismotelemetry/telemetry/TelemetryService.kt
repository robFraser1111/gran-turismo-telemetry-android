package com.robfraser.granturismotelemetry.telemetry

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.robfraser.granturismotelemetry.models.AppSettings
import kotlin.math.atan2
import kotlin.math.hypot

sealed class ConnectionState {
    data object Idle : ConnectionState()
    data object Waiting : ConnectionState()
    data object Live : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    val label: String
        get() = when (this) {
            Idle -> "Idle"
            Waiting -> "Waiting for telemetry"
            Live -> "Receiving telemetry"
            is Error -> message
        }

    val isLive: Boolean get() = this is Live
}

/**
 * Owns the active telemetry source (simulator or UDP) and publishes UI-sampled
 * packets at ~30 Hz plus rolling traces.
 */
class TelemetryService {
    var packet by mutableStateOf(TelemetryPacket.idle)
    var state by mutableStateOf<ConnectionState>(ConnectionState.Idle)
    val throttleTrace = mutableStateListOf<Double>()
    val brakeTrace = mutableStateListOf<Double>()
    val deltaTrace = mutableStateListOf<Double>()
    var rawPackets by mutableIntStateOf(0)
    var decodedPackets by mutableIntStateOf(0)
    var decodeFailures by mutableIntStateOf(0)
    var lastDecodeError by mutableStateOf<String?>(null)

    private var simulator: TelemetrySimulator? = null
    private var udp: Gt7UdpClient? = null
    @Volatile private var lastPublishNs = 0L
    private val minPublishNs = 1_000_000_000L / 30L
    private val maxTrace = 120
    private var started = false
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun start(settings: AppSettings) {
        if (!started) {
            started = true
        }
        applySource(settings)
    }

    fun applySource(settings: AppSettings) {
        if (settings.useSimulator) {
            startSimulator()
        } else {
            startUDP(settings.ps5IP)
        }
    }

    fun connect(settings: AppSettings) {
        postMain {
            state = ConnectionState.Waiting
        }
        applySource(settings)
    }

    fun disconnect() {
        simulator?.stop()
        simulator = null
        udp?.stop()
        udp = null
        postMain { state = ConnectionState.Idle }
    }

    private fun startSimulator() {
        udp?.stop()
        udp = null
        if (simulator == null) {
            val sim = TelemetrySimulator()
            sim.onPacket = { pkt -> ingest(pkt, fromSimulator = true) }
            simulator = sim
        }
        postMain { state = ConnectionState.Waiting }
        simulator?.start()
    }

    private fun startUDP(host: String) {
        simulator?.stop()
        simulator = null
        udp?.stop()
        val client = Gt7UdpClient()
        client.onPacket = { pkt -> ingest(pkt, fromSimulator = false) }
        client.onRaw = { _ -> postMain { rawPackets += 1 } }
        client.onDecodeFailed = { reason ->
            postMain {
                decodeFailures += 1
                lastDecodeError = reason
            }
        }
        client.onStatus = { msg ->
            postMain {
                val failed = msg.contains("failed", ignoreCase = true) ||
                    msg.contains("Invalid", ignoreCase = true)
                when {
                    failed -> state = ConnectionState.Error(msg)
                    state !is ConnectionState.Live && state !is ConnectionState.Error ->
                        state = ConnectionState.Waiting
                }
            }
        }
        udp = client
        postMain { state = ConnectionState.Waiting }
        client.start(host)
    }

    private fun ingest(pkt: TelemetryPacket, fromSimulator: Boolean) {
        val now = System.nanoTime()
        if (now - lastPublishNs < minPublishNs) return
        lastPublishNs = now

        val next = if (fromSimulator) {
            pkt
        } else {
            val prev = packet
            pkt.trackName = prev.trackName.ifEmpty { "Deep Forest Raceway" }
            pkt.carClass = prev.carClass.ifEmpty { "Gr.3" }
            if (pkt.bestLapMs > 0 && pkt.lastLapMs > 0) {
                pkt.liveDeltaSeconds = (pkt.lastLapMs - pkt.bestLapMs) / 1000.0
            }
            if (pkt.fuelCapacity > 0) {
                val pct = (pkt.fuelLevel / pkt.fuelCapacity).toDouble()
                pkt.fuelLapsRemaining = maxOf(0.0, pct * 15)
                pkt.pitWindowOpen = pct < 0.5 && pct > 0.18
            }
            val r = hypot(pkt.positionX.toDouble(), pkt.positionZ.toDouble())
            if (r > 1) {
                var ang = atan2(pkt.positionZ.toDouble(), pkt.positionX.toDouble())
                if (ang < 0) ang += 2 * Math.PI
                pkt.lapProgress = ang / (2 * Math.PI)
            }
            pkt
        }

        postMain {
            decodedPackets += 1
            packet = next
            state = ConnectionState.Live
            append(throttleTrace, next.throttleNorm)
            append(brakeTrace, next.brakeNorm)
            append(deltaTrace, next.liveDeltaSeconds)
        }
    }

    private fun append(values: MutableList<Double>, value: Double) {
        values.add(value)
        if (values.size > maxTrace) {
            values.removeAt(0)
        }
    }

    private fun postMain(block: () -> Unit) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
