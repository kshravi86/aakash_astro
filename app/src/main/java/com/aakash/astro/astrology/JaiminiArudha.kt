package com.aakash.astro.astrology

data class ArudhaEntry(
    val house: Int,
    val houseSign: ZodiacSign,
    val lord: Planet,
    val lordSign: ZodiacSign,
    val lordHouse: Int,
    val padaHouse: Int,
    val padaSign: ZodiacSign,
)

object JaiminiArudha {
    private val signLord: Map<ZodiacSign, Planet> = mapOf(
        ZodiacSign.ARIES to Planet.MARS,
        ZodiacSign.TAURUS to Planet.VENUS,
        ZodiacSign.GEMINI to Planet.MERCURY,
        ZodiacSign.CANCER to Planet.MOON,
        ZodiacSign.LEO to Planet.SUN,
        ZodiacSign.VIRGO to Planet.MERCURY,
        ZodiacSign.LIBRA to Planet.VENUS,
        ZodiacSign.SCORPIO to Planet.MARS,
        ZodiacSign.SAGITTARIUS to Planet.JUPITER,
        ZodiacSign.CAPRICORN to Planet.SATURN,
        ZodiacSign.AQUARIUS to Planet.SATURN,
        ZodiacSign.PISCES to Planet.JUPITER,
    )

    private fun advance(house: Int, steps: Int): Int {
        // house is 1..12, steps can be >=0. Inclusive counting uses steps = N-1
        val z = ((house - 1 + steps) % 12 + 12) % 12
        return z + 1
    }

    fun compute(chart: ChartResult): List<ArudhaEntry> {
        val byPlanet = chart.planets.associateBy { it.planet }
        val houseToSign = chart.houses.associate { it.number to it.sign }
        val list = mutableListOf<ArudhaEntry>()
        for (h in 1..12) {
            val sign = houseToSign[h] ?: continue
            val lord = signLord[sign] ?: continue
            val lordPos = byPlanet[lord] ?: continue
            val lordHouse = lordPos.house
            val same = lordHouse == h
            val seventh = lordHouse == advance(h, 6) // 7th from house

            val padaHouse = if (same || seventh) {
                // Exception: if lord is in same or 7th house, pada is 10th from the house
                advance(h, 9)
            } else {
                val distanceInclusive = ((lordHouse - h + 12) % 12) + 1 // 1..12
                advance(lordHouse, distanceInclusive - 1)
            }
            val padaSign = houseToSign[padaHouse] ?: ZodiacSign.fromDegree(((padaHouse - 1) * 30).toDouble())
            list += ArudhaEntry(
                house = h,
                houseSign = sign,
                lord = lord,
                lordSign = lordPos.sign,
                lordHouse = lordHouse,
                padaHouse = padaHouse,
                padaSign = padaSign,
            )
        }
        return list
    }
}

