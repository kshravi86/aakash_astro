package com.aakash.astro

import android.os.Bundle
import android.view.Gravity
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivitySarvatobhadraBinding
import java.time.Instant
import java.time.ZoneId

class SarvatobhadraActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySarvatobhadraBinding
    private val accurate = AccurateCalculator()
    private val cellViews: Array<Array<TextView?>> = Array(9) { arrayOfNulls<TextView>(9) }

    // Outer ring: 28 nakshatras (clockwise), starting NE edge after the corner
    private val ringNakshatras = listOf(
        // 28-star ring (clockwise) starting at NE edge (after corner)
        "Krittika", "Rohini", "Mrigashira", "Ardra", "Punarvasu", "Pushya", "Ashlesha",
        "Magha", "Purva Phalguni", "Uttara Phalguni", "Hasta", "Chitra", "Swati",
        "Vishakha", "Anuradha", "Jyeshtha", "Mula", "Purva Ashadha", "Uttara Ashadha",
        "Abhijit", "Shravana", "Dhanishta", "Shatabhisha", "Purva Bhadrapada",
        "Uttara Bhadrapada", "Revati", "Ashwini", "Bharani"
    )
    // Inner letter rings (akshara) Ã¢â‚¬â€œ transliterated placeholders to match the style
    private val ringLetters1 = listOf(
        // 7 + 7 + 7 + 7 = 28 items
        "rii","g","s","d","ch","l","u",
        "a","aa","i","ii","u","uu","e",
        "o","ka","kha","ga","gha","na","ca",
        "cha","ja","jha","nya","ta","tha","da",
        "dha"
    ).take(28)
    private val ringLetters2 = listOf(
        // 20 items for the 5x5 perimeter (without corners)
        "na","pa","pha","ba","bha",
        "ma","ya","ra","la","va",
        "sha","ssa","sa","ha","ksha",
        "aM","ah","ai","au","ru"
    )
    // Zodiac signs ring around the central 3x3 (12 items)
    private val signsRing = listOf(
        "Ar","Ta","Ge","Cn","Le","Vi","Li","Sc","Sg","Cp","Aq","Pi"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySarvatobhadraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val natalChart = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        val transitChart = accurate.generateChart(BirthDetails(name, Instant.now().atZone(zoneId), lat, lon))

        // Build SBC template and overlay current transit planets on outer ring
        val overlays = computeTransitOverlays(transitChart)
        buildTemplateGrid(overlays)
        // Start with no lines; user will tap a planet cell to reveal vedha lines
        findViewById<com.aakash.astro.ui.SbcOverlayView>(R.id.gridOverlay).setLines(emptyList())
        binding.summaryText.text = ""
    }

    private fun findRingIndexByName(name: String): Int? {
        // NakshatraCalc names are ASCII without diacritics, match against our list
        val idx = ringNakshatras.indexOfFirst { it.equals(name, ignoreCase = true) }
        return if (idx >= 0) idx else null
    }

    private fun advanceRing(from: Int, steps: Int): Int = (from + steps) % ringNakshatras.size

    private fun buildTemplateGrid(outerOverlays: Map<Int, List<String>>) {
        val grid = binding.gridSbc
        grid.removeAllViews()
        grid.columnCount = 9
        grid.rowCount = 9

        val primary = ContextCompat.getColor(this, R.color.primaryText)
        val secondary = ContextCompat.getColor(this, R.color.secondaryText)
        val cardBg = ContextCompat.getColor(this, R.color.card_bg)

        val outer = perimeterCoords(0)
        val ring1 = perimeterCoords(1)
        val ring2 = perimeterCoords(2)
        val ring3 = perimeterCoords(3) // signs

        // Corner vowels like the reference image
        val cornerText = mapOf(
            (0 to 0) to "ii",
            (0 to 8) to "a",
            (8 to 8) to "aa",
            (8 to 0) to "i"
        )

        // Exact outer ring abbreviations in the visual (clockwise) order used by `outer`:
        // Top edge NE->NW, Left edge NW->SW, Bottom edge SW->SE, Right edge SE->NE
        val outerAbbr = listOf(
            // Top (NE -> NW)
            "Bhar", "Aswi", "Reva", "UBha", "PBha", "Sata", "Dhan",
            // Left (NW -> SW)
            "Srav", "Abhi", "USha", "PSha", "Mool", "Jye", "Anu",
            // Bottom (SW -> SE)
            "Visa", "Swat", "Chit", "Hast", "UPha", "PPha", "Magh",
            // Right (SE -> NE)
            "Asre", "Push", "Puna", "Ardr", "Mrig", "Roh", "Krit"
        )

        // Sign abbreviations already provided in signsRing

        for (r in 0 until 9) {
            for (c in 0 until 9) {
                val tv = TextView(this)
                tv.gravity = Gravity.CENTER
                tv.setPadding(dp(8), dp(8), dp(8), dp(8)); tv.minHeight = dp(64); tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 13f)
                tv.setTextColor(primary)
                tv.text = ""
                tv.background = androidx.core.content.ContextCompat.getDrawable(this, R.drawable.bg_chart_highlight)

                // Corners
                cornerText[r to c]?.let {
                    tv.text = it
                    tv.setTextColor(secondary)
                }

                // Outer ring
                val idxOuter = outer.indexOf(r to c)
                if (idxOuter >= 0 && idxOuter < outerAbbr.size) {
                    val base = outerAbbr[idxOuter]
                    val planets = outerOverlays[idxOuter]
                    tv.text = if (planets != null && planets.isNotEmpty()) "$base\n${planets.joinToString(" ")}" else base
                    if (planets != null && planets.isNotEmpty()) {
                        tv.isClickable = true
                        tv.setOnClickListener {
                            val (r0, c0) = outer[idxOuter]
                            clearHighlights(); highlightVedhaBoxes(r0, c0); findViewById<com.aakash.astro.ui.SbcOverlayView>(R.id.gridOverlay).setLines(emptyList())
                        }
                    }
                }

                // First letter ring (7x7 perimeter)
                val idxR1 = ring1.indexOf(r to c)
                if (idxR1 >= 0 && idxR1 < ringLetters1.size) {
                    tv.text = ringLetters1[idxR1]
                    tv.setTextColor(secondary)
                }

                // Second letter ring (5x5 perimeter)
                val idxR2 = ring2.indexOf(r to c)
                if (idxR2 >= 0 && idxR2 < ringLetters2.size) {
                    tv.text = ringLetters2[idxR2]
                    tv.setTextColor(secondary)
                }

                // Signs ring (3x3 perimeter)
                val idxR3 = ring3.indexOf(r to c)
                if (idxR3 >= 0 && idxR3 < signsRing.size) {
                    tv.text = signsRing[idxR3]
                }

                // Central 3x3: match provided center-box.png exactly
                if (r in 3..5 && c in 3..5) {
                    val label = when (r to c) {
                        // corners carry syllables
                        3 to 3 -> "ah"
                        3 to 5 -> "o"
                        5 to 3 -> "am"
                        5 to 5 -> "au"
                        // tithi groups
                        3 to 4 -> "Rikta\nFri"
                        4 to 3 -> "Jaya\nThu"
                        4 to 4 -> "Poorna\nSat"
                        4 to 5 -> "Nanda\nSun Tue"
                        5 to 4 -> "Bhadra\nMon Wed"
                        else -> ""
                    }
                    if (label.isNotEmpty()) tv.text = label
                }

                // Override 2nd and 3rd rows to match provided 2ndand3rdrow.png
                if (r == 1 && c in 1..7) {
                    val row2 = listOf("rii", "g", "s", "d", "ch", "l", "u")
                    tv.text = row2[c - 1]
                    tv.setTextColor(secondary)
                }
                if (r == 2 && c in 1..7) {
                    val row3 = listOf("kh", "ai", "Aq", "Pi", "Ar", "lu", "a")
                    val v = row3[c - 1]
                    tv.text = v
                    // Color signs in primary, letters in secondary
                    tv.setTextColor(if (v in listOf("Aq","Pi","Ar")) primary else secondary)
                }
                if (r == 3 && c in 1..7) {
                    val row4 = listOf("j", "Cp", "ah", "Rikta\nFri", "o", "Ta", "v")
                    val v = row4[c - 1]
                    tv.text = v
                    tv.setTextColor(if (v in listOf("Cp","Ta")) primary else secondary)
                }
                if (r == 4 && c in 1..7) {
                    val row5 = listOf("bh", "Sg", "Jaya\nThu", "Poorna\nSat", "Nanda\nSun Tue", "Ge", "k")
                    val v = row5[c - 1]
                    tv.text = v
                    tv.setTextColor(if (v in listOf("Sg","Ge")) primary else secondary)
                }
                if (r == 5 && c in 1..7) {
                    val row6 = listOf("y", "Sc", "am", "Bhadra\nMon Wed", "au", "Cn", "h")
                    val v = row6[c - 1]
                    tv.text = v
                    tv.setTextColor(if (v in listOf("Sc","Cn")) primary else secondary)
                }
                if (r == 6 && c in 1..7) {
                    val row7 = listOf("n", "e", "Li", "Vi", "Le", "luu", "d")
                    val v = row7[c - 1]
                    tv.text = v
                    tv.setTextColor(if (v in listOf("Li","Vi","Le")) primary else secondary)
                }
                if (r == 7 && c in 1..7) {
                    val row8 = listOf("ri", "t", "r", "p", "t~", "m", "uu")
                    val v = row8[c - 1]
                    tv.text = v
                    tv.setTextColor(secondary)
                }

                val p = android.widget.GridLayout.LayoutParams().apply {
                    width = 0
                    height = android.widget.GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = android.widget.GridLayout.spec(c, 1f)
                    rowSpec = android.widget.GridLayout.spec(r, 1f)
                    setMargins(0, 0, 0, 0)
                }
                grid.addView(tv, p)
                cellViews[r][c] = tv
            }
        }
    }

    private fun perimeterCoords(offset: Int): List<Pair<Int, Int>> {
        val min = offset
        val max = 8 - offset
        val list = mutableListOf<Pair<Int, Int>>()
        for (c in (max - 1) downTo (min + 1)) list += min to c
        for (r in (min + 1)..(max - 1)) list += r to min
        for (c in (min + 1)..(max - 1)) list += max to c
        for (r in (max - 1) downTo (min + 1)) list += r to max
        return list
    }

    private fun dp(v: Int): Int {
        return (resources.displayMetrics.density * v).toInt()
    }

    private fun clearHighlights() {
        for (r in 0..8) for (c in 0..8) {
            cellViews[r][c]?.foreground = null
        }
    }

    private fun highlightVedhaBoxes(r: Int, c: Int) {
        val red = ContextCompat.getColor(this, R.color.planet_unfavorable)
        val color = ColorUtils.setAlphaComponent(red, 110)
        fun mark(rr: Int, cc: Int) { cellViews[rr][cc]?.foreground = android.graphics.drawable.ColorDrawable(color) }
        // Left diagonal: up-left until boundary
        var rr = r; var cc = c
        while (rr >= 0 && cc >= 0) { mark(rr, cc); rr--; cc-- }
        // Right diagonal: up-right until boundary
        rr = r; cc = c
        while (rr >= 0 && cc <= 8) { mark(rr, cc); rr--; cc++ }
        // Straight: vertical or horizontal depending on edge
        if (r == 0 || r == 8) for (i in 0..8) mark(i, c)
        if (c == 0 || c == 8) for (j in 0..8) mark(r, j)
    }



    private fun vedhaLinesFor(r: Int, c: Int): List<com.aakash.astro.ui.SbcLine> {
        val primary = ContextCompat.getColor(this, R.color.accent_teal)
        val diag1 = ContextCompat.getColor(this, R.color.accent_blue)
        val diag2 = ContextCompat.getColor(this, R.color.accent_gold)
        val out = mutableListOf<com.aakash.astro.ui.SbcLine>()

        // Left diagonal: from top-left corner of tapped box towards top-left boundary
        run {
            val d = kotlin.math.min(r, c)
            val sr = r - d; val sc = c - d
            out += com.aakash.astro.ui.SbcLine(r.toFloat(), c.toFloat(), sr.toFloat(), sc.toFloat(), diag1)
        }

        // Right diagonal: from top-right corner of tapped box towards top-right boundary
        run {
            val d = kotlin.math.min(r, 8 - c)
            val sr = r - d; val sc = c + 1 + d
            out += com.aakash.astro.ui.SbcLine(r.toFloat(), (c + 1).toFloat(), sr.toFloat(), sc.toFloat(), diag2)
        }

        // Vertical/horizontal straight line across from center to edges (only needed on perimeter rows/cols)
        if (r == 0 || r == 8) {
            out += com.aakash.astro.ui.SbcLine(r + 0.5f, c + 0.5f, 8f, c + 0.5f, primary)
            out += com.aakash.astro.ui.SbcLine(r + 0.5f, c + 0.5f, 0f, c + 0.5f, primary)
        }
        if (c == 0 || c == 8) {
            out += com.aakash.astro.ui.SbcLine(r + 0.5f, c + 0.5f, r + 0.5f, 8f, primary)
            out += com.aakash.astro.ui.SbcLine(r + 0.5f, c + 0.5f, r + 0.5f, 0f, primary)
        }
        return out
    }

    private fun computeTransitOverlays(transitChart: ChartResult?): Map<Int, List<String>> {
        if (transitChart == null) return emptyMap()
        // Abbreviation order must match the grid's outer ring order
        val outerAbbr = listOf(
            // Top (NE -> NW)
            "Bhar", "Aswi", "Reva", "UBha", "PBha", "Sata", "Dhan",
            // Left (NW -> SW)
            "Srav", "Abhi", "USha", "PSha", "Mool", "Jye", "Anu",
            // Bottom (SW -> SE)
            "Visa", "Swat", "Chit", "Hast", "UPha", "PPha", "Magh",
            // Right (SE -> NE)
            "Asre", "Push", "Puna", "Ardr", "Mrig", "Roh", "Krit"
        )
        val nameToAbbr = mapOf(
            "Ashwini" to "Aswi",
            "Bharani" to "Bhar",
            "Krittika" to "Krit",
            "Rohini" to "Roh",
            "Mrigashira" to "Mrig",
            "Ardra" to "Ardr",
            "Punarvasu" to "Puna",
            "Pushya" to "Push",
            "Ashlesha" to "Asre",
            "Magha" to "Magh",
            "Purva Phalguni" to "PPha",
            "Uttara Phalguni" to "UPha",
            "Hasta" to "Hast",
            "Chitra" to "Chit",
            "Swati" to "Swat",
            "Vishakha" to "Visa",
            "Anuradha" to "Anu",
            "Jyeshtha" to "Jye",
            "Mula" to "Mool",
            "Purva Ashadha" to "PSha",
            "Uttara Ashadha" to "USha",
            "Shravana" to "Srav",
            "Dhanishta" to "Dhan",
            "Shatabhisha" to "Sata",
            "Purva Bhadrapada" to "PBha",
            "Uttara Bhadrapada" to "UBha",
            "Revati" to "Reva",
        )
        val abbrToIndex = outerAbbr.withIndex().associate { it.value to it.index }
        val planetAbbr = mapOf(
            Planet.SUN to "Su",
            Planet.MOON to "Mo",
            Planet.MARS to "Ma",
            Planet.MERCURY to "Me",
            Planet.JUPITER to "Ju",
            Planet.VENUS to "Ve",
            Planet.SATURN to "Sa",
            Planet.RAHU to "Ra",
            Planet.KETU to "Ke",
        )
        val out = mutableMapOf<Int, MutableList<String>>()
        transitChart.planets.forEach { p ->
            val (nakName, _) = NakshatraCalc.fromLongitude(p.degree)
            val abbr = nameToAbbr[nakName] ?: return@forEach
            val idx = abbrToIndex[abbr] ?: return@forEach
            val tag = planetAbbr[p.planet] ?: p.planet.name.take(2)
            out.getOrPut(idx) { mutableListOf() }.add(tag)
        }
        return out.mapValues { it.value.sorted() }
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}















