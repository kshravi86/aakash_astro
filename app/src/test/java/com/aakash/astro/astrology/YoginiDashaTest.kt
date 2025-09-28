package com.aakash.astro.astrology

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs

class YoginiDashaTest {
    @Test
    fun startFromPunarvasuPada2_matchesPingalaAndBalance() {
        // Moon at 87°15' sidereal => 87.25 degrees (Punarvasu nakshatra, pada 2)
        val moonDeg = 87.25
        val details = BirthDetails("Test", ZonedDateTime.of(1993,5,18,22,30,0,0, ZoneId.of("Asia/Kolkata")), 12.97, 77.59)
        val periods = YoginiDasha.compute(details, moonDeg)
        val first = periods.first()
        assertEquals("Pingala", first.lord)
        // Remaining fraction = 0.45625; Pingala years = 2 => 0.9125 years ~ 333.306 days
        val days = java.time.Duration.between(first.start, first.end).toDays().toDouble()
        assert(abs(days - (0.9125 * 365.25)) < 2.0) { "Expected ~333.3 days, got $days" }
    }
}

