package com.aakash.astro.astrology

import com.aakash.astro.util.AppLog
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.floor

class AccurateCalculator {
    @Volatile private var ephePath: String? = null

    fun setEphePath(path: String) {
        ephePath = if (path.endsWith("/")) path else "$path/"
        AppLog.d("Ephemeris path configured.")
    }

    fun generateChart(details: BirthDetails): ChartResult? = try {
        val sweConst = Class.forName("swisseph.SweConst")
        val swissEphClass = Class.forName("swisseph.SwissEph")

        val SE_SIDM_LAHIRI = sweConst.getField("SE_SIDM_LAHIRI").getInt(null)
        val SE_ASC = sweConst.getField("SE_ASC").getInt(null)
        val SE_TRUE_NODE = sweConst.getField("SE_TRUE_NODE").getInt(null)
        val SE_SUN = sweConst.getField("SE_SUN").getInt(null)
        val SE_MOON = sweConst.getField("SE_MOON").getInt(null)
        val SE_MERCURY = sweConst.getField("SE_MERCURY").getInt(null)
        val SE_VENUS = sweConst.getField("SE_VENUS").getInt(null)
        val SE_MARS = sweConst.getField("SE_MARS").getInt(null)
        val SE_JUPITER = sweConst.getField("SE_JUPITER").getInt(null)
        val SE_SATURN = sweConst.getField("SE_SATURN").getInt(null)
        val SEFLG_SWIEPH = sweConst.getField("SEFLG_SWIEPH").getInt(null)
        val SEFLG_SPEED = sweConst.getField("SEFLG_SPEED").getInt(null)
        val SEFLG_SIDEREAL = sweConst.getField("SEFLG_SIDEREAL").getInt(null)
        // Some builds define calendar constants only in SweDate, not in SweConst.

        val swe = swissEphClass.getConstructor().newInstance()

        // Optional: set ephemeris path if provided
        ephePath?.let {
            try {
                val setPath = swissEphClass.getMethod("swe_set_ephe_path", String::class.java)
                setPath.invoke(swe, it)
            } catch (_: Throwable) {
            }
        }

        // Set sidereal mode and topo
        val setSid = swissEphClass.getMethod("swe_set_sid_mode", Integer.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE)
        setSid.invoke(swe, SE_SIDM_LAHIRI, 0.0, 0.0)
        val setTopo = swissEphClass.getMethod("swe_set_topo", java.lang.Double.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE)
        setTopo.invoke(swe, details.longitude, details.latitude, 0.0)

        // Julian day UT
        val utc = details.dateTime.withZoneSameInstant(ZoneOffset.UTC)
        val hour = utc.hour + (utc.minute / 60.0) + (utc.second / 3600.0)
        // Use SweDate.getJulDay(...) for broad compatibility
        val sweDateClass = Class.forName("swisseph.SweDate")
        val getJulDay = sweDateClass.getMethod(
            "getJulDay",
            Integer.TYPE, Integer.TYPE, Integer.TYPE, java.lang.Double.TYPE, java.lang.Boolean.TYPE
        )
        val jdUt = getJulDay.invoke(null, utc.year, utc.monthValue, utc.dayOfMonth, hour, true) as Double

        // Ascendant via houses (Placidus code 'P')
        // Some SwissEph builds expose swe_houses_ex, others expose swe_houses with the same signature.
        val sweHousesEx = try {
            swissEphClass.getMethod(
                "swe_houses_ex",
                java.lang.Double.TYPE, Integer.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE, Integer.TYPE,
                DoubleArray::class.java, DoubleArray::class.java
            )
        } catch (_: NoSuchMethodException) {
            swissEphClass.getMethod(
                "swe_houses",
                java.lang.Double.TYPE, Integer.TYPE, java.lang.Double.TYPE, java.lang.Double.TYPE, Integer.TYPE,
                DoubleArray::class.java, DoubleArray::class.java
            )
        }
        val cusps = DoubleArray(13)
        val ascmc = DoubleArray(10)
        sweHousesEx.invoke(swe, jdUt, SEFLG_SIDEREAL, details.latitude, details.longitude, 'P'.code, cusps, ascmc)
        val asc = normalize(ascmc[SE_ASC])
        val ascSign = ZodiacSign.fromDegree(asc)

        // Houses as whole signs starting from ascendant sign
        val houses = (0 until 12).map { i ->
            val signIndex = (ascSign.ordinal + i) % 12
            val deg = signIndex * 30.0
            HouseInfo(number = i + 1, startDegree = deg, sign = ZodiacSign.entries[signIndex])
        }

        // Planets sidereal longitudes
        val sweCalcUt = swissEphClass.getMethod(
            "swe_calc_ut",
            java.lang.Double.TYPE, Integer.TYPE, Integer.TYPE, DoubleArray::class.java, StringBuffer::class.java
        )
        val iflag = SEFLG_SWIEPH or SEFLG_SPEED or SEFLG_SIDEREAL
        val planetMap = mapOf(
            Planet.SUN to SE_SUN,
            Planet.MOON to SE_MOON,
            Planet.MERCURY to SE_MERCURY,
            Planet.VENUS to SE_VENUS,
            Planet.MARS to SE_MARS,
            Planet.JUPITER to SE_JUPITER,
            Planet.SATURN to SE_SATURN,
            Planet.RAHU to SE_TRUE_NODE,
            Planet.KETU to SE_TRUE_NODE,
        )
        val planets = mutableListOf<PlanetPosition>()
        val xx = DoubleArray(6)
        val serr = StringBuffer()
        for ((planet, ipl) in planetMap) {
            sweCalcUt.invoke(swe, jdUt, ipl, iflag, xx, serr)
            var lon = normalize(xx[0])
            val speedLon = xx[3]
            val isRetro = speedLon < 0.0
            if (planet == Planet.KETU) lon = normalize(lon + 180.0)
            val sign = ZodiacSign.fromDegree(lon)
            val house = 1 + (sign.ordinal - ascSign.ordinal + 12) % 12
            planets += PlanetPosition(planet = planet, degree = lon, sign = sign, house = house, isRetrograde = isRetro)
        }

        ChartResult(
            ascendantDegree = asc,
            ascendantSign = ascSign,
            houses = houses,
            planets = planets
        )
    } catch (t: Throwable) {
        when (t) {
            is ClassNotFoundException, is NoClassDefFoundError -> {
                AppLog.d("Swiss Ephemeris not available; returning null.")
            }
            else -> {
                AppLog.w("Swiss Ephemeris calculation failed.", t)
            }
        }
        null
    }

    private fun normalize(v: Double): Double { var x = v % 360.0; if (x < 0) x += 360.0; return x }
}
