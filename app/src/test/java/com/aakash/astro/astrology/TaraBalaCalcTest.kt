package com.aakash.astro.astrology

import org.junit.Assert.assertEquals
import org.junit.Test

class TaraBalaCalcTest {

    @Test
    fun compute_assigns_tara_classes_relative_to_moon() {
        val chart = ChartResult(
            ascendantDegree = 0.0,
            ascendantSign = ZodiacSign.ARIES,
            houses = emptyList(),
            planets = listOf(
                PlanetPosition(Planet.MOON, degree = 0.0, sign = ZodiacSign.ARIES, house = 1),
                PlanetPosition(Planet.MARS, degree = 40.0, sign = ZodiacSign.TAURUS, house = 2),
                PlanetPosition(Planet.JUPITER, degree = 200.0, sign = ZodiacSign.LIBRA, house = 7)
            )
        )

        val entries = TaraBalaCalc.compute(chart)

        val mars = entries.first { it.planet == Planet.MARS }
        assertEquals("Kema", mars.tara)
        assertEquals("Favorable", mars.result)

        val jupiter = entries.first { it.planet == Planet.JUPITER }
        assertEquals("Naidhana", jupiter.tara)
        assertEquals("Unfavorable", jupiter.result)
    }

    @Test
    fun computeForMoonNakshatra_cycles_through_nine_taras() {
        val table = TaraBalaCalc.computeForMoonNakshatra(1)

        assertEquals(27, table.size)
        assertEquals("Janma", table[0].tara)
        assertEquals("Neutral", table[0].result)

        assertEquals("Sampat", table[1].tara)
        assertEquals("Favorable", table[1].result)

        assertEquals("Paramamitra", table[8].tara)
        assertEquals("Favorable", table[8].result)

        // Cycle restarts after every 9 nakshatras
        assertEquals("Janma", table[9].tara)
    }
}
