package com.aakash.astro.astrology

import kotlin.math.floor

object D60Shashtiamsa {
    // Forward order names 1..60 (use for odd signs; reverse for even signs)
    private val names = listOf(
        "Ghorānsh", "Rākshasa", "Deva", "Kuber", "Rakshogana", "Kinnar",
        "Bhrshta", "Kulaghna", "Garala", "Agni", "Māyā", "Yama",
        "Varuna", "Indra", "Kāla", "Ahibhāga", "Amrtānshu", "Chandra",
        "Madu", "Komala", "Padma", "Vishnu", "Vāgāsh", "Dingchar",
        "Dava", "Ārdra", "Kālanāsha", "Kshitish", "Kamalākar", "Manda",
        "Mrtyu", "Kāla", "Dāvagni", "Ghora", "Maya", "Kantaka",
        "Sudhā", "Amrta", "Poornachandra", "Vishpradimā", "Kulanāsha", "Mukhya",
        "Vanshakshya", "Utpāta", "Kālroopa", "Saomaya", "Mrdu", "Sheetala",
        "Danshtrākarā", "Indumukha", "Praveena", "Kālagni", "Dandayudha", "Nirmala",
        "Shubha", "Ashubha", "Atisheeta", "Sudhāyoga", "Bhramana", "Indurekhā"
    )

    // Odd-sign malefic numbers per Phaladipika (one common recension)
    private val oddSignMalefics = setOf(
        1, 2, 8, 9, 10, 11, 12, 15, 16, 30, 31, 32, 33, 34, 35, 39, 40, 42, 43, 44, 48, 51, 52, 59
    )

    fun amshaNumber(degInSign: Double): Int {
        val size = 30.0 / 60.0 // 0.5 deg
        return floor(((degInSign % 30.0) + 30.0) % 30.0 / size).toInt() + 1
    }

    fun amshaName(number: Int, isOddSign: Boolean): String {
        val idx = if (isOddSign) number - 1 else (60 - number)
        return names[idx.coerceIn(0, names.size - 1)]
    }

    fun isBenefic(number: Int, isOddSign: Boolean): Boolean {
        val oddMal = oddSignMalefics.contains(number)
        return if (isOddSign) !oddMal else oddMal
    }
}

