package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityAshtakavargaBavBinding
import java.time.Instant
import java.time.ZonedDateTime
import java.time.ZoneId

class AshtakavargaBavActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAshtakavargaBavBinding
    private val accurate = AccurateCalculator()
    private lateinit var zoneIdExtra: ZoneId
    private var latExtra: Double = 0.0
    private var lonExtra: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAshtakavargaBavBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        zoneIdExtra = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        latExtra = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        lonExtra = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneIdExtra)
        val chart = accurate.generateChart(BirthDetails(name, zdt, latExtra, lonExtra))
        if (chart == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        renderBAV(chart)
    }

    private fun renderBAV(chart: ChartResult) {
        val container: LinearLayout = binding.sectionsContainer
        val inflater = LayoutInflater.from(this)
        val refs = listOf(
            Planet.SUN, Planet.MOON, Planet.MARS, Planet.MERCURY, Planet.JUPITER, Planet.VENUS, Planet.SATURN
        )

        // Compute current transit positions for highlighting using current time and provided location
        val nowZdt = ZonedDateTime.now(zoneIdExtra)
        val transitChart = accurate.generateChart(BirthDetails("Transit", nowZdt, latExtra, lonExtra))

        refs.forEach { ref ->
            val section = inflater.inflate(R.layout.item_sav_section, container, false)
            val title = section.findViewById<TextView>(R.id.sectionTitle)
            title.text = ref.displayName + " BAV"
            val chartView = section.findViewById<com.aakash.astro.ui.SavChartView>(R.id.sectionChart)
            val bav = AshtakavargaCalc.computeBAVFor(ref, chart).values
            chartView.setSav(bav)
            // Highlight the transit sign of this planet if available
            transitChart?.planets?.firstOrNull { it.planet == ref }?.let { pos ->
                chartView.setHighlight(pos.sign)
            }
            container.addView(section)
        }

        // Lagna BAV (LAV)
        run {
            val section = inflater.inflate(R.layout.item_sav_section, container, false)
            val title = section.findViewById<TextView>(R.id.sectionTitle)
            title.text = getString(R.string.lagna_bav_title)
            val chartView = section.findViewById<com.aakash.astro.ui.SavChartView>(R.id.sectionChart)
            val bav = AshtakavargaCalc.computeLagnaBAV(chart).values
            chartView.setSav(bav)
            // No transit highlight for Lagna BAV per request (planets only)
            container.addView(section)
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
