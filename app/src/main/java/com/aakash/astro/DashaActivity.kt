package com.aakash.astro

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityDashaBinding
import com.aakash.astro.databinding.ItemDashaBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DashaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

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
            binding.title.text = getString(R.string.dasha_title)
            binding.subtitle.text = getString(R.string.dasha_engine_missing)
            return
        }

        binding.title.text = getString(R.string.dasha_title)
        val fmt = DateTimeFormatter.ofPattern("dd MMM yyyy")
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""

        val moon = chart.planets.firstOrNull { it.planet == Planet.MOON }
        if (moon == null) {
            binding.subtitle.append("\nMoon not found in chart")
            return
        }

        val periods = DashaCalculator.vimshottariFrom(details, moon.degree)
        val inflater = LayoutInflater.from(this)
        binding.dashaContainer.removeAllViews()
        val items = mutableListOf<Pair<ItemDashaBinding, DashaPeriod>>()
        periods.forEach { p ->
            val item = ItemDashaBinding.inflate(inflater, binding.dashaContainer, false)
            item.dashaLord.text = p.lord
            item.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(p.start), fmt.format(p.end))
            item.root.setOnClickListener { toggleAntar(item, p, inflater, fmt) }
            binding.dashaContainer.addView(item.root)
            items += item to p
        }

        // Auto-expand to current maha-antar-pratyantar and highlight
        val now = Instant.now().atZone(zoneId)
        val maIndex = periods.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
        if (maIndex >= 0) {
            // Banner text
            val ma = periods[maIndex]
            val antars = DashaCalculator.antardashaFor(ma)
            val aIdx = antars.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
            var prName: String? = null
            if (aIdx >= 0) {
                val pr = DashaCalculator.pratyantarFor(antars[aIdx])
                val pIdx = pr.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                if (pIdx >= 0) prName = pr[pIdx].lord
            }
            val parts = mutableListOf<String>()
            parts += ma.lord
            if (aIdx >= 0) parts += antars[aIdx].lord
            prName?.let { parts += it }
            binding.todaysLine.text = parts.joinToString("-")
            binding.todaysCard.visibility = View.VISIBLE
        }
        if (maIndex >= 0) {
            val (maItem, maPeriod) = items[maIndex]
            // Highlight Mahadasha row
            maItem.root.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
            toggleAntar(maItem, maPeriod, inflater, fmt)
            val antars = DashaCalculator.antardashaFor(maPeriod)
            val antarIndex = antars.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
            if (antarIndex >= 0) {
                val antarChild = maItem.childrenContainer.getChildAt(antarIndex)
                if (antarChild != null) {
                    val antarBinding = ItemDashaBinding.bind(antarChild)
                    antarBinding.root.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
                    togglePratyantar(antarBinding, antars[antarIndex], inflater, fmt)
                    val praty = DashaCalculator.pratyantarFor(antars[antarIndex])
                    val pratyIndex = praty.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                    if (pratyIndex >= 0) {
                        val pratyChild = antarBinding.childrenContainer.getChildAt(pratyIndex)
                        pratyChild?.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
                        // Scroll into view
                        pratyChild?.post {
                            val rect = Rect(0, 0, pratyChild.width, pratyChild.height)
                            pratyChild.requestRectangleOnScreen(rect, true)
                        }
                    } else {
                        antarBinding.root.post {
                            val rect = Rect(0, 0, antarBinding.root.width, antarBinding.root.height)
                            antarBinding.root.requestRectangleOnScreen(rect, true)
                        }
                    }
                }
            } else {
                maItem.root.post {
                    val rect = Rect(0, 0, maItem.root.width, maItem.root.height)
                    maItem.root.requestRectangleOnScreen(rect, true)
                }
            }
        }
        // Ensure expansion and scroll after layout
        binding.dashaContainer.post {
            val now = Instant.now().atZone(zoneId)
            val maIndex2 = periods.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
            if (maIndex2 >= 0) {
                val ma = periods[maIndex2]
                val antars = DashaCalculator.antardashaFor(ma)
                val aIdx = antars.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                // Banner as well (post-layout)
                run {
                    var prName: String? = null
                    if (aIdx >= 0) {
                        val pr = DashaCalculator.pratyantarFor(antars[aIdx])
                        val pIdx2 = pr.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                        if (pIdx2 >= 0) prName = pr[pIdx2].lord
                    }
                    val parts2 = mutableListOf<String>()
                    parts2 += ma.lord
                    if (aIdx >= 0) parts2 += antars[aIdx].lord
                    prName?.let { parts2 += it }
                    binding.todaysLine.text = parts2.joinToString("-")
                    binding.todaysCard.visibility = View.VISIBLE
                }
                val (maItem, maPeriod) = items[maIndex2]
                maItem.root.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
                toggleAntar(maItem, maPeriod, inflater, fmt)
                if (aIdx >= 0) {
                    val antarChild = maItem.childrenContainer.getChildAt(aIdx)
                    if (antarChild != null) {
                        val antarBinding = ItemDashaBinding.bind(antarChild)
                        antarBinding.root.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
                        togglePratyantar(antarBinding, antars[aIdx], inflater, fmt)
                        val pr = DashaCalculator.pratyantarFor(antars[aIdx])
                        val pIdx = pr.indexOfFirst { !now.isBefore(it.start) && now.isBefore(it.end) }
                        if (pIdx >= 0) {
                            val pratyChild = antarBinding.childrenContainer.getChildAt(pIdx)
                            pratyChild?.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
                            pratyChild?.let { scrollToView(it) }
                        } else {
                            scrollToView(antarBinding.root)
                        }
                    } else {
                        scrollToView(maItem.root)
                    }
                } else {
                    scrollToView(maItem.root)
                }
            }
        }
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

    private fun toggleAntar(item: ItemDashaBinding, ma: DashaPeriod, inflater: LayoutInflater, fmt: DateTimeFormatter) {
        val container = item.childrenContainer
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return
        }
        if (container.childCount == 0) {
            val antars = DashaCalculator.antardashaFor(ma)
            antars.forEach { a ->
                val ai = ItemDashaBinding.inflate(inflater, container, false)
                ai.dashaLord.text = a.lord
                ai.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(a.start), fmt.format(a.end))
                ai.root.setPadding(ai.root.paddingLeft + dp(16), ai.root.paddingTop, ai.root.paddingRight, ai.root.paddingBottom)
                ai.root.setOnClickListener { togglePratyantar(ai, a, inflater, fmt) }
                container.addView(ai.root)
            }
        }
        container.visibility = View.VISIBLE
    }

    private fun togglePratyantar(item: ItemDashaBinding, antar: DashaPeriod, inflater: LayoutInflater, fmt: DateTimeFormatter) {
        val container = item.childrenContainer
        if (container.visibility == View.VISIBLE) {
            container.visibility = View.GONE
            return
        }
        if (container.childCount == 0) {
            val praty = DashaCalculator.pratyantarFor(antar)
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

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}



