package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZonedDateTime

data class DashaPeriod(
    val lord: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

object DashaCalculator {
    // Vimshottari order and durations (years)
    private val order = listOf(
        "Ketu" to 7.0,
        "Venus" to 20.0,
        "Sun" to 6.0,
        "Moon" to 10.0,
        "Mars" to 7.0,
        "Rahu" to 18.0,
        "Jupiter" to 16.0,
        "Saturn" to 19.0,
        "Mercury" to 17.0,
    )

    private const val NAK_LEN = 360.0 / 27.0
    private const val DAYS_PER_YEAR = 365.25

    fun vimshottariFrom(details: BirthDetails, moonSiderealLongitude: Double): List<DashaPeriod> {
        var start = details.dateTime
        // Determine starting mahadasha from Moon's nakshatra
        val deg = normalize(moonSiderealLongitude)
        val nakIndex = kotlin.math.floor(deg / NAK_LEN).toInt().coerceIn(0, 26)
        val padaFrac = (deg % NAK_LEN) / NAK_LEN
        val remainingFrac = 1.0 - padaFrac

        // Starting lord index
        val lordStartIndex = nakIndex % order.size
        val list = mutableListOf<DashaPeriod>()

        // First period is partial
        run {
            val (lord, years) = order[lordStartIndex]
            val durDays = years * remainingFrac * DAYS_PER_YEAR
            val end = start.plus(Duration.ofSeconds((durDays * 86400).toLong()))
            list += DashaPeriod(lord, start, end)
            start = end
        }

        // Continue full periods to complete 120 years horizon
        var idx = (lordStartIndex + 1) % order.size
        var elapsedYears = (order[lordStartIndex].second * remainingFrac)
        while (elapsedYears < 120.0 - 1e-6) {
            val (lord, years) = order[idx]
            val durDays = years * DAYS_PER_YEAR
            val end = start.plus(Duration.ofSeconds((durDays * 86400).toLong()))
            list += DashaPeriod(lord, start, end)
            start = end
            elapsedYears += years
            idx = (idx + 1) % order.size
        }
        return list
    }

    fun antardashaFor(ma: DashaPeriod): List<DashaPeriod> {
        val totalDays = Duration.between(ma.start, ma.end).toDays().toDouble()
        val startIndex = indexOf(ma.lord)
        var t = ma.start
        val list = mutableListOf<DashaPeriod>()
        for (i in 0 until order.size) {
            val (lord, years) = order[(startIndex + i) % order.size]
            val days = totalDays * (years / 120.0)
            val end = t.plusSeconds((days * 86400).toLong())
            list += DashaPeriod(lord, t, end)
            t = end
        }
        return list
    }

    fun pratyantarFor(antar: DashaPeriod): List<DashaPeriod> {
        val totalDays = Duration.between(antar.start, antar.end).toDays().toDouble()
        val startIndex = indexOf(antar.lord)
        var t = antar.start
        val list = mutableListOf<DashaPeriod>()
        for (i in 0 until order.size) {
            val (lord, years) = order[(startIndex + i) % order.size]
            val days = totalDays * (years / 120.0)
            val end = t.plusSeconds((days * 86400).toLong())
            list += DashaPeriod(lord, t, end)
            t = end
        }
        return list
    }

    private fun indexOf(lord: String): Int = order.indexOfFirst { it.first.equals(lord, ignoreCase = true) }.let { if (it >= 0) it else 0 }

    private fun normalize(v: Double): Double { var x = v % 360.0; if (x < 0) x += 360.0; return x }
}
