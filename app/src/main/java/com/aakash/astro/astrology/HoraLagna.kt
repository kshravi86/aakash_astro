package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime

data class HoraLagnaResult(
    val longitude: Double,
    val sign: ZodiacSign,
    val houseFromAsc: Int,
    val ishtaHours: Double,
    val sunSignAtSunrise: ZodiacSign,
    val sunDegreeAtSunrise: Double,
    val sunriseTime: ZonedDateTime
)

object HoraLagnaCalc {
    fun compute(natal: ChartResult, birthDateTime: ZonedDateTime, lat: Double, lon: Double, zoneId: ZoneId): HoraLagnaResult? {
        // 1) Find sunrise for the birth date (or previous day if born before sunrise)
        var sunrise = SunriseCalc.sunrise(birthDateTime.toLocalDate(), lat, lon, zoneId)
        if (sunrise == null) return null
        if (birthDateTime.isBefore(sunrise)) {
            val prev = birthDateTime.toLocalDate().minusDays(1)
            sunrise = SunriseCalc.sunrise(prev, lat, lon, zoneId) ?: sunrise
        }

        // 2) Ishta Kala (hours since sunrise)
        val minutes = Duration.between(sunrise, birthDateTime).toMinutes().coerceAtLeast(0)
        val ishtaHours = minutes / 60.0

        // 3) Sun's sidereal longitude at sunrise
        val sunAtRise = sunLongitudeAt(sunrise, lat, lon)

        // 4) Hora Lagna progress: 30 degrees per hour from Sun@sunrise
        val movement = ishtaHours * 30.0
        val horaLon = normalize360(sunAtRise + movement)
        val sign = ZodiacSign.fromDegree(horaLon)

        // House from ascendant
        val houseFromAsc = ((normalize360(horaLon - natal.ascendantDegree) / 30.0).toInt() + 1).let { if (it > 12) it - 12 else it }

        return HoraLagnaResult(
            longitude = horaLon,
            sign = sign,
            houseFromAsc = houseFromAsc,
            ishtaHours = ishtaHours,
            sunSignAtSunrise = ZodiacSign.fromDegree(sunAtRise),
            sunDegreeAtSunrise = sunAtRise,
            sunriseTime = sunrise
        )
    }

    private fun sunLongitudeAt(time: ZonedDateTime, lat: Double, lon: Double): Double {
        // Reuse the internal calculator to get sidereal longitude
        val calc = AstrologyCalculator()
        val bd = BirthDetails(name = null, dateTime = time, latitude = lat, longitude = lon)
        val chart = calc.generateChart(bd)
        val sun = chart.planets.first { it.planet == Planet.SUN }
        return sun.degree
    }

    private fun normalize360(value: Double): Double {
        var v = value % 360.0
        if (v < 0) v += 360.0
        return v
    }
}

