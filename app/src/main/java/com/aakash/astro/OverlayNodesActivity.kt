package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityOverlayNodesBinding
import java.time.Instant
import java.time.ZoneId

class OverlayNodesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOverlayNodesBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOverlayNodesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.overlay_nodes_title)
        binding.subtitle.text = getString(R.string.overlay_nodes_subtitle)
        name?.let { binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it)) }

        val natalDetails = BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon)
        val transitDetails = BirthDetails(name, Instant.now().atZone(zoneId), lat, lon)

        val natal = accurate.generateChart(natalDetails)
        val transit = accurate.generateChart(transitDetails)
        if (natal == null || transit == null) {
            binding.subtitle.append("\n" + getString(R.string.overlay_engine_missing))
            return
        }

        val trRahu = transit.planets.firstOrNull { it.planet == Planet.RAHU }
        val trKetu = transit.planets.firstOrNull { it.planet == Planet.KETU }

        val overlayPlanets = mutableListOf<PlanetPosition>()
        trRahu?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            overlayPlanets += PlanetPosition(Planet.RAHU, it.degree, it.sign, h, it.isRetrograde)
        }
        trKetu?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            overlayPlanets += PlanetPosition(Planet.KETU, it.degree, it.sign, h, it.isRetrograde)
        }

        val overlayChart = ChartResult(
            ascendantDegree = natal.ascendantDegree,
            ascendantSign = natal.ascendantSign,
            houses = natal.houses,
            planets = overlayPlanets
        )
        binding.vedicChartView.setChart(overlayChart)

        // Summary lines
        binding.summaryContainer.removeAllViews()
        fun addLine(label: String) {
            val tv = android.widget.TextView(this)
            tv.text = label
            tv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
            binding.summaryContainer.addView(tv)
        }
        trRahu?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addLine(getString(R.string.overlay_line, "Rahu", it.sign.displayName, h))
        }
        trKetu?.let {
            val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12
            addLine(getString(R.string.overlay_line, "Ketu", it.sign.displayName, h))
        }

        // Impacts table (1,5,9)
        val table = binding.impactTable
        fun impactedHouses(fromHouse: Int): List<Int> = listOf(
            fromHouse,
            ((fromHouse + 4 - 1) % 12) + 1,
            ((fromHouse + 8 - 1) % 12) + 1
        )
        fun addRow(planetName: String, signName: String, house: Int, impacted: List<Int>) {
            val row = android.widget.TableRow(this)
            fun cell(text: String) = android.widget.TextView(this).apply { this.text = text; setPadding(8,8,8,8) }
            row.addView(cell(planetName))
            row.addView(cell(signName))
            row.addView(cell(house.toString()))
            row.addView(cell(impacted.joinToString(", ")))
            table.addView(row)
        }
        trRahu?.let { val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12; addRow("Rahu", it.sign.displayName, h, impactedHouses(h)) }
        trKetu?.let { val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12; addRow("Ketu", it.sign.displayName, h, impactedHouses(h)) }

        // Natal planets in impacted houses table
        val planetTable = binding.impactPlanetTable
        val natalByHouse = natal.planets.groupBy { it.house }
        fun addPlanetRow(tpName: String, signName: String, impactedHouse: Int, natalPlanets: List<PlanetPosition>) {
            val row = android.widget.TableRow(this)
            fun cell(text: String) = android.widget.TextView(this).apply { this.text = text; setPadding(8,8,8,8) }
            val names = if (natalPlanets.isEmpty()) getString(R.string.none) else natalPlanets.joinToString(", ") { it.name }
            row.addView(cell(tpName))
            row.addView(cell(signName))
            row.addView(cell(impactedHouse.toString()))
            row.addView(cell(names))
            planetTable.addView(row)
        }
        fun processTransit(planetName: String, signName: String, baseHouse: Int) {
            impactedHouses(baseHouse).forEach { h -> addPlanetRow(planetName, signName, h, natalByHouse[h].orEmpty()) }
        }
        trRahu?.let { val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12; processTransit("Rahu", it.sign.displayName, h) }
        trKetu?.let { val h = 1 + (it.sign.ordinal - natal.ascendantSign.ordinal + 12) % 12; processTransit("Ketu", it.sign.displayName, h) }
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}

