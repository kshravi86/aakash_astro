package com.aakash.astro.astrology

/**
 * Computes Bhinnashtakavarga (BAV) for Sun..Saturn and aggregates to Sarva Ashtakavarga (SAV).
 * Implementation follows classical tables: For each reference planet P, each contributor (Sun..Saturn and Lagna)
 * gives a bindu to specific houses from P. That bindu is placed in the sign counted from P by that house.
 * SAV is the element-wise sum of all 7 BAV arrays.
 */
object AshtakavargaCalc {

    enum class Contributor { SUN, MOON, MARS, MERCURY, JUPITER, VENUS, SATURN, LAGNA }

    private fun h(vararg v: Int): Set<Int> = v.toSet()

    // Bhinnashtakavarga house sets for each reference planet, by contributor. Houses are 1..12 from the reference planet.
    // Note: These sets are a commonly used classical scheme. Subtle traditions may vary; this is a practical implementation for SAV.
    private val BAV_SUN = mapOf(
        Contributor.SUN to h(1,2,4,7,8,9,10,11),
        Contributor.MOON to h(3,6,10,11),
        Contributor.MARS to h(1,2,4,7,8,9,10,11),
        Contributor.MERCURY to h(3,5,6,9,10,11),
        Contributor.JUPITER to h(5,6,9,11),
        Contributor.VENUS to h(6,7,12),
        Contributor.SATURN to h(3,4,6,10,11),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_MOON = mapOf(
        Contributor.SUN to h(3,6,7,10,11),
        Contributor.MOON to h(1,3,6,7,10,11),
        Contributor.MARS to h(3,6,11),
        Contributor.MERCURY to h(1,3,4,5,7,8,10,11),
        Contributor.JUPITER to h(1,2,4,7,8,10,11,12),
        Contributor.VENUS to h(3,4,5,7,9,10,11),
        Contributor.SATURN to h(3,6,11,12),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_MARS = mapOf(
        Contributor.SUN to h(1,2,4,7,8,10,11),
        Contributor.MOON to h(3,6,11),
        Contributor.MARS to h(1,2,4,7,8,10,11),
        Contributor.MERCURY to h(3,5,6,9,10,11),
        Contributor.JUPITER to h(5,6,9,11),
        Contributor.VENUS to h(6,7,12),
        Contributor.SATURN to h(3,4,6,10,11),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_MERCURY = mapOf(
        Contributor.SUN to h(2,4,6,8,10,11),
        Contributor.MOON to h(1,2,3,4,5,7,8,10,11,12),
        Contributor.MARS to h(3,6,9,11),
        Contributor.MERCURY to h(1,2,4,5,6,8,9,10,11),
        Contributor.JUPITER to h(1,2,4,5,7,8,10,11,12),
        Contributor.VENUS to h(1,2,3,4,5,8,9,11,12),
        Contributor.SATURN to h(3,4,6,10,11,12),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_JUPITER = mapOf(
        Contributor.SUN to h(5,6,9,11),
        Contributor.MOON to h(1,2,4,7,8,10,11,12),
        Contributor.MARS to h(3,6,11),
        Contributor.MERCURY to h(1,2,4,5,6,8,9,10,11),
        Contributor.JUPITER to h(1,2,4,5,6,8,9,10,11,12),
        Contributor.VENUS to h(1,2,3,4,5,8,9,11,12),
        Contributor.SATURN to h(3,4,6,10,11,12),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_VENUS = mapOf(
        Contributor.SUN to h(6,7,12),
        Contributor.MOON to h(1,2,3,4,5,7,8,10,11,12),
        Contributor.MARS to h(3,6,9,11),
        Contributor.MERCURY to h(1,2,4,5,6,8,9,10,11,12),
        Contributor.JUPITER to h(1,2,4,5,7,8,10,11,12),
        Contributor.VENUS to h(1,2,3,4,5,8,9,11,12),
        Contributor.SATURN to h(3,4,6,10,11,12),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_SATURN = mapOf(
        Contributor.SUN to h(3,4,6,10,11),
        Contributor.MOON to h(3,6,11,12),
        Contributor.MARS to h(3,4,6,10,11),
        Contributor.MERCURY to h(1,2,4,5,6,8,9,10,11),
        Contributor.JUPITER to h(1,2,4,5,6,8,9,10,11,12),
        Contributor.VENUS to h(1,2,3,4,5,8,9,11,12),
        Contributor.SATURN to h(1,2,4,7,8,9,10,11),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val tableByRef: Map<Planet, Map<Contributor, Set<Int>>> = mapOf(
        Planet.SUN to BAV_SUN,
        Planet.MOON to BAV_MOON,
        Planet.MARS to BAV_MARS,
        Planet.MERCURY to BAV_MERCURY,
        Planet.JUPITER to BAV_JUPITER,
        Planet.VENUS to BAV_VENUS,
        Planet.SATURN to BAV_SATURN,
    )

    data class Binnashtakavarga(val ref: Planet, val values: IntArray)

    data class SarvaAshtakavarga(val values: IntArray) { // 12 signs, 0..11 Aries..Pisces
        fun total(): Int = values.sum()
    }

    private fun signIndexOf(planet: Planet, byPlanet: Map<Planet, PlanetPosition>): Int? =
        byPlanet[planet]?.sign?.ordinal

    fun computeBAVFor(ref: Planet, chart: ChartResult): Binnashtakavarga {
        val byPlanet = chart.planets.associateBy { it.planet }
        val ascIndex = chart.ascendantSign.ordinal
        val bav = IntArray(12)
        val table = tableByRef[ref] ?: return Binnashtakavarga(ref, bav)

        fun contribIndex(c: Contributor): Int? = when (c) {
            Contributor.LAGNA -> ascIndex
            Contributor.SUN -> signIndexOf(Planet.SUN, byPlanet)
            Contributor.MOON -> signIndexOf(Planet.MOON, byPlanet)
            Contributor.MARS -> signIndexOf(Planet.MARS, byPlanet)
            Contributor.MERCURY -> signIndexOf(Planet.MERCURY, byPlanet)
            Contributor.JUPITER -> signIndexOf(Planet.JUPITER, byPlanet)
            Contributor.VENUS -> signIndexOf(Planet.VENUS, byPlanet)
            Contributor.SATURN -> signIndexOf(Planet.SATURN, byPlanet)
        }

        for ((contrib, houses) in table) {
            val idx = contribIndex(contrib) ?: continue
            for (h in houses) { // Place bindu in each listed house counted from contributor's sign
                val target = (idx + (h - 1)) % 12
                bav[target] += 1
            }
        }
        return Binnashtakavarga(ref, bav)
    }

    fun computeSarva(chart: ChartResult): SarvaAshtakavarga {
        val refs = listOf(Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN)
        val sav = IntArray(12)
        refs.forEach { ref ->
            val bav = computeBAVFor(ref, chart)
            for (i in 0 until 12) sav[i] += bav.values[i]
        }
        return SarvaAshtakavarga(sav)
    }

    // Common practice: Lagnashtakavarga uses the Saturn row house-sets with reference = Lagna.
    fun computeLagnaBAV(chart: ChartResult): Binnashtakavarga {
        // Reuse computeBAVFor with Saturn table semantics
        // The computation uses contributor positions only, so the table choice is what matters.
        val byPlanet = chart.planets.associateBy { it.planet }
        val ascIndex = chart.ascendantSign.ordinal
        val bav = IntArray(12)
        val table = tableByRef[Planet.SATURN] ?: return Binnashtakavarga(Planet.SATURN, bav)

        fun contribIndex(c: Contributor): Int? = when (c) {
            Contributor.LAGNA -> ascIndex
            Contributor.SUN -> byPlanet[Planet.SUN]?.sign?.ordinal
            Contributor.MOON -> byPlanet[Planet.MOON]?.sign?.ordinal
            Contributor.MARS -> byPlanet[Planet.MARS]?.sign?.ordinal
            Contributor.MERCURY -> byPlanet[Planet.MERCURY]?.sign?.ordinal
            Contributor.JUPITER -> byPlanet[Planet.JUPITER]?.sign?.ordinal
            Contributor.VENUS -> byPlanet[Planet.VENUS]?.sign?.ordinal
            Contributor.SATURN -> byPlanet[Planet.SATURN]?.sign?.ordinal
        }

        for ((contrib, houses) in table) {
            val idx = contribIndex(contrib) ?: continue
            for (h in houses) {
                val target = (idx + (h - 1)) % 12
                bav[target] += 1
            }
        }
        return Binnashtakavarga(Planet.SATURN, bav)
    }
}
