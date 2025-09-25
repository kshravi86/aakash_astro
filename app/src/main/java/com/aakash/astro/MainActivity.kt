package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.AstrologyCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.databinding.ActivityMainBinding
import com.aakash.astro.databinding.ItemPlanetPositionBinding
import com.aakash.astro.geo.City
import com.aakash.astro.geo.CityDatabase
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val fallbackCalculator = AstrologyCalculator()
    private val accurateCalculator = AccurateCalculator()

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

        setupPlaceInput()

        binding.selectDateButton.setOnClickListener { showDatePicker() }
        binding.selectTimeButton.setOnClickListener { showTimePicker() }
        binding.generateButton.setOnClickListener { generateChart() }
        binding.dashaButton.setOnClickListener { openDasha() }
        prepareEphemeris()

        // Defaults: current date/time and Bengaluru as birthplace
        initializeDefaultsAndGenerate()
    }
    private fun prepareEphemeris() {
        val dir = EphemerisPreparer.prepare(this)
        dir?.let { accurateCalculator.setEphePath(it.absolutePath) }
    }
    private fun setupPlaceInput() {
        val names = CityDatabase.names()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.placeInput.setAdapter(adapter)
        binding.placeInput.threshold = 1
        binding.placeInput.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position)
            selectedCity = name?.let { CityDatabase.findByName(it) }; updatePlaceCoords()
        }
        binding.placeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.placeInput.text?.toString()?.trim().orEmpty()
                selectedCity = CityDatabase.findByName(text); updatePlaceCoords()
            }
        }
    }

    private fun showDatePicker() {
        val zone = ZoneId.systemDefault()
        val selection = selectedDate?.let { it.atStartOfDay(zone).toInstant().toEpochMilli() }
            ?: MaterialDatePicker.todayInUtcMilliseconds()

        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText("Select birth date")
            .setSelection(selection)
            .build()

        picker.addOnPositiveButtonClickListener { millis ->
            val instant = Instant.ofEpochMilli(millis)
            selectedDate = instant.atZone(zone).toLocalDate()
            updateDateTimeSummary()
        }
        picker.show(supportFragmentManager, "birth-date-picker")
    }

    private fun showTimePicker() {
        val initialTime = selectedTime ?: LocalTime.of(12, 0)
        val picker = MaterialTimePicker.Builder()
            .setTitleText("Select birth time")
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialTime.hour)
            .setMinute(initialTime.minute)
            .build()

        picker.addOnPositiveButtonClickListener {
            selectedTime = LocalTime.of(picker.hour, picker.minute)
            updateDateTimeSummary()
        }
        picker.show(supportFragmentManager, "birth-time-picker")
    }

    private fun updateDateTimeSummary() {
        val date = selectedDate
        val time = selectedTime
        binding.dateTimeValue.text = when {
            date == null && time == null -> "No date and time selected"
            date != null && time == null -> "Date: $date"
            date == null && time != null -> "Time: $time"
            else -> "$date - $time"
        }
    }

    private fun generateChart() {
        val date = selectedDate
        val time = selectedTime
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())

        if (date == null || time == null || city == null) {
            Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
            return
        }

        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val birthDetails = BirthDetails(
            name = binding.nameInput.text?.toString()?.takeIf { it.isNotBlank() },
            dateTime = birthDateTime,
            latitude = city.latitude,
            longitude = city.longitude
        )

        val accurate = accurateCalculator.generateChart(birthDetails)
        if (accurate == null) {
            binding.engineIndicator.text = "Engine: Swiss Ephemeris missing"
            Snackbar.make(
                binding.root,
                "Swiss Ephemeris not available. Add swisseph.jar and ephemeris files (see docs).",
                Snackbar.LENGTH_LONG
            ).show()
            return
        }
        renderPlanets(accurate)
        binding.engineIndicator.text = "Engine: Swiss Ephemeris (Lahiri)"
        birthDetails.name?.let { binding.subtitleText.text = getString(R.string.chart_generated_for, it) }
    }

    private fun openDasha() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, DashaActivity::class.java).apply {
            putExtra(DashaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(DashaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(DashaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(DashaActivity.EXTRA_LAT, city.latitude)
            putExtra(DashaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun renderPlanets(chart: com.aakash.astro.astrology.ChartResult) {
        binding.planetContainer.removeAllViews()
        val inflater: LayoutInflater = LayoutInflater.from(this)

        // Add Ascendant (Lagna) as a row
        run {
            val itemBinding = ItemPlanetPositionBinding.inflate(inflater, binding.planetContainer, false)
            itemBinding.planetName.text = "Ascendant (Lagna)"
            val degreeText = formatDegreeWithSign(chart.ascendantDegree)
            itemBinding.planetDetails.text = getString(
                R.string.planet_position_format,
                chart.ascendantSign.displayName,
                degreeText,
                "House 1"
            )
            val (nakNameA, padaA) = com.aakash.astro.astrology.NakshatraCalc.fromLongitude(chart.ascendantDegree)
            itemBinding.planetNakshatra.text = getString(R.string.nakshatra_format, nakNameA, padaA)
            binding.planetContainer.addView(itemBinding.root)
        }

        chart.planets.forEach { planet ->
            val itemBinding = ItemPlanetPositionBinding.inflate(inflater, binding.planetContainer, false)
            val nameWithRetro = if (planet.isRetrograde) "${planet.name} (R)" else planet.name
            itemBinding.planetName.text = nameWithRetro
            val degreeText = formatDegreeWithSign(planet.degree)
            itemBinding.planetDetails.text = getString(
                R.string.planet_position_format,
                planet.sign.displayName,
                degreeText,
                "House ${planet.house}"
            )
            val (nakName, pada) = com.aakash.astro.astrology.NakshatraCalc.fromLongitude(planet.degree)
            itemBinding.planetNakshatra.text = getString(R.string.nakshatra_format, nakName, pada)
            binding.planetContainer.addView(itemBinding.root)
        }

        // Finally, render the Vedic chart (South Indian style) at the end
        binding.vedicChartView.setChart(chart)
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



    private fun initializeDefaultsAndGenerate() {
        // Set current date and time by default
        if (selectedDate == null) selectedDate = LocalDate.now()
        if (selectedTime == null) selectedTime = LocalTime.now().withSecond(0).withNano(0)
        updateDateTimeSummary()

        // Default city: Bengaluru (fallback to Bangalore capitalization/alias)
        if (selectedCity == null) {
            selectedCity = CityDatabase.findByName("Bengaluru")
                ?: CityDatabase.findByName("Bangalore")
        }
        selectedCity?.let {
            // Avoid triggering filtering when setting text programmatically
            binding.placeInput.setText(it.name, false)
        }
        updatePlaceCoords()

        // Auto-generate chart once defaults are set
        generateChart()
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
}
