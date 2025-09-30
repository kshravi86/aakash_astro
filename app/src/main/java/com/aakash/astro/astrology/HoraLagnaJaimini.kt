package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

data class HoraLagnaSimpleResult(
    val longitude: Double,
    val sign: ZodiacSign
)

object HoraLagnaJaiminiCalc {
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
    ): HoraLagnaSimpleResult? {
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

        // Elapsed time H:M:S since sunrise
        val seconds = Duration.between(sunriseZdt, birthDateTime).seconds.coerceAtLeast(0)
        val hrs = seconds / 3600
        val mins = (seconds % 3600) / 60
        val secs = seconds % 60
        // Advance: 30° per hour, 0.5° per minute, 0.5/60° per second
        val advance = 30.0 * hrs + 0.5 * mins + (0.5 / 60.0) * secs
        val hl = normalize(sunAtSunrise + advance)
        return HoraLagnaSimpleResult(longitude = hl, sign = ZodiacSign.fromDegree(hl))
    }
}

