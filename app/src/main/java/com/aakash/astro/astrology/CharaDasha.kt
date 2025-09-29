package com.aakash.astro.astrology

import java.time.Duration
import java.time.ZonedDateTime

/**
 * Jaimini Chara Dasha implementation following the provided rules:
 * - Start from Lagna sign
 * - Direction decided by 9th-from-Lagna sign group (Savya/Apasavya)
 * - Duration for each sign = inclusive sign-count from the sign to its lord's sign (minus 1),
 *   counting in the same direction as the dasha sequence; if lord in own sign => 12 years
 * - For Scorpio/Aquarius (dual lords), choose stronger lord as per rules; if both in sign => 12 years
 * - Antardashas and Pratyantars: same order as Mahadasha; each is 1/12 of parent period
 */
object CharaDasha {
    private const val DAYS_PER_YEAR = 365.25

    private val savya = setOf(
        ZodiacSign.ARIES, ZodiacSign.TAURUS, ZodiacSign.GEMINI,
        ZodiacSign.LIBRA, ZodiacSign.SCORPIO, ZodiacSign.SAGITTARIUS
    )
    private val apasavya = setOf(
        ZodiacSign.CANCER, ZodiacSign.LEO, ZodiacSign.VIRGO,
        ZodiacSign.CAPRICORN, ZodiacSign.AQUARIUS, ZodiacSign.PISCES
    )

    fun compute(chart: ChartResult, start: ZonedDateTime): List<DashaPeriod> {
        val asc = chart.ascendantSign
        val ninth = signAtOffset(asc, +8) // 9th from Lagna
        val forward = ninth in savya || ninth !in apasavya // default forward if unknown

        // build the 12-sign order starting from Lagna
        val order = (0 until 12).map { step -> signAtOffset(asc, if (forward) step else -step) }

        val planetPos = chart.planets.associateBy { it.planet }
        val durations = order.map { sign -> yearsForSign(sign, forward, planetPos) }

        val periods = mutableListOf<DashaPeriod>()
        var t = start
        var elapsedYears = 0.0

        // Generate periods for up to ~120 years horizon by repeating cycles
        while (elapsedYears < 120.0 - 1e-6) {
            for (i in 0 until 12) {
                val sign = order[i]
                val years = durations[i].coerceAtLeast(0.0)
                val end = t.plusSeconds((years * DAYS_PER_YEAR * 86400).toLong())
                periods += DashaPeriod(sign.displayName, t, end)
                t = end
                elapsedYears += years
                if (elapsedYears >= 120.0 - 1e-6) break
            }
        }
        return periods
    }

    fun antardashaFor(ma: DashaPeriod, forward: Boolean): List<DashaPeriod> {
        val totalDays = Duration.between(ma.start, ma.end).toDays().toDouble()
        val sign = signFromLabel(ma.lord) ?: return emptyList()
        val order = (0 until 12).map { step -> signAtOffset(sign, if (forward) step else -step) }
        val perDays = totalDays / 12.0
        val list = mutableListOf<DashaPeriod>()
        var t = ma.start
        order.forEach { rashi ->
            val end = t.plusSeconds((perDays * 86400).toLong())
            list += DashaPeriod(rashi.displayName, t, end)
            t = end
        }
        return list
    }

    fun pratyantarFor(antar: DashaPeriod, forward: Boolean): List<DashaPeriod> {
        val totalDays = Duration.between(antar.start, antar.end).toDays().toDouble()
        val sign = signFromLabel(antar.lord) ?: return emptyList()
        val order = (0 until 12).map { step -> signAtOffset(sign, if (forward) step else -step) }
        val perDays = totalDays / 12.0
        val list = mutableListOf<DashaPeriod>()
        var t = antar.start
        order.forEach { rashi ->
            val end = t.plusSeconds((perDays * 86400).toLong())
            list += DashaPeriod(rashi.displayName, t, end)
            t = end
        }
        return list
    }

    private fun yearsForSign(
        sign: ZodiacSign,
        forward: Boolean,
        pos: Map<Planet, PlanetPosition>
    ): Double {
        val lordInfo = lordsOf(sign)
        // Single lord
        if (lordInfo.size == 1) {
            val lord = lordInfo.first()
            val lordPos = pos[lord] ?: return 0.0
            if (lordPos.sign == sign) return 12.0
            return countSigns(sign, lordPos.sign, forward).toDouble()
        }

        // Dual lordship (Scorpio/Aquarius)
        val p1 = pos[lordInfo[0]]
        val p2 = pos[lordInfo[1]]
        if (p1 == null || p2 == null) return 0.0

        val p1InSign = p1.sign == sign
        val p2InSign = p2.sign == sign
        if (p1InSign && p2InSign) return 12.0
        if (p1InSign.xor(p2InSign)) {
            // If one lord in the sign and other not, use the one NOT in the sign
            val chosen = if (p1InSign) p2 else p1
            return countSigns(sign, chosen.sign, forward).toDouble()
        }

        // Neither in the sign => pick stronger by conjunction count, then higher degree
        val c1 = conjunctionCount(p1, pos)
        val c2 = conjunctionCount(p2, pos)
        val chosen = when {
            c1 > c2 -> p1
            c2 > c1 -> p2
            else -> if (p1.degree >= p2.degree) p1 else p2
        }
        return countSigns(sign, chosen.sign, forward).toDouble()
    }

    private fun conjunctionCount(target: PlanetPosition, pos: Map<Planet, PlanetPosition>): Int {
        // Count other planets in the same sign (sign-level conjunction for this implementation)
        val sign = target.sign
        return pos.values.count { it.planet != target.planet && it.sign == sign }
    }

    private fun countSigns(from: ZodiacSign, to: ZodiacSign, forward: Boolean): Int {
        val a = from.ordinal
        val b = to.ordinal
        return if (forward) (b - a + 12) % 12 else (a - b + 12) % 12
    }

    private fun lordsOf(sign: ZodiacSign): List<Planet> = when (sign) {
        ZodiacSign.ARIES -> listOf(Planet.MARS)
        ZodiacSign.TAURUS -> listOf(Planet.VENUS)
        ZodiacSign.GEMINI -> listOf(Planet.MERCURY)
        ZodiacSign.CANCER -> listOf(Planet.MOON)
        ZodiacSign.LEO -> listOf(Planet.SUN)
        ZodiacSign.VIRGO -> listOf(Planet.MERCURY)
        ZodiacSign.LIBRA -> listOf(Planet.VENUS)
        ZodiacSign.SCORPIO -> listOf(Planet.MARS, Planet.KETU)
        ZodiacSign.SAGITTARIUS -> listOf(Planet.JUPITER)
        ZodiacSign.CAPRICORN -> listOf(Planet.SATURN)
        ZodiacSign.AQUARIUS -> listOf(Planet.SATURN, Planet.RAHU)
        ZodiacSign.PISCES -> listOf(Planet.JUPITER)
    }

    private fun signAtOffset(from: ZodiacSign, offset: Int): ZodiacSign {
        val idx = (from.ordinal + offset) floorMod 12
        return ZodiacSign.entries[idx]
    }

    private infix fun Int.floorMod(mod: Int): Int {
        val r = this % mod
        return if (r < 0) r + mod else r
    }

    private fun signFromLabel(label: String): ZodiacSign? = ZodiacSign.entries.firstOrNull {
        it.displayName.equals(label, ignoreCase = true)
    }
}

