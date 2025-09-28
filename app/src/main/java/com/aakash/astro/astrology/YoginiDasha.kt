package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZonedDateTime

data class YoginiPeriod(
    val lord: String,
    val start: ZonedDateTime,
    val end: ZonedDateTime
)

object YoginiDasha {
    // Standard Yogini order with durations in years; total = 36 years
    private val order = listOf(
        "Mangala" to 1.0,
        "Pingala" to 2.0,
        "Dhanya" to 3.0,
        "Bhramari" to 4.0,
        "Bhadrika" to 5.0,
        "Ulka" to 6.0,
        "Siddha" to 7.0,
        "Sankata" to 8.0,
    )

    private const val NAK_LEN = 360.0 / 27.0      // 13°20'
    private const val DAYS_PER_YEAR = 365.25

    // Planetary rulers for each Yogini
    private val rulers: Map<String, Planet> = mapOf(
        "Mangala" to Planet.MOON,
        "Pingala" to Planet.SUN,
        "Dhanya" to Planet.JUPITER,
        "Bhramari" to Planet.MARS,
        "Bhadrika" to Planet.MERCURY,
        "Ulka" to Planet.SATURN,
        "Siddha" to Planet.VENUS,
        "Sankata" to Planet.RAHU,
    )

    fun rulerOf(yogini: String): Planet? = rulers[yogini]

    fun label(yogini: String): String {
        val p = rulerOf(yogini)?.displayName ?: return yogini
        return "$yogini ($p)"
    }

    // Per standard rule: remainder of (nakshatra_number + 3) / 8 maps to
    // 1: Mangala, 2: Pingala, 3: Dhanya, 4: Bhramari, 5: Bhadrika, 6: Ulka, 7: Siddha, 0: Sankata
    private fun startIndexFromNakshatra(nakIndex: Int): Int {
        val nakNum = nakIndex + 1 // 1..27
        val remainder = (nakNum + 3) % 8
        return if (remainder == 0) 7 else remainder - 1 // 0-based index into 'order'
    }

    fun compute(details: BirthDetails, moonSiderealLongitude: Double): List<YoginiPeriod> {
        var start = details.dateTime
        val deg = normalize(moonSiderealLongitude)
        val nakIndex = kotlin.math.floor(deg / NAK_LEN).toInt().coerceIn(0, 26)
        val fracInNak = (deg % NAK_LEN) / NAK_LEN // 0..1 within nakshatra
        val remainingFrac = 1.0 - fracInNak

        val startIdx = startIndexFromNakshatra(nakIndex)
        val periods = mutableListOf<YoginiPeriod>()

        // First partial mahadasha
        run {
            val (lord, years) = order[startIdx]
            val durDays = years * remainingFrac * DAYS_PER_YEAR
            val end = start.plusSeconds((durDays * 86400).toLong())
            periods += YoginiPeriod(lord, start, end)
            start = end
        }

        // Continue for a 108-year horizon (3 full cycles)
        var idx = (startIdx + 1) % order.size
        var elapsedYears = order[startIdx].second * remainingFrac
        while (elapsedYears < 108.0 - 1e-6) {
            val (lord, years) = order[idx]
            val durDays = years * DAYS_PER_YEAR
            val end = start.plusSeconds((durDays * 86400).toLong())
            periods += YoginiPeriod(lord, start, end)
            start = end
            elapsedYears += years
            idx = (idx + 1) % order.size
        }
        return periods
    }

    fun antardashaFor(ma: YoginiPeriod): List<YoginiPeriod> {
        val totalDays = Duration.between(ma.start, ma.end).toDays().toDouble()
        val startIndex = indexOf(ma.lord)
        var t = ma.start
        val list = mutableListOf<YoginiPeriod>()
        for (i in 0 until order.size) {
            val (lord, years) = order[(startIndex + i) % order.size]
            val days = totalDays * (years / 36.0)
            val end = t.plusSeconds((days * 86400).toLong())
            list += YoginiPeriod(lord, t, end)
            t = end
        }
        return list
    }

    fun pratyantarFor(antar: YoginiPeriod): List<YoginiPeriod> {
        val totalDays = Duration.between(antar.start, antar.end).toDays().toDouble()
        val startIndex = indexOf(antar.lord)
        var t = antar.start
        val list = mutableListOf<YoginiPeriod>()
        for (i in 0 until order.size) {
            val (lord, years) = order[(startIndex + i) % order.size]
            val days = totalDays * (years / 36.0)
            val end = t.plusSeconds((days * 86400).toLong())
            list += YoginiPeriod(lord, t, end)
            t = end
        }
        return list
    }

    private fun indexOf(lord: String): Int = order.indexOfFirst { it.first.equals(lord, ignoreCase = true) }.let { if (it >= 0) it else 0 }
    private fun normalize(v: Double): Double { var x = v % 360.0; if (x < 0) x += 360.0; return x }
}
