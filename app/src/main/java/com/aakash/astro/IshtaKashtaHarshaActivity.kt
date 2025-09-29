package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.CompoundButton
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityIshtaKashtaHarshaBinding
import com.aakash.astro.databinding.ItemIshtaKashtaHarshaBinding
import java.time.Instant
import java.time.ZoneId

class IshtaKashtaHarshaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIshtaKashtaHarshaBinding
    private val accurate = AccurateCalculator()

    private var isDayFlag: Boolean = true
    private lateinit var details: BirthDetails
    private var chart: ChartResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIshtaKashtaHarshaBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val dateTime = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        details = BirthDetails(name, dateTime, lat, lon)
        chart = accurate.generateChart(details)

        binding.title.text = getString(R.string.ikh_title)
        binding.subtitle.text = name?.let { getString(R.string.chart_generated_for, it) } ?: ""

        binding.dayNightSwitch.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
            isDayFlag = isChecked
            render()
        }

        render()
    }

    private fun render() {
        val c = chart ?: run {
            binding.note.text = getString(R.string.dasha_engine_missing)
            return
        }
        val list = IshtaKashtaHarsha.compute(c, details, isDayFlag)
        val inflater = LayoutInflater.from(this)
        binding.container.removeAllViews()
        list.forEach { e ->
            val item = ItemIshtaKashtaHarshaBinding.inflate(inflater, binding.container, false)
            item.planetName.text = e.position.name
            item.posText.text = getString(
                R.string.pushkara_pos_format,
                e.position.sign.displayName,
                formatDeg(e.position.degree % 30.0)
            )
            item.ucchaText.text = getString(R.string.ikh_value_format, e.ucchaBala)
            item.cheText.text = getString(R.string.ikh_value_format, e.cheBala)
            item.ishtaText.text = getString(R.string.ikh_value_format, e.ishta)
            item.kashtaText.text = getString(R.string.ikh_value_format, e.kashta)
            item.harshalText.text = getString(R.string.ikh_harsha_format, e.harsha)
            item.hintsText.text = e.harshaChecks.joinToString(", ")
            binding.container.addView(item.root)
        }
    }

    private fun formatDeg(d: Double): String {
        val dN = if (d < 0) d + 30.0 else d
        val deg = dN.toInt()
        val minFloat = (dN - deg) * 60.0
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

