package com.aakash.astro.astrology

/**
 * Common Ashta-koota (Gun Milan) calculator using natural friendships and standard North Indian tables.
 */
object GunMilanCalculator {

    private enum class Varna(val rank: Int) { SHUDRA(1), VAISHYA(2), KSHATRIYA(3), BRAHMIN(4) }
    private enum class Vashya { CHATUSPADA, MANAV, JALACHAR, VANCHAR, KEETA }
    private enum class Gana { DEVA, MANUSHYA, RAKSHASA }
    private enum class Yoni { HORSE, ELEPHANT, SHEEP, SERPENT, DOG, CAT, RAT, COW, BUFFALO, TIGER, HARE, MONKEY, MONGOOSE, LION }
    private enum class Nadi { AADI, MADHYA, ANTYA }
    private enum class Relation { FRIEND, NEUTRAL, ENEMY }

    data class KootaScore(val name: String, val score: Double, val max: Double, val note: String)
    data class Result(val total: Double, val max: Double = 36.0, val parts: List<KootaScore>)

    fun match(bride: ChartResult, groom: ChartResult): Result {
        val brideMoon = moonInfo(bride)
        val groomMoon = moonInfo(groom)
        val scores = listOf(
            varna(brideMoon, groomMoon),
            vashya(brideMoon, groomMoon),
            tara(brideMoon, groomMoon),
            yoni(brideMoon, groomMoon),
            grahaMaitri(bride, groom, brideMoon, groomMoon),
            gana(brideMoon, groomMoon),
            bhakoot(brideMoon, groomMoon),
            nadi(brideMoon, groomMoon)
        )
        val total = scores.sumOf { it.score }
        return Result(total, 36.0, scores)
    }

    private data class MoonInfo(
        val sign: ZodiacSign,
        val nakIndex1Based: Int,
        val lord: Planet,
        val degreeInSign: Double
    )

    private fun moonInfo(chart: ChartResult): MoonInfo {
        val moonDeg = chart.planets.find { it.planet == Planet.MOON }?.degree ?: 0.0
        val sign = ZodiacSign.fromDegree(moonDeg)
        val degreeInSign = ((moonDeg % 30.0) + 30.0) % 30.0
        val nak = TaraBalaCalc.nakshatraNumber1Based(moonDeg)
        return MoonInfo(sign, nak, signLord(sign), degreeInSign)
    }

    private fun varna(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val b = signVarna(bride.sign)
        val g = signVarna(groom.sign)
        val score = if (g.rank >= b.rank) 1.0 else 0.0
        return KootaScore("Varna", score, 1.0, "Bride: ${b.name}, Groom: ${g.name}")
    }

    private fun vashya(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val b = vashyaOf(bride.sign, bride.degreeInSign)
        val g = vashyaOf(groom.sign, groom.degreeInSign)
        val score = vashyaMatrix[g]!![b] ?: 0.0
        return KootaScore("Vashya", score, 2.0, "Bride: ${b.name}, Groom: ${g.name}")
    }

    private fun tara(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val diff = (groom.nakIndex1Based - bride.nakIndex1Based + 27) % 27 // bride -> groom direction
        val rem = diff % 9
        val score = if (rem % 2 == 0) 3.0 else 0.0
        return KootaScore("Tara", score, 3.0, "Nakshatra distance (bride->groom) mod 9 = $rem (even=auspicious)")
    }

    private fun yoni(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val b = yoniOf(bride.nakIndex1Based)
        val g = yoniOf(groom.nakIndex1Based)
        val score = yoniMatrix[g.ordinal][b.ordinal]
        return KootaScore("Yoni", score, 4.0, "Bride: ${b.name}, Groom: ${g.name}")
    }

    private fun grahaMaitri(
        brideChart: ChartResult,
        groomChart: ChartResult,
        bride: MoonInfo,
        groom: MoonInfo
    ): KootaScore {
        val relBG = compoundRelation(brideChart, bride.lord, groom.lord)
        val relGB = compoundRelation(groomChart, groom.lord, bride.lord)
        val score = grahaScore(relBG, relGB, bride.lord == groom.lord)
        val note = "Bride lord: ${bride.lord.displayName} (${relBG.name.lowercase()}), Groom lord: ${groom.lord.displayName} (${relGB.name.lowercase()})"
        return KootaScore("Graha Maitri", score, 5.0, note)
    }

    private fun gana(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val b = ganaOf(bride.nakIndex1Based)
        val g = ganaOf(groom.nakIndex1Based)
        val score = ganaMatrix[g]!![b] ?: 0.0
        return KootaScore("Gana", score, 6.0, "Bride: ${b.name}, Groom: ${g.name}")
    }

    private fun bhakoot(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val dist = (groom.sign.ordinal - bride.sign.ordinal + 12) % 12 + 1 // 1..12 bride -> groom
        if (dist == 1) return KootaScore("Bhakoot", 7.0, 7.0, "Same Moon sign")
        val inauspicious = setOf(2, 12, 5, 9, 6, 8) // classic 2/12, 5/9, 6/8 pairs
        val score = if (inauspicious.contains(dist)) 0.0 else 7.0
        return KootaScore("Bhakoot", score, 7.0, "Sign distance (bride->groom): $dist")
    }

    private fun nadi(bride: MoonInfo, groom: MoonInfo): KootaScore {
        val b = nadiOf(bride.nakIndex1Based)
        val g = nadiOf(groom.nakIndex1Based)
        val score = if (b == g) 0.0 else 8.0
        return KootaScore("Nadi", score, 8.0, "Bride: ${b.name}, Groom: ${g.name}")
    }

    private fun signVarna(sign: ZodiacSign): Varna = when (sign) {
        ZodiacSign.CANCER, ZodiacSign.SCORPIO, ZodiacSign.PISCES -> Varna.BRAHMIN
        ZodiacSign.ARIES, ZodiacSign.LEO, ZodiacSign.SAGITTARIUS -> Varna.KSHATRIYA
        ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.CAPRICORN -> Varna.VAISHYA
        else -> Varna.SHUDRA
    }

    private fun signLord(sign: ZodiacSign): Planet = when (sign) {
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

    private fun relation(from: Planet, to: Planet): Relation = naturalRelations[from]!![to] ?: Relation.NEUTRAL

    private fun compoundRelation(chart: ChartResult, from: Planet, to: Planet): Relation {
        val natural = relation(from, to)
        val temporary = temporaryRelation(chart, from, to)
        return combineRelations(natural, temporary)
    }

    private fun temporaryRelation(chart: ChartResult, from: Planet, to: Planet): Relation {
        val fromDeg = chart.planets.find { it.planet == from }?.degree ?: return Relation.NEUTRAL
        val toDeg = chart.planets.find { it.planet == to }?.degree ?: return Relation.NEUTRAL
        val diff = (toDeg - fromDeg + 360.0) % 360.0
        val houseOffset = (diff / 30.0).toInt() + 1 // 1..12 relative to 'from'
        return when (houseOffset) {
            2, 3, 4, 10, 11, 12 -> Relation.FRIEND
            6, 7, 8 -> Relation.ENEMY
            else -> Relation.NEUTRAL
        }
    }

    private fun combineRelations(natural: Relation, temporary: Relation): Relation {
        if (natural == temporary) return natural
        return when (natural) {
            Relation.FRIEND -> when (temporary) {
                Relation.NEUTRAL -> Relation.FRIEND
                Relation.ENEMY -> Relation.NEUTRAL
                else -> Relation.FRIEND
            }
            Relation.NEUTRAL -> when (temporary) {
                Relation.FRIEND -> Relation.FRIEND
                Relation.ENEMY -> Relation.ENEMY
                else -> Relation.NEUTRAL
            }
            Relation.ENEMY -> when (temporary) {
                Relation.FRIEND -> Relation.NEUTRAL
                Relation.NEUTRAL -> Relation.ENEMY
                else -> Relation.ENEMY
            }
        }
    }

    private fun grahaScore(relBG: Relation, relGB: Relation, same: Boolean): Double = when {
        same -> 5.0
        relBG == Relation.FRIEND && relGB == Relation.FRIEND -> 5.0
        (relBG == Relation.FRIEND && relGB == Relation.NEUTRAL) || (relBG == Relation.NEUTRAL && relGB == Relation.FRIEND) -> 4.0
        relBG == Relation.NEUTRAL && relGB == Relation.NEUTRAL -> 3.0
        (relBG == Relation.FRIEND && relGB == Relation.ENEMY) || (relBG == Relation.ENEMY && relGB == Relation.FRIEND) -> 1.0
        (relBG == Relation.NEUTRAL && relGB == Relation.ENEMY) || (relBG == Relation.ENEMY && relGB == Relation.NEUTRAL) -> 0.5
        else -> 0.0
    }

    private fun yoniOf(nak1Based: Int): Yoni = yoniMap.getOrElse(nak1Based) { Yoni.HORSE }
    private fun ganaOf(nak1Based: Int): Gana = ganaMap.getOrElse(nak1Based) { Gana.MANUSHYA }
    private fun nadiOf(nak1Based: Int): Nadi = nadiMap.getOrElse(nak1Based) { Nadi.AADI }

    // Static tables
    private val naturalRelations: Map<Planet, Map<Planet, Relation>> = mapOf(
        Planet.SUN to mapOf(Planet.MOON to Relation.FRIEND, Planet.MARS to Relation.FRIEND, Planet.MERCURY to Relation.NEUTRAL, Planet.JUPITER to Relation.FRIEND, Planet.VENUS to Relation.ENEMY, Planet.SATURN to Relation.ENEMY),
        Planet.MOON to mapOf(Planet.SUN to Relation.FRIEND, Planet.MARS to Relation.NEUTRAL, Planet.MERCURY to Relation.FRIEND, Planet.JUPITER to Relation.NEUTRAL, Planet.VENUS to Relation.NEUTRAL, Planet.SATURN to Relation.NEUTRAL),
        Planet.MARS to mapOf(Planet.SUN to Relation.FRIEND, Planet.MOON to Relation.FRIEND, Planet.MERCURY to Relation.ENEMY, Planet.JUPITER to Relation.FRIEND, Planet.VENUS to Relation.NEUTRAL, Planet.SATURN to Relation.NEUTRAL),
        Planet.MERCURY to mapOf(Planet.SUN to Relation.NEUTRAL, Planet.MOON to Relation.ENEMY, Planet.MARS to Relation.ENEMY, Planet.JUPITER to Relation.ENEMY, Planet.VENUS to Relation.FRIEND, Planet.SATURN to Relation.NEUTRAL),
        Planet.JUPITER to mapOf(Planet.SUN to Relation.FRIEND, Planet.MOON to Relation.FRIEND, Planet.MARS to Relation.FRIEND, Planet.MERCURY to Relation.ENEMY, Planet.VENUS to Relation.ENEMY, Planet.SATURN to Relation.NEUTRAL),
        Planet.VENUS to mapOf(Planet.SUN to Relation.ENEMY, Planet.MOON to Relation.NEUTRAL, Planet.MARS to Relation.NEUTRAL, Planet.MERCURY to Relation.FRIEND, Planet.JUPITER to Relation.ENEMY, Planet.SATURN to Relation.FRIEND),
        Planet.SATURN to mapOf(Planet.SUN to Relation.ENEMY, Planet.MOON to Relation.NEUTRAL, Planet.MARS to Relation.ENEMY, Planet.MERCURY to Relation.NEUTRAL, Planet.JUPITER to Relation.NEUTRAL, Planet.VENUS to Relation.FRIEND)
    )

    private val vashyaMatrix: Map<Vashya, Map<Vashya, Double>> = mapOf(
        Vashya.CHATUSPADA to mapOf(Vashya.CHATUSPADA to 2.0, Vashya.MANAV to 1.0, Vashya.JALACHAR to 1.0, Vashya.VANCHAR to 1.0, Vashya.KEETA to 0.5),
        Vashya.MANAV to mapOf(Vashya.CHATUSPADA to 1.0, Vashya.MANAV to 2.0, Vashya.JALACHAR to 1.0, Vashya.VANCHAR to 1.5, Vashya.KEETA to 1.0),
        Vashya.JALACHAR to mapOf(Vashya.CHATUSPADA to 1.0, Vashya.MANAV to 1.0, Vashya.JALACHAR to 2.0, Vashya.VANCHAR to 1.0, Vashya.KEETA to 1.0),
        Vashya.VANCHAR to mapOf(Vashya.CHATUSPADA to 1.0, Vashya.MANAV to 1.5, Vashya.JALACHAR to 1.0, Vashya.VANCHAR to 2.0, Vashya.KEETA to 0.0),
        Vashya.KEETA to mapOf(Vashya.CHATUSPADA to 0.5, Vashya.MANAV to 1.0, Vashya.JALACHAR to 1.0, Vashya.VANCHAR to 0.0, Vashya.KEETA to 2.0)
    )

    private val yoniMatrix: Array<DoubleArray> = arrayOf(
        doubleArrayOf(4.0, 3.0, 2.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 2.0, 2.0, 2.0, 1.0, 2.0), // Horse
        doubleArrayOf(3.0, 4.0, 3.0, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0), // Elephant
        doubleArrayOf(2.0, 3.0, 4.0, 2.0, 2.0, 2.0, 2.0, 3.0, 3.0, 2.0, 2.0, 2.0, 2.0, 1.0), // Sheep
        doubleArrayOf(1.0, 2.0, 2.0, 4.0, 0.0, 1.0, 1.0, 2.0, 2.0, 1.0, 1.0, 1.0, 0.0, 1.0), // Serpent
        doubleArrayOf(1.0, 2.0, 2.0, 0.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 1.0), // Dog
        doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0, 2.0, 1.0, 1.0), // Cat
        doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 4.0, 2.0, 2.0, 1.0, 1.0, 1.0, 0.0, 1.0), // Rat
        doubleArrayOf(3.0, 3.0, 3.0, 2.0, 2.0, 2.0, 2.0, 4.0, 3.0, 2.0, 2.0, 2.0, 2.0, 2.0), // Cow
        doubleArrayOf(3.0, 3.0, 3.0, 2.0, 2.0, 2.0, 2.0, 3.0, 4.0, 2.0, 2.0, 2.0, 2.0, 2.0), // Buffalo
        doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0, 4.0, 3.0, 2.0, 2.0, 3.0), // Tiger
        doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0, 3.0, 4.0, 2.0, 2.0, 3.0), // Hare/Deer
        doubleArrayOf(2.0, 2.0, 2.0, 1.0, 2.0, 2.0, 1.0, 2.0, 2.0, 2.0, 2.0, 4.0, 3.0, 2.0), // Monkey
        doubleArrayOf(1.0, 2.0, 2.0, 0.0, 2.0, 1.0, 0.0, 2.0, 2.0, 2.0, 2.0, 3.0, 4.0, 2.0), // Mongoose
        doubleArrayOf(2.0, 2.0, 1.0, 1.0, 1.0, 1.0, 1.0, 2.0, 2.0, 3.0, 3.0, 2.0, 2.0, 4.0)  // Lion
    )

    private val yoniMap: Map<Int, Yoni> = mapOf(
        1 to Yoni.HORSE, 2 to Yoni.ELEPHANT, 3 to Yoni.SHEEP, 4 to Yoni.SERPENT, 5 to Yoni.SERPENT, 6 to Yoni.DOG,
        7 to Yoni.CAT, 8 to Yoni.SHEEP, 9 to Yoni.CAT, 10 to Yoni.RAT, 11 to Yoni.RAT, 12 to Yoni.COW,
        13 to Yoni.BUFFALO, 14 to Yoni.TIGER, 15 to Yoni.BUFFALO, 16 to Yoni.TIGER, 17 to Yoni.HARE, 18 to Yoni.HARE,
        19 to Yoni.DOG, 20 to Yoni.MONKEY, 21 to Yoni.MONGOOSE, 22 to Yoni.MONKEY, 23 to Yoni.LION, 24 to Yoni.HORSE,
        25 to Yoni.LION, 26 to Yoni.COW, 27 to Yoni.ELEPHANT
    )

    private val ganaMap: Map<Int, Gana> = mapOf(
        1 to Gana.DEVA, 2 to Gana.MANUSHYA, 3 to Gana.RAKSHASA, 4 to Gana.MANUSHYA, 5 to Gana.DEVA, 6 to Gana.RAKSHASA,
        7 to Gana.DEVA, 8 to Gana.DEVA, 9 to Gana.RAKSHASA, 10 to Gana.RAKSHASA, 11 to Gana.MANUSHYA, 12 to Gana.MANUSHYA,
        13 to Gana.DEVA, 14 to Gana.RAKSHASA, 15 to Gana.DEVA, 16 to Gana.RAKSHASA, 17 to Gana.DEVA, 18 to Gana.RAKSHASA,
        19 to Gana.RAKSHASA, 20 to Gana.MANUSHYA, 21 to Gana.MANUSHYA, 22 to Gana.DEVA, 23 to Gana.RAKSHASA, 24 to Gana.RAKSHASA,
        25 to Gana.MANUSHYA, 26 to Gana.MANUSHYA, 27 to Gana.DEVA
    )

    private val ganaMatrix: Map<Gana, Map<Gana, Double>> = mapOf(
        Gana.DEVA to mapOf(Gana.DEVA to 6.0, Gana.MANUSHYA to 5.0, Gana.RAKSHASA to 0.0),
        Gana.MANUSHYA to mapOf(Gana.DEVA to 5.0, Gana.MANUSHYA to 6.0, Gana.RAKSHASA to 1.0),
        Gana.RAKSHASA to mapOf(Gana.DEVA to 0.0, Gana.MANUSHYA to 1.0, Gana.RAKSHASA to 6.0)
    )

    private val nadiMap: Map<Int, Nadi> = mapOf(
        1 to Nadi.AADI, 2 to Nadi.MADHYA, 3 to Nadi.ANTYA, 4 to Nadi.ANTYA, 5 to Nadi.MADHYA, 6 to Nadi.AADI,
        7 to Nadi.AADI, 8 to Nadi.MADHYA, 9 to Nadi.ANTYA, 10 to Nadi.ANTYA, 11 to Nadi.MADHYA, 12 to Nadi.ANTYA,
        13 to Nadi.ANTYA, 14 to Nadi.MADHYA, 15 to Nadi.ANTYA, 16 to Nadi.ANTYA, 17 to Nadi.MADHYA, 18 to Nadi.AADI,
        19 to Nadi.AADI, 20 to Nadi.MADHYA, 21 to Nadi.ANTYA, 22 to Nadi.MADHYA, 23 to Nadi.ANTYA, 24 to Nadi.AADI,
        25 to Nadi.ANTYA, 26 to Nadi.MADHYA, 27 to Nadi.AADI
    )

    // Helper to handle Sag dual nature: use dwipada for 0-15°, chatuspada for 15-30°
    private fun vashyaOf(sign: ZodiacSign, degreeWithinSign: Double? = null): Vashya = when (sign) {
        ZodiacSign.ARIES, ZodiacSign.TAURUS, ZodiacSign.CAPRICORN -> Vashya.CHATUSPADA
        ZodiacSign.SAGITTARIUS -> {
            val deg = degreeWithinSign ?: 0.0
            if (deg < 15.0) Vashya.MANAV else Vashya.CHATUSPADA
        }
        ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.LIBRA, ZodiacSign.AQUARIUS -> Vashya.MANAV
        ZodiacSign.CANCER, ZodiacSign.PISCES -> Vashya.JALACHAR
        ZodiacSign.LEO -> Vashya.VANCHAR
        ZodiacSign.SCORPIO -> Vashya.KEETA
    }
}



