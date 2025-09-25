package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
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

        // Obtain birth details from intent
        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        // Prepare ephemeris path if available (copied by EphemerisPreparer)
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
            binding.dashaContainer.addView(item.root)
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

