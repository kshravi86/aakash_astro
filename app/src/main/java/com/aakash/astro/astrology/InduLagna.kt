package com.aakash.astro.astrology

data class InduLagnaResult(
    val sign: ZodiacSign,
    val houseFromAsc: Int,
    val ninthLordFromAsc: Planet,
    val ninthLordFromMoon: Planet,
    val sumValue: Int,
    val remainder: Int
)

object InduLagnaCalc {
    private fun signLordOf(sign: ZodiacSign): Planet = when (sign) {
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

    private fun kalaValue(planet: Planet): Int = when (planet) {
        Planet.SUN -> 30
        Planet.MOON -> 16
        Planet.MARS -> 6
        Planet.MERCURY -> 8
        Planet.JUPITER -> 10
        Planet.VENUS -> 12
        Planet.SATURN -> 1
        // Rahu/Ketu are excluded from this calculation
        Planet.RAHU, Planet.KETU -> 0
    }

    fun compute(chart: ChartResult): InduLagnaResult? {
        val ascSign = chart.ascendantSign
        val moon = chart.planets.firstOrNull { it.planet == Planet.MOON } ?: return null
        val moonSign = moon.sign

        // 9th from Lagna and 9th from Moon (inclusive count -> +8 index)
        val ninthFromAsc = ZodiacSign.entries[(ascSign.ordinal + 8) % 12]
        val ninthFromMoon = ZodiacSign.entries[(moonSign.ordinal + 8) % 12]

        val lordAsc = signLordOf(ninthFromAsc)
        val lordMoon = signLordOf(ninthFromMoon)

        val sum = kalaValue(lordAsc) + kalaValue(lordMoon)
        var r = sum % 12
        if (r == 0) r = 12

        // Count r houses from the Moon (Moon as 1)
        val induSign = ZodiacSign.entries[(moonSign.ordinal + (r - 1)) % 12]

        // House number of Indu Lagna from Ascendant
        val houseFromAsc = ((induSign.ordinal - ascSign.ordinal + 12) % 12) + 1

        return InduLagnaResult(
            sign = induSign,
            houseFromAsc = houseFromAsc,
            ninthLordFromAsc = lordAsc,
            ninthLordFromMoon = lordMoon,
            sumValue = sum,
            remainder = r
        )
    }
}

