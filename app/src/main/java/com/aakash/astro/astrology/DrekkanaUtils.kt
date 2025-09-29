package com.aakash.astro.astrology

object DrekkanaUtils {
    enum class Modality { MOVABLE, FIXED, DUAL }

    private fun modality(sign: ZodiacSign): Modality = when (sign) {
        ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN -> Modality.MOVABLE
        ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS -> Modality.FIXED
        ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES -> Modality.DUAL
    }

    private fun inSignDegree(absDegree: Double): Double {
        val norm = ((absDegree % 360.0) + 360.0) % 360.0
        return ((norm % 30.0) + 30.0) % 30.0
    }

    fun isUttamaDrekkana(sign: ZodiacSign, absDegree: Double): Boolean {
        val d = inSignDegree(absDegree)
        return when (modality(sign)) {
            Modality.MOVABLE -> d >= 0.0 && d < 10.0
            Modality.FIXED -> d >= 10.0 && d < 20.0
            Modality.DUAL -> d >= 20.0 && d < 30.0
        }
    }
}

