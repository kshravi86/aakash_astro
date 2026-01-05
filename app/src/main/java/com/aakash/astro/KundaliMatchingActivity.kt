package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.AstrologyCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.GunMilanCalculator
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.PlanetPosition
import com.aakash.astro.databinding.ActivityKundaliMatchingBinding
import com.aakash.astro.databinding.ItemKootaRowBinding
import com.aakash.astro.databinding.ItemSynastryRowBinding
import com.aakash.astro.storage.SavedHoroscope
import com.aakash.astro.storage.SavedStore
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.ZoneId
import kotlin.math.abs

class KundaliMatchingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKundaliMatchingBinding
    private val accurate = AccurateCalculator()
    private val fallback = AstrologyCalculator()
    private val saved = mutableListOf<SavedHoroscope>()
    private val synastryAspects = listOf(
        Aspect("Conjunction", 0.0),
        Aspect("Sextile", 60.0),
        Aspect("Square", 90.0),
        Aspect("Trine", 120.0),
        Aspect("Opposition", 180.0)
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKundaliMatchingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }
        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        saved.addAll(SavedStore.list(this))
        if (saved.isEmpty()) {
            binding.emptyView.visibility = View.VISIBLE
            binding.contentContainer.visibility = View.GONE
            return
        }

        setupPickers()
        binding.matchButton.setOnClickListener { runMatch() }
    }

    private fun setupPickers() {
        val names = saved.map { it.name }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.brideInput.setAdapter(adapter)
        binding.groomInput.setAdapter(adapter)
    }

    private fun runMatch() {
        val brideName = binding.brideInput.text?.toString()?.trim()
        val groomName = binding.groomInput.text?.toString()?.trim()

        val bride = saved.firstOrNull { it.name == brideName }
        val groom = saved.firstOrNull { it.name == groomName }

        if (bride == null || groom == null) {
            Snackbar.make(binding.root, getString(R.string.kundali_pick_both), Snackbar.LENGTH_LONG).show()
            return
        }

        val brideChart = chartFrom(bride)
        val groomChart = chartFrom(groom)

        if (brideChart == null || groomChart == null) {
            Snackbar.make(binding.root, getString(R.string.transit_engine_missing), Snackbar.LENGTH_LONG).show()
            return
        }

        val result = GunMilanCalculator.match(brideChart, groomChart)

        binding.summaryTitle.text = getString(R.string.kundali_score_title, result.total, result.max)
        binding.summaryNames.text = getString(R.string.kundali_pair_label, bride.name, groom.name)

        renderParts(result)
        renderSynastry(bride, groom, brideChart, groomChart)
        binding.resultContainer.visibility = View.VISIBLE
    }

    private fun renderParts(result: GunMilanCalculator.Result) {
        binding.kootaContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        result.parts.forEach { part ->
            val row = ItemKootaRowBinding.inflate(inflater, binding.kootaContainer, false)
            row.title.text = part.name
            row.score.text = getString(R.string.kundali_score_item, part.score, part.max)
            row.note.text = part.note
            binding.kootaContainer.addView(row.root)
        }
    }

    private fun renderSynastry(
        bride: SavedHoroscope,
        groom: SavedHoroscope,
        brideChart: ChartResult,
        groomChart: ChartResult
    ) {
        binding.synastryContainer.removeAllViews()

        val brideMars = brideChart.planets.firstOrNull { it.planet == Planet.MARS }
        val brideVenus = brideChart.planets.firstOrNull { it.planet == Planet.VENUS }
        val groomMars = groomChart.planets.firstOrNull { it.planet == Planet.MARS }
        val groomVenus = groomChart.planets.firstOrNull { it.planet == Planet.VENUS }

        if (brideMars == null || brideVenus == null || groomMars == null || groomVenus == null) {
            val row = ItemSynastryRowBinding.inflate(LayoutInflater.from(this), binding.synastryContainer, false)
            row.title.text = getString(R.string.kundali_synastry_title)
            row.detail.text = getString(R.string.kundali_synastry_missing)
            binding.synastryContainer.addView(row.root)
            return
        }

        val rows = listOf(
            buildSynastryRow("${bride.name} Mars", brideMars, "${groom.name} Venus", groomVenus),
            buildSynastryRow("${groom.name} Mars", groomMars, "${bride.name} Venus", brideVenus)
        )

        val inflater = LayoutInflater.from(this)
        rows.forEach { rowData ->
            val row = ItemSynastryRowBinding.inflate(inflater, binding.synastryContainer, false)
            row.title.text = rowData.title
            row.detail.text = rowData.detail
            binding.synastryContainer.addView(row.root)
        }
    }

    private fun buildSynastryRow(
        leftLabel: String,
        leftPos: PlanetPosition,
        rightLabel: String,
        rightPos: PlanetPosition
    ): SynastryRow {
        val match = bestAspect(leftPos.degree, rightPos.degree)
        val detail = if (match != null) {
            getString(
                R.string.kundali_synastry_detail,
                match.aspect.name,
                match.orb,
                leftPos.sign.displayName,
                rightPos.sign.displayName
            )
        } else {
            getString(
                R.string.kundali_synastry_none,
                leftPos.sign.displayName,
                rightPos.sign.displayName
            )
        }
        return SynastryRow("$leftLabel to $rightLabel", detail)
    }

    private fun bestAspect(a: Double, b: Double, maxOrb: Double = 8.0): AspectMatch? {
        val separation = angularSeparation(a, b)
        val candidate = synastryAspects.minByOrNull { abs(separation - it.angle) } ?: return null
        val orb = abs(separation - candidate.angle)
        return if (orb <= maxOrb) AspectMatch(candidate, orb) else null
    }

    private fun angularSeparation(a: Double, b: Double): Double {
        return abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)
    }

    private data class SynastryRow(val title: String, val detail: String)
    private data class Aspect(val name: String, val angle: Double)
    private data class AspectMatch(val aspect: Aspect, val orb: Double)

    private fun chartFrom(saved: SavedHoroscope): ChartResult? {
        val zdt = Instant.ofEpochMilli(saved.epochMillis).atZone(ZoneId.of(saved.zoneId))
        val details = BirthDetails(saved.name, zdt, saved.lat, saved.lon)
        return accurate.generateChart(details) ?: fallback.generateChart(details)
    }
}
