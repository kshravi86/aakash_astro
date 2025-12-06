package com.aakash.astro.astrology

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun tara_direction_counts_bride_to_groom() {
        val bride = chartWithMoon(0.0) // Ashwini
        val groom = chartWithMoon(26.9) // Krittika (2 away forward, 7 away reverse)
        val forward = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Tara" }.score
        val reverse = GunMilanCalculator.match(groom, bride).parts.first { it.name == "Tara" }.score
        assertEquals(3.0, forward, 0.0001)
        assertEquals(0.0, reverse, 0.0001)
    }

    @Test
    fun bhakoot_two_twelve_pairs_score_zero() {
        val bride = chartWithMoon(0.0) // Aries
        val groom = chartWithMoon(40.0) // Taurus (2/12 from Aries)
        val bhakoot = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Bhakoot" }.score
        assertEquals(0.0, bhakoot, 0.0001)
    }

    @Test
    fun bhakoot_three_eleven_pairs_score_full() {
        val bride = chartWithMoon(0.0) // Aries
        val groom = chartWithMoon(60.0) // Gemini (3/11 from Aries)
        val bhakoot = GunMilanCalculator.match(bride, groom).parts.first { it.name == "Bhakoot" }.score
        assertEquals(7.0, bhakoot, 0.0001)
    }

    private fun chartWithAscAndMoon(ascDeg: Double, moonDeg: Double): ChartResult {
        val ascSign = ZodiacSign.fromDegree(ascDeg)
        val moonSign = ZodiacSign.fromDegree(moonDeg)
        val moon = PlanetPosition(Planet.MOON, moonDeg, moonSign, house = 1)
        return ChartResult(
            ascendantDegree = ascDeg,
            ascendantSign = ascSign,
            houses = emptyList(),
            planets = listOf(moon)
        )
    }

    @Test
    fun known_pair_ashwini_to_krittika_validates_tara_varna_and_ascendants() {
        val bride = chartWithAscAndMoon(0.0, 0.0) // Aries lagna, Ashwini Moon
        val groom = chartWithAscAndMoon(180.0, 35.0) // Libra lagna, Krittika Moon

        assertEquals(ZodiacSign.ARIES, bride.ascendantSign)
        assertEquals(ZodiacSign.LIBRA, groom.ascendantSign)

        val result = GunMilanCalculator.match(bride, groom)
        val tara = result.parts.first { it.name == "Tara" }
        val varna = result.parts.first { it.name == "Varna" }

        assertEquals(3.0, tara.score, 0.0001) // 2 nakshatras forward (even)
        assertEquals(0.0, varna.score, 0.0001) // Groom (Vaishya) below bride (Kshatriya)
        assertTrue(tara.note.contains("bride->groom"))
        assertTrue(varna.note.contains("Bride: KSHATRIYA") && varna.note.contains("Groom: VAISHYA"))
    }

    @Test
    fun known_pair_revati_to_ashwini_hits_odd_tara_and_preserves_ascendants() {
        val bride = chartWithAscAndMoon(270.0, 356.0) // Capricorn lagna, Revati Moon
        val groom = chartWithAscAndMoon(90.0, 1.0) // Cancer lagna, Ashwini Moon

        assertEquals(ZodiacSign.CAPRICORN, bride.ascendantSign)
        assertEquals(ZodiacSign.CANCER, groom.ascendantSign)

        val result = GunMilanCalculator.match(bride, groom)
        val tara = result.parts.first { it.name == "Tara" }
        val varna = result.parts.first { it.name == "Varna" }

        assertEquals(0.0, tara.score, 0.0001) // 1 nakshatra forward (odd)
        assertEquals(0.0, varna.score, 0.0001) // Groom (Kshatriya) below bride (Brahmin)
        assertTrue(tara.note.contains("bride->groom"))
        assertTrue(varna.note.contains("Bride: BRAHMIN") && varna.note.contains("Groom: KSHATRIYA"))
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
