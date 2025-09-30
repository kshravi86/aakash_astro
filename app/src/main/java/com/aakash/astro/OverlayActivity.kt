package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityOverlayBinding
import java.time.Instant
import java.time.ZoneId

class OverlayActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOverlayBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        // Natal inputs
        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.overlay_title)
        binding.subtitle.text = getString(R.string.overlay_subtitle)
        name?.let {
            binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it))
        }

        val natalDetails = BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon)
        val transitDetails = BirthDetails(name, Instant.now().atZone(zoneId), lat, lon)

        val natal = accurate.generateChart(natalDetails)
        val transit = accurate.generateChart(transitDetails)
        if (natal == null || transit == null) {
            binding.subtitle.append("\n" + getString(R.string.overlay_engine_missing))
            return
        }

        // Extract transit Saturn and Jupiter
        val trSaturn = transit.planets.firstOrNull { it.planet == Planet.SATURN }
        val trJupiter = transit.planets.firstOrNull { it.planet == Planet.JUPITER }

        // Build an overlay chart anchored to natal ascendant; only Sa/Ju as planets
        val overlayPlanets = mutableListOf<PlanetPosition>()
        trSaturn?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            overlayPlanets += PlanetPosition(
                planet = Planet.SATURN,
                degree = it.degree,
                sign = it.sign,
                house = h,
                isRetrograde = it.isRetrograde
            )
        }
        trJupiter?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            overlayPlanets += PlanetPosition(
                planet = Planet.JUPITER,
                degree = it.degree,
                sign = it.sign,
                house = h,
                isRetrograde = it.isRetrograde
            )
        }

        val overlayChart = ChartResult(
            ascendantDegree = natal.ascendantDegree,
            ascendantSign = natal.ascendantSign,
            houses = natal.houses,
            planets = overlayPlanets
        )

        binding.vedicChartView.setChart(overlayChart)

        // Summary lines: "Saturn in Aquarius → House 11" etc.
        binding.summaryContainer.removeAllViews()
        fun addLine(label: String) {
            val tv = android.widget.TextView(this)
            tv.text = label
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            binding.summaryContainer.addView(tv)
        }
        trSaturn?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addLine(getString(R.string.overlay_line, "Saturn", it.sign.displayName, h))
        }
        trJupiter?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addLine(getString(R.string.overlay_line, "Jupiter", it.sign.displayName, h))
        }

        // Build impacts table (1, 5, 9 from the transit planet's house)
        val table = binding.impactTable
        fun impactedHouses(fromHouse: Int): List<Int> = listOf(
            fromHouse,
            ((fromHouse + 4 - 1) % 12) + 1,
            ((fromHouse + 8 - 1) % 12) + 1
        )
        fun addRow(planetName: String, signName: String, house: Int, impacted: List<Int>) {
            val row = android.widget.TableRow(this)
            fun cell(text: String): android.widget.TextView = android.widget.TextView(this).apply {
                this.text = text
                setPadding(8, 8, 8, 8)
            }
            row.addView(cell(planetName))
            row.addView(cell(signName))
            row.addView(cell(house.toString()))
            row.addView(cell(impacted.joinToString(", ")))
            table.addView(row)
        }

        trSaturn?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addRow("Saturn", it.sign.displayName, h, impactedHouses(h))
        }
        trJupiter?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addRow("Jupiter", it.sign.displayName, h, impactedHouses(h))
        }

        // Next table: natal planets present in each impacted house
        val planetTable = binding.impactPlanetTable
        val natalByHouse = natal.planets.groupBy { it.house }
        fun addPlanetRow(tpName: String, signName: String, impactedHouse: Int, natalPlanets: List<PlanetPosition>) {
            val row = android.widget.TableRow(this)
            fun cell(text: String): android.widget.TextView = android.widget.TextView(this).apply {
                this.text = text
                setPadding(8, 8, 8, 8)
            }
            val names = if (natalPlanets.isEmpty()) getString(R.string.none)
            else natalPlanets.joinToString(", ") { it.name }
            row.addView(cell(tpName))
            row.addView(cell(signName))
            row.addView(cell(impactedHouse.toString()))
            row.addView(cell(names))
            planetTable.addView(row)
        }

        fun processTransit(planetName: String, signName: String, baseHouse: Int) {
            val impacted = impactedHouses(baseHouse)
            impacted.forEach { h ->
                val natalPlanets = natalByHouse[h].orEmpty()
                addPlanetRow(planetName, signName, h, natalPlanets)
            }
        }

        trSaturn?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            processTransit("Saturn", it.sign.displayName, h)
        }
        trJupiter?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            processTransit("Jupiter", it.sign.displayName, h)
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
