package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityYogasBinding
import java.time.Instant
import java.time.ZoneId

class YogasActivity : AppCompatActivity() {
    private lateinit var binding: ActivityYogasBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityYogasBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
            return
        }

        renderYogas(natal)
    }

    private fun renderYogas(natal: ChartResult) {
        val container: LinearLayout = binding.yogaList
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val yogas = YogaDetector.detect(natal)
        if (yogas.isEmpty()) {
            val tv = TextView(this)
            tv.text = getString(R.string.yogas_none)
            container.addView(tv)
            return
        }

        yogas.forEach { y ->
            val item = inflater.inflate(R.layout.item_yoga, container, false)
            val title = item.findViewById<TextView>(R.id.yogaTitle)
            val desc = item.findViewById<TextView>(R.id.yogaDescription)
            title.text = y.name
            desc.text = y.description
            container.addView(item)
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

