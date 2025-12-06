package com.aakash.astro

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aakash.astro.EphemerisPreparer
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.YogiCalculator
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivityYogiBinding
import com.aakash.astro.databinding.ItemYogiRowBinding
import com.aakash.astro.databinding.ItemYogiSectionBinding
import com.google.android.material.chip.Chip
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class YogiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYogiBinding
    private val accurate = AccurateCalculator()
    private lateinit var detailAdapter: YogiSectionAdapter
    private var currentSections: List<YogiDetailSection> = emptyList()
    private val pinPrefs by lazy { getSharedPreferences("yogi_pins", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYogiBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        detailAdapter = YogiSectionAdapter()
        binding.detailList.apply {
            layoutManager = LinearLayoutManager(this@YogiActivity)
            adapter = detailAdapter
        }

        binding.yogiScroll.setOnScrollChangeListener { _, _, scrollY, _, _ ->
            val collapseRange = resources.displayMetrics.density * 240f
            val progress = (scrollY / collapseRange).coerceIn(0f, 1f)
            binding.yogiMotionLayout.progress = progress
        }

        binding.ctaShare.setOnClickListener { shareCurrentSummary() }
        binding.ctaPin.setOnClickListener { pinCurrentSummary() }

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

        val summary = YogiSummary(
            yogiLord = yogiLord,
            sahayogi = sahayogi,
            avayogiLord = avayogiLord,
            yogiNakshatra = yogiNak,
            name = name
        )
        renderSummary(summary)

        val sections = buildSections(
            yogiLord,
            yogiPoint,
            yogiNak,
            yogiSign,
            yogiHouse,
            sahayogi,
            avayogiLord,
            avayogiPoint,
            avayogiNak,
            avayogiSign,
            avayogiHouse,
            avayogiVia6th
        )
        currentSections = sections
        detailAdapter.submitList(sections)
        binding.emptyState.isVisible = false
        binding.detailList.isVisible = true
        enableCtas(true)
    }

    private fun renderSummary(summary: YogiSummary) {
        binding.summaryPrimary.text = getString(R.string.yogi_summary_primary, summary.yogiLord)
        val subtitle = summary.name?.let { getString(R.string.chart_generated_for, it) } ?: ""
        binding.summarySecondary.text = subtitle
        binding.highlightChips.apply {
            removeAllViews()
            isVisible = true
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_point, summary.yogiNakshatra)))
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_sahayogi, summary.sahayogi.displayName)))
            addView(createHighlightChip(getString(R.string.yogi_highlight_chip_avayogi, summary.avayogiLord)))
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

    private fun buildSections(
        yogiLord: String,
        yogiPoint: Double,
        yogiNak: String,
        yogiSign: ZodiacSign,
        yogiHouse: Int,
        sahayogi: Planet,
        avayogiLord: String,
        avayogiPoint: Double,
        avayogiNak: String,
        avayogiSign: ZodiacSign,
        avayogiHouse: Int,
        avayogiVia6th: String
    ): List<YogiDetailSection> {
        val yogiSection = YogiDetailSection(
            title = getString(R.string.yogi_section_yogi),
            rows = listOf(
                YogiDetailRow(getString(R.string.yogi_row_lord), yogiLord),
                YogiDetailRow(getString(R.string.yogi_row_point), getString(R.string.yogi_point_format, formatDegree(yogiPoint), yogiNak)),
                YogiDetailRow(getString(R.string.yogi_row_nakshatra), yogiNak),
                YogiDetailRow(
                    getString(R.string.yogi_row_sign),
                    getString(R.string.karaka_house_format, yogiSign.displayName, yogiHouse)
                )
            )
        )

        val sahayogiSection = YogiDetailSection(
            title = getString(R.string.yogi_section_sahayogi),
            rows = listOf(
                YogiDetailRow(getString(R.string.yogi_row_sahayogi), sahayogi.displayName),
                YogiDetailRow(
                    getString(R.string.yogi_row_sahayogi_sign),
                    getString(R.string.karaka_house_format, yogiSign.displayName, yogiHouse)
                )
            )
        )

        val avayogiSection = YogiDetailSection(
            title = getString(R.string.yogi_section_avayogi),
            rows = listOf(
                YogiDetailRow(getString(R.string.yogi_row_avayogi), avayogiLord),
                YogiDetailRow(
                    getString(R.string.yogi_row_avayogi_point),
                    getString(R.string.yogi_point_format, formatDegree(avayogiPoint), avayogiNak)
                ),
                YogiDetailRow(
                    getString(R.string.yogi_row_avayogi_sign),
                    getString(R.string.karaka_house_format, avayogiSign.displayName, avayogiHouse)
                ),
                YogiDetailRow(getString(R.string.yogi_row_avayogi_via), avayogiVia6th)
            )
        )

        return listOf(yogiSection, sahayogiSection, avayogiSection)
    }

    private fun shareCurrentSummary() {
        val sections = currentSections
        if (sections.isEmpty()) return
        val sb = StringBuilder()
        sb.appendLine(getString(R.string.yogi_title))
        sections.forEach { section ->
            sb.appendLine(section.title)
            section.rows.forEach { row ->
                sb.appendLine("• ${row.label}: ${row.value}")
            }
            sb.appendLine()
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_TEXT, sb.toString())
        startActivity(Intent.createChooser(intent, getString(R.string.share_result)))
    }

    private fun pinCurrentSummary() {
        val sections = currentSections
        if (sections.isEmpty()) return
        val pinnedText = buildString {
            appendLine(getString(R.string.yogi_title))
            sections.forEach { section ->
                appendLine(section.title)
            }
        }
        pinPrefs.edit().putString("pinned_yogi", pinnedText).apply()
        Snackbar.make(binding.root, getString(R.string.yogi_pinned_message), Snackbar.LENGTH_SHORT).show()
    }

    private fun showEmptyState(message: String?) {
        binding.summaryPrimary.text = getString(R.string.yogi_summary_title)
        binding.summarySecondary.text = message ?: getString(R.string.yogi_empty_state)
        binding.highlightChips.isVisible = false
        binding.emptyState.isVisible = true
        binding.emptyState.text = message ?: getString(R.string.yogi_empty_state)
        binding.detailList.isVisible = false
        currentSections = emptyList()
        enableCtas(false)
    }

    private fun enableCtas(enabled: Boolean) {
        binding.ctaShare.isEnabled = enabled
        binding.ctaPin.isEnabled = enabled
    }

    private fun formatDegree(value: Double): String {
        val normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60.0).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format(Locale.US, "%03d\u00B0 %02d'", degrees, minutes)
    }

    private fun houseFromAsc(sign: ZodiacSign, chart: ChartResult): Int {
        val asc = chart.ascendantSign.ordinal
        return 1 + (sign.ordinal - asc + 12) % 12
    }

    private data class YogiDetailSection(val title: String, val rows: List<YogiDetailRow>)

    private data class YogiDetailRow(val label: String, val value: String)

    private data class YogiSummary(
        val yogiLord: String,
        val sahayogi: Planet,
        val avayogiLord: String,
        val yogiNakshatra: String,
        val name: String?
    )

    private inner class YogiSectionAdapter : RecyclerView.Adapter<YogiSectionAdapter.VH>() {
        private var items: List<YogiDetailSection> = emptyList()

        fun submitList(newItems: List<YogiDetailSection>) {
            items = newItems
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val binding = ItemYogiSectionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VH(binding)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.bind(items[position])
        }

        override fun getItemCount(): Int = items.size

        inner class VH(private val sectionBinding: ItemYogiSectionBinding) : RecyclerView.ViewHolder(sectionBinding.root) {
            fun bind(section: YogiDetailSection) {
                sectionBinding.sectionTitle.text = section.title
                val container = sectionBinding.rowContainer
                container.removeAllViews()
                val inflater = LayoutInflater.from(container.context)
                section.rows.forEach { row ->
                    val rowBinding = ItemYogiRowBinding.inflate(inflater, container, false)
                    rowBinding.rowLabel.text = row.label
                    rowBinding.rowValue.text = row.value
                    container.addView(rowBinding.root)
                }
            }
        }
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
