package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.databinding.ActivityTransitAnyBinding
import com.aakash.astro.databinding.ItemTransitPlanetBinding
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.astrology.Planet
import com.aakash.astro.geo.City
import com.aakash.astro.geo.CityDatabase
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.*
import kotlin.math.floor
import kotlin.math.roundToInt

class TransitAnyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransitAnyBinding
    private val accurate = AccurateCalculator()
    private var natalChart: com.aakash.astro.astrology.ChartResult? = null

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransitAnyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        // Prepare ephemeris
        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        setupInputs()
        setupActions()

        // Start with results hidden until user provides inputs
        binding.resultContainer.visibility = android.view.View.GONE

        // Preload natal chart from extras (for house evaluation)
        preloadNatalFromIntent()
    }

    private fun preloadNatalFromIntent() {
        val name = intent.getStringExtra(TransitActivity.EXTRA_NAME)
        val zoneId = intent.getStringExtra(TransitActivity.EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(TransitActivity.EXTRA_LAT, Double.NaN)
        val lon = intent.getDoubleExtra(TransitActivity.EXTRA_LON, Double.NaN)
        val birthEpochMillis = intent.getLongExtra(TransitActivity.EXTRA_EPOCH_MILLIS, -1L)
        if (birthEpochMillis > 0 && !lat.isNaN() && !lon.isNaN()) {
            val birthInstant = Instant.ofEpochMilli(birthEpochMillis).atZone(zoneId)
            val birthDetails = BirthDetails(name, birthInstant, lat, lon)
            natalChart = accurate.generateChart(birthDetails)
        }
    }

    private fun setupInputs() {
        // Place suggestions from built-in database
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, CityDatabase.names())
        binding.placeInput.setAdapter(adapter)
        binding.placeInput.threshold = 1
        binding.placeInput.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position)
            selectedCity = name?.let { CityDatabase.findByName(it) }
            updatePlaceCoords()
        }

        // Date picker
        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker() }
        binding.dateInput.setOnClickListener { showDatePicker() }

        // Time picker
        binding.timeInputLayout.setEndIconOnClickListener { showTimePicker() }
        binding.timeInput.setOnClickListener { showTimePicker() }
    }

    private fun setupActions() {
        binding.computeButton.setOnClickListener {
            val date = selectedDate
            val time = selectedTime
            val city = selectedCity
            if (date == null || time == null || city == null) {
                if (date == null && time == null && city == null) {
                    // No inputs at all -> go back to main page
                    finish()
                } else {
                    Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }
            computeTransit(date, time, city)
        }
    }

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_date))
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            selectedDate = ld
            binding.dateInput.setText(ld.toString())
        }
        picker.show(supportFragmentManager, "transit_any_date")
    }

    private fun showTimePicker() {
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(12)
            .setMinute(0)
            .setTitleText(getString(R.string.pick_time))
            .build()
        picker.addOnPositiveButtonClickListener {
            val lt = LocalTime.of(picker.hour, picker.minute)
            selectedTime = lt
            binding.timeInput.setText(String.format("%02d:%02d", picker.hour, picker.minute))
        }
        picker.show(supportFragmentManager, "transit_any_time")
    }

    private fun computeTransit(date: LocalDate, time: LocalTime, city: City) {
        val zone = ZoneId.systemDefault()
        val zdt = LocalDateTime.of(date, time).atZone(zone)

        val details = BirthDetails(null, zdt, city.latitude, city.longitude)
        val chart = accurate.generateChart(details)
        if (chart == null) {
            Snackbar.make(binding.root, getString(R.string.transit_engine_missing), Snackbar.LENGTH_LONG).show()
            return
        }

        binding.title.text = getString(R.string.transit_any_title)
        binding.subtitle.text = "@ ${zdt.toLocalDate()} ${zdt.toLocalTime().withSecond(0).withNano(0)} (${zone.id})\n${city.name}: " +
                String.format("%.4f, %.4f", city.latitude, city.longitude)

        // Render chart and transit list
        binding.vedicChartView.setChart(chart)
        renderTransitPlanets(chart)
        binding.resultContainer.visibility = android.view.View.VISIBLE
    }

    private fun renderTransitPlanets(transitChart: com.aakash.astro.astrology.ChartResult) {
        binding.transitPlanetContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        transitChart.planets.forEach { transitPlanet ->
            val itemBinding = ItemTransitPlanetBinding.inflate(inflater, binding.transitPlanetContainer, false)
            val nameWithRetro = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name
            itemBinding.planetName.text = nameWithRetro
            val degreeText = formatDegreeWithSign(transitPlanet.degree)
            itemBinding.transitPosition.text = "Transit: ${transitPlanet.sign.displayName} $degreeText (House ${transitPlanet.house})"
            val natal = natalChart
            if (natal != null) {
                val natalHouse = calculateNatalHouse(transitPlanet.degree, natal.ascendantSign)
                val verdict = houseVerdict(transitPlanet.planet, natalHouse)
                val verdictText = when (verdict) {
                    HouseVerdict.Good -> getString(R.string.transit_verdict_good)
                    HouseVerdict.Bad -> getString(R.string.transit_verdict_bad)
                    HouseVerdict.Neutral -> getString(R.string.transit_verdict_neutral)
                }
                var color = when (verdict) {
                    HouseVerdict.Good -> resources.getColor(R.color.planet_favorable, theme)
                    HouseVerdict.Bad -> resources.getColor(R.color.planet_unfavorable, theme)
                    HouseVerdict.Neutral -> resources.getColor(R.color.planet_neutral, theme)
                }
                val caution = if (transitPlanet.isRetrograde) " \u2022 " + getString(R.string.retro_caution) else ""
                if (transitPlanet.isRetrograde) {
                    color = resources.getColor(R.color.planet_retrograde, theme)
                }
                itemBinding.natalHouseInfo.text = getString(R.string.transit_natal_house_verdict, natalHouse, verdictText) + caution
                itemBinding.natalHouseInfo.setTextColor(color)
            } else {
                itemBinding.natalHouseInfo.text = getString(R.string.transit_natal_missing)
            }
            binding.transitPlanetContainer.addView(itemBinding.root)
        }
    }

    private fun calculateNatalHouse(transitDegree: Double, natalAscSign: ZodiacSign): Int {
        val transitSign = ZodiacSign.fromDegree(transitDegree)
        return 1 + (transitSign.ordinal - natalAscSign.ordinal + 12) % 12
    }

    private enum class HouseVerdict { Good, Bad, Neutral }

    private fun isMalefic(planet: Planet): Boolean = when (planet) {
        Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU, Planet.SUN -> true
        else -> false // MOON, MERCURY, JUPITER, VENUS treated as benefic/neutral
    }

    private fun houseVerdict(planet: Planet, house: Int): HouseVerdict {
        val upachaya = setOf(3, 6, 10, 11)
        val dusthana = setOf(6, 8, 12)
        val trine = setOf(1, 5, 9)
        val kendra = setOf(1, 4, 7, 10)
        val beneficGood = trine + kendra + setOf(2, 11)
        val maleficBad = setOf(1, 4, 5, 7, 8, 9, 12)

        return if (isMalefic(planet)) {
            when {
                house in upachaya -> HouseVerdict.Good
                house in maleficBad -> HouseVerdict.Bad
                else -> HouseVerdict.Neutral // e.g., 2
            }
        } else {
            when {
                house in beneficGood -> HouseVerdict.Good
                house in dusthana -> HouseVerdict.Bad
                else -> HouseVerdict.Neutral // e.g., 3
            }
        }
    }

    private fun updatePlaceCoords() {
        val c = selectedCity
        val summary = if (c != null) {
            val lat = String.format("%.4f", c.latitude)
            val lon = String.format("%.4f", c.longitude)
            "${c.name}: ${lat}, ${lon}"
        } else ""
        binding.placeCoords.text = summary
    }

    private fun formatDegree(value: Double): String {
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
        val absText = formatDegree(value)
        val inSign = ((value % 30.0) + 30.0) % 30.0
        val inSignText = formatDegree(inSign)
        return "$absText ($inSignText)"
    }
}
