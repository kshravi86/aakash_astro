package com.aakash.astro.astrology

import java.time.*

object SunriseCalc {
    private const val ZENITH = 90.833 // degrees for sunrise/sunset

    private fun sinDeg(d: Double) = kotlin.math.sin(Math.toRadians(d))
    private fun cosDeg(d: Double) = kotlin.math.cos(Math.toRadians(d))
    private fun tanDeg(d: Double) = kotlin.math.tan(Math.toRadians(d))
    private fun asinDeg(x: Double) = Math.toDegrees(kotlin.math.asin(x))
    private fun acosDeg(x: Double) = Math.toDegrees(kotlin.math.acos(x))
    private fun atanDeg(x: Double) = Math.toDegrees(kotlin.math.atan(x))
    private fun normalize360(d: Double): Double {
        var v = d % 360.0
        if (v < 0) v += 360.0
        return v
    }

    private fun dayOfYear(date: LocalDate): Int = date.dayOfYear

    fun sunrise(date: LocalDate, latitude: Double, longitude: Double, zoneId: ZoneId): ZonedDateTime? {
        return computeSunEvent(date, latitude, longitude, zoneId, isSunrise = true)
    }

    fun sunset(date: LocalDate, latitude: Double, longitude: Double, zoneId: ZoneId): ZonedDateTime? {
        return computeSunEvent(date, latitude, longitude, zoneId, isSunrise = false)
    }

    private fun computeSunEvent(date: LocalDate, lat: Double, lon: Double, zoneId: ZoneId, isSunrise: Boolean): ZonedDateTime? {
        val n = dayOfYear(date)
        val lngHour = lon / 15.0
        val t = if (isSunrise) n + ((6.0 - lngHour) / 24.0) else n + ((18.0 - lngHour) / 24.0)

        val m = (0.9856 * t) - 3.289
        var l = m + (1.916 * sinDeg(m)) + (0.020 * sinDeg(2 * m)) + 282.634
        l = normalize360(l)

        var ra = atanDeg(0.91764 * tanDeg(l))
        ra = normalize360(ra)
        val lQuadrant = (kotlin.math.floor(l / 90.0) * 90.0)
        val raQuadrant = (kotlin.math.floor(ra / 90.0) * 90.0)
        ra += (lQuadrant - raQuadrant)
        ra /= 15.0

        val sinDec = 0.39782 * sinDeg(l)
        val cosDec = cosDeg(asinDeg(sinDec))

        val cosH = (cosDeg(ZENITH) - (sinDec * sinDeg(lat))) / (cosDec * cosDeg(lat))
        if (cosH > 1.0 && isSunrise) return null // Sun never rises on this date at this location
        if (cosH < -1.0 && !isSunrise) return null // Sun never sets on this date at this location

        val h = if (isSunrise) 360.0 - acosDeg(cosH) else acosDeg(cosH)
        val hHours = h / 15.0
        val tLocal = hHours + ra - (0.06571 * t) - 6.622
        val ut = tLocal - lngHour

        // Convert UT hours to Instant for the given date
        val utHours = ((ut % 24.0) + 24.0) % 24.0
        val hour = utHours.toInt()
        val minute = ((utHours - hour) * 60.0).toInt()
        val second = kotlin.math.round(((utHours - hour) * 3600.0) - (minute * 60)).toInt()

        val utcDateTime = LocalDateTime.of(date, LocalTime.of(hour, minute, second))
        val instant = utcDateTime.toInstant(ZoneOffset.UTC)
        return instant.atZone(zoneId)
    }
}

