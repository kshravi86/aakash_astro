package com.aakash.astro.astrology

object Vargottama {
    private fun deg(d: Int, m: Int) = d + m / 60.0

    fun isVargottama(sign: ZodiacSign, degreeWithinSign: Double): Boolean {
        val d = degreeWithinSign.coerceIn(0.0, 30.0)
        return when (sign) {
            // Movable: Aries, Cancer, Libra, Capricorn => 0°00'–3°20'
            ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN -> d >= 0.0 && d <= deg(3, 20)
            // Fixed: Taurus, Leo, Scorpio, Aquarius => 13°20'–16°40'
            ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS -> d >= deg(13, 20) && d <= deg(16, 40)
            // Dual: Gemini, Virgo, Sagittarius, Pisces => 26°40'–30°00'
            ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES -> d >= deg(26, 40) && d <= 30.0
        }
    }
}

