package com.aakash.astro

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityAshtakavargaBavBinding
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

class AshtakavargaBavActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAshtakavargaBavBinding
    private val accurate = AccurateCalculator()
    private lateinit var zoneIdExtra: ZoneId
    private var latExtra: Double = 0.0
    private var lonExtra: Double = 0.0

    private val bavRefs = listOf(
        Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN
    )

    private val headerFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy • HH:mm z", Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAshtakavargaBavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        zoneIdExtra = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        latExtra = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        lonExtra = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneIdExtra)
        val chart = accurate.generateChart(BirthDetails(name, zdt, latExtra, lonExtra))
        if (chart == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        renderHeader(chart, name, zdt)
        renderBAV(chart)
    }

    private fun renderHeader(chart: ChartResult, name: String?, zdt: ZonedDateTime) {
        val displayName = name?.takeIf { it.isNotBlank() } ?: getString(R.string.ashtakavarga_anonymous_native)
        binding.nativeName.text = displayName
        binding.birthInfo.text = headerFormatter.format(zdt)
        binding.locationInfo.text = if (latExtra == 0.0 && lonExtra == 0.0) {
            getString(R.string.ashtakavarga_location_placeholder)
        } else {
            getString(
                R.string.ashtakavarga_location_format,
                formatCoordinate(latExtra, "N", "S"),
                formatCoordinate(lonExtra, "E", "W")
            )
        }

        val highlights = buildHighlights(chart)
        binding.highlightChips.apply {
            removeAllViews()
            isVisible = highlights.isNotEmpty()
            highlights.forEach { addView(createHighlightChip(it)) }
        }
    }

    private fun buildHighlights(chart: ChartResult): List<String> {
        val highlights = mutableListOf<String>()

        val savValues = AshtakavargaCalc.computeSarva(chart).values
        val savMaxIndex = savValues.indices.maxByOrNull { savValues[it] }
        val savMinIndex = savValues.indices.minByOrNull { savValues[it] }
        savMaxIndex?.let {
            highlights += getString(
                R.string.ashtakavarga_sav_peak_chip,
                ZodiacSign.entries[it].displayName,
                savValues[it]
            )
        }
        savMinIndex?.let {
            highlights += getString(
                R.string.ashtakavarga_sav_low_chip,
                ZodiacSign.entries[it].displayName,
                savValues[it]
            )
        }

        val dominantPlanet = bavRefs
            .map { it to AshtakavargaCalc.computeBAVParashara(it, chart).values.sum() }
            .maxByOrNull { it.second }
        dominantPlanet?.let { (planet, total) ->
            highlights += getString(R.string.ashtakavarga_bav_peak_chip, planet.displayName, total)
        }

        val lagnaBav = AshtakavargaCalc.computeLagnaBAV(chart).values
        val lagnaMaxIndex = lagnaBav.indices.maxByOrNull { lagnaBav[it] }
        lagnaMaxIndex?.let {
            highlights += getString(
                R.string.ashtakavarga_lagna_chip,
                ZodiacSign.entries[it].displayName,
                lagnaBav[it]
            )
        }

        return highlights
    }

    private fun createHighlightChip(text: String): Chip {
        return Chip(this).apply {
            this.text = text
            isCheckable = false
            isClickable = false
            ensureMinTouchTargetSize = false
            chipBackgroundColor = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.card_bg_elevated))
            setTextColor(ContextCompat.getColor(context, R.color.primaryText))
            chipIcon = AppCompatResources.getDrawable(context, R.drawable.ic_chip_star)
            chipIconTint = ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent_gold))
            chipIconSize = resources.displayMetrics.density * 16
            iconStartPadding = resources.displayMetrics.density * 4
            textStartPadding = resources.displayMetrics.density * 4
            textEndPadding = resources.displayMetrics.density * 8
        }
    }

    private fun renderBAV(chart: ChartResult) {
        val container: LinearLayout = binding.sectionsContainer
        val inflater = LayoutInflater.from(this)
        container.removeAllViews()

        bavRefs.forEach { ref ->
            val bav = AshtakavargaCalc.computeBAVParashara(ref, chart).values
            addBavSection("${ref.displayName} BAV", bav, chart, container, inflater)
        }

        val lagnaBav = AshtakavargaCalc.computeLagnaBAV(chart).values
        addBavSection(getString(R.string.lagna_bav_title), lagnaBav, chart, container, inflater)
    }

    private fun addBavSection(
        title: String,
        values: IntArray,
        chart: ChartResult,
        container: LinearLayout,
        inflater: LayoutInflater
    ) {
        val section = inflater.inflate(R.layout.item_sav_section, container, false)
        section.findViewById<TextView>(R.id.sectionTitle).text = title
        val list = section.findViewById<LinearLayout>(R.id.sectionList)
        list.removeAllViews()
        val ascOrdinal = chart.ascendantSign.ordinal
        for (i in 0 until 12) {
            val row = inflater.inflate(R.layout.item_sign_value, list, false)
            bindRow(row, ZodiacSign.entries[i], ascOrdinal, i, values[i])
            list.addView(row)
        }
        container.addView(section)
    }

    private fun bindRow(row: View, sign: ZodiacSign, ascOrdinal: Int, index: Int, value: Int) {
        val house = 1 + (index - ascOrdinal + 12) % 12
        row.findViewById<TextView>(R.id.signName).text = sign.displayName
        row.findViewById<TextView>(R.id.houseBadge).text =
            getString(R.string.ashtakavarga_house_badge_format, ordinal(house))
        val valueView = row.findViewById<TextView>(R.id.signValue).apply {
            text = getString(R.string.ashtakavarga_value_points, value)
        }
        val progress = row.findViewById<LinearProgressIndicator>(R.id.valueProgress)
        progress.setProgressCompat(value, true)
        styleValue(value, valueView, progress)
    }

    private fun styleValue(value: Int, valueView: TextView, progress: LinearProgressIndicator) {
        val (bgRes, colorRes) = when {
            value >= 6 -> R.drawable.bg_value_chip_high to R.color.accent_teal
            value <= 3 -> R.drawable.bg_value_chip_low to R.color.accent_orange
            else -> R.drawable.bg_value_chip_neutral to R.color.primaryText
        }
        valueView.setBackgroundResource(bgRes)
        valueView.setTextColor(ContextCompat.getColor(this, colorRes))
        progress.setIndicatorColor(ContextCompat.getColor(this, colorRes))
    }

    private fun formatCoordinate(value: Double, positiveSuffix: String, negativeSuffix: String): String {
        val suffix = if (value >= 0) positiveSuffix else negativeSuffix
        return String.format(Locale.US, "%.2f°%s", abs(value), suffix)
    }

    private fun ordinal(n: Int): String = when {
        n % 100 in 11..13 -> "${n}th"
        n % 10 == 1 -> "${n}st"
        n % 10 == 2 -> "${n}nd"
        n % 10 == 3 -> "${n}rd"
        else -> "${n}th"
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
