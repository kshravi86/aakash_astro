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
        binding.topBar.setNavigationOnClickListener { finish() }

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

        setupVargaSelector(natal)
        
        // Entrance animation
        binding.contentContainer.alpha = 0f
        binding.contentContainer.translationY = 30f
        binding.contentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .start()
    }

    private fun setupVargaSelector(natal: ChartResult) {
        val vargas = listOf(
            Varga.D1, Varga.D2, Varga.D3, Varga.D4, Varga.D7, Varga.D9, Varga.D10, Varga.D12,
            Varga.D16, Varga.D20, Varga.D24, Varga.D27, Varga.D30, Varga.D40, Varga.D45, Varga.D60
        )

        val selector = binding.vargaSelector
        vargas.forEach { v ->
            val chip = com.google.android.material.chip.Chip(this).apply {
                text = v.code
                isCheckable = true
                id = android.view.View.generateViewId()
                tag = v
                
                // Style to match system
                chipBackgroundColor = android.content.res.ColorStateList.valueOf(getColor(R.color.glass_white_5))
                chipStrokeColor = android.content.res.ColorStateList.valueOf(getColor(R.color.glass_white_10))
                chipStrokeWidth = resources.displayMetrics.density
                setTextColor(getColor(R.color.primaryText))
            }
            selector.addView(chip)
        }

        selector.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedId = checkedIds.firstOrNull() ?: return@setOnCheckedStateChangeListener
            val chip = group.findViewById<com.google.android.material.chip.Chip>(checkedId)
            val varga = chip.tag as Varga
            updateVargaDisplay(natal, varga)
        }

        // Default selection
        (selector.getChildAt(0) as? com.google.android.material.chip.Chip)?.isChecked = true
    }

    private fun updateVargaDisplay(natal: ChartResult, varga: Varga) {
        binding.selectedVargaTitle.text = "${varga.code} - ${VargaCalculator.vargaName(varga)}"
        binding.vargaDescription.text = VargaCalculator.description(varga)
        
        val vc = VargaCalculator.computeVargaChart(natal, varga, natal.ascendantDegree)
        binding.vedicChartView.setChart(vc)
        
        // Animation for smooth transition
        binding.vargaCard.alpha = 0.5f
        binding.vargaCard.animate().alpha(1f).setDuration(300).start()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
