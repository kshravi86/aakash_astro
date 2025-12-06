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

    @Test
    fun tara_odd_remainder_scores_zero() {
        val bride = chartWithMoon(0.0) // Ashwini
        val groom = chartWithMoon(13.5) // Bharani (1 nakshatra away)
        val taraScore = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Tara" }.score
        assertEquals(0.0, taraScore, 0.0001)
    }

    @Test
    fun tara_even_remainder_scores_full() {
        val bride = chartWithMoon(0.0) // Ashwini
        val groom = chartWithMoon(26.9) // Krittika (2 nakshatras away)
        val taraScore = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Tara" }.score
        assertEquals(3.0, taraScore, 0.0001)
    }

    private fun chartWithPlanets(planets: Map<Planet, Double>): ChartResult {
        val positions = planets.map { (planet, deg) ->
            PlanetPosition(
                planet = planet,
                degree = deg,
                sign = ZodiacSign.fromDegree(deg),
                house = 1
            )
        }
        return ChartResult(
            ascendantDegree = 0.0,
            ascendantSign = ZodiacSign.ARIES,
            houses = emptyList(),
            planets = positions
        )
    }

    @Test
    fun graha_maitri_uses_compound_friendship() {
        // Mars (bride Moon lord) vs Mercury (groom Moon lord) are natural enemies,
        // but placed in temporary friendly houses from each other in both charts,
        // so compound relation should lift the score to neutral/neutral (3 points).
        val bride = chartWithPlanets(
            mapOf(
                Planet.MOON to 0.0,      // Aries Moon (lord Mars)
                Planet.MARS to 0.0,
                Planet.MERCURY to 40.0   // 2nd from Mars -> temporary friend
            )
        )
        val groom = chartWithPlanets(
            mapOf(
                Planet.MOON to 60.0,     // Gemini Moon (lord Mercury)
                Planet.MERCURY to 0.0,
                Planet.MARS to 40.0      // 2nd from Mercury -> temporary friend
            )
        )

        val grahaScore = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Graha Maitri" }.score
        assertEquals(3.0, grahaScore, 0.0001)
    }
}
