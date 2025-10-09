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
        val isToday = intent.getBooleanExtra(EXTRA_IS_TODAY, false)

        if (isToday) {
            binding.topBar.title = getString(R.string.todays_panchanga_title)
        }

        // Compute natal chart (Sun/Moon longitudes, Asc, etc.)
        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            binding.engineNote.text = getString(R.string.transit_engine_missing)
            return
        }

        val p = PanchangaCalc.compute(chart, zdt.dayOfWeek)

        // Table values
        binding.tithiValue.text = p.tithi
        binding.tithiGroupValue.text = p.tithiGroup
        // Tooltip: brief meaning per tithi group
        val tgMeaningRes = when (p.tithiGroup) {
            "Nanda" -> R.string.tithi_group_meaning_nanda
            "Bhadra" -> R.string.tithi_group_meaning_bhadra
            "Jaya" -> R.string.tithi_group_meaning_jaya
            "Rikta" -> R.string.tithi_group_meaning_rikta
            "Poorna" -> R.string.tithi_group_meaning_poorna
            else -> 0
        }
        if (tgMeaningRes != 0) {
            val tip = getString(R.string.tithi_group_tooltip_format, p.tithiGroup, getString(tgMeaningRes))
            androidx.core.view.ViewCompat.setTooltipText(binding.tithiGroupValue, tip)
            binding.tithiGroupValue.contentDescription = tip
        }
        binding.varaValue.text = p.vara
        binding.nakshatraValue.text = p.nakshatra
        binding.yogaValue.text = p.yoga
        binding.yogaLordValue.text = p.yogaLord
        binding.karanaValue.text = p.karana
        binding.karanaLordValue.text = p.karanaLord

        binding.engineNote.text = "Engine: Swiss Ephemeris"

        // Tooltips for yoga/karana
        if (p.yogaLord.isNotBlank() && p.yogaLord != "—") {
            val tip = "Lord: ${p.yogaLord}"
            androidx.core.view.ViewCompat.setTooltipText(binding.yogaValue, tip)
            binding.yogaValue.contentDescription = "${binding.yogaValue.text}. ${tip}"
            androidx.core.view.ViewCompat.setTooltipText(binding.yogaLordValue, tip)
        }
        if (p.karanaLord.isNotBlank() && p.karanaLord != "—") {
            val tip = "Lord: ${p.karanaLord}"
            androidx.core.view.ViewCompat.setTooltipText(binding.karanaValue, tip)
            binding.karanaValue.contentDescription = "${binding.karanaValue.text}. ${tip}"
            androidx.core.view.ViewCompat.setTooltipText(binding.karanaLordValue, tip)
        }

        // Sunrise/Sunset for that date and place
        val sr = com.aakash.astro.astrology.SunriseCalc.sunrise(zdt.toLocalDate(), lat, lon, zoneId)
        val ss = com.aakash.astro.astrology.SunriseCalc.sunset(zdt.toLocalDate(), lat, lon, zoneId)
        val tf = java.time.format.DateTimeFormatter.ofPattern("hh:mm a")
        binding.sunriseValue.text = sr?.format(tf) ?: "—"
        binding.sunsetValue.text = ss?.format(tf) ?: "—"
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_IS_TODAY = "isToday"
    }
}
