package com.aakash.astro.astrology

import kotlin.math.floor

data class SixtyFourTwentyTwoResult(
    val fromLagnaDrekkanaSign: ZodiacSign,
    val fromLagnaDrekkanaNo: Int,
    val fromLagnaDrekkanaLord: Planet,
    val fromLagnaNavamsaSign: ZodiacSign,
    val fromLagnaNavamsaNo: Int,
    val fromLagnaNavamsaLord: Planet,

    val fromMoonDrekkanaSign: ZodiacSign,
    val fromMoonDrekkanaNo: Int,
    val fromMoonDrekkanaLord: Planet,
    val fromMoonNavamsaSign: ZodiacSign,
    val fromMoonNavamsaNo: Int,
    val fromMoonNavamsaLord: Planet,
)

object SixtyFourTwentyTwoCalc {
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

    private fun inSignDegree(absDegree: Double): Double {
        val norm = ((absDegree % 360.0) + 360.0) % 360.0
        return ((norm % 30.0) + 30.0) % 30.0
    }

    private fun drekkanaNo(inSign: Double): Int = floor(inSign / 10.0).toInt() + 1 // 1..3
    private fun navamsaNo(inSign: Double): Int = floor(inSign / (30.0 / 9.0)).toInt() + 1 // 1..9

    private fun eighthFrom(sign: ZodiacSign): ZodiacSign = ZodiacSign.entries[(sign.ordinal + 7) % 12]

    private fun nthFrom(sign: ZodiacSign, steps: Int): ZodiacSign = ZodiacSign.entries[(sign.ordinal + steps) % 12]

    private fun drekkanaLordFor(sign: ZodiacSign, drekkanaNo: Int): Planet {
        val targetSign = when (drekkanaNo) {
            1 -> sign
            2 -> nthFrom(sign, 4) // 5th from sign
            else -> nthFrom(sign, 8) // 9th from sign
        }
        return signLordOf(targetSign)
    }

    private fun navamsaSignFor(sign: ZodiacSign, navNo: Int): ZodiacSign {
        val startOffset = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN -> 0 // movable
            ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS -> 8 // fixed → 9th
            ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES -> 4 // dual → 5th
        }
        return ZodiacSign.entries[(sign.ordinal + startOffset + (navNo - 1)) % 12]
    }

    fun compute(chart: ChartResult): SixtyFourTwentyTwoResult {
        val ascSign = chart.ascendantSign
        val ascInSign = inSignDegree(chart.ascendantDegree)
        val ascDrekkanaNo = drekkanaNo(ascInSign)
        val ascNavNo = navamsaNo(ascInSign)
        val ascEighth = eighthFrom(ascSign)
        val ascDrekkanaLord = drekkanaLordFor(ascEighth, ascDrekkanaNo)
        val ascNavSign = navamsaSignFor(ascEighth, ascNavNo)
        val ascNavLord = signLordOf(ascNavSign)

        val moonPos = chart.planets.find { it.planet == Planet.MOON }
        val moonSign = moonPos?.sign ?: ascSign
        val moonInSign = inSignDegree(moonPos?.degree ?: chart.ascendantDegree)
        val moonDrekkanaNo = drekkanaNo(moonInSign)
        val moonNavNo = navamsaNo(moonInSign)
        val moonEighth = eighthFrom(moonSign)
        val moonDrekkanaLord = drekkanaLordFor(moonEighth, moonDrekkanaNo)
        val moonNavSign = navamsaSignFor(moonEighth, moonNavNo)
        val moonNavLord = signLordOf(moonNavSign)

        return SixtyFourTwentyTwoResult(
            fromLagnaDrekkanaSign = ascEighth,
            fromLagnaDrekkanaNo = ascDrekkanaNo,
            fromLagnaDrekkanaLord = ascDrekkanaLord,
            fromLagnaNavamsaSign = ascNavSign,
            fromLagnaNavamsaNo = ascNavNo,
            fromLagnaNavamsaLord = ascNavLord,

            fromMoonDrekkanaSign = moonEighth,
            fromMoonDrekkanaNo = moonDrekkanaNo,
            fromMoonDrekkanaLord = moonDrekkanaLord,
            fromMoonNavamsaSign = moonNavSign,
            fromMoonNavamsaNo = moonNavNo,
            fromMoonNavamsaLord = moonNavLord,
        )
    }
}

