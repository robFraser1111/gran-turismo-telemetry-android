package com.robfraser.granturismotelemetry.telemetry

/**
 * GT7 Salsa20 packet encryption helpers.
 *
 * Protocol (community reverse-engineering, as implemented in
 * https://github.com/robFraser1111/gran-turismo-telemetry):
 *
 * - Key: first 32 bytes of UTF-8 `"Simulator Interface Packet GT7 ver 0.0"`
 * - Nonce: ciphertext bytes at 0x40..<0x44 as LE UInt32. That value XOR
 *   `0xDEADBEAF` is written as the first 4 bytes of the 8-byte Salsa20 nonce
 *   (state[6]); the original 4 bytes are the second half (state[7]).
 *   Salsa20 20 rounds.
 * - After decrypt, magic at offset 0 must be `"G7S0"` (`0x47375330`).
 */
object Gt7Crypto {
    const val KEY_SEED = "Simulator Interface Packet GT7 ver 0.0"
    val DEAD_BEAF: UInt = 0xDEADBEAFu
    val MAGIC: UInt = 0x47375330u

    val key: ByteArray = KEY_SEED.encodeToByteArray().copyOf(32)

    fun decrypt(raw: ByteArray): ByteArray {
        val buffer = raw.copyOf()
        decryptInPlace(buffer)
        return buffer
    }

    fun decryptInPlace(buffer: ByteArray) {
        require(buffer.size >= 0x44)
        val oiv = Binary.readU32(buffer, 0x40)
        val nonce = ByteArray(8)
        buildNonce(oiv, nonce)
        Salsa20.xorInPlace(key, nonce, buffer)
    }

    /**
     * Builds a ciphertext that will decrypt back to [plaintext] when passed to
     * [tryDecode]. The four IV bytes at offset 0x40 in the returned ciphertext
     * are literally [ciphertextIV], which is what the receiver uses to derive
     * the Salsa20 nonce.
     */
    fun encryptForTest(plaintext: ByteArray, ciphertextIV: UInt): ByteArray {
        val nonce = ByteArray(8)
        buildNonce(ciphertextIV, nonce)
        val cipher = plaintext.copyOf()
        Salsa20.xorInPlace(key, nonce, cipher)
        Binary.writeU32(cipher, 0x40, ciphertextIV)
        return cipher
    }

    data class DecodeResult(val packet: TelemetryPacket?, val reason: String?)

    fun tryDecode(raw: ByteArray): DecodeResult {
        if (raw.size < TelemetryPacket.MINIMUM_SIZE) {
            return DecodeResult(
                null,
                "short packet (${raw.size} bytes, need >= ${TelemetryPacket.MINIMUM_SIZE})",
            )
        }
        return try {
            val buffer = raw.copyOf()
            decryptInPlace(buffer)
            val magic = Binary.readU32(buffer, 0)
            if (magic != MAGIC) {
                DecodeResult(
                    null,
                    "bad magic 0x${magic.toString(16)} after decrypt (expected 0x47375330 'G7S0')",
                )
            } else {
                DecodeResult(TelemetryPacket.parse(buffer), null)
            }
        } catch (e: Exception) {
            DecodeResult(null, e.message ?: e.toString())
        }
    }

    /**
     * Nonce layout matches the C# / Python community receivers:
     * bytes 0..<4 = (IV XOR 0xDEADBEAF) LE, bytes 4..<8 = original IV LE.
     */
    fun buildNonce(oivInt: UInt, nonce: ByteArray) {
        val xored = oivInt xor DEAD_BEAF
        Binary.writeU32(nonce, 0, xored)
        Binary.writeU32(nonce, 4, oivInt)
    }
}
