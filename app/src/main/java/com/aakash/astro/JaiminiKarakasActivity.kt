package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.JaiminiKarakas
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.EphemerisPreparer
import com.aakash.astro.databinding.ActivityJaiminiKarakasBinding
import com.google.android.material.chip.Chip
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class JaiminiKarakasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityJaiminiKarakasBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJaiminiKarakasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.karakas_title)
        binding.subtitle.text = getString(R.string.karakas_subtitle)
        name?.let { binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it)) }

        val natal = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        if (natal == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            showEmptyState()
            return
        }

        renderKarakas(natal)
    }

    private fun renderKarakas(natal: ChartResult) {
        val karakas = JaiminiKarakas.compute(natal, includeRahuKetu = false)
        if (karakas.isEmpty()) {
            showEmptyState()
            return
        }

        binding.emptyState.isVisible = false
        val ak = karakas.firstOrNull()?.planet?.displayName ?: getString(R.string.karaka_summary_missing)
        val amk = karakas.getOrNull(1)?.planet?.displayName ?: getString(R.string.karaka_summary_missing)
        val bk = karakas.getOrNull(2)?.planet?.displayName ?: getString(R.string.karaka_summary_missing)
        binding.summaryPrimary.text = getString(R.string.karaka_summary_primary, ak)
        binding.summarySecondary.text = getString(R.string.karaka_summary_secondary, amk, bk)

        binding.highlightChips.apply {
            removeAllViews()
            isVisible = true
            karakas.take(3).forEach {
                addView(createHighlightChip(getString(R.string.karaka_chip_format, it.karakaName, it.planet.displayName)))
            }
        }

        val container: LinearLayout = binding.karakaList
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)
        karakas.forEach { entry ->
            val item = inflater.inflate(R.layout.item_karaka, container, false)
            item.findViewById<TextView>(R.id.karakaName).text = entry.karakaName
            item.findViewById<TextView>(R.id.karakaPlanet).text = entry.planet.displayName
            item.findViewById<TextView>(R.id.karakaPosition).text =
                getString(R.string.karaka_house_format, entry.sign.displayName, entry.house)
            val absDegree = formatDegree(entry.absoluteDegree)
            val signDegree = String.format(Locale.US, "%.2f°", entry.degreeInSign)
            item.findViewById<TextView>(R.id.karakaDegree).text =
                getString(R.string.karaka_degree_format, absDegree, signDegree)
            container.addView(item)
        }
    }

    private fun showEmptyState() {
        binding.summaryPrimary.text = getString(R.string.karaka_summary_placeholder)
        binding.summarySecondary.text = getString(R.string.karaka_summary_secondary_placeholder)
        binding.highlightChips.isVisible = false
        binding.emptyState.isVisible = true
        binding.karakaList.removeAllViews()
    }

    private fun createHighlightChip(text: String): Chip {
        return Chip(this).apply {
            this.text = text
            isClickable = false
            isCheckable = false
            setEnsureMinTouchTargetSize(false)
            chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.card_bg_elevated)
            setTextColor(ContextCompat.getColor(context, R.color.primaryText))
        }
    }

    private fun formatDegree(value: Double): String {
        val normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format(Locale.US, "%02d° %02d'", degrees, minutes)
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}


