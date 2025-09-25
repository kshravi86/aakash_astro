package com.aakash.astro.astrology

import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val J2000 = 2451545.0

data class BirthDetails(
    val name: String?,
    val dateTime: ZonedDateTime,
    val latitude: Double,
    val longitude: Double
)

data class PlanetPosition(
    val planet: Planet,
    val degree: Double,
    val sign: ZodiacSign,
    val house: Int,
    val isRetrograde: Boolean = false
) {
    val name: String = planet.displayName
}

data class HouseInfo(
    val number: Int,
    val startDegree: Double,
    val sign: ZodiacSign
)

data class ChartResult(
    val ascendantDegree: Double,
    val ascendantSign: ZodiacSign,
    val houses: List<HouseInfo>,
    val planets: List<PlanetPosition>
)

enum class Planet(val displayName: String) {
    SUN("Sun"),
    MOON("Moon"),
    MERCURY("Mercury"),
    VENUS("Venus"),
    MARS("Mars"),
    JUPITER("Jupiter"),
    SATURN("Saturn"),
    RAHU("Rahu"),
    KETU("Ketu");
}

enum class ZodiacSign(val displayName: String, val symbol: String) {
    ARIES("Aries", "\u2648"),
    TAURUS("Taurus", "\u2649"),
    GEMINI("Gemini", "\u264A"),
    CANCER("Cancer", "\u264B"),
    LEO("Leo", "\u264C"),
    VIRGO("Virgo", "\u264D"),
    LIBRA("Libra", "\u264E"),
    SCORPIO("Scorpio", "\u264F"),
    SAGITTARIUS("Sagittarius", "\u2650"),
    CAPRICORN("Capricorn", "\u2651"),
    AQUARIUS("Aquarius", "\u2652"),
    PISCES("Pisces", "\u2653");

    companion object {
        fun fromDegree(degree: Double): ZodiacSign {
            val normalized = normalizeDegree(degree)
            val index = (normalized / 30.0).toInt().coerceIn(0, 11)
            return entries[index]
        }
    }
}

class AstrologyCalculator {

    fun generateChart(details: BirthDetails): ChartResult {
        val utcDateTime = details.dateTime.withZoneSameInstant(ZoneOffset.UTC)
        val julianDay = julianDay(utcDateTime)
        val ayanamsa = lahiriAyanamsa(julianDay)

        val lst = localSiderealTime(julianDay, details.longitude)
        val obliquity = meanObliquity(julianDay)
        val ascendantTropical = ascendant(lst, obliquity, details.latitude)
        val ascendantSidereal = normalizeDegree(ascendantTropical - ayanamsa)
        val ascSign = ZodiacSign.fromDegree(ascendantSidereal)

        val houses = (0 until 12).map { index ->
            val startDegree = normalizeDegree(ascendantSidereal + index * 30.0)
            HouseInfo(index + 1, startDegree, ZodiacSign.fromDegree(startDegree))
        }

        val planetaryPositions = computePlanets(julianDay, ayanamsa, ascendantSidereal)

        return ChartResult(
            ascendantDegree = ascendantSidereal,
            ascendantSign = ascSign,
            houses = houses,
            planets = planetaryPositions
        )
    }

    private fun computePlanets(julianDay: Double, ayanamsa: Double, ascendant: Double): List<PlanetPosition> {
        // Days since J2000.0 (JD 2451545.0)
        val d = julianDay - J2000
        val earth = computeHeliocentric(EARTH_ELEMENTS, d)
        val nodeLongitude = lunarNodeLongitude(julianDay)

        return Planet.entries.map { planet ->
            val tropicalLongitude = when (planet) {
                Planet.SUN -> normalizeDegree(earth.trueLongitude + 180.0)
                Planet.MOON -> moonLongitude(julianDay)
                Planet.MERCURY -> geocentricLongitude(MERCURY_ELEMENTS, earth, d)
                Planet.VENUS -> geocentricLongitude(VENUS_ELEMENTS, earth, d)
                Planet.MARS -> geocentricLongitude(MARS_ELEMENTS, earth, d)
                Planet.JUPITER -> geocentricLongitude(JUPITER_ELEMENTS, earth, d)
                Planet.SATURN -> geocentricLongitude(SATURN_ELEMENTS, earth, d)
                Planet.RAHU -> nodeLongitude
                Planet.KETU -> normalizeDegree(nodeLongitude + 180.0)
            }
            val siderealLongitude = normalizeDegree(tropicalLongitude - ayanamsa)
            val house = ((normalizeDegree(siderealLongitude - ascendant) / 30.0).toInt() + 1).let {
                if (it > 12) it - 12 else it
            }
            PlanetPosition(
                planet = planet,
                degree = siderealLongitude,
                sign = ZodiacSign.fromDegree(siderealLongitude),
                house = house,
                isRetrograde = false
            )
        }
    }

    private fun julianDay(dateTime: ZonedDateTime): Double {
        var year = dateTime.year
        var month = dateTime.monthValue
        val dayFraction = (dateTime.hour + dateTime.minute / 60.0 + dateTime.second / 3600.0) / 24.0
        val day = dateTime.dayOfMonth + dayFraction

        if (month <= 2) {
            year -= 1
            month += 12
        }

        val a = year / 100
        val b = 2 - a + (a / 4)

        return (365.25 * (year + 4716)).toInt() +
            (30.6001 * (month + 1)).toInt() +
            day + b - 1524.5
    }

    private fun localSiderealTime(jd: Double, longitude: Double): Double {
        val t = (jd - J2000) / 36525.0
        val theta = 280.46061837 + 360.98564736629 * (jd - J2000) + 0.000387933 * t * t - t * t * t / 38710000.0
        return normalizeDegree(theta + longitude)
    }

    private fun meanObliquity(jd: Double): Double {
        val t = (jd - J2000) / 36525.0
        val seconds = 21.448 - t * (46.8150 + t * (0.00059 - t * 0.001813))
        return 23.0 + (26.0 + seconds / 60.0) / 60.0
    }

    private fun ascendant(lstDegrees: Double, obliquityDegrees: Double, latitudeDegrees: Double): Double {
        val lst = Math.toRadians(lstDegrees)
        val obliquity = Math.toRadians(obliquityDegrees)
        val latitude = Math.toRadians(latitudeDegrees)

        val numerator = -cos(lst)
        val denominator = sin(lst) * cos(obliquity) + kotlin.math.tan(latitude) * sin(obliquity)
        var asc = Math.toDegrees(atan2(numerator, denominator))
        if (asc < 0) asc += 360.0
        return asc
    }

    private fun lahiriAyanamsa(jd: Double): Double {
        val t = (jd - 2415020.0) / 36525.0
        return 22.460148 + 1.396042 * t + 0.000308 * t * t
    }

    private fun moonLongitude(jd: Double): Double {
        val t = (jd - J2000) / 36525.0
        val lPrime = Math.toRadians(normalizeDegree(218.3164477 + 481267.88123421 * t - 0.0015786 * t * t + t * t * t / 538841.0 - t * t * t * t / 65194000.0))
        val d = Math.toRadians(normalizeDegree(297.8501921 + 445267.1114034 * t - 0.0018819 * t * t + t * t * t / 545868.0 - t * t * t * t / 113065000.0))
        val m = Math.toRadians(normalizeDegree(357.5291092 + 35999.0502909 * t - 0.0001536 * t * t + t * t * t / 24490000.0))
        val mPrime = Math.toRadians(normalizeDegree(134.9633964 + 477198.8675055 * t + 0.0087414 * t * t + t * t * t / 69699.0 - t * t * t * t / 14712000.0))
        val f = Math.toRadians(normalizeDegree(93.2720950 + 483202.0175233 * t - 0.0036539 * t * t - t * t * t / 3526000.0 + t * t * t * t / 863310000.0))

        val lon = lPrime +
            Math.toRadians(6.289 * sin(mPrime)) +
            Math.toRadians(1.274 * sin(2 * d - mPrime)) +
            Math.toRadians(0.658 * sin(2 * d)) +
            Math.toRadians(0.214 * sin(2 * mPrime)) +
            Math.toRadians(0.11 * sin(d))

        return normalizeDegree(Math.toDegrees(lon))
    }

    private fun lunarNodeLongitude(jd: Double): Double {
        val t = (jd - J2000) / 36525.0
        val omega = 125.0445550 - 1934.1361849 * t + 0.0020762 * t * t + t * t * t / 467410.0 - t * t * t * t / 60616000.0
        return normalizeDegree(omega)
    }

    private fun geocentricLongitude(elements: OrbitalElements, earth: HeliocentricPosition, d: Double): Double {
        val planet = computeHeliocentric(elements, d)
        val xg = planet.x - earth.x
        val yg = planet.y - earth.y
        val lon = atan2(yg, xg)
        return normalizeDegree(Math.toDegrees(lon))
    }

    private fun computeHeliocentric(elements: OrbitalElements, d: Double): HeliocentricPosition {
        val n = Math.toRadians(normalizeDegree(elements.N0 + elements.NDot * d))
        val i = Math.toRadians(elements.i0 + elements.iDot * d)
        val w = Math.toRadians(normalizeDegree(elements.w0 + elements.wDot * d))
        val e = elements.e0 + elements.eDot * d
        val a = elements.a
        val mDegrees = normalizeDegree(elements.m0 + elements.mDot * d)
        val m = Math.toRadians(mDegrees)

        val eAnomaly = solveKepler(m, e)
        val xv = a * (cos(eAnomaly) - e)
        val yv = a * (sqrt(1.0 - e * e) * sin(eAnomaly))

        val v = atan2(yv, xv)
        val r = sqrt(xv * xv + yv * yv)

        val cosN = cos(n)
        val sinN = sin(n)
        val cosVw = cos(v + w)
        val sinVw = sin(v + w)
        val cosI = cos(i)
        val sinI = sin(i)

        val xh = r * (cosN * cosVw - sinN * sinVw * cosI)
        val yh = r * (sinN * cosVw + cosN * sinVw * cosI)
        val zh = r * (sinVw * sinI)
        val lon = atan2(yh, xh)

        return HeliocentricPosition(xh, yh, zh, normalizeDegree(Math.toDegrees(lon)), r)
    }

    private fun solveKepler(m: Double, e: Double): Double {
        var eAnomaly = m
        var delta: Double
        do {
            delta = (eAnomaly - e * sin(eAnomaly) - m) / (1 - e * cos(eAnomaly))
            eAnomaly -= delta
        } while (abs(delta) > 1e-6)
        return eAnomaly
    }

    private data class OrbitalElements(
        val N0: Double,
        val NDot: Double,
        val i0: Double,
        val iDot: Double,
        val w0: Double,
        val wDot: Double,
        val a: Double,
        val e0: Double,
        val eDot: Double,
        val m0: Double,
        val mDot: Double
    )

    private data class HeliocentricPosition(
        val x: Double,
        val y: Double,
        val z: Double,
        val trueLongitude: Double,
        val radius: Double
    )

    companion object {
        private fun normalizeDegree(value: Double): Double {
            var result = value % 360.0
            if (result < 0) {
                result += 360.0
            }
            return result
        }

        private val EARTH_ELEMENTS = OrbitalElements(
            N0 = 0.0,
            NDot = 0.0,
            i0 = 0.0,
            iDot = 0.0,
            w0 = 282.9404,
            wDot = 4.70935E-5,
            a = 1.000000,
            e0 = 0.016709,
            eDot = -1.151E-9,
            m0 = 356.0470,
            mDot = 0.9856002585
        )

        private val MERCURY_ELEMENTS = OrbitalElements(
            N0 = 48.3313,
            NDot = 3.24587E-5,
            i0 = 7.0047,
            iDot = 5.00E-8,
            w0 = 29.1241,
            wDot = 1.01444E-5,
            a = 0.387098,
            e0 = 0.205635,
            eDot = 5.59E-10,
            m0 = 168.6562,
            mDot = 4.0923344368
        )

        private val VENUS_ELEMENTS = OrbitalElements(
            N0 = 76.6799,
            NDot = 2.46590E-5,
            i0 = 3.3946,
            iDot = 2.75E-8,
            w0 = 54.8910,
            wDot = 1.38374E-5,
            a = 0.723330,
            e0 = 0.006773,
            eDot = -1.302E-9,
            m0 = 48.0052,
            mDot = 1.6021302244
        )

        private val MARS_ELEMENTS = OrbitalElements(
            N0 = 49.5574,
            NDot = 2.11081E-5,
            i0 = 1.8497,
            iDot = -1.78E-8,
            w0 = 286.5016,
            wDot = 2.92961E-5,
            a = 1.523688,
            e0 = 0.093405,
            eDot = 2.516E-9,
            m0 = 18.6021,
            mDot = 0.5240207766
        )

        private val JUPITER_ELEMENTS = OrbitalElements(
            N0 = 100.4542,
            NDot = 2.76854E-5,
            i0 = 1.3030,
            iDot = -1.557E-7,
            w0 = 273.8777,
            wDot = 1.64505E-5,
            a = 5.20256,
            e0 = 0.048498,
            eDot = 4.469E-9,
            m0 = 19.8950,
            mDot = 0.0830853001
        )

        private val SATURN_ELEMENTS = OrbitalElements(
            N0 = 113.6634,
            NDot = 2.38980E-5,
            i0 = 2.4886,
            iDot = -1.081E-7,
            w0 = 339.3939,
            wDot = 2.97661E-5,
            a = 9.55475,
            e0 = 0.055546,
            eDot = -9.499E-9,
            m0 = 316.9670,
            mDot = 0.0334442282
        )
    }
}

private fun normalizeDegree(value: Double): Double {
    var result = value % 360.0
    if (result < 0) {
        result += 360.0
    }
    return result
}
