package com.aakash.astro.astrology

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AstrologyCalculatorTest {

    private val calculator = AstrologyCalculator()

    @Test
    fun generateChart_basicInvariants() {
        val z = ZoneId.of("Asia/Kolkata")
        val dt = ZonedDateTime.of(1990, 1, 1, 12, 0, 0, 0, z)
        val details = BirthDetails(
            name = "Test",
            dateTime = dt,
            latitude = 28.6139,  // Delhi
            longitude = 77.2090
        )

        val chart = calculator.generateChart(details)

        // Houses
        assertEquals(12, chart.houses.size)
        val houseNumbers = chart.houses.map { it.number }.sorted()
        assertEquals((1..12).toList(), houseNumbers)
        assertTrue(chart.houses.all { it.startDegree >= 0.0 && it.startDegree < 360.0 })

        // Ascendant
        assertTrue(chart.ascendantDegree >= 0.0 && chart.ascendantDegree < 360.0)
        assertEquals(ZodiacSign.fromDegree(chart.ascendantDegree), chart.ascendantSign)

        // Planets
        val expectedPlanets = Planet.entries
        assertEquals(expectedPlanets.size, chart.planets.size)
        assertEquals(expectedPlanets.toSet(), chart.planets.map { it.planet }.toSet())
        assertTrue(chart.planets.all { it.degree >= 0.0 && it.degree < 360.0 })
        assertTrue(chart.planets.all { it.house in 1..12 })
    }

    @Test
    fun zodiacSign_fromDegree_boundaries() {
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromDegree(0.0))
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromDegree(29.999))
        assertEquals(ZodiacSign.TAURUS, ZodiacSign.fromDegree(30.0))
        assertEquals(ZodiacSign.PISCES, ZodiacSign.fromDegree(359.9))
        // wrap-around
        assertEquals(ZodiacSign.ARIES, ZodiacSign.fromDegree(360.0))
    }

    @Test
    fun houses_progress_every_30_degrees() {
        val z = ZoneId.of("Asia/Kolkata")
        val dt = ZonedDateTime.of(2000, 6, 15, 6, 30, 0, 0, z)
        val details = BirthDetails(
            name = null,
            dateTime = dt,
            latitude = 19.0760, // Mumbai
            longitude = 72.8777
        )
        val chart = calculator.generateChart(details)

        val asc = chart.ascendantDegree
        chart.houses.forEachIndexed { index, house ->
            val expected = normalize(asc + index * 30.0)
            assertTrue(angleClose(expected, house.startDegree, 1e-6))
        }
    }

    @Test
    fun bengaluru_today_smoke() {
        val z = ZoneId.of("Asia/Kolkata")
        val now = ZonedDateTime.now(z).withSecond(0).withNano(0)
        val details = BirthDetails(
            name = "Bengaluru Now",
            dateTime = now,
            latitude = 12.9716,
            longitude = 77.5946
        )
        val chart = calculator.generateChart(details)
        // Sanity: still 12 houses, 9 planets, consistent ascendant
        assertEquals(12, chart.houses.size)
        assertEquals(Planet.entries.size, chart.planets.size)
        assertEquals(chart.ascendantSign, ZodiacSign.fromDegree(chart.ascendantDegree))
    }

    private fun normalize(v: Double): Double {
        var x = v % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private fun angleClose(a: Double, b: Double, eps: Double): Boolean {
        val d = kotlin.math.abs(normalize(a - b))
        return d < eps || kotlin.math.abs(360.0 - d) < eps
    }
}
