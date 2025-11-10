package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityYogasBinding
import com.google.android.material.chip.Chip
import java.time.Instant
import java.time.ZoneId

class YogasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYogasBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYogasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.yogas_title)
        binding.subtitle.text = getString(R.string.yogas_subtitle)
        name?.let { binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it)) }

        val natal = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        if (natal == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            showEmptyState()
            return
        }

        renderYogas(natal)
    }

    private fun renderYogas(natal: ChartResult) {
        val yogas = YogaDetector.detect(natal)
        renderSummary(yogas)

        val container: LinearLayout = binding.yogaList
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        if (yogas.isEmpty()) {
            showEmptyState()
            return
        }
        binding.emptyState.isVisible = false

        yogas.forEachIndexed { index, yoga ->
            val item = inflater.inflate(R.layout.item_yoga, container, false)
            item.findViewById<TextView>(R.id.yogaTitle).text = yoga.name
            item.findViewById<TextView>(R.id.yogaDescription).text = yoga.description
            item.findViewById<TextView>(R.id.yogaBadge).text = getString(R.string.yogas_badge_format, index + 1)
            item.findViewById<TextView>(R.id.yogaCategory).text = extractCategory(yoga.name)
            container.addView(item)
        }
    }

    private fun renderSummary(yogas: List<YogaResult>) {
        if (yogas.isEmpty()) {
            binding.summaryCount.text = getString(R.string.yogas_total_placeholder)
            binding.summaryLine.text = getString(R.string.yogas_summary_none)
            binding.highlightChips.isVisible = false
            return
        }

        binding.summaryCount.text = getString(R.string.yogas_summary_count, yogas.size)
        binding.summaryLine.text = getString(R.string.yogas_summary_hint)
        val chips = binding.highlightChips
        chips.removeAllViews()
        chips.isVisible = true
        yogas.take(3).forEach { chips.addView(createHighlightChip(it.name)) }
    }

    private fun showEmptyState() {
        binding.emptyState.isVisible = true
        binding.summaryCount.text = getString(R.string.yogas_total_placeholder)
        binding.summaryLine.text = getString(R.string.yogas_summary_none)
        binding.highlightChips.isVisible = false
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

    private fun extractCategory(name: String): String {
        val token = name.split(" ", limit = 2)
            .firstOrNull { it.isNotBlank() && !it.equals("Yoga", ignoreCase = true) }
        return token ?: getString(R.string.yogas_category_generic)
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

