package com.aakash.astro.astrology

data class TaraEntry(
    val planet: Planet,
    val tara: String,
    val result: String,
    val note: String
)

data class TaraTableEntry(
    val nakshatraName: String,
    val tara: String,
    val result: String,
    val taraNumber: Int
)

object TaraBalaCalc {
    private const val NAK_LEN = 360.0 / 27.0

    val taraNames = listOf(
        "Janma", "Sampat", "Vipat", "Kema", "Pratyak", "Sadhana", "Naidhana", "Mitra", "Paramamitra"
    )

    val taraNotes = mapOf(
        1 to "Birth",
        2 to "Wealth",
        3 to "Danger",
        4 to "Well-being",
        5 to "Obstacles",
        6 to "Accomplishment",
        7 to "Destructive",
        8 to "Friendly",
        9 to "Very friendly"
    )

    private val nakshatraNames = listOf(
        "Ashwini", "Bharani", "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu",
        "Pushya", "Ashlesha", "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta",
        "Chitra", "Swati", "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha",
        "Uttara Ashadha", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
        "Uttara Bhadrapada", "Revati"
    )

    fun nakshatraNumber1Based(deg: Double): Int {
        var d = deg % 360.0
        if (d < 0) d += 360.0
        val idx0 = kotlin.math.floor(d / NAK_LEN).toInt().coerceIn(0, 26)
        return idx0 + 1 // 1..27
    }

    fun tClass(j: Int, p: Int): Int {
        val n = ((p - j) mod 27) + 1
        return ((n - 1) mod 9) + 1
    }

    private infix fun Int.mod(m: Int): Int {
        val r = this % m
        return if (r < 0) r + m else r
    }

    fun compute(chart: ChartResult): List<TaraEntry> {
        val moonDeg = chart.planets.find { it.planet == Planet.MOON }?.degree ?: 0.0
        val j = nakshatraNumber1Based(moonDeg)

        return Planet.entries.map { planet ->
            val pdeg = chart.planets.find { it.planet == planet }?.degree ?: return@map TaraEntry(planet, "-", "-", "")
            val p = nakshatraNumber1Based(pdeg)
            val cls = tClass(j, p)
            val name = taraNames[cls - 1]
            val note = taraNotes[cls] ?: ""
            val result = when (cls) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }
            TaraEntry(planet = planet, tara = name, result = result, note = note)
        }
    }

    // Builds a 27-row table of Tara Bala for each nakshatra relative to a given Moon nakshatra index (1..27)
    fun computeForMoonNakshatra(moonNakshatraIndex: Int): List<TaraTableEntry> {
        val j = moonNakshatraIndex.coerceIn(1, 27)
        return (1..27).map { p ->
            val cls = tClass(j, p)
            val taraName = taraNames[cls - 1]
            val result = when (cls) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }
            val nakName = nakshatraNames[p - 1]
            TaraTableEntry(
                nakshatraName = nakName,
                tara = taraName,
                result = result,
                taraNumber = cls
            )
        }
    }
}
