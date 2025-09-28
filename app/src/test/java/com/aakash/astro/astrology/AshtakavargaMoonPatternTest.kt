package com.aakash.astro.astrology

import org.junit.Assert.assertArrayEquals
import org.junit.Test

class AshtakavargaMoonPatternTest {
    private fun chartWithMoonIn(sign: ZodiacSign): ChartResult {
        val planets = Planet.entries.map { p ->
            val s = if (p == Planet.MOON) sign else ZodiacSign.ARIES
            PlanetPosition(p, s.ordinal * 30.0, s, 1, false)
        }
        return ChartResult(
            ascendantDegree = 0.0,
            ascendantSign = ZodiacSign.ARIES,
            houses = emptyList(),
            planets = planets
        )
    }

    @Test
    fun moonBavStaticMatchesProvidedPatternWhenMoonInSagittarius() {
        val chart = chartWithMoonIn(ZodiacSign.SAGITTARIUS)
        val bav = AshtakavargaCalc.computeBAVStaticFor(Planet.MOON, chart).values
        // Expected mapping per user for Moon BAV with Moon in Sagittarius:
        // Aries 4, Taurus 4, Gemini 4, Cancer 5, Leo 2, Virgo 6, Libra 4, Scorpio 4, Sagittarius 7, Capricorn 2, Aquarius 4, Pisces 3
        val expected = IntArray(12)
        expected[ZodiacSign.ARIES.ordinal] = 4
        expected[ZodiacSign.TAURUS.ordinal] = 4
        expected[ZodiacSign.GEMINI.ordinal] = 4
        expected[ZodiacSign.CANCER.ordinal] = 5
        expected[ZodiacSign.LEO.ordinal] = 2
        expected[ZodiacSign.VIRGO.ordinal] = 6
        expected[ZodiacSign.LIBRA.ordinal] = 4
        expected[ZodiacSign.SCORPIO.ordinal] = 4
        expected[ZodiacSign.SAGITTARIUS.ordinal] = 7
        expected[ZodiacSign.CAPRICORN.ordinal] = 2
        expected[ZodiacSign.AQUARIUS.ordinal] = 4
        expected[ZodiacSign.PISCES.ordinal] = 3
        assertArrayEquals(expected, bav)
    }
}

