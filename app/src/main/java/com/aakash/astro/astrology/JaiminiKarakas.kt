package com.aakash.astro.astrology

data class KarakaEntry(
    val rank: Int,
    val karakaName: String,
    val planet: Planet,
    val degreeInSign: Double,
    val absoluteDegree: Double,
    val sign: ZodiacSign,
    val house: Int
)

object JaiminiKarakas {
    private val karakaLabels7 = listOf(
        "Atmakaraka",
        "Amatyakaraka",
        "Bhratrikaraka",
        "Matrikaraka",
        "Putrakaraka",
        "Gnatikaraka",
        "Darakaraka"
    )

    fun compute(chart: ChartResult, includeRahuKetu: Boolean = false): List<KarakaEntry> {
        val core = chart.planets.filter { it.planet in setOf(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN
        ) }.toMutableList()
        if (includeRahuKetu) {
            // Most traditions include only Rahu (not Ketu) and reverse measure; keeping optional off by default.
            val rahu = chart.planets.find { it.planet == Planet.RAHU }
            rahu?.let { core += it }
        }
        val ranked = core
            .map { p ->
                val inSignDeg = p.degree % 30.0
                val score = if (p.planet == Planet.RAHU) 30.0 - inSignDeg else inSignDeg
                Triple(p, score, inSignDeg)
            }
            .sortedByDescending { it.second }

        val labels = karakaLabels7
        return ranked.take(labels.size).mapIndexed { idx, t ->
            val p = t.first
            KarakaEntry(
                rank = idx + 1,
                karakaName = labels[idx],
                planet = p.planet,
                degreeInSign = t.third,
                absoluteDegree = p.degree,
                sign = p.sign,
                house = p.house
            )
        }
    }
}

