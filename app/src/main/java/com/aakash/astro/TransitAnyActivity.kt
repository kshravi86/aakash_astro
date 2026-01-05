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
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt

class TransitAnyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransitAnyBinding
    private val accurate = AccurateCalculator()
    private var natalChart: com.aakash.astro.astrology.ChartResult? = null

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null
    private val deviceZone: ZoneId by lazy { ZoneId.systemDefault() }
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTransitAnyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        // Prepare ephemeris
        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        setupInputs()
        setupActions()
        applyNow()

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

        binding.nowButton.setOnClickListener { applyNow() }
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

    private fun applyNow() {
        val now = ZonedDateTime.now(deviceZone).truncatedTo(ChronoUnit.MINUTES)
        selectedDate = now.toLocalDate()
        selectedTime = now.toLocalTime()
        updateDateTimeFields()
    }

    private fun updateDateTimeFields() {
        val dateText = selectedDate?.format(dateFormatter).orEmpty()
        val timeText = selectedTime?.format(timeFormatter12).orEmpty()
        binding.dateInput.setText(dateText)
        binding.timeInput.setText(timeText)
    }

    private fun showDatePicker() {
        val selection = selectedDate
            ?.atStartOfDay(deviceZone)
            ?.toInstant()
            ?.toEpochMilli()
            ?: MaterialDatePicker.todayInUtcMilliseconds()
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_date))
            .setSelection(selection)
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            selectedDate = Instant.ofEpochMilli(millis).atZone(deviceZone).toLocalDate()
            updateDateTimeFields()
        }
        picker.show(supportFragmentManager, "transit_any_date")
    }

    private fun showTimePicker() {
        val initial = selectedTime ?: LocalTime.now(deviceZone).truncatedTo(ChronoUnit.MINUTES)
        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_12H)
            .setHour(initial.hour)
            .setMinute(initial.minute)
            .setTitleText(getString(R.string.pick_time))
            .build()
        picker.addOnPositiveButtonClickListener {
            selectedTime = LocalTime.of(picker.hour, picker.minute)
            updateDateTimeFields()
        }
        picker.show(supportFragmentManager, "transit_any_time")
    }

    private fun computeTransit(date: LocalDate, time: LocalTime, city: City) {
        val zone = deviceZone
        val zdt = LocalDateTime.of(date, time).atZone(zone)

        val details = BirthDetails(null, zdt, city.latitude, city.longitude)
        val chart = accurate.generateChart(details)
        if (chart == null) {
            Snackbar.make(binding.root, getString(R.string.transit_engine_missing), Snackbar.LENGTH_LONG).show()
            return
        }

        binding.title.text = getString(R.string.transit_any_title)
        val dateText = dateFormatter.format(zdt)
        val timeText = timeFormatter12.format(zdt)
        binding.subtitle.text = "@ $dateText $timeText (${zone.id})\n${city.name}: " +
                String.format("%.4f, %.4f", city.latitude, city.longitude)

        // Render chart and transit list
        binding.vedicChartView.setChart(chart)
        renderTransitPlanets(chart)
        binding.resultContainer.visibility = android.view.View.VISIBLE
    }

    private fun renderTransitPlanets(transitChart: com.aakash.astro.astrology.ChartResult) {
        binding.transitPlanetContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        // Cache Sun degree for combustion check
        val sunDegree = transitChart.planets.find { it.planet == Planet.SUN }?.degree

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
                // Cautions: retrograde and combustion (+/- 10 degrees from Sun)
                val combust = sunDegree?.let { isCombust(transitPlanet.planet, transitPlanet.degree, it) } ?: false
                val cautionParts = mutableListOf<String>()
                if (transitPlanet.isRetrograde) cautionParts += getString(R.string.retro_caution)
                if (combust) cautionParts += getString(R.string.combust_caution)
                val caution = if (cautionParts.isNotEmpty()) " \u2022 " + cautionParts.joinToString(", ") else ""
                if (transitPlanet.isRetrograde || combust) {
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

    private fun isCombust(planet: Planet, planetDegree: Double, sunDegree: Double): Boolean {
        if (planet == Planet.SUN) return false
        // Within +/- 10 degrees of Sun (circular distance)
        val diff = angularSeparation(planetDegree, sunDegree)
        return diff <= 10.0
    }

    private fun angularSeparation(a: Double, b: Double): Double {
        val diff = kotlin.math.abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)
        return diff
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
