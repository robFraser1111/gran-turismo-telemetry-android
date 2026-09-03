package com.robfraser.granturismotelemetry.telemetry

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException

/**
 * Connects to a PS5 running Gran Turismo 7, sends the periodic heartbeat
 * (ASCII `'A'` to port 33739 every ~10 s) and listens for encrypted packets
 * on UDP 33740.
 */
class Gt7UdpClient {
    @Volatile private var running = false
    @Volatile private var socket: DatagramSocket? = null

    var onPacket: ((TelemetryPacket) -> Unit)? = null
    var onRaw: ((Int) -> Unit)? = null
    var onDecodeFailed: ((String) -> Unit)? = null
    var onStatus: ((String) -> Unit)? = null

    fun start(
        ps5Host: String,
        sendPort: Int = DEFAULT_SEND_PORT,
        receivePort: Int = DEFAULT_RECEIVE_PORT,
    ) {
        stop()
        running = true
        Thread({
            run(ps5Host, sendPort, receivePort)
        }, "gt7-udp").apply {
            isDaemon = true
            start()
        }
    }

    fun stop() {
        running = false
        try {
            socket?.close()
        } catch (_: Exception) {
        }
        socket = null
    }

    private fun run(host: String, sendPort: Int, receivePort: Int) {
        val sock = try {
            DatagramSocket(null).apply {
                reuseAddress = true
                soTimeout = 1000
                receiveBufferSize = 1 shl 16
                bind(InetSocketAddress(receivePort))
            }
        } catch (e: Exception) {
            onStatus?.invoke("bind(:$receivePort) failed: ${e.message}")
            return
        }
        socket = sock
        onStatus?.invoke("Listening UDP :$receivePort, heartbeat → $host:$sendPort")
        sendHeartbeat(sock, host, sendPort)
        var lastHeartbeat = System.currentTimeMillis()

        val buffer = ByteArray(4096)
        while (running) {
            val now = System.currentTimeMillis()
            if (now - lastHeartbeat >= 10_000) {
                sendHeartbeat(sock, host, sendPort)
                lastHeartbeat = now
            }
            try {
                val packet = DatagramPacket(buffer, buffer.size)
                sock.receive(packet)
                val n = packet.length
                if (n <= 0) continue
                onRaw?.invoke(n)
                val payload = buffer.copyOf(n)
                val decoded = Gt7Crypto.tryDecode(payload)
                val pkt = decoded.packet
                if (pkt != null) {
                    onPacket?.invoke(pkt)
                } else {
                    onDecodeFailed?.invoke(decoded.reason ?: "unknown")
                }
            } catch (_: SocketTimeoutException) {
                // heartbeat / stop check
            } catch (e: Exception) {
                if (running) {
                    onStatus?.invoke("recv failed: ${e.message}")
                }
            }
        }
        try {
            sock.close()
        } catch (_: Exception) {
        }
    }

    private fun sendHeartbeat(sock: DatagramSocket, host: String, port: Int) {
        try {
            val addr = InetAddress.getByName(host)
            val data = byteArrayOf(0x41) // ASCII 'A'
            sock.send(DatagramPacket(data, data.size, addr, port))
        } catch (e: Exception) {
            onStatus?.invoke("Invalid IPv4 address: $host (${e.message})")
        }
    }

    companion object {
        const val DEFAULT_SEND_PORT = 33739
        const val DEFAULT_RECEIVE_PORT = 33740
    }
}
