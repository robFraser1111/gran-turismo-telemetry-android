package com.robfraser.granturismotelemetry.telemetry

/**
 * Minimal Salsa20 stream cipher (20 rounds). Only supports what GT7 needs:
 * 256-bit key, 64-bit nonce, keystream-XOR decryption.
 *
 * Ported from the public reference implementation:
 * https://github.com/robFraser1111/gran-turismo-telemetry
 */
object Salsa20 {
    private val sigma: ByteArray = "expand 32-byte k".encodeToByteArray()

    fun xorInPlace(key: ByteArray, nonce: ByteArray, cipher: ByteArray) {
        require(key.size == 32) { "Key must be 32 bytes" }
        require(nonce.size == 8) { "Nonce must be 8 bytes" }

        val state = UIntArray(16)
        // Row 0: constants ("expand 32-byte k")
        state[0] = Binary.readU32(sigma, 0)
        state[5] = Binary.readU32(sigma, 4)
        state[10] = Binary.readU32(sigma, 8)
        state[15] = Binary.readU32(sigma, 12)
        // Key
        state[1] = Binary.readU32(key, 0)
        state[2] = Binary.readU32(key, 4)
        state[3] = Binary.readU32(key, 8)
        state[4] = Binary.readU32(key, 12)
        state[11] = Binary.readU32(key, 16)
        state[12] = Binary.readU32(key, 20)
        state[13] = Binary.readU32(key, 24)
        state[14] = Binary.readU32(key, 28)
        // Nonce
        state[6] = Binary.readU32(nonce, 0)
        state[7] = Binary.readU32(nonce, 4)
        // Block counter (state[8], state[9]) starts at 0

        val block = ByteArray(64)
        var offset = 0
        while (offset < cipher.size) {
            generateBlock(state, block)
            val take = minOf(64, cipher.size - offset)
            for (i in 0 until take) {
                cipher[offset + i] = (cipher[offset + i].toInt() xor block[i].toInt()).toByte()
            }
            offset += take

            state[8] = state[8] + 1u
            if (state[8] == 0u) {
                state[9] = state[9] + 1u
            }
        }
    }

    /** First 64 bytes of keystream for (key, nonce) with counter 0. Used by tests. */
    fun firstBlock(key: ByteArray, nonce: ByteArray): ByteArray {
        val zeros = ByteArray(64)
        xorInPlace(key, nonce, zeros)
        return zeros
    }

    private fun generateBlock(input: UIntArray, output: ByteArray) {
        val x = UIntArray(16)
        for (i in 0 until 16) x[i] = input[i]

        repeat(10) { // 20 rounds = 10 double-rounds
            // Column round
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

            // Row round
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

        for (i in 0 until 16) {
            val v = x[i] + input[i]
            Binary.writeU32(output, i * 4, v)
        }
    }

    private fun rotl(v: UInt, c: Int): UInt = (v shl c) or (v shr (32 - c))
}
