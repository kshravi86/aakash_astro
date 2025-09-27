package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.AshtakavargaCalc
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivitySarvaAshtakavargaBinding
import java.time.Instant
import java.time.ZoneId

class SarvaAshtakavargaActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySarvaAshtakavargaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySarvaAshtakavargaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            binding.engineNote.text = getString(R.string.transit_engine_missing)
            return
        }

        val sav = AshtakavargaCalc.computeSarva(chart)
        val total = sav.total()
        binding.summary.text = getString(R.string.sav_summary_format, total)

        val grid: GridLayout = binding.grid
        grid.removeAllViews()
        val inflater = LayoutInflater.from(this)
        for (i in 0 until 12) {
            val item = inflater.inflate(R.layout.item_sav_cell, grid, false)
            val signName = item.findViewById<TextView>(R.id.savSign)
            val value = item.findViewById<TextView>(R.id.savValue)
            val sign = ZodiacSign.entries[i]
            signName.text = sign.displayName
            value.text = sav.values[i].toString()
            grid.addView(item)
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

