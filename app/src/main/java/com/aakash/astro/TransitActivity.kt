package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivityTransitBinding
import com.aakash.astro.databinding.ItemTransitPlanetBinding
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class TransitActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransitBinding
    private val accurate = AccurateCalculator()
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Build the transit chart (and optional natal chart) and render the screen.
        super.onCreate(savedInstanceState)
        binding = ActivityTransitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)
        val birthEpochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, -1L)

        // Generate natal chart from birth details
        val natalChart = if (birthEpochMillis > 0) {
            val birthInstant = Instant.ofEpochMilli(birthEpochMillis).atZone(zoneId)
            val birthDetails = BirthDetails(name, birthInstant, lat, lon)
            accurate.generateChart(birthDetails)
        } else null

        // Transit time: either provided via extras or current instant in provided/system zone
        val transitZdt = intent.getLongExtra(EXTRA_TRANSIT_EPOCH_MILLIS, -1L)
            .takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(zoneId) }
            ?: Instant.now().atZone(zoneId)
        val transitDetails = BirthDetails(name, transitZdt, lat, lon)
        val transitChart = accurate.generateChart(transitDetails)

        binding.title.text = getString(R.string.transit_title)
        val who = name?.let { getString(R.string.chart_generated_for, it) } ?: ""
        val whenText = "@ ${dateFormatter.format(transitZdt)} ${timeFormatter12.format(transitZdt)} (${zoneId.id})"
        binding.subtitle.text = (who + if (who.isNotEmpty()) "\n" else "") + whenText

        if (transitChart == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        binding.vedicChartView.setChart(transitChart)

        // Display transit planets with their natal house positions
        if (natalChart != null) {
            renderTransitPlanets(transitChart, natalChart)
        } else {
            renderTransitPlanets(transitChart, null)
        }
    }

    private fun renderTransitPlanets(
        transitChart: com.aakash.astro.astrology.ChartResult,
        natalChart: com.aakash.astro.astrology.ChartResult?
    ) {
        // Render each transit planet with its natal house context when available.
        binding.transitPlanetContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)

        transitChart.planets.forEach { transitPlanet ->
            val itemBinding = ItemTransitPlanetBinding.inflate(inflater, binding.transitPlanetContainer, false)

            val nameWithRetro = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name
            itemBinding.planetName.text = nameWithRetro

            // Transit position
            val degreeText = formatDegreeWithSign(transitPlanet.degree)
            itemBinding.transitPosition.text = "Transit: ${transitPlanet.sign.displayName} $degreeText (House ${transitPlanet.house})"

            // Calculate natal house this transit planet is occupying
            if (natalChart != null) {
                val natalHouse = calculateNatalHouse(transitPlanet.degree, natalChart.ascendantSign)
                itemBinding.natalHouseInfo.text = "Transiting through Natal House $natalHouse"
            } else {
                itemBinding.natalHouseInfo.text = "Natal chart not available"
            }

            binding.transitPlanetContainer.addView(itemBinding.root)
        }
    }

    private fun calculateNatalHouse(transitDegree: Double, natalAscSign: ZodiacSign): Int {
        // Convert a transit degree to the natal house index using whole-sign logic.
        // Convert transit degree to sign
        val transitSign = ZodiacSign.fromDegree(transitDegree)
        // Calculate house based on whole sign system from natal ascendant
        return 1 + (transitSign.ordinal - natalAscSign.ordinal + 12) % 12
    }

    private fun formatDegree(value: Double): String {
        // Format a longitude into degrees and minutes with roll-over handling.
        val normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format("%02d\u00B0 %02d'", degrees, minutes)
    }

    private fun formatDegreeWithSign(value: Double): String {
        // Show absolute degree and the intra-sign degree in one label.
        val absText = formatDegree(value)
        val inSign = ((value % 30.0) + 30.0) % 30.0
        val inSignText = formatDegree(inSign)
        return "$absText ($inSignText)"
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
        const val EXTRA_TRANSIT_EPOCH_MILLIS = "transitEpochMillis"
    }
}

