package com.robfraser.slickdash

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ConnectionQualityTests(
    private val pps: Double,
    private val err: Double,
    private val expected: QualityRating,
) {
    @Test
    fun classifiesPacketRateAndErrors() {
        assertEquals(expected, ConnectionQuality.classify(pps, err))
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "pps={0}, err={1} -> {2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(60.0, 0.0, QualityRating.Good),
            arrayOf(45.0, 0.05, QualityRating.Good),
            arrayOf(20.0, 0.1, QualityRating.Fair),
            arrayOf(12.0, 0.2, QualityRating.Fair),
            arrayOf(5.0, 0.0, QualityRating.Poor),
            arrayOf(50.0, 0.5, QualityRating.Poor),
            arrayOf(0.0, 1.0, QualityRating.Poor),
        )
    }
}
