package com.aakash.astro.astrology

import kotlin.math.floor

enum class Varga(val code: String, val parts: Int) {
    D1("D1 Rasi", 1),
    D2("D2 Hora", 2),
    D3("D3 Drekkana", 3),
    D4("D4 Chaturthamsha", 4),
    D7("D7 Saptamsha", 7),
    D9("D9 Navamsha", 9),
    D10("D10 Dashamsha", 10),
    D12("D12 Dwadashamsha", 12),
    D16("D16 Shodasamsha", 16),
    D20("D20 Vimsamsha", 20),
    D24("D24 Siddhamsha", 24),
    D27("D27 Nakshatramsa", 27),
    D30("D30 Trimsamsha", 30),
    D40("D40 Khavedamsha", 40),
    D45("D45 Akshavedamsha", 45),
    D60("D60 Shashtyamsha", 60),
}

object VargaCalculator {

    fun description(v: Varga): String = when (v) {
        Varga.D1 -> "D1 Rasi: Identity chart; planets remain in their natal signs."
        Varga.D2 -> "D2 Hora: Odd signs → 0–15° Sun(Leo), 15–30° Moon(Cancer); Even signs → 0–15° Moon(Cancer), 15–30° Sun(Leo)."
        Varga.D3 -> "D3 Drekkana: 0–10° same sign, 10–20° 5th sign, 20–30° 9th sign from the natal sign."
        Varga.D4 -> "D4 Chaturthamsha: 4 parts of 7°30' each; progressive mapping advancing one sign per part."
        Varga.D7 -> "D7 Saptamsa: 7 parts; odd signs start from same sign, even signs from 7th; advance one sign per part."
        Varga.D9 -> "D9 Navamsha: Movable start at same sign, Fixed at 9th, Dual at 5th; 9 parts of 3°20', advancing one sign per part."
        Varga.D10 -> "D10 Dashamsha: Modality-based start (Movable 1st, Fixed 9th, Dual 5th); 10 equal parts; advance one sign per part."
        Varga.D12 -> "D12 Dwadashamsha: 12 parts of 2°30' each; progressive mapping advancing one sign per part."
        Varga.D16 -> "D16 Shodasamsha: Modality-based start; 16 equal parts (~1°52'30\u2033); advance one sign per part."
        Varga.D20 -> "D20 Vimsamsha: Modality-based start; 20 equal parts (1°30'); advance one sign per part."
        Varga.D24 -> "D24 Siddhamsha: 24 parts (1°15'); progressive mapping advancing one sign per part."
        Varga.D27 -> "D27 Nakshatramsa: 27 parts (~1°06'40\u2033); progressive mapping advancing one sign per part."
        Varga.D30 -> "D30 Trimsamsha: 30 parts (1° each); progressive mapping advancing one sign per part."
        Varga.D40 -> "D40 Khavedamsha: 40 parts (0°45' each); progressive mapping advancing one sign per part."
        Varga.D45 -> "D45 Akshavedamsha: 45 parts (0°40' each); progressive mapping advancing one sign per part."
        Varga.D60 -> "D60 Shashtyamsha: 60 parts (0°30' each); progressive mapping advancing one sign per part."
    }

    fun computeVargaChart(natal: ChartResult, varga: Varga, ascendantDegree: Double): ChartResult {
        val ascVargaSign = mapLongitudeToVargaSign(ascendantDegree, varga)
        val planetsVarga = natal.planets.map { p ->
            val vSign = mapLongitudeToVargaSign(p.degree, varga)
            val house = 1 + (vSign.ordinal - ascVargaSign.ordinal + 12) % 12
            PlanetPosition(
                planet = p.planet,
                degree = p.degree,
                sign = vSign,
                house = house,
                isRetrograde = p.isRetrograde
            )
        }
        val houses = (0 until 12).map { index ->
            val signIndex = (ascVargaSign.ordinal + index) % 12
            HouseInfo(number = index + 1, startDegree = signIndex * 30.0, sign = ZodiacSign.entries[signIndex])
        }
        return ChartResult(
            ascendantDegree = ascendantDegree,
            ascendantSign = ascVargaSign,
            houses = houses,
            planets = planetsVarga
        )
    }

    fun mapLongitudeToVargaSign(longitude: Double, varga: Varga): ZodiacSign {
        val deg = ((longitude % 360.0) + 360.0) % 360.0
        val signIndex = floor(deg / 30.0).toInt()
        val inSign = deg - signIndex * 30.0
        val sign = ZodiacSign.entries[signIndex]
        val n = varga.parts

        return when (varga) {
            Varga.D1 -> sign
            Varga.D2 -> horaSign(sign, inSign)
            Varga.D3 -> drekkanaSign(signIndex, inSign)
            Varga.D9 -> navamsaSign(signIndex, sign, inSign)
            Varga.D10 -> modalityStartSign(signIndex, sign, inSign, 10)
            Varga.D7 -> saptamsaSign(signIndex, sign, inSign)
            Varga.D12 -> generalProgressive(signIndex, inSign, 12)
            // For remaining vargas, use a reasonable approximation: progressive mapping
            Varga.D4 -> generalProgressive(signIndex, inSign, 4)
            Varga.D16 -> modalityStartSign(signIndex, sign, inSign, 16)
            Varga.D20 -> modalityStartSign(signIndex, sign, inSign, 20)
            Varga.D24 -> generalProgressive(signIndex, inSign, 24)
            Varga.D27 -> generalProgressive(signIndex, inSign, 27)
            Varga.D30 -> generalProgressive(signIndex, inSign, 30)
            Varga.D40 -> generalProgressive(signIndex, inSign, 40)
            Varga.D45 -> generalProgressive(signIndex, inSign, 45)
            Varga.D60 -> generalProgressive(signIndex, inSign, 60)
        }
    }

    private fun generalProgressive(signIndex: Int, inSign: Double, parts: Int): ZodiacSign {
        val size = 30.0 / parts
        val p = floor(inSign / size).toInt().coerceIn(0, parts - 1)
        val targetIndex = (signIndex + p) % 12
        return ZodiacSign.entries[targetIndex]
    }

    private fun modalityStartSign(signIndex: Int, sign: ZodiacSign, inSign: Double, parts: Int): ZodiacSign {
        val size = 30.0 / parts
        val p = floor(inSign / size).toInt().coerceIn(0, parts - 1)
        val startOffset = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN -> 0 // movable
            ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS -> 8 // fixed: 9th from sign
            ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES -> 4 // dual: 5th from sign
        }
        val targetIndex = (signIndex + startOffset + p) % 12
        return ZodiacSign.entries[targetIndex]
    }

    private fun navamsaSign(signIndex: Int, sign: ZodiacSign, inSign: Double): ZodiacSign {
        val size = 30.0 / 9.0
        val p = floor(inSign / size).toInt().coerceIn(0, 8)
        val startOffset = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.CANCER, ZodiacSign.LIBRA, ZodiacSign.CAPRICORN -> 0 // movable
            ZodiacSign.TAURUS, ZodiacSign.LEO, ZodiacSign.SCORPIO, ZodiacSign.AQUARIUS -> 8 // fixed (9th)
            ZodiacSign.GEMINI, ZodiacSign.VIRGO, ZodiacSign.SAGITTARIUS, ZodiacSign.PISCES -> 4 // dual (5th)
        }
        val targetIndex = (signIndex + startOffset + p) % 12
        return ZodiacSign.entries[targetIndex]
    }

    private fun saptamsaSign(signIndex: Int, sign: ZodiacSign, inSign: Double): ZodiacSign {
        val size = 30.0 / 7.0
        val p = floor(inSign / size).toInt().coerceIn(0, 6)
        val startOffset = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.GEMINI, ZodiacSign.LEO, ZodiacSign.LIBRA, ZodiacSign.SAGITTARIUS, ZodiacSign.AQUARIUS -> 0 // odd signs
            else -> 6 // even signs: 7th from sign
        }
        val targetIndex = (signIndex + startOffset + p) % 12
        return ZodiacSign.entries[targetIndex]
    }

    private fun drekkanaSign(signIndex: Int, inSign: Double): ZodiacSign {
        val p = floor(inSign / 10.0).toInt().coerceIn(0, 2)
        val targetIndex = (signIndex + 4 * p) % 12 // 0 -> same, 1 -> +4(5th), 2 -> +8(9th)
        return ZodiacSign.entries[targetIndex]
    }

    private fun horaSign(sign: ZodiacSign, inSign: Double): ZodiacSign {
        val firstHalf = inSign < 15.0
        val odd = when (sign) {
            ZodiacSign.ARIES, ZodiacSign.GEMINI, ZodiacSign.LEO, ZodiacSign.LIBRA, ZodiacSign.SAGITTARIUS, ZodiacSign.AQUARIUS -> true
            else -> false
        }
        val isMoon = if (odd) !firstHalf else firstHalf
        return if (isMoon) ZodiacSign.CANCER else ZodiacSign.LEO
    }
}
