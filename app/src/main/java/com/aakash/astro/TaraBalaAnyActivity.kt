package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.databinding.ActivityTaraBalaAnyBinding
import com.aakash.astro.databinding.ItemTransitTaraBinding
import com.aakash.astro.geo.City
import com.aakash.astro.geo.CityDatabase
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.*

class TaraBalaAnyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTaraBalaAnyBinding
    private val accurate = AccurateCalculator()
    private var natalChart: com.aakash.astro.astrology.ChartResult? = null

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTaraBalaAnyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        preloadNatalFromIntent()
        setupInputs()
        setupActions()

        binding.resultContainer.visibility = android.view.View.GONE
    }

    private fun preloadNatalFromIntent() {
        val name = intent.getStringExtra(TaraBalaActivity.EXTRA_NAME)
        val zoneId = intent.getStringExtra(TaraBalaActivity.EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(TaraBalaActivity.EXTRA_LAT, Double.NaN)
        val lon = intent.getDoubleExtra(TaraBalaActivity.EXTRA_LON, Double.NaN)
        val birthEpochMillis = intent.getLongExtra(TaraBalaActivity.EXTRA_EPOCH_MILLIS, -1L)
        if (birthEpochMillis > 0 && !lat.isNaN() && !lon.isNaN()) {
            val birthInstant = Instant.ofEpochMilli(birthEpochMillis).atZone(zoneId)
            val birthDetails = BirthDetails(name, birthInstant, lat, lon)
            natalChart = accurate.generateChart(birthDetails)
        }
    }

    private fun setupInputs() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, CityDatabase.names())
        binding.placeInput.setAdapter(adapter)
        binding.placeInput.threshold = 1
        binding.placeInput.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position)
            selectedCity = name?.let { CityDatabase.findByName(it) }
            updatePlaceCoords()
        }

        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker() }
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.timeInputLayout.setEndIconOnClickListener { showTimePicker() }
        binding.timeInput.setOnClickListener { showTimePicker() }
    }

    private fun setupActions() {
        binding.computeButton.setOnClickListener {
            val date = selectedDate
            val time = selectedTime
            val city = selectedCity
            if (date == null || time == null || city == null) {
                if (date == null && time == null && city == null) finish() else Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            computeTransitTara(date, time, city)
        }
    }

    private fun computeTransitTara(date: LocalDate, time: LocalTime, city: City) {
        val zone = ZoneId.systemDefault()
        val zdt = LocalDateTime.of(date, time).atZone(zone)
        val details = BirthDetails(null, zdt, city.latitude, city.longitude)
        val transitChart = accurate.generateChart(details)
        val natal = natalChart ?: run {
            Snackbar.make(binding.root, getString(R.string.transit_natal_missing), Snackbar.LENGTH_LONG).show()
            return
        }
        if (transitChart == null) {
            Snackbar.make(binding.root, getString(R.string.transit_engine_missing), Snackbar.LENGTH_LONG).show()
            return
        }

        binding.title.text = getString(R.string.tara_any_title)
        binding.subtitle.text = "@ ${zdt.toLocalDate()} ${zdt.toLocalTime().withSecond(0).withNano(0)} (${zone.id})\n${city.name}: " +
                String.format("%.4f, %.4f", city.latitude, city.longitude)

        renderTransitTaraBala(transitChart, natal)
        binding.resultContainer.visibility = android.view.View.VISIBLE
    }

    private fun renderTransitTaraBala(
        transitChart: com.aakash.astro.astrology.ChartResult,
        natalChart: com.aakash.astro.astrology.ChartResult
    ) {
        val inflater = LayoutInflater.from(this)
        binding.transitRowContainer.removeAllViews()

        val natalMoonDeg = natalChart.planets.find { it.planet == com.aakash.astro.astrology.Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)

        transitChart.planets.forEach { transitPlanet ->
            val item = ItemTransitTaraBinding.inflate(inflater, binding.transitRowContainer, false)
            val nameWithRetro = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name
            item.planetName.text = nameWithRetro

            val (nakName, pada) = NakshatraCalc.fromLongitude(transitPlanet.degree)
            item.nakshatraInfo.text = "$nakName (Pada $pada)"

            val transitNak = TaraBalaCalc.nakshatraNumber1Based(transitPlanet.degree)
            val taraClass = TaraBalaCalc.tClass(natalMoonNak, transitNak)
            val taraName = TaraBalaCalc.taraNames[taraClass - 1]
            val taraNote = TaraBalaCalc.taraNotes[taraClass] ?: ""
            val taraResult = when (taraClass) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }

            item.taraValue.text = "$taraName ($taraNote)"
            item.resultValue.text = taraResult
            val colorRes = when (taraResult) {
                "Favorable" -> R.color.planet_favorable
                "Unfavorable" -> R.color.planet_unfavorable
                else -> R.color.planet_neutral
            }
            val c = androidx.core.content.ContextCompat.getColor(this, colorRes)
            item.resultValue.setTextColor(c)
            item.taraValue.setTextColor(c)

            binding.transitRowContainer.addView(item.root)
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

    private fun showDatePicker() {
        val picker = MaterialDatePicker.Builder.datePicker()
            .setTitleText(getString(R.string.pick_date))
            .build()
        picker.addOnPositiveButtonClickListener { millis ->
            val ld = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
            selectedDate = ld
            binding.dateInput.setText(ld.toString())
        }
        picker.show(supportFragmentManager, "tara_any_date")
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
        picker.show(supportFragmentManager, "tara_any_time")
    }
}
