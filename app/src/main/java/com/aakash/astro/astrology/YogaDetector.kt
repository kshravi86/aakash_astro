package com.aakash.astro.astrology

data class YogaResult(
    val name: String,
    val description: String
)

object YogaDetector {

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

    private val exaltation: Map<Planet, ZodiacSign> = mapOf(
        Planet.SUN to ZodiacSign.ARIES,
        Planet.MOON to ZodiacSign.TAURUS,
        Planet.MARS to ZodiacSign.CAPRICORN,
        Planet.MERCURY to ZodiacSign.VIRGO,
        Planet.JUPITER to ZodiacSign.CANCER,
        Planet.VENUS to ZodiacSign.PISCES,
        Planet.SATURN to ZodiacSign.LIBRA,
    )

    private val debilitation: Map<Planet, ZodiacSign> = mapOf(
        Planet.SUN to ZodiacSign.LIBRA,
        Planet.MOON to ZodiacSign.SCORPIO,
        Planet.MARS to ZodiacSign.CANCER,
        Planet.MERCURY to ZodiacSign.PISCES,
        Planet.JUPITER to ZodiacSign.CAPRICORN,
        Planet.VENUS to ZodiacSign.VIRGO,
        Planet.SATURN to ZodiacSign.ARIES,
    )

    fun detect(chart: ChartResult): List<YogaResult> {
        val results = mutableListOf<YogaResult>()

        results += detectPanchaMahapurusha(chart)
        detectGajaKesari(chart)?.let { results += it }
        detectBudhaAditya(chart)?.let { results += it }
        detectChandraMangala(chart)?.let { results += it }
        // Moon-centered yogas adjacent to Kemadruma family
        results += detectAnaphaSunaphaDurdhara(chart)
        results += detectRajaYogaSimple(chart)
        results += detectDhanaYogaSimple(chart)
        // Use safe helper version
        results += detectParivartanaSafe(chart)
        results += detectViparitaRaja(chart)
        detectKemadruma(chart)?.let { results += it }
        results += detectNeechaBhanga(chart)

        // Nabhasa (Āsraya, Dala, Saṅkhya)
        results += NabhasaYoga.detectNabhasa(chart)

        return results
    }

    private fun detectAnaphaSunaphaDurdhara(chart: ChartResult): List<YogaResult> {
        val moon = chart.planets.find { it.planet == Planet.MOON } ?: return emptyList()
        val secondFromMoon = (moon.house % 12) + 1
        val twelfthFromMoon = ((moon.house + 10) % 12) + 1
        val allowed = setOf(Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN)
        val inSecond = chart.planets.filter { it.house == secondFromMoon && it.planet in allowed }
        val inTwelfth = chart.planets.filter { it.house == twelfthFromMoon && it.planet in allowed }

        val results = mutableListOf<YogaResult>()
        if (inSecond.isNotEmpty() && inTwelfth.isNotEmpty()) {
            val left = inTwelfth.joinToString { it.name }
            val right = inSecond.joinToString { it.name }
            results += YogaResult(
                name = "Durdhara Yoga",
                description = "Planets flank the Moon: 12th (H$twelfthFromMoon: $left) and 2nd (H$secondFromMoon: $right)."
            )
            return results
        }
        if (inTwelfth.isNotEmpty()) {
            val list = inTwelfth.joinToString { it.name }
            results += YogaResult(
                name = "Anapha Yoga",
                description = "Planets in 12th from Moon (H$twelfthFromMoon): $list."
            )
        }
        if (inSecond.isNotEmpty()) {
            val list = inSecond.joinToString { it.name }
            results += YogaResult(
                name = "Sunapha Yoga",
                description = "Planets in 2nd from Moon (H$secondFromMoon): $list."
            )
        }
        return results
    }

    private fun detectPanchaMahapurusha(chart: ChartResult): List<YogaResult> {
        val list = mutableListOf<YogaResult>()
        val kendraHouses = setOf(1, 4, 7, 10)
        fun isOwnSign(planet: Planet, sign: ZodiacSign): Boolean = signLord[sign] == planet
        fun isExalted(planet: Planet, sign: ZodiacSign): Boolean = exaltation[planet] == sign

        chart.planets.forEach { p ->
            if (p.planet !in setOf(Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN)) return@forEach
            if (p.house !in kendraHouses) return@forEach
            val strong = isOwnSign(p.planet, p.sign) || isExalted(p.planet, p.sign)
            if (!strong) return@forEach
            when (p.planet) {
                Planet.MARS -> list += YogaResult(
                    name = "Ruchaka Yoga",
                    description = "Mars strong (own/exalted) in a Kendra house (${p.sign.displayName}, H${p.house})."
                )
                Planet.MERCURY -> list += YogaResult(
                    name = "Bhadra Yoga",
                    description = "Mercury strong (own/exalted) in a Kendra house (${p.sign.displayName}, H${p.house})."
                )
                Planet.JUPITER -> list += YogaResult(
                    name = "Hamsa Yoga",
                    description = "Jupiter strong (own/exalted) in a Kendra house (${p.sign.displayName}, H${p.house})."
                )
                Planet.VENUS -> list += YogaResult(
                    name = "Malavya Yoga",
                    description = "Venus strong (own/exalted) in a Kendra house (${p.sign.displayName}, H${p.house})."
                )
                Planet.SATURN -> list += YogaResult(
                    name = "Shasha Yoga",
                    description = "Saturn strong (own/exalted) in a Kendra house (${p.sign.displayName}, H${p.house})."
                )
                else -> {}
            }
        }
        return list
    }

    private fun detectGajaKesari(chart: ChartResult): YogaResult? {
        val moon = chart.planets.find { it.planet == Planet.MOON } ?: return null
        val jupiter = chart.planets.find { it.planet == Planet.JUPITER } ?: return null
        val diff = (jupiter.house - moon.house + 12) % 12
        return if (diff in setOf(0, 3, 6, 9)) {
            YogaResult(
                name = "Gaja Kesari Yoga",
                description = "Jupiter is in a Kendra from the Moon (Moon: H${moon.house}, Jupiter: H${jupiter.house})."
            )
        } else null
    }

    private fun detectBudhaAditya(chart: ChartResult): YogaResult? {
        val sun = chart.planets.find { it.planet == Planet.SUN } ?: return null
        val mercury = chart.planets.find { it.planet == Planet.MERCURY } ?: return null
        return if (sun.house == mercury.house) {
            YogaResult(
                name = "Budha Aditya Yoga",
                description = "Sun and Mercury are conjoined in House ${sun.house} (${sun.sign.displayName})."
            )
        } else null
    }

    private fun detectChandraMangala(chart: ChartResult): YogaResult? {
        val moon = chart.planets.find { it.planet == Planet.MOON } ?: return null
        val mars = chart.planets.find { it.planet == Planet.MARS } ?: return null
        return if (moon.house == mars.house) {
            YogaResult(
                name = "Chandra-Mangala Yoga",
                description = "Moon and Mars are conjoined in House ${moon.house} (${moon.sign.displayName})."
            )
        } else null
    }

    private fun detectRajaYogaSimple(chart: ChartResult): List<YogaResult> {
        val kendras = setOf(1, 4, 7, 10)
        val trikonas = setOf(1, 5, 9)
        val houseSign = chart.houses.associate { it.number to it.sign }
        fun lordOfHouse(h: Int): Planet = signLord[houseSign[h]!!]!!
        val kendraLords = kendras.map { lordOfHouse(it) }.toSet()
        val trikonaLords = trikonas.map { lordOfHouse(it) }.toSet()
        val byPlanet = chart.planets.associateBy { it.planet }
        val list = mutableListOf<YogaResult>()
        trikonaLords.forEach { tLord ->
            kendraLords.forEach { kLord ->
                val tp = byPlanet[tLord]
                val kp = byPlanet[kLord]
                if (tp != null && kp != null && tp.house == kp.house) {
                    list += YogaResult(
                        name = "Raja Yoga (conjunction)",
                        description = "Lord of ${houseOfFixed(trikonas, tLord, houseSign)} and lord of ${houseOfFixed(kendras, kLord, houseSign)} conjoined in H${tp.house}."
                    )
                }
            }
        }
        return list
    }

    private fun detectDhanaYogaSimple(chart: ChartResult): List<YogaResult> {
        val houseSign = chart.houses.associate { it.number to it.sign }
        fun lordOfHouse(h: Int): Planet = signLord[houseSign[h]!!]!!
        val lord2 = lordOfHouse(2)
        val lord11 = lordOfHouse(11)
        val byPlanet = chart.planets.associateBy { it.planet }
        val p2 = byPlanet[lord2]
        val p11 = byPlanet[lord11]
        val list = mutableListOf<YogaResult>()
        if (p2 != null && p11 != null && p2.house == p11.house) {
            list += YogaResult(
                name = "Dhana Yoga (2nd & 11th lords)",
                description = "Lords of 2nd and 11th are conjoined in H${p2.house}."
            )
        }
        if (p2 != null && p2.house == 11) {
            list += YogaResult(
                name = "Dhana Yoga (2L in 11H)",
                description = "2nd lord is placed in the 11th house."
            )
        }
        if (p11 != null && p11.house == 2) {
            list += YogaResult(
                name = "Dhana Yoga (11L in 2H)",
                description = "11th lord is placed in the 2nd house."
            )
        }
        return list
    }

    private fun detectViparitaRaja(chart: ChartResult): List<YogaResult> {
        val dusthanas = setOf(6, 8, 12)
        val houseSign = chart.houses.associate { it.number to it.sign }
        fun lordOfHouse(h: Int): Planet = signLord[houseSign[h]!!]!!
        val byPlanet = chart.planets.associateBy { it.planet }
        val list = mutableListOf<YogaResult>()
        list += listOf(6, 8, 12).mapNotNull { h ->
            val lord = lordOfHouse(h)
            val pos = byPlanet[lord]
            if (pos != null && pos.house in dusthanas && pos.house != h) {
                YogaResult(
                    name = "Viparita Raja Yoga",
                    description = "Lord of ${h}th in ${pos.house}th (dusthana-in-dusthana)."
                )
            } else null
        }
        return list
    }

    private fun detectKemadruma(chart: ChartResult): YogaResult? {
        val moon = chart.planets.find { it.planet == Planet.MOON } ?: return null
        val neighbors = chart.planets.filter {
            it.planet != Planet.MOON && (
                it.house == ((moon.house % 12) + 1) ||
                    it.house == (((moon.house + 10) % 12) + 1) ||
                    it.house == moon.house
                )
        }
        return if (neighbors.isEmpty()) {
            YogaResult(
                name = "Kemadruma Yoga",
                description = "No planets conjoined with Moon or in 2nd/12th from Moon (base rule)."
            )
        } else null
    }

    private fun detectNeechaBhanga(chart: ChartResult): List<YogaResult> {
        val byPlanet = chart.planets.associateBy { it.planet }
        val kendras = setOf(1, 4, 7, 10)
        val list = mutableListOf<YogaResult>()
        chart.planets.forEach { p ->
            val debSign = debilitation[p.planet] ?: return@forEach
            if (p.sign == debSign) {
                val debSignLord = signLord[debSign]
                val lordPos = debSignLord?.let { byPlanet[it] }
                if (lordPos != null && lordPos.house in kendras) {
                    list += YogaResult(
                        name = "Neecha Bhanga Raja Yoga",
                        description = "${p.name} debilitated in ${p.sign.displayName}, cancelled by dispositor in Kendra (H${lordPos.house})."
                    )
                }
            }
        }
        return list
    }
}

