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
import com.aakash.astro.databinding.ActivityKundaliMatchingBinding
import com.aakash.astro.databinding.ItemKootaRowBinding
import com.aakash.astro.storage.SavedStore
import com.google.android.material.snackbar.Snackbar
import java.time.Instant
import java.time.ZoneId

class KundaliMatchingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKundaliMatchingBinding
    private val accurate = AccurateCalculator()
    private val fallback = AstrologyCalculator()
    private val saved = mutableListOf<SavedStore.SavedHoroscope>()

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
        binding.summaryNames.text = getString(
            R.string.kundali_pair_label,
            bride.name,
            groom.name
        )

        renderParts(result)
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

    private fun chartFrom(saved: SavedStore.SavedHoroscope): ChartResult? {
        val zdt = Instant.ofEpochMilli(saved.epochMillis).atZone(ZoneId.of(saved.zoneId))
        val details = BirthDetails(saved.name, zdt, saved.lat, saved.lon)
        return accurate.generateChart(details) ?: fallback.generateChart(details)
    }
}
