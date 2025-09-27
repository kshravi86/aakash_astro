package com.aakash.astro.astrology

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class AshtakavargaCalcTest {

    private fun chartWithAllIn(sign: ZodiacSign): ChartResult {
        val planets = listOf(
            Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY,
            Planet.JUPITER, Planet.VENUS, Planet.SATURN, Planet.RAHU, Planet.KETU
        ).map {
            PlanetPosition(
                planet = it,
                degree = sign.ordinal * 30.0,
                sign = sign,
                house = 1,
                isRetrograde = false
            )
        }
        return ChartResult(
            ascendantDegree = sign.ordinal * 30.0,
            ascendantSign = sign,
            houses = emptyList(),
            planets = planets
        )
    }

    @Test
    fun `SAV when all planets and lagna in Aries has only Aries non-zero`() {
        val chart = chartWithAllIn(ZodiacSign.ARIES)
        val sav = AshtakavargaCalc.computeSarva(chart)

        // Precomputed expected bindus when houseFromRef = 1 for every contributor
        // Sum across ref tables for house=1 (see AshtakavargaCalc tables): 23
        val expected = IntArray(12)
        expected[ZodiacSign.ARIES.ordinal] = 23

        assertArrayEquals("Only Aries should have bindus", expected, sav.values)
        assertEquals(23, sav.total())
    }

    @Test
    fun `SAV rotation invariance when all bodies in same sign`() {
        val signs = listOf(ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.PISCES)
        for (sign in signs) {
            val chart = chartWithAllIn(sign)
            val sav = AshtakavargaCalc.computeSarva(chart)

            val expected = IntArray(12)
            expected[sign.ordinal] = 23

            assertArrayEquals("Only ${sign.displayName} should have bindus", expected, sav.values)
            assertEquals(23, sav.total())
        }
    }
}

