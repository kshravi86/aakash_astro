package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.databinding.ActivityTransitBinding
import java.time.Instant
import java.time.ZoneId

class TransitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransitBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        // Transit time: current instant in provided/system zone
        val now = Instant.now().atZone(zoneId)
        val details = BirthDetails(name, now, lat, lon)

        val chart = accurate.generateChart(details)
        binding.title.text = getString(R.string.transit_title)
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""
        if (chart == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        binding.vedicChartView.setChart(chart)
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

