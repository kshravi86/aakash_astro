package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityPushkaraNavamshaBinding
import com.aakash.astro.databinding.ItemPushkaraBinding
import java.time.Instant
import java.time.ZoneId

class PushkaraNavamshaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPushkaraNavamshaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPushkaraNavamshaBinding.inflate(layoutInflater)
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
        binding.title.text = getString(R.string.pushkara_title)
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""
        if (chart == null) {
            binding.note.text = getString(R.string.dasha_engine_missing)
            return
        }

        render(chart)
    }

    private fun render(chart: ChartResult) {
        val inflater = LayoutInflater.from(this)
        binding.pushkaraContainer.removeAllViews()
        // Add Ascendant (Lagna)
        run {
            val degInSign = (chart.ascendantDegree % 30.0).let { if (it < 0) it + 30.0 else it }
            val band = PushkaraNavamsha.bandIfPushkara(chart.ascendantSign, degInSign)
            val item = ItemPushkaraBinding.inflate(inflater, binding.pushkaraContainer, false)
            item.planetName.text = "Ascendant (Lagna)"
            item.planetDetails.text = getString(
                R.string.pushkara_pos_format,
                chart.ascendantSign.displayName,
                formatDeg(degInSign)
            )
            if (band != null) {
                item.pushkaraStatus.text = getString(R.string.pushkara_yes)
                item.pushkaraBand.text = band
                item.indicator.visibility = View.VISIBLE
            } else {
                item.pushkaraStatus.text = getString(R.string.pushkara_no)
                item.pushkaraBand.text = ""
                item.indicator.visibility = View.GONE
            }
            binding.pushkaraContainer.addView(item.root)
        }

        // Add all 9 planets
        chart.planets.forEach { p ->
            val degInSign = (p.degree % 30.0).let { if (it < 0) it + 30.0 else it }
            val band = PushkaraNavamsha.bandIfPushkara(p.sign, degInSign)
            val item = ItemPushkaraBinding.inflate(inflater, binding.pushkaraContainer, false)
            item.planetName.text = p.name
            item.planetDetails.text = getString(
                R.string.pushkara_pos_format,
                p.sign.displayName,
                formatDeg(degInSign)
            )
            if (band != null) {
                item.pushkaraStatus.text = getString(R.string.pushkara_yes)
                item.pushkaraBand.text = band
                item.indicator.visibility = View.VISIBLE
            } else {
                item.pushkaraStatus.text = getString(R.string.pushkara_no)
                item.pushkaraBand.text = ""
                item.indicator.visibility = View.GONE
            }
            binding.pushkaraContainer.addView(item.root)
        }
    }

    private fun formatDeg(d: Double): String {
        val deg = d.toInt()
        val minFloat = (d - deg) * 60.0
        val min = minFloat.toInt()
        val sec = ((minFloat - min) * 60.0).toInt()
        return String.format("%02d° %02d' %02d\"", deg, min, sec)
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
