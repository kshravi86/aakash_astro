package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

data class GhatikaLagnaResult(
    val longitude: Double,
    val sign: ZodiacSign
)

object GhatikaLagnaCalc {
    private fun normalize(deg: Double): Double {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        return d
    }

    fun compute(
        birthDateTime: ZonedDateTime,
        latitude: Double,
        longitude: Double,
        zoneId: ZoneId,
        accurate: AccurateCalculator
    ): GhatikaLagnaResult? {
        val todaySunrise = SunriseCalc.sunrise(birthDateTime.toLocalDate(), latitude, longitude, zoneId)
        val sunriseZdt = if (todaySunrise != null && !birthDateTime.isBefore(todaySunrise)) {
            todaySunrise
        } else {
            // If birth before today's sunrise or sunrise unavailable, use previous day's sunrise
            val prev = SunriseCalc.sunrise(birthDateTime.toLocalDate().minusDays(1), latitude, longitude, zoneId)
            prev ?: todaySunrise ?: return null
        }

        // Sun's sidereal longitude at sunrise
        val sunriseDetails = BirthDetails(name = null, dateTime = sunriseZdt, latitude = latitude, longitude = longitude)
        val chartAtSunrise = accurate.generateChart(sunriseDetails) ?: return null
        val sunAtSunrise = chartAtSunrise.planets.find { it.planet == Planet.SUN }?.degree ?: return null

        // Elapsed minutes since sunrise to birth time
        val minutes = Duration.between(sunriseZdt, birthDateTime).toMinutes().toDouble()
        // Advance: 1.25 degrees per minute (30° per 24 min)
        val advance = minutes * 1.25
        val gl = normalize(sunAtSunrise + advance)
        return GhatikaLagnaResult(longitude = gl, sign = ZodiacSign.fromDegree(gl))
    }
}

