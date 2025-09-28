package com.aakash.astro.astrology

object YogiCalculator {
    private const val NAK_LEN = 360.0 / 27.0 // 13°20'
    private const val YOGI_OFFSET = 93.0 + 20.0 / 60.0 // 93°20'
    private const val AVAYOGI_OFFSET = 186.0 + 40.0 / 60.0 // 186°40'

    private val vimOrder = listOf(
        "Ketu", "Venus", "Sun", "Moon", "Mars", "Rahu", "Jupiter", "Saturn", "Mercury"
    )

    private fun normalize(deg: Double): Double {
        var x = deg % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private fun nakshatraIndexOf(deg: Double): Int {
        val d = normalize(deg)
        val idx = kotlin.math.floor(d / NAK_LEN).toInt()
        return idx.coerceIn(0, 26)
    }

    fun nakshatraLordOf(deg: Double): String {
        val idx = nakshatraIndexOf(deg)
        return vimOrder[idx % vimOrder.size]
    }

    fun signOf(deg: Double): ZodiacSign {
        val d = normalize(deg)
        val idx = (d / 30.0).toInt().coerceIn(0, 11)
        return ZodiacSign.entries[idx]
    }

    fun signLordOf(sign: ZodiacSign): Planet = when (sign) {
        ZodiacSign.ARIES -> Planet.MARS
        ZodiacSign.TAURUS -> Planet.VENUS
        ZodiacSign.GEMINI -> Planet.MERCURY
        ZodiacSign.CANCER -> Planet.MOON
        ZodiacSign.LEO -> Planet.SUN
        ZodiacSign.VIRGO -> Planet.MERCURY
        ZodiacSign.LIBRA -> Planet.VENUS
        ZodiacSign.SCORPIO -> Planet.MARS
        ZodiacSign.SAGITTARIUS -> Planet.JUPITER
        ZodiacSign.CAPRICORN -> Planet.SATURN
        ZodiacSign.AQUARIUS -> Planet.SATURN
        ZodiacSign.PISCES -> Planet.JUPITER
    }

    fun computeYogiPoint(sunSidereal: Double, moonSidereal: Double): Double =
        normalize(sunSidereal + moonSidereal + YOGI_OFFSET)

    fun computeAvayogiPoint(yogiPoint: Double): Double =
        normalize(yogiPoint + AVAYOGI_OFFSET)

    fun avayogiBy6th(yogiLord: String): String {
        val i = vimOrder.indexOfFirst { it.equals(yogiLord, ignoreCase = true) }
        if (i < 0) return ""
        val idx = (i + 5) % vimOrder.size // inclusive 6th = +5 zero-based
        return vimOrder[idx]
    }
}

