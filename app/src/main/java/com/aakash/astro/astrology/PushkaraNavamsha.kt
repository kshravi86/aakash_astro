package com.aakash.astro.astrology

/**
 * Pushkara Navamsha detection per element-based degree bands.
 * Bands (degrees within sign):
 * Fire (Ar, Le, Sg): 20°00'–23°20' and 26°40'–30°00'
 * Earth (Ta, Vi, Cp): 06°40'–10°00' and 13°20'–16°40'
 * Air (Ge, Li, Aq): 16°40'–20°00' and 23°20'–26°40'
 * Water (Cn, Sc, Pi): 00°00'–03°20' and 06°40'–10°00'
 */
object PushkaraNavamsha {
    private data class Band(val startDeg: Double, val endDeg: Double, val label: String)

    private val fire = listOf(
        Band(20.0, deg(23, 20), "20°00'–23°20' (Libra navamsha)"),
        Band(deg(26, 40), 30.0, "26°40'–30°00' (Sagittarius navamsha)")
    )
    private val earth = listOf(
        Band(deg(6, 40), 10.0, "06°40'–10°00' (Pisces navamsha)"),
        Band(deg(13, 20), deg(16, 40), "13°20'–16°40' (Taurus navamsha)")
    )
    private val air = listOf(
        Band(deg(16, 40), 20.0, "16°40'–20°00' (Pisces navamsha)"),
        Band(deg(23, 20), deg(26, 40), "23°20'–26°40' (Taurus navamsha)")
    )
    private val water = listOf(
        Band(0.0, deg(3, 20), "00°00'–03°20' (Cancer navamsha)"),
        Band(deg(6, 40), 10.0, "06°40'–10°00' (Virgo navamsha)")
    )

    private fun deg(d: Int, m: Int): Double = d + m / 60.0

    private fun elementBands(sign: ZodiacSign): List<Band> = when (sign) {
        ZodiacSign.ARIES, ZodiacSign.LEO, ZodiacSign.SAGITTARIUS -> fire
        ZodiacSign.TAURUS, ZodiacSign.VIRGO, ZodiacSign.CAPRICORN -> earth
        ZodiacSign.GEMINI, ZodiacSign.LIBRA, ZodiacSign.AQUARIUS -> air
        ZodiacSign.CANCER, ZodiacSign.SCORPIO, ZodiacSign.PISCES -> water
    }

    /** Returns label if degree lies in a Pushkara band for the sign, else null. */
    fun bandIfPushkara(sign: ZodiacSign, degreeWithinSign: Double): String? {
        val bands = elementBands(sign)
        val d = degreeWithinSign.coerceIn(0.0, 30.0)
        return bands.firstOrNull { d >= it.startDeg - 1e-9 && d <= it.endDeg + 1e-9 }?.label
    }

    fun isPushkara(sign: ZodiacSign, degreeWithinSign: Double): Boolean = bandIfPushkara(sign, degreeWithinSign) != null
}

