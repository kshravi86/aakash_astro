package com.aakash.astro.astrology

data class IshtaDevataResult(
    val atmakaraka: Planet,
    val akRasiSign: ZodiacSign,
    val akNavamsaSign: ZodiacSign,
    val twelfthFromAKNavamsaSign: ZodiacSign,
    val twelfthLord: Planet,
    val twelfthOccupant: Planet?,
    val ishtaDeterminingPlanet: Planet,
    val deity: String,
    val suggestion: String,
    val amatyakaraka: Planet,
    val amkRasiSign: ZodiacSign,
    val amkNavamsaSign: ZodiacSign,
    val sixthFromAMKNavamsaSign: ZodiacSign,
    val sixthLord: Planet,
    val sixthOccupant: Planet?,
    val palanaDeterminingPlanet: Planet,
    val palanaDeity: String,
    val palanaSuggestion: String
)

object IshtaDevataCalc {
    fun compute(natal: ChartResult): IshtaDevataResult? {
        val karakas = JaiminiKarakas.compute(natal, includeRahuKetu = false)
        val ak = karakas.firstOrNull() ?: return null
        val akPlanet = ak.planet
        val amkPlanet = karakas.getOrNull(1)?.planet ?: return null

        // D9 (Navamsha)
        val d9 = VargaCalculator.computeVargaChart(natal, Varga.D9, natal.ascendantDegree)
        val akInD9 = d9.planets.find { it.planet == akPlanet } ?: return null
        val amkInD9 = d9.planets.find { it.planet == amkPlanet } ?: return null
        val akD9Sign = akInD9.sign
        val twelfthSign = ZodiacSign.entries[(akD9Sign.ordinal + 11) % 12]
        val lord = signLordOf(twelfthSign)
        val targetHouseAk = ((akInD9.house + 10) % 12) + 1
        val occupantAk = d9.planets.firstOrNull { it.house == targetHouseAk }?.planet
        val ishtaPlanet = occupantAk ?: lord
        val deity = deityOf(ishtaPlanet)
        val suggestion = suggestionFor(deity)

        // Palana Devata: 6th from Amatyakaraka in Navamsha
        val amkD9Sign = amkInD9.sign
        val sixthSign = ZodiacSign.entries[(amkD9Sign.ordinal + 5) % 12]
        val sixthLord = signLordOf(sixthSign)
        val targetHouseAmk = ((amkInD9.house + 4) % 12) + 1
        val occupantAmk = d9.planets.firstOrNull { it.house == targetHouseAmk }?.planet
        val palanaPlanet = occupantAmk ?: sixthLord
        val palana = deityOf(palanaPlanet)
        val palanaSugg = suggestionFor(palana)

        return IshtaDevataResult(
            atmakaraka = akPlanet,
            akRasiSign = ak.sign,
            akNavamsaSign = akD9Sign,
            twelfthFromAKNavamsaSign = twelfthSign,
            twelfthLord = lord,
            twelfthOccupant = occupantAk,
            ishtaDeterminingPlanet = ishtaPlanet,
            deity = deity,
            suggestion = suggestion,
            amatyakaraka = amkPlanet,
            amkRasiSign = karakas[1].sign,
            amkNavamsaSign = amkD9Sign,
            sixthFromAMKNavamsaSign = sixthSign,
            sixthLord = sixthLord,
            sixthOccupant = occupantAmk,
            palanaDeterminingPlanet = palanaPlanet,
            palanaDeity = palana,
            palanaSuggestion = palanaSugg
        )
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

    private fun deityOf(planet: Planet): String = when (planet) {
        Planet.SUN -> "Shiva"
        Planet.MOON -> "Gauri/Parvati"
        Planet.MARS -> "Subrahmanya/Skanda"
        Planet.MERCURY -> "Vishnu"
        Planet.JUPITER -> "Narayana"
        Planet.VENUS -> "Lakshmi"
        Planet.SATURN -> "Hanuman/Kala Bhairava"
        Planet.RAHU -> "Durga/Pratyangira/Kalaratri"
        Planet.KETU -> "Ganesha"
        else -> "—"
    }

    private fun suggestionFor(deity: String): String = when (deity) {
        "Shiva" -> "Maha Mrityunjaya mantra, Rudra Abhishekam, Mondays"
        "Gauri/Parvati" -> "Devi stotras, fasting on Mondays/Fridays"
        "Subrahmanya/Skanda" -> "Skanda Shashti vrata, Subrahmanya stotra"
        "Vishnu" -> "Vishnu Sahasranama, Ekadashi upavasa"
        "Narayana" -> "Narayana Kavacham, Guru mantra on Thursdays"
        "Lakshmi" -> "Shri Suktam, Fridays, charitable acts"
        "Hanuman/Kala Bhairava" -> "Hanuman Chalisa/Tailabhishekam; Bhairava Ashtakam"
        "Durga/Pratyangira/Kalaratri" -> "Chandi/Devi Mahatmyam, Rahu remedies on Saturdays"
        "Ganesha" -> "Ganesha Atharvashirsha, Sankashti Chaturthi"
        else -> "General sattvic practices and japa"
    }
}
