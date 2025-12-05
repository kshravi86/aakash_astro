package com.aakash.astro.astrology

import org.junit.Assert.assertEquals
import org.junit.Test

class GunMilanCalculatorTest {

    private fun chartWithMoon(deg: Double): ChartResult {
        val sign = ZodiacSign.fromDegree(deg)
        val moon = PlanetPosition(Planet.MOON, deg, sign, house = 1)
        return ChartResult(
            ascendantDegree = 0.0,
            ascendantSign = ZodiacSign.ARIES,
            houses = emptyList(),
            planets = listOf(moon)
        )
    }

    @Test
    fun same_charts_get_full_score() {
        val chart = chartWithMoon(10.0) // Ashwini / Aries Moon
        val result = GunMilanCalculator.match(chart, chart)
        assertEquals(28.0, result.total, 0.0001)
    }
}
