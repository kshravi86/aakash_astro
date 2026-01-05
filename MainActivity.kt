package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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
import java.time.format.DateTimeFormatter
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlin.math.floor
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val calculator = AstrologyCalculator()

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

        // Date/time inputs look like fields; make the whole field and end icons open pickers
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker() }
        binding.timeInput.setOnClickListener { showTimePicker() }
        binding.timeInputLayout.setEndIconOnClickListener { showTimePicker() }
        binding.generateButton.setOnClickListener { generateChart() }

        binding.vedicChartView.setChart(null)
    }

    private fun setupPlaceInput() {
        val names = CityDatabase.names()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, names)
        binding.placeInput.setAdapter(adapter)
        binding.placeInput.threshold = 1
        binding.placeInput.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position)
            selectedCity = name?.let { CityDatabase.findByName(it) }
        }
        // Also handle manual text where user types and presses Generate
        binding.placeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val text = binding.placeInput.text?.toString()?.trim().orEmpty()
                selectedCity = CityDatabase.findByName(text)
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
            .setTimeFormat(TimeFormat.CLOCK_12H)
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
        val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
        val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")

        val dateText = date?.format(dateFormatter) ?: ""
        val timeText = time?.format(timeFormatter) ?: ""

        binding.dateInput.setText(dateText)
        binding.timeInput.setText(timeText)

        binding.dateTimeValue.text = when {
            date == null && time == null -> "No date and time selected"
            date != null && time == null -> "Date: $dateText"
            date == null && time != null -> "Time: $timeText"
            else -> "$dateText • $timeText"
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

        runCatching { calculator.generateChart(birthDetails) }
            .onSuccess { chart ->
                binding.vedicChartView.setChart(chart)
                val ascText = "Ascendant: ${chart.ascendantSign.symbol} ${chart.ascendantSign.displayName} (${formatDegree(chart.ascendantDegree)})"
                binding.chartTitle.text = ascText
                renderPlanets(chart)
                birthDetails.name?.let { binding.subtitleText.text = getString(R.string.chart_generated_for, it) }
            }
            .onFailure {
                Snackbar.make(binding.root, getString(R.string.calculation_error), Snackbar.LENGTH_LONG).show()
            }
    }

    private fun renderPlanets(chart: com.aakash.astro.astrology.ChartResult) {
        binding.planetContainer.removeAllViews()
        val inflater: LayoutInflater = LayoutInflater.from(this)

        chart.planets.forEach { planet ->
            val itemBinding = ItemPlanetPositionBinding.inflate(inflater, binding.planetContainer, false)
            itemBinding.planetName.text = planet.name
            val degreeText = formatDegree(planet.degree)
            itemBinding.planetDetails.text = getString(
                R.string.planet_position_format,
                "${planet.sign.symbol} ${planet.sign.displayName}",
                degreeText,
                "House ${planet.house}"
            )
            binding.planetContainer.addView(itemBinding.root)
        }
    }

    private fun formatDegree(value: Double): String {
        var normalized = ((value % 360.0) + 360.0) % 360.0
        var degrees = floor(normalized).toInt()
        var minutes = ((normalized - degrees) * 60).roundToInt()
        if (minutes == 60) {
            minutes = 0
            degrees = (degrees + 1) % 360
        }
        return String.format("%02d° %02d'", degrees, minutes)
    }
}


