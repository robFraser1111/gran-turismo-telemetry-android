package com.robfraser.granturismotelemetry.telemetry

object Binary {
    fun readU32(bytes: ByteArray, offset: Int): UInt {
        return bytes[offset].toUByte().toUInt() or
            (bytes[offset + 1].toUByte().toUInt() shl 8) or
            (bytes[offset + 2].toUByte().toUInt() shl 16) or
            (bytes[offset + 3].toUByte().toUInt() shl 24)
    }

    fun writeU32(bytes: ByteArray, offset: Int, value: UInt) {
        bytes[offset] = (value and 0xFFu).toByte()
        bytes[offset + 1] = ((value shr 8) and 0xFFu).toByte()
        bytes[offset + 2] = ((value shr 16) and 0xFFu).toByte()
        bytes[offset + 3] = ((value shr 24) and 0xFFu).toByte()
    }

    fun readF32(bytes: ByteArray, offset: Int): Float =
        Float.fromBits(readU32(bytes, offset).toInt())

    fun writeF32(bytes: ByteArray, offset: Int, value: Float) =
        writeU32(bytes, offset, value.toRawBits().toUInt())

    fun readI32(bytes: ByteArray, offset: Int): Int =
        readU32(bytes, offset).toInt()

    fun writeI32(bytes: ByteArray, offset: Int, value: Int) =
        writeU32(bytes, offset, value.toUInt())

    fun readI16(bytes: ByteArray, offset: Int): Short {
        val u = bytes[offset].toUByte().toUInt() or
            (bytes[offset + 1].toUByte().toUInt() shl 8)
        return u.toShort()
    }

    fun writeI16(bytes: ByteArray, offset: Int, value: Short) {
        val u = value.toUShort().toUInt()
        bytes[offset] = (u and 0xFFu).toByte()
        bytes[offset + 1] = ((u shr 8) and 0xFFu).toByte()
    }
}
