package com.aakash.astro

import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityJaiminiKarakasBinding
import java.time.Instant
import java.time.ZoneId

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
            return
        }

        renderKarakas(natal)
    }

    private fun renderKarakas(natal: ChartResult) {
        val table = binding.karakaTable
        fun addRow(cells: List<String>) {
            val row = TableRow(this)
            cells.forEach { text ->
                val tv = TextView(this)
                tv.text = text
                tv.setPadding(8, 8, 8, 8)
                row.addView(tv)
            }
            table.addView(row)
        }

        val list = JaiminiKarakas.compute(natal, includeRahuKetu = false)
        list.forEach { e ->
            addRow(
                listOf(
                    e.karakaName,
                    e.planet.displayName,
                    "${e.sign.displayName} (H${e.house})",
                    String.format("%s", String.format("%.2f°", e.degreeInSign))
                )
            )
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

