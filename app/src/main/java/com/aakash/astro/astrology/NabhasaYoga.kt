package com.aakash.astro.astrology

object NabhasaYoga {
    fun detectNabhasa(chart: ChartResult): List<YogaResult> {
        val list = mutableListOf<YogaResult>()
        list += detectAsraya(chart)
        detectDala(chart)?.let { list += it }
        detectSankhya(chart)?.let { list += it }
        return list
    }

    private fun coreSeven(planets: List<PlanetPosition>): List<PlanetPosition> =
        planets.filter { it.planet in setOf(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN
        ) }

    private fun isMovable(sign: ZodiacSign) = sign in setOf(
        ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN
    )
    private fun isFixed(sign: ZodiacSign) = sign in setOf(
        ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS
    )
    private fun isDual(sign: ZodiacSign) = sign in setOf(
        ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES
    )

    private fun detectAsraya(chart: ChartResult): List<YogaResult> {
        val core = coreSeven(chart.planets)
        if (core.isEmpty()) return emptyList()
        val allMovable = core.all { isMovable(it.sign) }
        val allFixed = core.all { isFixed(it.sign) }
        val allDual = core.all { isDual(it.sign) }
        val planetSummary = core.joinToString { "${it.name}(${it.sign.displayName})" }
        return when {
            allMovable -> listOf(
                YogaResult(
                    name = "Rajju (Āsraya)",
                    description = "All seven classical planets occupy movable signs: $planetSummary."
                )
            )
            allFixed -> listOf(
                YogaResult(
                    name = "Musala (Āsraya)",
                    description = "All seven classical planets occupy fixed signs: $planetSummary."
                )
            )
            allDual -> listOf(
                YogaResult(
                    name = "Nala (Āsraya)",
                    description = "All seven classical planets occupy dual signs: $planetSummary."
                )
            )
            else -> emptyList()
        }
    }

    private fun detectDala(chart: ChartResult): YogaResult? {
        val core = coreSeven(chart.planets)
        val kendras = setOf(1, 4, 7, 10)
        val inKendra = core.filter { it.house in kendras }
        if (inKendra.isEmpty()) return null
        val benefics = setOf(Planet.JUPITER, Planet.VENUS, Planet.MERCURY, Planet.MOON)
        val malefics = setOf(Planet.SUN, Planet.MARS, Planet.SATURN)
        val allBenefic = inKendra.all { it.planet in benefics }
        val allMalefic = inKendra.all { it.planet in malefics }
        val positions = inKendra.sortedBy { it.house }.joinToString { "H${it.house}:${it.name}" }
        return when {
            allBenefic -> YogaResult(
                name = "Mala (Dala)",
                description = "Benefics occupy Kendra houses only: $positions."
            )
            allMalefic -> YogaResult(
                name = "Sarpa (Dala)",
                description = "Malefics occupy Kendra houses only: $positions."
            )
            else -> null
        }
    }

    private fun detectSankhya(chart: ChartResult): YogaResult? {
        val core = coreSeven(chart.planets)
        if (core.isEmpty()) return null
        val bySign = core.groupBy { it.sign }
        val count = bySign.keys.size
        val name = when (count) {
            1 -> "Gola (Sankhya)"
            2 -> "Yuga (Sankhya)"
            3 -> "Shoola (Sankhya)"
            4 -> "Kedara (Sankhya)"
            5 -> "Pasha (Sankhya)"
            6 -> "Dama (Sankhya)"
            7 -> "Veena (Sankhya)"
            else -> null
        } ?: return null
        val distribution = bySign.entries
            .sortedBy { it.key.ordinal }
            .joinToString { e -> "${e.key.displayName}: ${e.value.joinToString { it.name }}" }
        return YogaResult(
            name = name,
            description = "Seven planets span $count sign(s). Distribution: $distribution."
        )
    }
}

// Safe helpers used by YogaDetector
fun houseOfFixed(pool: Set<Int>, planet: Planet, houseSign: Map<Int, ZodiacSign>): String {
    val h = pool.firstOrNull { signLordOf(houseSign[it]!!) == planet }
    return h?.toString() ?: "?"
}

fun detectParivartanaSafe(chart: ChartResult): List<YogaResult> {
    val byPlanet = chart.planets.associateBy { it.planet }
    val list = mutableListOf<YogaResult>()
    val planets = chart.planets.map { it.planet }
    for (i in planets.indices) {
        for (j in i + 1 until planets.size) {
            val a = planets[i]
            val b = planets[j]
            val pa = byPlanet[a] ?: continue
            val pb = byPlanet[b] ?: continue
            val lordOfSignA = signLordOf(pa.sign)
            val lordOfSignB = signLordOf(pb.sign)
            if (lordOfSignA == b && lordOfSignB == a) {
                list += YogaResult(
                    name = "Parivartana Yoga",
                    description = "Mutual exchange between ${a.displayName} and ${b.displayName} (signs ${pa.sign.displayName} <-> ${pb.sign.displayName})."
                )
            }
        }
    }
    return list
}

// Local sign lord map to avoid accessing YogaDetector internals
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
