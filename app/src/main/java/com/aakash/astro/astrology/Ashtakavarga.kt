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

    // Bhinnashtakavarga house sets per Parashara (as provided), houses are 1..12 counted from the source (contributor) position.
    private val BAV_SUN = mapOf(
        Contributor.SUN to h(1,2,4,7,8,9,10,11),
        Contributor.MOON to h(3,6,10,11),
        Contributor.MARS to h(1,2,4,7,8,9,10,11),
        Contributor.MERCURY to h(3,5,6,9,10,11,12),
        Contributor.JUPITER to h(5,6,9,11),
        Contributor.VENUS to h(6,7,12),
        Contributor.SATURN to h(1,2,4,7,8,9,10,11),
        Contributor.LAGNA to h(3,4,6,10,11,12),
    )

    private val BAV_MOON = mapOf(
        Contributor.SUN to h(3,6,7,8,10,11),
        Contributor.MOON to h(1,3,6,7,10,11),
        Contributor.MARS to h(2,3,5,6,9,10,11),
        Contributor.MERCURY to h(1,3,4,5,7,8,10,11),
        Contributor.JUPITER to h(1,4,7,8,10,11,12),
        Contributor.VENUS to h(3,4,5,7,9,10,11),
        Contributor.SATURN to h(3,5,6,11),
        Contributor.LAGNA to h(3,6,10,11),
    )

    private val BAV_MARS = mapOf(
        Contributor.SUN to h(1,2,4,7,8,10,11),
        Contributor.MOON to h(3,6,11),
        Contributor.MARS to h(1,2,4,7,8,10,11),
        Contributor.MERCURY to h(3,5,6,11),
        Contributor.JUPITER to h(6,10,11,12),
        Contributor.VENUS to h(6,8,11,12),
        Contributor.SATURN to h(1,4,7,8,9,10,11),
        Contributor.LAGNA to h(1,3,6,10,11),
    )

    private val BAV_MERCURY = mapOf(
        Contributor.SUN to h(5,6,9,11,12),
        Contributor.MOON to h(2,4,6,8,10,11),
        Contributor.MARS to h(1,2,4,7,8,9,10,11),
        Contributor.MERCURY to h(1,3,5,6,9,10,11,12),
        Contributor.JUPITER to h(6,8,11,12),
        Contributor.VENUS to h(1,2,3,4,5,8,9,11),
        Contributor.SATURN to h(1,2,4,7,8,9,10,11),
        Contributor.LAGNA to h(1,2,4,6,8,10,11),
    )

    private val BAV_JUPITER = mapOf(
        Contributor.SUN to h(1,2,3,4,7,8,9,10,11),
        Contributor.MOON to h(2,5,7,9,11),
        Contributor.MARS to h(1,2,4,7,8,10,11),
        Contributor.MERCURY to h(1,2,4,5,6,9,10,11),
        Contributor.JUPITER to h(1,2,3,4,7,8,10,11),
        Contributor.VENUS to h(2,5,6,9,10,11),
        Contributor.SATURN to h(3,5,6,12),
        Contributor.LAGNA to h(1,2,4,5,6,7,9,10,11),
    )

    private val BAV_VENUS = mapOf(
        Contributor.SUN to h(8,11,12),
        Contributor.MOON to h(1,2,3,4,5,8,9,11,12),
        Contributor.MARS to h(3,5,6,9,11,12),
        Contributor.MERCURY to h(3,5,6,9,11),
        Contributor.JUPITER to h(5,8,9,10,11),
        Contributor.VENUS to h(1,2,3,4,5,8,9,10,11),
        Contributor.SATURN to h(3,4,5,8,9,10,11),
        Contributor.LAGNA to h(1,2,3,4,5,8,9,11),
    )

    private val BAV_SATURN = mapOf(
        Contributor.SUN to h(1,2,4,7,8,10,11),
        Contributor.MOON to h(3,6,11),
        Contributor.MARS to h(3,5,6,10,11,12),
        Contributor.MERCURY to h(6,8,9,10,11,12),
        Contributor.JUPITER to h(5,6,11,12),
        Contributor.VENUS to h(6,11,12),
        Contributor.SATURN to h(3,5,6,11),
        Contributor.LAGNA to h(1,3,4,6,10,11),
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

    // Dynamic (position-dependent) BAV per Parashara rule
    // For reference P and contributor C: d = distance from P->C (1..12).
    // If table[P][C] contains d, add 1 bindu to target sign = P + d (which equals C's sign).
    fun computeBAVFor(ref: Planet, chart: ChartResult): Binnashtakavarga {
        val byPlanet = chart.planets.associateBy { it.planet }
        val ascIndex = chart.ascendantSign.ordinal
        val refIndex = signIndexOf(ref, byPlanet) ?: return Binnashtakavarga(ref, IntArray(12))
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

        for ((contrib, allowed) in table) {
            val cIdx = contribIndex(contrib) ?: continue
            var d = (cIdx - refIndex) % 12
            if (d < 0) d += 12
            if (d == 0) d = 12
            if (allowed.contains(d)) {
                // target = ref + d == contributor's sign
                val target = (refIndex + d) % 12
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

    // Static (table-based) patterns and rotated outputs (optional display mode)
    private val overridePatterns: Map<Planet, IntArray> = mapOf(
        // House 1..12 for MOON BAV (sum = 49) per provided standard
        Planet.MOON to intArrayOf(7, 2, 4, 3, 4, 4, 4, 5, 2, 6, 4, 4)
    )

    private fun patternFromTables(ref: Planet): IntArray {
        val table = tableByRef[ref] ?: return IntArray(12)
        val arr = IntArray(12)
        for (h in 1..12) {
            var c = 0
            for ((_, set) in table) if (set.contains(h)) c++
            arr[h - 1] = c
        }
        return arr
    }

    private fun bavPattern(ref: Planet): IntArray = overridePatterns[ref] ?: patternFromTables(ref)

    private fun rotateToZodiac(patternHouse1To12: IntArray, refIndex: Int): IntArray {
        val out = IntArray(12)
        for (offset in 0 until 12) {
            val absIndex = (refIndex + offset) % 12
            out[absIndex] = patternHouse1To12[offset]
        }
        return out
    }

    fun computeBAVStaticFor(ref: Planet, chart: ChartResult): Binnashtakavarga {
        val byPlanet = chart.planets.associateBy { it.planet }
        val refIndex = signIndexOf(ref, byPlanet) ?: return Binnashtakavarga(ref, IntArray(12))
        val pattern = bavPattern(ref)
        val rotated = rotateToZodiac(pattern, refIndex)
        return Binnashtakavarga(ref, rotated)
    }

    // Parashara contributor-based multi-placement (exact workflow): houses counted from each source
    fun computeBAVParashara(ref: Planet, chart: ChartResult): Binnashtakavarga {
        val bav = IntArray(12)
        val rules = tableByRef[ref] ?: return Binnashtakavarga(ref, bav)
        val byPlanet = chart.planets.associateBy { it.planet }
        val ascIndex = chart.ascendantSign.ordinal

        fun sourceIndex(c: Contributor): Int? = when (c) {
            Contributor.LAGNA -> ascIndex
            Contributor.SUN -> byPlanet[Planet.SUN]?.sign?.ordinal
            Contributor.MOON -> byPlanet[Planet.MOON]?.sign?.ordinal
            Contributor.MARS -> byPlanet[Planet.MARS]?.sign?.ordinal
            Contributor.MERCURY -> byPlanet[Planet.MERCURY]?.sign?.ordinal
            Contributor.JUPITER -> byPlanet[Planet.JUPITER]?.sign?.ordinal
            Contributor.VENUS -> byPlanet[Planet.VENUS]?.sign?.ordinal
            Contributor.SATURN -> byPlanet[Planet.SATURN]?.sign?.ordinal
        }

        for ((contrib, houseSet) in rules) {
            val sIdx = sourceIndex(contrib) ?: continue
            for (h in houseSet) {
                val t = (sIdx + (h - 1)) % 12
                bav[t] += 1
            }
        }
        return Binnashtakavarga(ref, bav)
    }

    fun computeSAVParashara(chart: ChartResult): SarvaAshtakavarga {
        val sav = IntArray(12)
        val refs = listOf(Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN)
        refs.forEach { ref ->
            val bav = computeBAVParashara(ref, chart)
            for (i in 0 until 12) sav[i] += bav.values[i]
        }
        return SarvaAshtakavarga(sav)
    }

    fun computeSarvaStatic(chart: ChartResult): SarvaAshtakavarga {
        val refs = listOf(Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN)
        val sav = IntArray(12)
        refs.forEach { ref ->
            val bav = computeBAVStaticFor(ref, chart).values
            for (i in 0 until 12) sav[i] += bav[i]
        }
        return SarvaAshtakavarga(sav)
    }

    // Lagnashtakavarga (LAV): use Saturn's BAV house sets, applied from each contributor's sign.
    // This mirrors the Parashara-style computation used for other planets, but with the Saturn rule table.
    fun computeLagnaBAV(chart: ChartResult): Binnashtakavarga = computeBAVParashara(Planet.SATURN, chart)
}
