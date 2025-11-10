package com.aakash.astro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.YogiCalculator
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.EphemerisPreparer
import com.aakash.astro.databinding.ActivityYogiBinding
import com.google.android.material.chip.Chip
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class YogiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYogiBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYogiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }
        binding.topBar.title = getString(R.string.yogi_title)

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val details = BirthDetails(name, dateTime, lat, lon)
        val chart = accurate.generateChart(details)
        if (chart == null) {
            showEmptyState(getString(R.string.dasha_engine_missing))
            return
        }

        binding.title.text = getString(R.string.yogi_title)
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""

        val sun = chart.planets.firstOrNull { it.planet == Planet.SUN }
        val moon = chart.planets.firstOrNull { it.planet == Planet.MOON }
        if (sun == null || moon == null) {
            showEmptyState(null)
            return
        }

        val yogiPoint = YogiCalculator.computeYogiPoint(sun.degree, moon.degree)
        val yogiNak = NakshatraCalc.fromLongitude(yogiPoint).first
        val yogiLord = YogiCalculator.nakshatraLordOf(yogiPoint)
        val yogiSign = YogiCalculator.signOf(yogiPoint)
        val yogiHouse = houseFromAsc(yogiSign, chart)
        val sahayogi = YogiCalculator.signLordOf(yogiSign)
        val avayogiPoint = YogiCalculator.computeAvayogiPoint(yogiPoint)
        val avayogiNak = NakshatraCalc.fromLongitude(avayogiPoint).first
        val avayogiLord = YogiCalculator.nakshatraLordOf(avayogiPoint)
        val avayogiSign = YogiCalculator.signOf(avayogiPoint)
        val avayogiHouse = houseFromAsc(avayogiSign, chart)
        val avayogiVia6th = YogiCalculator.avayogiBy6th(yogiLord)

        renderSummary(yogiLord, sahayogi, avayogiLord, yogiNak)
        binding.emptyState.isVisible = false

        binding.yogiPlanetValue.text = yogiLord
        binding.yogiPointValue.text = getString(R.string.yogi_point_format, formatDegree(yogiPoint), yogiNak)
        binding.yogiNakshatraValue.text = yogiNak
        binding.yogiSignValue.text = getString(R.string.karaka_house_format, yogiSign.displayName, yogiHouse)

        binding.sahayogiPlanetValue.text = sahayogi.displayName
        binding.sahayogiSignValue.text = getString(R.string.karaka_house_format, yogiSign.displayName, yogiHouse)

        binding.avayogiPlanetValue.text = avayogiLord
        binding.avayogiPointValue.text = getString(R.string.yogi_point_format, formatDegree(avayogiPoint), avayogiNak)
        binding.avayogiNakshatraValue.text = avayogiNak
        binding.avayogiSignValue.text = getString(R.string.karaka_house_format, avayogiSign.displayName, avayogiHouse)
        binding.avayogiCardNote.text = getString(R.string.yogi_card_avayogi_note, avayogiVia6th)
    }

    private fun renderSummary(yogiLord: String, sahayogi: Planet, avayogiLord: String, yogiNak: String) {
        binding.summaryPrimary.text = getString(R.string.yogi_summary_primary, yogiLord)
        binding.summarySecondary.text = getString(R.string.yogi_summary_secondary, sahayogi.displayName, avayogiLord)
        binding.highlightChips.apply {
            removeAllViews()
            isVisible = true
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_point, yogiNak)))
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_sahayogi, sahayogi.displayName)))
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_avayogi, avayogiLord)))
        }
    }

    private fun createHighlightChip(text: String): Chip {
        return Chip(this).apply {
            this.text = text
            isCheckable = false
            isClickable = false
            setEnsureMinTouchTargetSize(false)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.card_bg_elevated)
            setTextColor(ContextCompat.getColor(context, R.color.primaryText))
        }
    }

    private fun showEmptyState(message: String?) {
        binding.summaryPrimary.text = getString(R.string.yogi_summary_title)
        binding.summarySecondary.text = message ?: getString(R.string.yogi_empty_state)
        binding.highlightChips.isVisible = false
        binding.emptyState.isVisible = true
        binding.emptyState.text = message ?: getString(R.string.yogi_empty_state)
    }

    private fun formatDegree(value: Double): String {
        val normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60.0).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format(Locale.US, "%03d° %02d'", degrees, minutes)
    }

    private fun houseFromAsc(sign: ZodiacSign, chart: ChartResult): Int {
        val asc = chart.ascendantSign.ordinal
        return 1 + (sign.ordinal - asc + 12) % 12
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
