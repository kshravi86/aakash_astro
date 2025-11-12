package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.AshtakavargaCalc
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivitySarvaAshtakavargaBinding
import com.google.android.material.chip.Chip
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.time.Instant
import java.time.ZoneId

class SarvaAshtakavargaActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySarvaAshtakavargaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySarvaAshtakavargaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            showEmptyState(getString(R.string.transit_engine_missing))
            return
        }

        binding.emptyState.isVisible = false
        val sav = AshtakavargaCalc.computeSAVParashara(chart)
        val savValues = sav.values
        binding.savChartView.setSav(savValues)

        renderSummary(savValues, chart)
        renderList(savValues, chart)
    }

    private fun renderSummary(values: IntArray, chart: com.aakash.astro.astrology.ChartResult) {
        if (values.isEmpty()) {
            showEmptyState(null)
            return
        }
        val total = values.sum()
        val average = values.average()
        binding.summaryPrimary.text = getString(R.string.sav_summary_primary, total)
        binding.summarySecondary.text = getString(R.string.sav_summary_secondary, average)

        val peakIndex = values.indices.maxByOrNull { values[it] }
        val lowIndex = values.indices.minByOrNull { values[it] }
        val ascHouse = 1 + chart.ascendantSign.ordinal

        binding.highlightChips.apply {
            removeAllViews()
            isVisible = true
            peakIndex?.let {
                addView(createHighlightChip(getString(R.string.sav_highlight_peak, ZodiacSign.entries[it].displayName, values[it])))
            }
            lowIndex?.let {
                addView(createHighlightChip(getString(R.string.sav_highlight_low, ZodiacSign.entries[it].displayName, values[it])))
            }
            addView(createHighlightChip(getString(R.string.sav_highlight_reference, ordinal(ascHouse))))
        }
    }

    private fun renderList(values: IntArray, chart: com.aakash.astro.astrology.ChartResult) {
        val container: LinearLayout = binding.savList
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val ascOrdinal = chart.ascendantSign.ordinal

        values.forEachIndexed { index, value ->
            val row = inflater.inflate(R.layout.item_sav_house, container, false)
            val sign = ZodiacSign.entries[index]
            val house = 1 + (index - ascOrdinal + 12) % 12

            row.findViewById<TextView>(R.id.signName).text = sign.displayName
            row.findViewById<TextView>(R.id.houseBadge).text =
                getString(R.string.ashtakavarga_house_badge_format, ordinal(house))
            val valueView = row.findViewById<TextView>(R.id.signValue).apply {
                text = getString(R.string.ashtakavarga_value_points, value)
            }
            row.findViewById<TextView>(R.id.valueHint).text =
                getString(R.string.sav_value_hint, house, value)
            val progress = row.findViewById<LinearProgressIndicator>(R.id.valueProgress)
            progress.max = 60
            progress.setProgressCompat(value, true)
            styleValue(value, valueView, progress)
            row.setOnClickListener { binding.savChartView.setHighlight(sign) }
            container.addView(row)
        }
    }

    private fun createHighlightChip(text: String): Chip =
        Chip(this).apply {
            this.text = text
            isCheckable = false
            isClickable = false
            setEnsureMinTouchTargetSize(false)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.card_bg_elevated)
            setTextColor(ContextCompat.getColor(context, R.color.primaryText))
        }

    private fun styleValue(value: Int, valueView: TextView, progress: LinearProgressIndicator) {
        val (bgRes, colorRes) = when {
            value >= 40 -> R.drawable.bg_value_chip_high to R.color.accent_teal
            value <= 20 -> R.drawable.bg_value_chip_low to R.color.accent_orange
            else -> R.drawable.bg_value_chip_neutral to R.color.primaryText
        }
        valueView.setBackgroundResource(bgRes)
        valueView.setTextColor(ContextCompat.getColor(this, colorRes))
        progress.setIndicatorColor(ContextCompat.getColor(this, colorRes))
    }

    private fun showEmptyState(message: String?) {
        binding.summaryPrimary.text = getString(R.string.sav_summary_primary_placeholder)
        binding.summarySecondary.text = getString(R.string.sav_summary_secondary_placeholder)
        binding.highlightChips.isVisible = false
        binding.emptyState.isVisible = true
        binding.emptyState.text = message ?: getString(R.string.sav_empty_state)
        binding.savList.removeAllViews()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

private fun ordinal(n: Int): String = when {
    n % 100 in 11..13 -> "${n}th"
    n % 10 == 1 -> "${n}st"
    n % 10 == 2 -> "${n}nd"
    n % 10 == 3 -> "${n}rd"
    else -> "${n}th"
}
