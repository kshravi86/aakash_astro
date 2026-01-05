package com.aakash.astro

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.EphemerisPreparer
import com.aakash.astro.astrology.GhatikaLagnaCalc
import com.aakash.astro.astrology.HoraLagnaJaiminiCalc
import com.aakash.astro.astrology.JaiminiArudha
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivitySpecialLagnasBinding
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToInt

class SpecialLagnasActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySpecialLagnasBinding
    private val accurate = AccurateCalculator()
    private lateinit var zoneIdExtra: ZoneId
    private var latExtra: Double = 0.0
    private var lonExtra: Double = 0.0

    private val headerFormatter: DateTimeFormatter by lazy {
        DateTimeFormatter.ofPattern("dd MMM yyyy • hh:mm a z", Locale.getDefault())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Build the chart and render the special lagna panels.
        super.onCreate(savedInstanceState)
        binding = ActivitySpecialLagnasBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        zoneIdExtra = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        latExtra = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        lonExtra = intent.getDoubleExtra(EXTRA_LON, 0.0)

        val birthZdt = Instant.ofEpochMilli(epochMillis).atZone(zoneIdExtra)
        renderProfile(name, birthZdt)

        val chart = accurate.generateChart(BirthDetails(name, birthZdt, latExtra, lonExtra))
        if (chart == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            showUnavailableState()
            return
        }

        renderGhatikaLagna(chart, birthZdt)
        renderArudhaLagna(chart)
        renderHoraLagna(chart, birthZdt)
    }

    private fun renderProfile(name: String?, birthZdt: ZonedDateTime) {
        // Populate header identity, birth info, and location details.
        val subtitleText = buildString {
            append(getString(R.string.special_lagnas_subtitle))
            if (!name.isNullOrBlank()) {
                append("\n")
                append(getString(R.string.chart_generated_for, name))
            }
        }
        binding.subtitle.text = subtitleText

        val displayName = name?.takeIf { it.isNotBlank() } ?: getString(R.string.ashtakavarga_anonymous_native)
        binding.nativeName.text = displayName
        binding.birthInfo.text = headerFormatter.format(birthZdt)
        binding.locationInfo.text = if (latExtra == 0.0 && lonExtra == 0.0) {
            getString(R.string.ashtakavarga_location_placeholder)
        } else {
            getString(
                R.string.ashtakavarga_location_format,
                formatCoordinate(latExtra, "N", "S"),
                formatCoordinate(lonExtra, "E", "W")
            )
        }
    }

    private fun renderGhatikaLagna(chart: ChartResult, birthZdt: ZonedDateTime) {
        // Compute and render the Ghatika lagna values.
        val result = GhatikaLagnaCalc.compute(birthZdt, latExtra, lonExtra, zoneIdExtra, accurate)
        if (result == null) {
            applyMissingState(
                binding.glSignValue,
                binding.glDegreeValue,
                binding.glNakValue,
                binding.glHouseValue
            )
            binding.glNoteValue.text = getString(R.string.special_lagna_note_ghatika)
            return
        }

        binding.glSignValue.text = result.sign.displayName
        binding.glDegreeValue.text = formatDegreeWithSign(result.longitude)
        val (nak, pada) = NakshatraCalc.fromLongitude(result.longitude)
        binding.glNakValue.text = getString(R.string.nakshatra_format, nak, pada)
        binding.glHouseValue.text = getString(R.string.special_lagna_house_value, houseFromAsc(result.sign, chart))
        binding.glNoteValue.text = getString(R.string.special_lagna_note_ghatika)
    }

    private fun renderArudhaLagna(chart: ChartResult) {
        // Compute and render Arudha lagna values.
        val arudha = JaiminiArudha.compute(chart).firstOrNull { it.house == 1 }
        if (arudha == null) {
            applyMissingState(
                binding.alSignValue,
                binding.alDegreeValue,
                binding.alNakValue,
                binding.alHouseValue
            )
            binding.alNoteValue.text = getString(R.string.special_lagna_note_arudha, getString(R.string.special_lagna_na))
            return
        }

        binding.alSignValue.text = arudha.padaSign.displayName
        binding.alDegreeValue.text = getString(R.string.special_lagna_na)
        binding.alNakValue.text = getString(R.string.special_lagna_na)
        binding.alHouseValue.text = getString(R.string.special_lagna_house_value, arudha.padaHouse)
        binding.alNoteValue.text = getString(R.string.special_lagna_note_arudha, arudha.lord.displayName)
    }

    private fun renderHoraLagna(chart: ChartResult, birthZdt: ZonedDateTime) {
        // Compute and render Hora lagna values.
        val result = HoraLagnaJaiminiCalc.compute(birthZdt, latExtra, lonExtra, zoneIdExtra, accurate)
        if (result == null) {
            applyMissingState(
                binding.hlSignValue,
                binding.hlDegreeValue,
                binding.hlNakValue,
                binding.hlHouseValue
            )
            binding.hlNoteValue.text = getString(R.string.special_lagna_note_hora)
            return
        }

        binding.hlSignValue.text = result.sign.displayName
        binding.hlDegreeValue.text = formatDegreeWithSign(result.longitude)
        val (nak, pada) = NakshatraCalc.fromLongitude(result.longitude)
        binding.hlNakValue.text = getString(R.string.nakshatra_format, nak, pada)
        binding.hlHouseValue.text = getString(R.string.special_lagna_house_value, houseFromAsc(result.sign, chart))
        binding.hlNoteValue.text = getString(R.string.special_lagna_note_hora)
    }

    private fun applyMissingState(vararg views: android.widget.TextView) {
        // Apply a consistent missing-data label to a set of views.
        val missingText = getString(R.string.special_lagna_missing)
        views.forEach {
            it.text = missingText
        }
    }

    private fun houseFromAsc(sign: ZodiacSign, chart: ChartResult): Int {
        // Map a sign to its house number from the ascendant.
        val asc = chart.ascendantSign.ordinal
        return 1 + (sign.ordinal - asc + 12) % 12
    }

    private fun formatDegreeWithSign(value: Double): String {
        // Combine absolute and intra-sign degrees for display.
        val absText = formatDegree(value)
        val inSign = ((value % 30.0) + 30.0) % 30.0
        val inSignText = formatDegree(inSign)
        return "$absText ($inSignText)"
    }

    private fun formatDegree(value: Double): String {
        // Format degrees with minute rounding and carry-over.
        val normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format(Locale.US, "%02d° %02d'", degrees, minutes)
    }

    private fun formatCoordinate(value: Double, positiveSuffix: String, negativeSuffix: String): String {
        // Format coordinates with N/S or E/W suffix.
        val suffix = if (value >= 0) positiveSuffix else negativeSuffix
        return String.format(Locale.US, "%.2f°%s", abs(value), suffix)
    }

    private fun showUnavailableState() {
        // Apply missing state to all lagna fields.
        applyMissingState(
            binding.glSignValue,
            binding.glDegreeValue,
            binding.glNakValue,
            binding.glHouseValue,
            binding.alSignValue,
            binding.alDegreeValue,
            binding.alNakValue,
            binding.alHouseValue,
            binding.hlSignValue,
            binding.hlDegreeValue,
            binding.hlNakValue,
            binding.hlHouseValue
        )
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
