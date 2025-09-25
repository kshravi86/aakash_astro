package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.Varga
import com.aakash.astro.astrology.VargaCalculator
import com.aakash.astro.databinding.ActivityDivisionalChartsBinding
import com.aakash.astro.ui.VedicChartView
import java.time.Instant
import java.time.ZoneId

class DivisionalChartsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDivisionalChartsBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDivisionalChartsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        binding.title.text = getString(R.string.varga_title)
        name?.let { binding.subtitle.text = getString(R.string.chart_generated_for, it) }

        val natal = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        if (natal == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        renderAllVargas(natal)
    }

    private fun renderAllVargas(natal: ChartResult) {
        val container: LinearLayout = binding.listContainer
        container.removeAllViews()
        val inflater = LayoutInflater.from(this)

        val vargas = listOf(
            Varga.D1, Varga.D2, Varga.D3, Varga.D4, Varga.D7, Varga.D9, Varga.D10, Varga.D12,
            Varga.D16, Varga.D20, Varga.D24, Varga.D27, Varga.D30, Varga.D40, Varga.D45, Varga.D60
        )

        vargas.forEach { v ->
            val card = inflater.inflate(R.layout.item_varga_chart, container, false) as LinearLayout
            val title = card.findViewById<TextView>(R.id.vargaTitle)
            val subtitle = card.findViewById<TextView>(R.id.vargaSubtitle)
            val chartView = card.findViewById<VedicChartView>(R.id.vargaChart)
            title.text = v.code
            subtitle.text = VargaCalculator.description(v)
            val vc = VargaCalculator.computeVargaChart(natal, v, natal.ascendantDegree)
            chartView.setChart(vc)
            container.addView(card)
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
