package com.aakash.astro.astrology

import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.min

data class IKHEntry(
    val planet: Planet,
    val position: PlanetPosition,
    val ucchaBala: Double,
    val cheBala: Double,
    val ishta: Double,
    val kashta: Double,
    val harsha: Int,
    val harshaChecks: List<String>
)

object IshtaKashtaHarsha {
    fun compute(
        chart: ChartResult,
        details: BirthDetails,
        varshaphalaIsDay: Boolean
    ): List<IKHEntry> {
        val positions = chart.planets.filter { it.planet in setOf(
            Planet.SUN, Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN
        ) }
        val speeds = try { planetSpeeds(details) } catch (_: Throwable) { emptyMap() }

        val sunPos = positions.first { it.planet == Planet.SUN }
        val moonPos = positions.first { it.planet == Planet.MOON }
        val elong = angularDistance(moonPos.degree, sunPos.degree) // 0..180

        return positions.map { p ->
            val uccha = ucchaBala(p)
            val che = cheBala(p, speeds[p.planet] ?: 0.0, elong)
            val ishta = (uccha * che) / 60.0
            val kashta = ((60.0 - uccha) * (60.0 - che)) / 60.0
            val (harsha, checks) = harshaBala(chart, p, varshaphalaIsDay)
            IKHEntry(p.planet, p, round1(uccha), round1(che), round1(ishta), round1(kashta), harsha, checks)
        }
    }

    // Uchcha Bala = distance from debilitation to longitude, scaled to 0..60 (distance/3)
    private fun ucchaBala(p: PlanetPosition): Double {
        val exaltDeg = exaltationPoint(p.planet) ?: return 0.0
        val debilDeg = normalize(exaltDeg + 180.0)
        val diff = angularDistance(p.degree, debilDeg)
        return diff / 3.0
    }

    // Che Bala mapping per prompt:
    // - Mars..Saturn: retro=60; fast direct=45; normal=30; slow=15; stationary=15
    // - Sun: use Yana Bala (not implemented in repo) -> neutral 30.0 placeholder
    // - Moon: Paksha Bala = (elongation/180)*60
    private fun cheBala(p: PlanetPosition, speedDegPerDay: Double, sunMoonElong: Double): Double {
        return when (p.planet) {
            Planet.MOON -> 60.0 * (sunMoonElong / 180.0)
            Planet.SUN -> 30.0
            Planet.MERCURY, Planet.VENUS, Planet.MARS, Planet.JUPITER, Planet.SATURN -> {
                if (speedDegPerDay < -1e-6) 60.0
                else if (abs(speedDegPerDay) < 1e-3) 15.0
                else classifyDirectChe(p.planet, speedDegPerDay)
            }
            else -> 0.0
        }
    }

    // Classify direct speeds relative to typical mean speeds
    private fun classifyDirectChe(planet: Planet, speed: Double): Double {
        val mean = when (planet) {
            Planet.MERCURY -> 1.20
            Planet.VENUS -> 1.20
            Planet.MARS -> 0.52
            Planet.JUPITER -> 0.083
            Planet.SATURN -> 0.033
            else -> 0.5
        }
        val fast = mean * 1.25
        val slow = mean * 0.75
        return when {
            speed >= fast -> 45.0
            speed <= slow -> 15.0
            else -> 30.0
        }
    }

    // Harsha Bala (Tajika) per prompt: 4 checks (0/5 each), exclude Rahu/Ketu
    private fun harshaBala(chart: ChartResult, p: PlanetPosition, varshaphalaIsDay: Boolean): Pair<Int, List<String>> {
        var total = 0
        val notes = mutableListOf<String>()
        // 1) Favored house
        val favored = when (p.planet) {
            Planet.SUN -> 9
            Planet.MOON -> 3
            Planet.JUPITER -> 11
            Planet.VENUS -> 5
            Planet.SATURN -> 12
            Planet.MARS -> 6
            Planet.MERCURY -> 1
            else -> 0
        }
        if (favored != 0 && p.house == favored) { total += 5; notes += "Favored house" } else { notes += "Favored house: 0" }

        // 2) Exaltation / Own / Moolatrikona
        val exSign = exaltationSign(p.planet)
        val own = signLordOf(p.sign) == p.planet
        val mool = moolatrikonaSign(p.planet) == p.sign
        if (p.sign == exSign || own || mool) { total += 5; notes += "Exalt/Own/Mool: +5" } else { notes += "Exalt/Own/Mool: 0" }

        // 3) Stri/Purusha house match
        val feminineHouses = setOf(1,2,3,7,8,9)
        val masculineHouses = setOf(4,5,6,10,11,12)
        val masculinePlanets = setOf(Planet.SUN, Planet.MARS, Planet.JUPITER)
        val femininePlanets = setOf(Planet.MOON, Planet.MERCURY, Planet.VENUS, Planet.SATURN)
        val ok = when (p.planet) {
            in masculinePlanets -> p.house in masculineHouses
            in femininePlanets -> p.house in feminineHouses
            else -> false
        }
        if (ok) { total += 5; notes += "Gender match: +5" } else { notes += "Gender match: 0" }

        // 4) Day/Night bonus (Varshaphala event time)
        val bonus = when (p.planet) {
            in masculinePlanets -> if (varshaphalaIsDay) 5 else 0
            in femininePlanets -> if (!varshaphalaIsDay) 5 else 0
            else -> 0
        }
        total += bonus
        notes += if (bonus == 5) "Day/Night: +5" else "Day/Night: 0"

        return total to notes
    }

    // --- Helpers (duplicated minimal maps from ShadbalaCalculator) ---
    private fun exaltationPoint(planet: Planet): Double? = when (planet) {
        Planet.SUN -> signDeg(ZodiacSign.ARIES, 10.0)
        Planet.MOON -> signDeg(ZodiacSign.TAURUS, 3.0)
        Planet.MARS -> signDeg(ZodiacSign.CAPRICORN, 28.0)
        Planet.MERCURY -> signDeg(ZodiacSign.VIRGO, 15.0)
        Planet.JUPITER -> signDeg(ZodiacSign.CANCER, 5.0)
        Planet.VENUS -> signDeg(ZodiacSign.PISCES, 27.0)
        Planet.SATURN -> signDeg(ZodiacSign.LIBRA, 20.0)
        else -> null
    }
    private fun exaltationSign(planet: Planet): ZodiacSign? = when (planet) {
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
    private fun moolatrikonaSign(planet: Planet): ZodiacSign? = when (planet) {
        Planet.SUN -> ZodiacSign.LEO
        Planet.MOON -> ZodiacSign.TAURUS
        Planet.MARS -> ZodiacSign.ARIES
        Planet.MERCURY -> ZodiacSign.VIRGO
        Planet.JUPITER -> ZodiacSign.SAGITTARIUS
        Planet.VENUS -> ZodiacSign.LIBRA
        Planet.SATURN -> ZodiacSign.AQUARIUS
        else -> null
    }

    private fun angularDistance(a: Double, b: Double): Double {
        val d = abs(normalize(a - b))
        return if (d > 180.0) 360.0 - d else d
    }
    private fun normalize(v: Double): Double { var x = v % 360.0; if (x < 0) x += 360.0; return x }
    private fun signDeg(sign: ZodiacSign, deg: Double): Double = sign.ordinal * 30.0 + deg
    private fun round1(x: Double) = kotlin.math.round(x * 10.0) / 10.0

    // Fetch planetary speeds (deg/day) using SwissEph
    private fun planetSpeeds(details: BirthDetails): Map<Planet, Double> {
        val map = mutableMapOf<Planet, Double>()
        val sweConst = Class.forName("swisseph.SweConst")
        val swissEphClass = Class.forName("swisseph.SwissEph")
        val sweDateClass = Class.forName("swisseph.SweDate")
        val SE_SIDM_LAHIRI = sweConst.getField("SE_SIDM_LAHIRI").getInt(null)
        val SEFLG_SWIEPH = sweConst.getField("SEFLG_SWIEPH").getInt(null)
        val SEFLG_SPEED = sweConst.getField("SEFLG_SPEED").getInt(null)
        val SEFLG_SIDEREAL = sweConst.getField("SEFLG_SIDEREAL").getInt(null)
        val iplMap = mapOf(
            Planet.SUN to sweConst.getField("SE_SUN").getInt(null),
            Planet.MOON to sweConst.getField("SE_MOON").getInt(null),
            Planet.MERCURY to sweConst.getField("SE_MERCURY").getInt(null),
            Planet.VENUS to sweConst.getField("SE_VENUS").getInt(null),
            Planet.MARS to sweConst.getField("SE_MARS").getInt(null),
            Planet.JUPITER to sweConst.getField("SE_JUPITER").getInt(null),
            Planet.SATURN to sweConst.getField("SE_SATURN").getInt(null),
        )
        val swe = swissEphClass.getConstructor().newInstance()
        val setSid = swissEphClass.getMethod("swe_set_sid_mode", Integer.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE)
        setSid.invoke(swe, SE_SIDM_LAHIRI, 0.0, 0.0)
        val setTopo = swissEphClass.getMethod("swe_set_topo", java.lang.Double.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE)
        setTopo.invoke(swe, details.longitude, details.latitude, 0.0)

        val utc = details.dateTime.withZoneSameInstant(ZoneOffset.UTC)
        val hour = utc.hour + (utc.minute / 60.0) + (utc.second / 3600.0)
        val getJulDay = sweDateClass.getMethod(
            "getJulDay",
            Integer.TYPE, Integer.TYPE, Integer.TYPE, java.lang.Double.TYPE, java.lang.Boolean.TYPE
        )
        val jdUt = getJulDay.invoke(null, utc.year, utc.monthValue, utc.dayOfMonth, hour, true) as Double
        val sweCalcUt = swissEphClass.getMethod(
            "swe_calc_ut",
            java.lang.Double.TYPE, Integer.TYPE, Integer.TYPE, DoubleArray::class.java, StringBuffer::class.java
        )
        val iflag = SEFLG_SWIEPH or SEFLG_SPEED or SEFLG_SIDEREAL
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        for ((planet, ipl) in iplMap) {
            sweCalcUt.invoke(swe, jdUt, ipl, iflag, xx, serr)
            val speed = xx[3] // deg/day
            map[planet] = speed
        }
        return map
    }
}

