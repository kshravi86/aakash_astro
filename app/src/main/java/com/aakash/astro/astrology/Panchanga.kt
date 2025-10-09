package com.aakash.astro.astrology

import java.time.DayOfWeek

data class PanchangaResult(
    val tithi: String,
    val tithiGroup: String,
    val vara: String,
    val nakshatra: String,
    val yoga: String,
    val karana: String
)

object PanchangaCalc {
    private fun normalize(deg: Double): Double {
        var x = deg % 360.0
        if (x < 0) x += 360.0
        return x
    }

    private val tithiNames = listOf(
        "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Purnima",
        "Pratipada", "Dvitiya", "Tritiya", "Chaturthi", "Panchami",
        "Shashthi", "Saptami", "Ashtami", "Navami", "Dashami",
        "Ekadashi", "Dvadashi", "Trayodashi", "Chaturdashi", "Amavasya"
    )

    private val yogaNames = listOf(
        "Vishkambha", "Priti", "Ayushman", "Saubhagya", "Shobhana",
        "Atiganda", "Sukarma", "Dhriti", "Shoola", "Ganda",
        "Vriddhi", "Dhruva", "Vyaghata", "Harshana", "Vajra",
        "Siddhi", "Vyatipata", "Variyan", "Parigha", "Shiva",
        "Siddha", "Sadhya", "Shubha", "Shukla", "Brahma",
        "Indra", "Vaidhriti"
    )

    private val karanaMovable = listOf("Bava", "Balava", "Kaulava", "Taitila", "Gara", "Vanija", "Vishti")

    fun tithiName(moonSidereal: Double, sunSidereal: Double): String {
        val diff = normalize(moonSidereal - sunSidereal)
        val index = kotlin.math.floor(diff / 12.0).toInt().coerceIn(0, 29) // 30 tithis
        val paksha = if (index < 15) "Shukla" else "Krishna"
        return "$paksha ${tithiNames[index]}"
    }

    fun yogaName(moonSidereal: Double, sunSidereal: Double): String {
        val sum = normalize(moonSidereal + sunSidereal)
        val idx = kotlin.math.floor(sum / (360.0 / 27.0)).toInt().coerceIn(0, 26)
        return yogaNames[idx]
    }

    fun karanaName(moonSidereal: Double, sunSidereal: Double): String {
        val diff = normalize(moonSidereal - sunSidereal)
        val halfTithiIndex = kotlin.math.floor(diff / 6.0).toInt() + 1 // 1..60
        return when (halfTithiIndex) {
            1 -> "Kimstughna"
            in 2..57 -> karanaMovable[(halfTithiIndex - 2) % 7]
            58 -> "Shakuni"
            59 -> "Chatushpada"
            60 -> "Naga"
            else -> ""
        }
    }

    fun tithiGroupName(moonSidereal: Double, sunSidereal: Double): String {
        val diff = normalize(moonSidereal - sunSidereal)
        val index = kotlin.math.floor(diff / 12.0).toInt().coerceIn(0, 29) // 30 tithis
        val inPaksha = index % 15 // 0..14
        return when ((inPaksha + 1) % 5) {
            1 -> "Nanda"
            2 -> "Bhadra"
            3 -> "Jaya"
            4 -> "Rikta"
            else -> "Poorna"
        }
    }

    fun varaName(dow: DayOfWeek): String = when (dow) {
        DayOfWeek.SUNDAY -> "Ravivara"
        DayOfWeek.MONDAY -> "Somavara"
        DayOfWeek.TUESDAY -> "Mangalavara"
        DayOfWeek.WEDNESDAY -> "Budhavara"
        DayOfWeek.THURSDAY -> "Guruvara"
        DayOfWeek.FRIDAY -> "Shukravara"
        DayOfWeek.SATURDAY -> "Shanivara"
    }

    fun nakshatraDisplay(moonSidereal: Double): String {
        val (name, pada) = NakshatraCalc.fromLongitude(moonSidereal)
        return "$name • Pada $pada"
    }

    fun compute(chart: ChartResult, dayOfWeek: java.time.DayOfWeek): PanchangaResult {
        val sun = chart.planets.find { it.planet == Planet.SUN }?.degree ?: 0.0
        val moon = chart.planets.find { it.planet == Planet.MOON }?.degree ?: 0.0
        return PanchangaResult(
            tithi = tithiName(moon, sun),
            tithiGroup = tithiGroupName(moon, sun),
            vara = varaName(dayOfWeek),
            nakshatra = nakshatraDisplay(moon),
            yoga = yogaName(moon, sun),
            karana = karanaName(moon, sun)
        )
    }
}
