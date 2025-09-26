package com.aakash.astro.astrology

import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.min

data class ShadbalaValues(
    val planet: Planet,
    val position: PlanetPosition,
    val naisargika: Double,
    val sthana: Double,   // Uccha + Saptavargaja + Ojayugmariamsa + Kendradi + Drekkana
    val dig: Double,      // directional strength (0..60)
    val kala: Double,     // placeholder (0..60)
    val cheshta: Double,  // retrograde-based approximation (0..60)
    val drik: Double,     // placeholder (0..60)
    val total: Double
)

object ShadbalaCalculator {

    fun compute(chart: ChartResult, birthDateTime: ZonedDateTime? = null): List<ShadbalaValues> {
        val seven = chart.planets.filter { it.planet in setOf(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN
        ) }
        return seven.map { p ->
            val nb = naisargikaBala(p.planet)
            val uccha = ucchaBala(p)
            val saptavargaja = saptavargajaBala(chart, p)
            val ojayu = ojayugmariamsaBala(p)
            val kendradi = kendradiBala(p)
            val drekkana = drekkanaBala(p)
            val sthanaTotal = uccha + saptavargaja + ojayu + kendradi + drekkana
            val dig = digBala(p)
            val cheshta = cheshtaBala(p)
            val kala = kalaBala(chart, p, birthDateTime)
            val drik = drikBala(chart, p)
            val total = nb + sthanaTotal + dig + cheshta + kala + drik
            ShadbalaValues(p.planet, p, nb, sthanaTotal, dig, kala, cheshta, drik, total)
        }
    }

    private fun naisargikaBala(planet: Planet): Double = when (planet) {
        Planet.SUN -> 60.0
        Planet.MOON -> 51.0
        Planet.MARS -> 32.0
        Planet.MERCURY -> 43.0
        Planet.JUPITER -> 34.0
        Planet.VENUS -> 54.0
        Planet.SATURN -> 39.0
        else -> 0.0
    }

    // Uccha Bala: 0 at debilitation point, 60 at exaltation point, linear w.r.t. angular distance
    private fun ucchaBala(p: PlanetPosition): Double {
        val exaltDeg = exaltationPointDegrees(p.planet) ?: return 0.0
        val debilDeg = normalize(exaltDeg + 180.0)
        val diff = angularDistance(p.degree, debilDeg)
        return (diff / 180.0) * 60.0
    }

    private fun digBala(p: PlanetPosition): Double {
        val peakHouse = when (p.planet) {
            Planet.SUN, Planet.MARS -> 10
            Planet.SATURN -> 7
            Planet.JUPITER, Planet.MERCURY -> 1
            Planet.MOON, Planet.VENUS -> 4
            else -> 1
        }
        val dh = min((p.house - peakHouse).modWrap(), (peakHouse - p.house).modWrap())
        val deg = dh * 30.0
        return (1.0 - (deg / 180.0)) * 60.0
    }

    private fun cheshtaBala(p: PlanetPosition): Double {
        return when (p.planet) {
            Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN -> if (p.isRetrograde) 48.0 else 12.0
            Planet.SUN, Planet.MOON -> 30.0
            else -> 0.0
        }
    }

    // ---------------- Sthana Bala subcomponents ----------------

    private fun saptavargajaBala(chart: ChartResult, p: PlanetPosition): Double {
        // Vargas considered: D1, D2, D3, D7, D9, D12, D30
        val vargas = listOf(Varga.D1, Varga.D2, Varga.D3, Varga.D7, Varga.D9, Varga.D12, Varga.D30)
        // BPHS-style dignity weights per varga (shastiamsa units before scaling):
        // Exalted=20, Moolatrikona=15, Own=10, Friend=7.5, Neutral=5, Enemy=2.5, Debilitated=0
        fun dignityWeight(planet: Planet, sign: ZodiacSign): Double {
            val exalt = exaltationSignOf(planet)
            val deb = exalt?.let { oppositeSign(it) }
            val mool = moolatrikonaSignOf(planet)
            return when {
                exalt == sign -> 20.0
                deb == sign -> 0.0
                mool == sign -> 15.0
                signLordOf(sign) == planet -> 10.0
                areFriends(planet, signLordOf(sign)) -> 7.5
                areEnemies(planet, signLordOf(sign)) -> 2.5
                else -> 5.0
            }
        }
        val raw = vargas.sumOf { v ->
            val vSign = VargaCalculator.mapLongitudeToVargaSign(p.degree, v)
            dignityWeight(p.planet, vSign)
        }
        // Max raw = 20 * 7 = 140. Normalize to a 45 shastiamsa cap per classics.
        return raw * (45.0 / 140.0)
    }

    private fun ojayugmariamsaBala(p: PlanetPosition): Double {
        val male = setOf(Planet.SUN, Planet.MARS, Planet.JUPITER, Planet.SATURN)
        val female = setOf(Planet.MOON, Planet.VENUS)
        val isOddSign = p.sign in setOf(
            ZodiacSign.ARIES, ZodiacSign.GEMINI, ZodiacSign.LEO, ZodiacSign.LIBRA, ZodiacSign.SAGITTARIUS, ZodiacSign.AQUARIUS
        )
        val match = when (p.planet) {
            in male -> isOddSign
            in female -> !isOddSign
            else -> isDual(p.sign) // Mercury: count when dual signs; simple convention
        }
        return if (match) 15.0 else 0.0
    }

    private fun kendradiBala(p: PlanetPosition): Double {
        return when (p.house) {
            1, 4, 7, 10 -> 60.0 // Kendra
            2, 5, 8, 11 -> 30.0 // Panaphara
            else -> 15.0       // Apoklima (3,6,9,12)
        }
    }

    private fun drekkanaBala(p: PlanetPosition): Double {
        val inSign = p.degree % 30.0
        val drekkanaIndex = (inSign / 10.0).toInt() // 0,1,2
        val male = setOf(Planet.SUN, Planet.MARS, Planet.JUPITER, Planet.SATURN)
        val female = setOf(Planet.MOON, Planet.VENUS)
        val match = when (p.planet) {
            in male -> drekkanaIndex != 1 // favor 1st or 3rd
            in female -> drekkanaIndex == 1 // favor 2nd
            else -> drekkanaIndex == 0 // Mercury: favor 1st
        }
        return if (match) 15.0 else 0.0
    }

    // ---------------- Kala Bala (simplified, normalized to 60) ----------------
    private fun kalaBala(chart: ChartResult, p: PlanetPosition, birthDateTime: ZonedDateTime?): Double {
        // Natonnata: day/night strength (max 12)
        val sun = chart.planets.first { it.planet == Planet.SUN }
        val isDay = sun.house in 7..12
        val natonnata = when (p.planet) {
            Planet.MERCURY -> 12.0
            Planet.SUN, Planet.JUPITER, Planet.VENUS -> if (isDay) 12.0 else 0.0
            Planet.MOON, Planet.MARS, Planet.SATURN -> if (!isDay) 12.0 else 0.0
            else -> 0.0
        }
        // Paksha Bala: waxing benefits benefics, waning benefits malefics (max 36)
        val moon = chart.planets.first { it.planet == Planet.MOON }
        val elong = angularDistance(moon.degree, sun.degree) // 0..180
        val waxingFrac = elong / 180.0 // 0..1 (0 new, 1 full)
        val isBenefic = isBeneficPlanet(p.planet, chart)
        val paksha = when (p.planet) {
            Planet.MERCURY -> 18.0 // neutral baseline
            else -> if (isBenefic) 36.0 * waxingFrac else 36.0 * (1.0 - waxingFrac)
        }
        // Dina Bala: day lord gets 12 (requires birth time). If unknown, give 0 to avoid guessing.
        val dina = birthDateTime?.let { zdt ->
            val dayLord = weekdayLord(zdt)
            if (p.planet == dayLord) 12.0 else 0.0
        } ?: 0.0
        return (natonnata + paksha + dina).coerceIn(0.0, 60.0)
    }

    private fun weekdayLord(zdt: ZonedDateTime): Planet = when (zdt.dayOfWeek) {
        java.time.DayOfWeek.SUNDAY -> Planet.SUN
        java.time.DayOfWeek.MONDAY -> Planet.MOON
        java.time.DayOfWeek.TUESDAY -> Planet.MARS
        java.time.DayOfWeek.WEDNESDAY -> Planet.MERCURY
        java.time.DayOfWeek.THURSDAY -> Planet.JUPITER
        java.time.DayOfWeek.FRIDAY -> Planet.VENUS
        java.time.DayOfWeek.SATURDAY -> Planet.SATURN
    }

    // ---------------- Drik Bala (benefic/malefic aspects, normalized to 0..60) ----------------
    private fun drikBala(chart: ChartResult, target: PlanetPosition): Double {
        val others = chart.planets.filter { it.planet != target.planet && it.planet in setOf(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN
        ) }
        var raw = 0.0
        for (q in others) {
            val diffH = ((target.house - q.house + 12) % 12) + 1 // 1..12
            val aspects = mutableSetOf(7)
            aspects.add(7)
            when (q.planet) {
                Planet.MARS -> { aspects.add(4); aspects.add(8) }
                Planet.JUPITER -> { aspects.add(5); aspects.add(9) }
                Planet.SATURN -> { aspects.add(3); aspects.add(10) }
                else -> {}
            }
            if (diffH in aspects) {
                raw += if (isBeneficPlanet(q.planet, chart)) 15.0 else -15.0
            }
        }
        // Map raw from [-60, 60] to [0, 60]
        raw = raw.coerceIn(-60.0, 60.0)
        return (raw + 60.0) / 2.0
    }

    private fun isBeneficPlanet(planet: Planet, chart: ChartResult): Boolean {
        return when (planet) {
            Planet.JUPITER, Planet.VENUS -> true
            Planet.MERCURY -> true // simplified
            Planet.MOON -> {
                val sun = chart.planets.first { it.planet == Planet.SUN }
                val moon = chart.planets.first { it.planet == Planet.MOON }
                val elong = angularDistance(moon.degree, sun.degree)
                elong <= 180.0 // waxing
            }
            else -> false
        }
    }

    private fun Int.modWrap(): Int {
        var x = this % 12
        if (x < 0) x += 12
        return x
    }

    private fun angularDistance(a: Double, b: Double): Double {
        val diff = abs(normalize(a - b))
        return if (diff > 180.0) 360.0 - diff else diff
    }

    private fun normalize(v: Double): Double { var x = v % 360.0; if (x < 0) x += 360.0; return x }

    private fun exaltationPointDegrees(planet: Planet): Double? = when (planet) {
        Planet.SUN -> signDeg(ZodiacSign.ARIES, 10.0)
        Planet.MOON -> signDeg(ZodiacSign.TAURUS, 3.0)
        Planet.MARS -> signDeg(ZodiacSign.CAPRICORN, 28.0)
        Planet.MERCURY -> signDeg(ZodiacSign.VIRGO, 15.0)
        Planet.JUPITER -> signDeg(ZodiacSign.CANCER, 5.0)
        Planet.VENUS -> signDeg(ZodiacSign.PISCES, 27.0)
        Planet.SATURN -> signDeg(ZodiacSign.LIBRA, 20.0)
        else -> null
    }

    private fun exaltationSignOf(planet: Planet): ZodiacSign? = when (planet) {
        Planet.SUN -> ZodiacSign.ARIES
        Planet.MOON -> ZodiacSign.TAURUS
        Planet.MARS -> ZodiacSign.CAPRICORN
        Planet.MERCURY -> ZodiacSign.VIRGO
        Planet.JUPITER -> ZodiacSign.CANCER
        Planet.VENUS -> ZodiacSign.PISCES
        Planet.SATURN -> ZodiacSign.LIBRA
        else -> null
    }

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

    private fun isDual(sign: ZodiacSign) = sign in setOf(
        ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES
    )

    private fun signDeg(sign: ZodiacSign, deg: Double): Double = sign.ordinal * 30.0 + deg
    private fun oppositeSign(sign: ZodiacSign): ZodiacSign = ZodiacSign.entries[(sign.ordinal + 6) % 12]

    private fun moolatrikonaSignOf(planet: Planet): ZodiacSign? = when (planet) {
        // Using sign-level moolatrikona attribution (degree-specific variants can be added later)
        Planet.SUN -> ZodiacSign.LEO
        Planet.MOON -> ZodiacSign.TAURUS
        Planet.MARS -> ZodiacSign.ARIES
        Planet.MERCURY -> ZodiacSign.VIRGO
        Planet.JUPITER -> ZodiacSign.SAGITTARIUS
        Planet.VENUS -> ZodiacSign.LIBRA
        Planet.SATURN -> ZodiacSign.AQUARIUS
        else -> null
    }

    private val permanentFriends: Map<Planet, Set<Planet>> = mapOf(
        Planet.SUN to setOf(Planet.MOON, Planet.MARS, Planet.JUPITER),
        Planet.MOON to setOf(Planet.SUN, Planet.MERCURY),
        Planet.MARS to setOf(Planet.SUN, Planet.MOON, Planet.JUPITER),
        Planet.MERCURY to setOf(Planet.SUN, Planet.VENUS),
        Planet.JUPITER to setOf(Planet.SUN, Planet.MOON, Planet.MARS),
        Planet.VENUS to setOf(Planet.MERCURY, Planet.SATURN),
        Planet.SATURN to setOf(Planet.MERCURY, Planet.VENUS)
    )
    private val permanentEnemies: Map<Planet, Set<Planet>> = mapOf(
        Planet.SUN to setOf(Planet.SATURN, Planet.VENUS),
        Planet.MOON to emptySet(),
        Planet.MARS to setOf(Planet.MERCURY),
        Planet.MERCURY to setOf(Planet.MOON),
        Planet.JUPITER to setOf(Planet.VENUS, Planet.MERCURY),
        Planet.VENUS to setOf(Planet.SUN, Planet.MOON),
        Planet.SATURN to setOf(Planet.SUN, Planet.MOON, Planet.MARS)
    )
    private fun areFriends(p: Planet, q: Planet): Boolean = permanentFriends[p]?.contains(q) == true
    private fun areEnemies(p: Planet, q: Planet): Boolean = permanentEnemies[p]?.contains(q) == true
}
