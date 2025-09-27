package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.PanchangaCalc
import com.aakash.astro.databinding.ActivityPanchangaBinding
import java.time.Instant
import java.time.ZoneId

class PanchangaActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPanchangaBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPanchangaBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        // Edge-to-edge + apply system bar insets as padding
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val startPaddingLeft = binding.root.paddingLeft
        val startPaddingTop = binding.root.paddingTop
        val startPaddingRight = binding.root.paddingRight
        val startPaddingBottom = binding.root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                startPaddingLeft + systemBars.left,
                startPaddingTop + systemBars.top,
                startPaddingRight + systemBars.right,
                startPaddingBottom + systemBars.bottom
            )
            insets
        }

        // Prepare Swiss Ephemeris (if bundled)
        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        // Compute natal chart (Sun/Moon longitudes, Asc, etc.)
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            binding.engineNote.text = getString(R.string.transit_engine_missing)
            return
        }

        val p = PanchangaCalc.compute(chart, zdt.dayOfWeek)

        // Headline summary
        binding.summaryLine.text = listOf(p.tithi, p.vara, p.nakshatra).joinToString(" • ")

        // Tiles
        binding.tithiValue.text = p.tithi
        binding.varaValue.text = p.vara
        binding.nakshatraValue.text = p.nakshatra
        binding.yogaValue.text = p.yoga
        binding.karanaValue.text = p.karana

        // Chips row
        val ctx = this
        fun addChip(text: String, @androidx.annotation.ColorRes color: Int? = null) {
            val chip = com.google.android.material.chip.Chip(ctx).apply {
                this.text = text
                isCheckable = false
                isClickable = false
                color?.let { setChipBackgroundColorResource(it) }
            }
            binding.chipGroup.addView(chip)
        }
        val tithiColor = if (p.tithi.startsWith("Shukla")) R.color.accent_orange else R.color.accent_blue
        addChip(p.tithi, tithiColor)
        addChip(p.vara)
        addChip(p.nakshatra)
        addChip(p.yoga)
        addChip(p.karana)

        binding.engineNote.text = "Engine: Swiss Ephemeris"
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
