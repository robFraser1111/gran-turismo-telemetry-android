package com.robfraser.slickdash

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class Salsa20Test {
    @Test
    fun xorIsDeterministicAndReversible() {
        val key = ByteArray(32) { it.toByte() }
        val nonce = ByteArray(8) { (it + 3).toByte() }
        val plain = "slickdash-gt7-telemetry".toByteArray()

        val once = plain.copyOf()
        Salsa20.xorInPlace(key, nonce, once)
        assertFalse(once.contentEquals(plain))

        val twice = once.copyOf()
        Salsa20.xorInPlace(key, nonce, twice)
        assertArrayEquals(plain, twice)

        val again = plain.copyOf()
        Salsa20.xorInPlace(key, nonce, again)
        assertArrayEquals(once, again)
    }
}
