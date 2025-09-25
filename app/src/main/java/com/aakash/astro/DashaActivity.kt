package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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
        periods.forEach { p ->
            val item = ItemDashaBinding.inflate(inflater, binding.dashaContainer, false)
            item.dashaLord.text = p.lord
            item.dashaRange.text = getString(R.string.dasha_range_format, fmt.format(p.start), fmt.format(p.end))
            item.root.setOnClickListener { toggleAntar(item, p, inflater, fmt) }
            binding.dashaContainer.addView(item.root)
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

