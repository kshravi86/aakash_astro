package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ActivityTransitComboAnyBinding
import com.aakash.astro.databinding.ItemTransitComboPlanetBinding
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

class TransitComboAnyActivity : AppCompatActivity() {
    private lateinit var binding: ActivityTransitComboAnyBinding
    private val accurate = AccurateCalculator()
    private var natalChart: com.aakash.astro.astrology.ChartResult? = null

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null
    private val deviceZone: ZoneId by lazy { ZoneId.systemDefault() }
    private val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault())
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize UI, load ephemeris, and prepare default inputs.
        super.onCreate(savedInstanceState)
        binding = ActivityTransitComboAnyBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        preloadNatalFromIntent()
        setupInputs()
        setupActions()
        applyNow()
        binding.resultContainer.visibility = android.view.View.GONE
    }

    private fun preloadNatalFromIntent() {
        // Load natal chart data from the intent for combined analysis.
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
        // Wire the place/date/time inputs and helpers.
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
        binding.nowButton.setOnClickListener { applyNow() }
    }

    private fun setupActions() {
        // Handle compute requests and basic validation.
        binding.computeButton.setOnClickListener {
            val date = selectedDate
            val time = selectedTime
            val city = selectedCity
            if (date == null || time == null || city == null) {
                if (date == null && time == null && city == null) finish() else Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
                return@setOnClickListener
            }
            computeBoth(date, time, city)
        }
    }

    private fun applyNow() {
        // Prefill date/time inputs with the current moment.
        val now = ZonedDateTime.now(deviceZone).truncatedTo(ChronoUnit.MINUTES)
        selectedDate = now.toLocalDate()
        selectedTime = now.toLocalTime()
        updateDateTimeFields()
    }

    private fun updateDateTimeFields() {
        // Sync selected values back into the input fields.
        val dateText = selectedDate?.format(dateFormatter).orEmpty()
        val timeText = selectedTime?.format(timeFormatter12).orEmpty()
        binding.dateInput.setText(dateText)
        binding.timeInput.setText(timeText)
    }

    private fun computeBoth(date: LocalDate, time: LocalTime, city: City) {
        // Generate transit and natal outputs and render combined results.
        val zone = deviceZone
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

        binding.title.text = getString(R.string.transit_combo_any_title)
        val dateText = dateFormatter.format(zdt)
        val timeText = timeFormatter12.format(zdt)
        binding.subtitle.text = "@ $dateText $timeText (${zone.id})\n${city.name}: " +
                String.format("%.4f, %.4f", city.latitude, city.longitude)

        renderCombined(transitChart, natal)
        binding.resultContainer.visibility = android.view.View.VISIBLE
    }

    private fun renderCombined(transitChart: com.aakash.astro.astrology.ChartResult, natal: com.aakash.astro.astrology.ChartResult) {
        // Render combined transit verdicts with Tara Bala context.
        binding.comboContainer.removeAllViews()
        val inflater = LayoutInflater.from(this)
        val sunDegree = transitChart.planets.find { it.planet == Planet.SUN }?.degree
        val natalMoonDeg = natal.planets.find { it.planet == Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)

        transitChart.planets.forEach { transitPlanet ->
            val item = ItemTransitComboPlanetBinding.inflate(inflater, binding.comboContainer, false)
            val nameWithRetro = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name
            item.planetName.text = nameWithRetro

            // Transit position
            val degreeText = formatDegreeWithSign(transitPlanet.degree)
            item.transitPosition.text = "Transit: ${transitPlanet.sign.displayName} $degreeText (House ${transitPlanet.house})"

            // Verdict by natal house
            val natalHouse = calculateNatalHouse(transitPlanet.degree, natal.ascendantSign)
            val verdict = houseVerdict(transitPlanet.planet, natalHouse)
            val verdictText = when (verdict) {
                HouseVerdict.Good -> getString(R.string.transit_verdict_good)
                HouseVerdict.Bad -> getString(R.string.transit_verdict_bad)
                HouseVerdict.Neutral -> getString(R.string.transit_verdict_neutral)
            }
            var verdictColor = when (verdict) {
                HouseVerdict.Good -> resources.getColor(R.color.planet_favorable, theme)
                HouseVerdict.Bad -> resources.getColor(R.color.planet_unfavorable, theme)
                HouseVerdict.Neutral -> resources.getColor(R.color.planet_neutral, theme)
            }
            val combust = sunDegree?.let { isCombust(transitPlanet.planet, transitPlanet.degree, it) } ?: false
            val cautionParts = mutableListOf<String>()
            if (transitPlanet.isRetrograde) cautionParts += getString(R.string.retro_caution)
            if (combust) cautionParts += getString(R.string.combust_caution)
            val caution = if (cautionParts.isNotEmpty()) " \u2022 " + cautionParts.joinToString(", ") else ""
            if (transitPlanet.isRetrograde || combust) {
                verdictColor = resources.getColor(R.color.planet_retrograde, theme)
            }
            item.natalHouseVerdict.text = getString(R.string.transit_natal_house_verdict, natalHouse, verdictText) + caution
            item.natalHouseVerdict.setTextColor(verdictColor)

            // Tara Bala
            val (nakName, pada) = NakshatraCalc.fromLongitude(transitPlanet.degree)
            item.taraInfo.text = "$nakName (Pada $pada)"
            val transitNak = TaraBalaCalc.nakshatraNumber1Based(transitPlanet.degree)
            val taraClass = TaraBalaCalc.tClass(natalMoonNak, transitNak)
            val taraName = TaraBalaCalc.taraNames[taraClass - 1]
            val taraNote = TaraBalaCalc.taraNotes[taraClass] ?: ""
            val taraResult = when (taraClass) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }
            item.taraInfo.text = "$taraName ($taraNote) • $nakName (Pada $pada)"
            item.taraResult.text = taraResult
            val taraColor = when (taraResult) {
                "Favorable" -> R.color.planet_favorable
                "Unfavorable" -> R.color.planet_unfavorable
                else -> R.color.planet_neutral
            }
            val c = androidx.core.content.ContextCompat.getColor(this, taraColor)
            item.taraResult.setTextColor(c)
            item.taraInfo.setTextColor(c)

            binding.comboContainer.addView(item.root)
        }
    }

    private fun calculateNatalHouse(transitDegree: Double, natalAscSign: ZodiacSign): Int {
        // Map a transit longitude into the natal house number.
        val transitSign = ZodiacSign.fromDegree(transitDegree)
        return 1 + (transitSign.ordinal - natalAscSign.ordinal + 12) % 12
    }

    private enum class HouseVerdict { Good, Bad, Neutral }
    // Mark natural malefics to drive verdict logic.
    private fun isMalefic(planet: Planet): Boolean = when (planet) {
        Planet.SATURN, Planet.MARS, Planet.RAHU, Planet.KETU, Planet.SUN -> true
        else -> false
    }
    private fun houseVerdict(planet: Planet, house: Int): HouseVerdict {
        // Classify the transit house impact based on benefic/malefic rules.
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
                else -> HouseVerdict.Neutral
            }
        } else {
            when {
                house in beneficGood -> HouseVerdict.Good
                house in dusthana -> HouseVerdict.Bad
                else -> HouseVerdict.Neutral
            }
        }
    }

    private fun isCombust(planet: Planet, planetDegree: Double, sunDegree: Double): Boolean {
        // Flag combustion by checking proximity to the Sun.
        if (planet == Planet.SUN) return false
        val diff = angularSeparation(planetDegree, sunDegree)
        return diff <= 10.0
    }
    private fun angularSeparation(a: Double, b: Double): Double {
        // Compute the smallest angular distance on a circle.
        val diff = kotlin.math.abs(((a - b) % 360.0 + 540.0) % 360.0 - 180.0)
        return diff
    }

    private fun updatePlaceCoords() {
        // Show resolved coordinates for the selected city.
        val c = selectedCity
        val summary = if (c != null) {
            val lat = String.format("%.4f", c.latitude)
            val lon = String.format("%.4f", c.longitude)
            "${c.name}: ${lat}, ${lon}"
        } else ""
        binding.placeCoords.text = summary
    }

    private fun showDatePicker() {
        // Show date picker with current selection preselected.
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
        picker.show(supportFragmentManager, "transit_combo_any_date")
    }

    private fun showTimePicker() {
        // Show time picker with current selection preselected.
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
        picker.show(supportFragmentManager, "transit_combo_any_time")
    }

    private fun formatDegree(value: Double): String {
        // Format degrees and minutes with proper roll-over.
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
        // Combine absolute and intra-sign degrees for display.
        val absText = formatDegree(value)
        val inSign = ((value % 30.0) + 30.0) % 30.0
        val inSignText = formatDegree(inSign)
        return "$absText ($inSignText)"
    }
}
