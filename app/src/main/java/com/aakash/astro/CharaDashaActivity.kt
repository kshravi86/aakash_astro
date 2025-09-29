package com.aakash.astro

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityCharaDashaBinding
import com.aakash.astro.databinding.ItemDashaBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class CharaDashaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCharaDashaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCharaDashaBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            binding.title.text = getString(R.string.chara_dasha_title)
            binding.subtitle.text = getString(R.string.dasha_engine_missing)
            return
        }

        binding.title.text = getString(R.string.chara_dasha_title)
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""

        val forward = run {
            val asc = chart.ascendantSign
            val ninth = signAtOffset(asc, +8)
            isSavya(ninth)
        }

        val periods = CharaDasha.compute(chart, details.dateTime)
        val inflater = LayoutInflater.from(this)
        binding.dashaContainer.removeAllViews()
        val items = mutableListOf<Pair<ItemDashaBinding, DashaPeriod>>()
        periods.forEach { p ->
            val item = ItemDashaBinding.inflate(inflater, binding.dashaContainer, false)
            item.dashaLord.text = p.lord
            item.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(p.start), fmt.format(p.end))
            item.root.setOnClickListener { toggleAntar(item, p, inflater, fmt, forward) }
            binding.dashaContainer.addView(item.root)
            items += item to p
        }

        // Auto-expand to current maha/antar/pratyantar and highlight
        val now = Instant.now().atZone(zoneId)
        val maIndex = periods.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
        if (maIndex >= 0) {
            val ma = periods[maIndex]
            val antars = CharaDasha.antardashaFor(ma, forward)
            val antarIdx = antars.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
            var prName: String? = null
            if (antarIdx >= 0) {
                val pr = CharaDasha.pratyantarFor(antars[antarIdx], forward)
                val pIdx = pr.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                if (pIdx >= 0) prName = pr[pIdx].lord
            }
            val parts = mutableListOf<String>()
            parts += ma.lord
            if (antarIdx >= 0) parts += CharaDasha.antardashaFor(ma, forward)[antarIdx].lord
            prName?.let { parts += it }
            binding.todaysLine.text = parts.joinToString("-")
            binding.todaysCard.visibility = View.VISIBLE
        }

        if (maIndex >= 0) {
            val (maItem, maPeriod) = items[maIndex]
            maItem.root.setBackgroundColor(getColor(R.color.surface_variant))
            toggleAntar(maItem, maPeriod, inflater, fmt, forward)
            val antars = CharaDasha.antardashaFor(maPeriod, forward)
            val antarIndex = antars.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
            if (antarIndex >= 0) {
                val antarChild = maItem.childrenContainer.getChildAt(antarIndex)
                if (antarChild != null) {
                    val antarBinding = ItemDashaBinding.bind(antarChild)
                    antarBinding.root.setBackgroundColor(getColor(R.color.surface_variant))
                    togglePratyantar(antarBinding, antars[antarIndex], inflater, fmt, forward)
                }
            }
        }
    }

    private fun toggleAntar(
        item: ItemDashaBinding,
        ma: DashaPeriod,
        inflater: LayoutInflater,
        fmt: java.time.format.DateTimeFormatter,
        forward: Boolean
    ) {
        val container = item.childrenContainer
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return
        }
        if (container.childCount == 0) {
            val antars = CharaDasha.antardashaFor(ma, forward)
            antars.forEach { a ->
                val ai = ItemDashaBinding.inflate(inflater, container, false)
                ai.dashaLord.text = a.lord
                ai.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(a.start), fmt.format(a.end))
                ai.root.setPadding(ai.root.paddingLeft + dp(16), ai.root.paddingTop, ai.root.paddingRight, ai.root.paddingBottom)
                ai.root.setOnClickListener { togglePratyantar(ai, a, inflater, fmt, forward) }
                container.addView(ai.root)
            }
        }
        container.visibility = View.VISIBLE
    }

    private fun togglePratyantar(
        item: ItemDashaBinding,
        antar: DashaPeriod,
        inflater: LayoutInflater,
        fmt: java.time.format.DateTimeFormatter,
        forward: Boolean
    ) {
        val container = item.childrenContainer
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return
        }
        if (container.childCount == 0) {
            val praty = CharaDasha.pratyantarFor(antar, forward)
            praty.forEach { pr ->
                val pi = ItemDashaBinding.inflate(inflater, container, false)
                pi.dashaLord.text = pr.lord
                pi.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(pr.start), fmt.format(pr.end))
                pi.root.setPadding(pi.root.paddingLeft + dp(32), pi.root.paddingTop, pi.root.paddingRight, pi.root.paddingBottom)
                container.addView(pi.root)
            }
        }
        container.visibility = View.VISIBLE
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }

    private fun scrollToView(view: View) {
        try {
            val rect = Rect()
            view.getDrawingRect(rect)
            (view.parent as? ViewGroup)?.offsetDescendantRectToMyCoords(view, rect)
            binding.scrollRoot.smoothScrollTo(0, rect.top)
        } catch (_: Throwable) {
            view.requestRectangleOnScreen(Rect(0, 0, view.width, view.height), true)
        }
    }

    private fun isSavya(sign: ZodiacSign): Boolean {
        return sign in setOf(
            ZodiacSign.ARIES, ZodiacSign.TAURUS, ZodiacSign.GEMINI,
            ZodiacSign.LIBRA, ZodiacSign.SCORPIO, ZodiacSign.SAGITTARIUS
        )
    }

    private fun signAtOffset(from: ZodiacSign, offset: Int): ZodiacSign {
        val idx = (from.ordinal + offset) % 12
        return ZodiacSign.entries[if (idx < 0) idx + 12 else idx]
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

