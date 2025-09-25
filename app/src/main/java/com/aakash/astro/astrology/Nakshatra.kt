package com.aakash.astro.astrology

object NakshatraCalc {
    private val names = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu",
        "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
        "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
        "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
        "Uttara Bhadrapada", "Revati"
    )

    private const val NAK_LEN = 360.0 / 27.0           // 13°20' = 13.333...
    private const val PADA_LEN = NAK_LEN / 4.0         // 3°20' = 3.333...

    fun fromLongitude(siderealDeg: Double): Pair<String, Int> {
        var deg = siderealDeg % 360.0
        if (deg < 0) deg += 360.0
        val nakIndex = kotlin.math.floor(deg / NAK_LEN).toInt().coerceIn(0, 26)
        val rem = deg - nakIndex * NAK_LEN
        val pada = kotlin.math.floor(rem / PADA_LEN).toInt().coerceIn(0, 3) + 1
        return names[nakIndex] to pada
    }
}

