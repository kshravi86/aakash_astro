package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityDashaBinding
import java.time.Instant
import java.time.ZoneId

class YogiActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDashaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        binding.topBar.title = getString(R.string.yogi_title)

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
            binding.title.text = getString(R.string.yogi_title)
            binding.subtitle.text = getString(R.string.dasha_engine_missing)
            return
        }

        binding.title.text = getString(R.string.yogi_title)
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""

        val sun = chart.planets.firstOrNull { it.planet == Planet.SUN } ?: return
        val moon = chart.planets.firstOrNull { it.planet == Planet.MOON } ?: return

        val yogiPoint = YogiCalculator.computeYogiPoint(sun.degree, moon.degree)
        val yogiNak = NakshatraCalc.fromLongitude(yogiPoint).first
        val yogiLord = YogiCalculator.nakshatraLordOf(yogiPoint)
        val yogiSign = YogiCalculator.signOf(yogiPoint)
        val sahayogi = YogiCalculator.signLordOf(yogiSign)
        val avayogiPoint = YogiCalculator.computeAvayogiPoint(yogiPoint)
        val avayogiLord = YogiCalculator.nakshatraLordOf(avayogiPoint)
        val avayogiVia6th = YogiCalculator.avayogiBy6th(yogiLord)

        val list: LinearLayout = binding.dashaContainer
        list.removeAllViews()
        val inflater = LayoutInflater.from(this)

        fun addRow(label: String, value: String) {
            val row = inflater.inflate(R.layout.item_sign_value, list, false)
            row.findViewById<TextView>(R.id.signName).text = label
            row.findViewById<TextView>(R.id.signValue).text = value
            list.addView(row)
        }

        addRow(getString(R.string.yogi_point_label), formatDegree(yogiPoint) + "  •  " + yogiNak)
        addRow(getString(R.string.yogi_planet_label), yogiLord)
        addRow(getString(R.string.sahayogi_label), sahayogi.displayName)
        addRow(getString(R.string.avayogi_point_label), formatDegree(avayogiPoint))
        addRow(getString(R.string.avayogi_planet_label), avayogiLord + "  (6th: " + avayogiVia6th + ")")
    }

    private fun formatDegree(value: Double): String {
        val normalized = ((value % 360.0) + 360.0) % 360.0
        val degrees = normalized.toInt()
        val minutes = ((normalized - degrees) * 60.0).toInt()
        return String.format("%03d° %02d'", degrees, minutes)
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

