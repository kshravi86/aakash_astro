package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.location.Geocoder
import android.text.Editable
import android.text.TextWatcher
import android.os.Handler
import android.os.Looper
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
import androidx.recyclerview.widget.GridLayoutManager
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
    private val fallbackCalculator = AstrologyCalculator()
    private val accurateCalculator = AccurateCalculator()
    private val uiHandler = Handler(Looper.getMainLooper())
    private var placeSearchRunnable: Runnable? = null
    private val dynamicCityMap: MutableMap<String, City> = mutableMapOf()
    private var suppressPlaceSuggestions: Boolean = false

    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.topAppBar)

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

        // Make date/time fields and end icons open pickers
        binding.dateInput.setOnClickListener { showDatePicker() }
        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker() }
        binding.timeInput.setOnClickListener { showTimePicker() }
        binding.timeInputLayout.setEndIconOnClickListener { showTimePicker() }
        binding.generateButton.setOnClickListener { generateChart() }
        setupActionGrid()
        prepareEphemeris()

        // Defaults: current date/time and Bengaluru as birthplace
        initializeDefaultsAndGenerate()
        applyPrefillFromIntent(intent)
    }
    private fun setupActionGrid() {
        val grid = binding.actionGrid
        grid.layoutManager = GridLayoutManager(this, 2)
        val items = listOf(
            com.aakash.astro.ui.ActionTile("dasha", "Vimshottari Dasha", "Mahadasha/Antar periods"),
            com.aakash.astro.ui.ActionTile("chara", "Chara Dasha", "Jaimini periods"),
            com.aakash.astro.ui.ActionTile("yogini", "Yogini Dasha", "8-yogini cycle"),
            com.aakash.astro.ui.ActionTile("panchanga", "Panchanga", "Tithi, Vara, Nakshatra, Yoga, Karana"),
            com.aakash.astro.ui.ActionTile("transit", "Transit Chart", "Current transits"),
            com.aakash.astro.ui.ActionTile("overlay_sa_ju", "Transit Overlay (Sa/Ju)", "Overlay on natal houses"),
            com.aakash.astro.ui.ActionTile("overlay_nodes", "Transit Overlay (Ra/Ke)", "Overlay on natal houses"),
            com.aakash.astro.ui.ActionTile("vargas", "Divisional Charts", "Shodasha Vargas"),
            com.aakash.astro.ui.ActionTile("d60", "D60 Shashtiamsha", "60th divisional placements"),
            com.aakash.astro.ui.ActionTile("yogas", "Yogas", "Detected yogas"),
            com.aakash.astro.ui.ActionTile("pushkara", "Pushkara Navamsha", "Elemental pushkara bands"),
            com.aakash.astro.ui.ActionTile("yogi", "Yogi / Sahayogi / Avayogi", "Yogi point and lords"),
            com.aakash.astro.ui.ActionTile("shadbala", "Shadbala", "Sixfold strength"),
            com.aakash.astro.ui.ActionTile("sav", "Sarva Ashtakavarga", "Total bindus"),
            com.aakash.astro.ui.ActionTile("bav", "Ashtakavarga Details (BAV)", "Bhinnashtakavarga"),
            com.aakash.astro.ui.ActionTile("karakas", "Jaimini Karakas", "Atmakaraka … Darakaraka"),
            com.aakash.astro.ui.ActionTile("arudha", "Arudha Padas", "Padas for all houses"),
            com.aakash.astro.ui.ActionTile("ishta", "Ishta Devata", "Karakamsa based"),
            com.aakash.astro.ui.ActionTile("ikh", "Ishta/Kashta/Harsha", "IKH metrics"),
            com.aakash.astro.ui.ActionTile("tara", "Tara Bala", "Favorable by nakshatra")
        )
        grid.adapter = com.aakash.astro.ui.ActionTileAdapter(items) { item ->
            when (item.id) {
                "dasha" -> openDasha()
                "chara" -> openCharaDasha()
                "yogini" -> openYoginiDasha()
                "panchanga" -> openPanchanga()
                "transit" -> openTransit()
                "overlay_sa_ju" -> openTransitOverlay()
                "overlay_nodes" -> openTransitOverlayNodes()
                "vargas" -> openVargas()
                "d60" -> openD60()
                "yogas" -> openYogas()
                "pushkara" -> openPushkara()
                "yogi" -> openYogi()
                "shadbala" -> openShadbala()
                "sav" -> openSAV()
                "bav" -> openAshtakavargaBAV()
                "karakas" -> openJaiminiKarakas()
                "arudha" -> openArudha()
                "ishta" -> openIshtaDevata()
                "ikh" -> openIKH()
                "tara" -> openTaraBala()
            }
        }
    }
    private fun openTaraBala() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, TaraBalaActivity::class.java).apply {
            putExtra(TaraBalaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(TaraBalaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(TaraBalaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(TaraBalaActivity.EXTRA_LAT, city.latitude)
            putExtra(TaraBalaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save -> {
                saveCurrentHoroscope()
                true
            }
            R.id.action_saved -> {
                startActivity(android.content.Intent(this, SavedHoroscopesActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun saveCurrentHoroscope() {
        val date = selectedDate
        val time = selectedTime
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
        if (date == null || time == null || city == null) {
            Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
            return
        }
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val name = binding.nameInput.text?.toString()
        val saved = com.aakash.astro.storage.SavedStore.save(
            this,
            name,
            birthDateTime.toInstant().toEpochMilli(),
            zone.id,
            city.latitude,
            city.longitude
        )
        Snackbar.make(binding.root, "Saved: ${saved.id}", Snackbar.LENGTH_LONG).show()
    }
    private fun prepareEphemeris() {
        val dir = EphemerisPreparer.prepare(this)
        dir?.let { accurateCalculator.setEphePath(it.absolutePath) }
    }
    private fun setupPlaceInput() {
        val baseNames = CityDatabase.names().toMutableList()
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, baseNames)
        adapter.setNotifyOnChange(true)
        binding.placeInput.setAdapter(adapter)
        binding.placeInput.threshold = 1

        binding.placeInput.setOnItemClickListener { _, _, position, _ ->
            val name = adapter.getItem(position)
            selectedCity = when {
                name.isNullOrBlank() -> null
                dynamicCityMap.containsKey(name) -> dynamicCityMap[name]
                else -> CityDatabase.findByName(name)
            }
            updatePlaceCoords()

            // Stop showing suggestions once a selection is made
            suppressPlaceSuggestions = true
            placeSearchRunnable?.let { uiHandler.removeCallbacks(it) }
            binding.placeInput.dismissDropDown()
            binding.placeInput.clearFocus()
            hideKeyboard()
            // Ensure long names show from the start
            binding.placeInput.setSelection(0)
        }

        // Debounced online geocoding suggestions within India
        binding.placeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressPlaceSuggestions) {
                    // Skip one cycle after programmatic or selection-driven text change
                    suppressPlaceSuggestions = false
                    return
                }
                val query = s?.toString()?.trim().orEmpty()
                placeSearchRunnable?.let { uiHandler.removeCallbacks(it) }
                if (query.length < 3) return
                val runnable = Runnable {
                    fetchIndiaSuggestions(query)?.let { results ->
                        // Merge: dynamic results first, then base names that match
                        dynamicCityMap.clear()
                        val dynamicNames = results.map { city ->
                            dynamicCityMap[city.name] = city
                            city.name
                        }
                        val fallbackMatches = CityDatabase.names().filter { it.contains(query, ignoreCase = true) }
                        val merged = (dynamicNames + fallbackMatches).distinct().take(20)
                        runOnUiThread {
                            adapter.clear()
                            adapter.addAll(merged)
                            adapter.notifyDataSetChanged()
                            if (binding.placeInput.hasFocus() && !binding.placeInput.isPopupShowing) {
                                binding.placeInput.showDropDown()
                            }
                        }
                    }
                }
                placeSearchRunnable = runnable
                uiHandler.postDelayed(runnable, 300)
            }
        })

        // Resolve to a city when focus leaves the field
        binding.placeInput.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                binding.placeInput.dismissDropDown()
                val text = binding.placeInput.text?.toString()?.trim().orEmpty()
                selectedCity = when {
                    text.isBlank() -> null
                    dynamicCityMap.containsKey(text) -> dynamicCityMap[text]
                    CityDatabase.findByName(text) != null -> CityDatabase.findByName(text)
                    else -> geocodeFirstIndia(text)
                }
                updatePlaceCoords()
            }
        }
    }

    private fun fetchIndiaSuggestions(query: String): List<City>? {
        return runCatching {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(this)
            // Rough India bounding box: lat 6..37, lon 68..97
            val list = geocoder.getFromLocationName("$query, India", 10, 6.0, 68.0, 37.0, 97.0)
            list?.mapNotNull { addr ->
                val lat = addr.latitude
                val lon = addr.longitude
                val locality = listOfNotNull(addr.locality, addr.subAdminArea, addr.adminArea).joinToString(", ")
                val name = if (locality.isNotBlank()) locality else addr.featureName ?: return@mapNotNull null
                City(name, lat, lon)
            }?.distinctBy { it.name }
        }.getOrNull()
    }

    private fun geocodeFirstIndia(query: String): City? {
        return runCatching {
            if (!Geocoder.isPresent()) return null
            val geocoder = Geocoder(this)
            val list = geocoder.getFromLocationName("$query, India", 1, 6.0, 68.0, 37.0, 97.0)
            val a = list?.firstOrNull() ?: return null
            val locality = listOfNotNull(a.locality, a.subAdminArea, a.adminArea).joinToString(", ")
            val name = if (locality.isNotBlank()) locality else a.featureName ?: query
            City(name, a.latitude, a.longitude)
        }.getOrNull()
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
        val dateText = date?.format(DateTimeFormatter.ofPattern("dd MMM yyyy")) ?: ""
        val timeText = time?.format(DateTimeFormatter.ofPattern("hh:mm a")) ?: ""

        // Reflect selection in the input fields
        binding.dateInput.setText(dateText)
        binding.timeInput.setText(timeText)

        // Keep a compact summary below
        binding.dateTimeValue.text = when {
            date == null && time == null -> "No date and time selected"
            date != null && time == null -> "Date: $dateText"
            date == null && time != null -> "Time: $timeText"
            else -> "$dateText • $timeText"
        }
    }

    private fun generateChart() {
        // Hide keyboard and clear focus
        hideKeyboard()
        binding.root.clearFocus()

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

    private fun openYoginiDasha() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, YoginiDashaActivity::class.java).apply {
            putExtra(YoginiDashaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(YoginiDashaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(YoginiDashaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(YoginiDashaActivity.EXTRA_LAT, city.latitude)
            putExtra(YoginiDashaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openPushkara() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, PushkaraNavamshaActivity::class.java).apply {
            putExtra(PushkaraNavamshaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(PushkaraNavamshaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(PushkaraNavamshaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(PushkaraNavamshaActivity.EXTRA_LAT, city.latitude)
            putExtra(PushkaraNavamshaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openIKH() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, IshtaKashtaHarshaActivity::class.java).apply {
            putExtra(IshtaKashtaHarshaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(IshtaKashtaHarshaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(IshtaKashtaHarshaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(IshtaKashtaHarshaActivity.EXTRA_LAT, city.latitude)
            putExtra(IshtaKashtaHarshaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openCharaDasha() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, CharaDashaActivity::class.java).apply {
            putExtra(CharaDashaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(CharaDashaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(CharaDashaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(CharaDashaActivity.EXTRA_LAT, city.latitude)
            putExtra(CharaDashaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openYogi() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty()) ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, YogiActivity::class.java).apply {
            putExtra(YogiActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(YogiActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(YogiActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(YogiActivity.EXTRA_LAT, city.latitude)
            putExtra(YogiActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openTransit() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, TransitActivity::class.java).apply {
            putExtra(TransitActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(TransitActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(TransitActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(TransitActivity.EXTRA_LAT, city.latitude)
            putExtra(TransitActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openTransitOverlay() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, OverlayActivity::class.java).apply {
            putExtra(OverlayActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(OverlayActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(OverlayActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(OverlayActivity.EXTRA_LAT, city.latitude)
            putExtra(OverlayActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openTransitOverlayNodes() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, OverlayNodesActivity::class.java).apply {
            putExtra(OverlayNodesActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(OverlayNodesActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(OverlayNodesActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(OverlayNodesActivity.EXTRA_LAT, city.latitude)
            putExtra(OverlayNodesActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openVargas() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, DivisionalChartsActivity::class.java).apply {
            putExtra(DivisionalChartsActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(DivisionalChartsActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(DivisionalChartsActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(DivisionalChartsActivity.EXTRA_LAT, city.latitude)
            putExtra(DivisionalChartsActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openD60() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, D60Activity::class.java).apply {
            putExtra(D60Activity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(D60Activity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(D60Activity.EXTRA_ZONE_ID, zone.id)
            putExtra(D60Activity.EXTRA_LAT, city.latitude)
            putExtra(D60Activity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openShadbala() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, ShadbalaActivity::class.java).apply {
            putExtra(ShadbalaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(ShadbalaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(ShadbalaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(ShadbalaActivity.EXTRA_LAT, city.latitude)
            putExtra(ShadbalaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openSAV() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, SarvaAshtakavargaActivity::class.java).apply {
            putExtra(SarvaAshtakavargaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(SarvaAshtakavargaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(SarvaAshtakavargaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(SarvaAshtakavargaActivity.EXTRA_LAT, city.latitude)
            putExtra(SarvaAshtakavargaActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openAshtakavargaBAV() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, AshtakavargaBavActivity::class.java).apply {
            putExtra(AshtakavargaBavActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(AshtakavargaBavActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(AshtakavargaBavActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(AshtakavargaBavActivity.EXTRA_LAT, city.latitude)
            putExtra(AshtakavargaBavActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openJaiminiKarakas() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, JaiminiKarakasActivity::class.java).apply {
            putExtra(JaiminiKarakasActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(JaiminiKarakasActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(JaiminiKarakasActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(JaiminiKarakasActivity.EXTRA_LAT, city.latitude)
            putExtra(JaiminiKarakasActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openArudha() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, ArudhaPadasActivity::class.java).apply {
            putExtra(ArudhaPadasActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(ArudhaPadasActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(ArudhaPadasActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(ArudhaPadasActivity.EXTRA_LAT, city.latitude)
            putExtra(ArudhaPadasActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openIshtaDevata() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, IshtaDevataActivity::class.java).apply {
            putExtra(IshtaDevataActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(IshtaDevataActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(IshtaDevataActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(IshtaDevataActivity.EXTRA_LAT, city.latitude)
            putExtra(IshtaDevataActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openYogas() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, YogasActivity::class.java).apply {
            putExtra(YogasActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(YogasActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(YogasActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(YogasActivity.EXTRA_LAT, city.latitude)
            putExtra(YogasActivity.EXTRA_LON, city.longitude)
        }
        startActivity(intent)
    }

    private fun openPanchanga() {
        val date = selectedDate ?: return
        val time = selectedTime ?: return
        val city = selectedCity
            ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            ?: return
        val zone = ZoneId.systemDefault()
        val birthDateTime = LocalDateTime.of(date, time).atZone(zone)
        val intent = android.content.Intent(this, PanchangaActivity::class.java).apply {
            putExtra(PanchangaActivity.EXTRA_NAME, binding.nameInput.text?.toString())
            putExtra(PanchangaActivity.EXTRA_EPOCH_MILLIS, birthDateTime.toInstant().toEpochMilli())
            putExtra(PanchangaActivity.EXTRA_ZONE_ID, zone.id)
            putExtra(PanchangaActivity.EXTRA_LAT, city.latitude)
            putExtra(PanchangaActivity.EXTRA_LON, city.longitude)
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
            val lagnaUttama = com.aakash.astro.astrology.DrekkanaUtils.isUttamaDrekkana(chart.ascendantSign, chart.ascendantDegree)
            itemBinding.uttamaStatus.text = getString(
                R.string.uttama_drekkana_format,
                if (lagnaUttama) getString(R.string.yes_label) else getString(R.string.no_label)
            )
            val lagnaDegInSign = ((chart.ascendantDegree % 30.0) + 30.0) % 30.0
            val lagnaVarg = com.aakash.astro.astrology.Vargottama.isVargottama(chart.ascendantSign, lagnaDegInSign)
            itemBinding.vargottamaStatus.text = getString(
                R.string.vargottama_format,
                if (lagnaVarg) getString(R.string.yes_label) else getString(R.string.no_label)
            )
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
            val isUttama = com.aakash.astro.astrology.DrekkanaUtils.isUttamaDrekkana(planet.sign, planet.degree)
            itemBinding.uttamaStatus.text = getString(
                R.string.uttama_drekkana_format,
                if (isUttama) getString(R.string.yes_label) else getString(R.string.no_label)
            )
            val degInSign = ((planet.degree % 30.0) + 30.0) % 30.0
            val isVarg = com.aakash.astro.astrology.Vargottama.isVargottama(planet.sign, degInSign)
            itemBinding.vargottamaStatus.text = getString(
                R.string.vargottama_format,
                if (isVarg) getString(R.string.yes_label) else getString(R.string.no_label)
            )
            binding.planetContainer.addView(itemBinding.root)
        }

        // Removed: Indu Lagna and Hora Lagna per user request

        // Finally, render the Vedic chart (South Indian style) at the end
        // Compute and show Ghatika Lagna (Jaimini) using exact steps
        run {
            val date = selectedDate
            val time = selectedTime
            val city = selectedCity ?: CityDatabase.findByName(binding.placeInput.text?.toString()?.trim().orEmpty())
            if (date != null && time != null && city != null) {
                val zone = java.time.ZoneId.systemDefault()
                val birthZdt = java.time.LocalDateTime.of(date, time).atZone(zone)
                com.aakash.astro.astrology.GhatikaLagnaCalc.compute(
                    birthZdt,
                    city.latitude,
                    city.longitude,
                    zone,
                    accurateCalculator
                )?.let { gl ->
                    val itemBinding = ItemPlanetPositionBinding.inflate(inflater, binding.planetContainer, false)
                    itemBinding.planetName.text = "Ghatika Lagna (Jaimini)"
                    val degText = formatDegreeWithSign(gl.longitude)
                    itemBinding.planetDetails.text = getString(
                        R.string.planet_position_format,
                        gl.sign.displayName,
                        degText,
                        "—"
                    )
                    itemBinding.planetNakshatra.text = ""
                    itemBinding.uttamaStatus.text = ""
                    binding.planetContainer.addView(itemBinding.root)
                }

                // Immediately below GL, show Hora Lagna (Jaimini)
                com.aakash.astro.astrology.HoraLagnaJaiminiCalc.compute(
                    birthZdt,
                    city.latitude,
                    city.longitude,
                    zone,
                    accurateCalculator
                )?.let { hl ->
                    val itemBinding = ItemPlanetPositionBinding.inflate(inflater, binding.planetContainer, false)
                    itemBinding.planetName.text = "Hora Lagna (Jaimini)"
                    val degText = formatDegreeWithSign(hl.longitude)
                    itemBinding.planetDetails.text = getString(
                        R.string.planet_position_format,
                        hl.sign.displayName,
                        degText,
                        "—"
                    )
                    itemBinding.planetNakshatra.text = ""
                    itemBinding.uttamaStatus.text = ""
                    binding.planetContainer.addView(itemBinding.root)
                }
            }
        }

        binding.vedicChartView.setChart(chart)

        // Notify and guide the user to the chart
        Snackbar.make(binding.root, "Chart generated", Snackbar.LENGTH_LONG)
            .setAction("View") { scrollAndHighlightChart() }
            .show()
        scrollAndHighlightChart()
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
        // Set default date/time (18/05/1993, 10:30 PM) if not provided
        if (selectedDate == null) selectedDate = LocalDate.of(1993, 5, 18)
        if (selectedTime == null) selectedTime = LocalTime.of(22, 30)
        updateDateTimeSummary()

        // Default city: Bengaluru (fallback to Bangalore capitalization/alias)
        if (selectedCity == null) {
            selectedCity = CityDatabase.findByName("Bengaluru")
                ?: CityDatabase.findByName("Bangalore")
        }
        selectedCity?.let {
            // Avoid triggering filtering when setting text programmatically
            binding.placeInput.setText(it.name, false)
            // Show start of long city names
            binding.placeInput.setSelection(0)
        }
        updatePlaceCoords()

        // Auto-generate chart once defaults are set
        generateChart()
    }

    private fun applyPrefillFromIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val epoch = intent.getLongExtra("prefill_epochMillis", -1L)
        val zoneId = intent.getStringExtra("prefill_zoneId")
        val name = intent.getStringExtra("prefill_name")
        val lat = intent.getDoubleExtra("prefill_lat", Double.NaN)
        val lon = intent.getDoubleExtra("prefill_lon", Double.NaN)
        if (epoch > 0 && zoneId != null && !lat.isNaN() && !lon.isNaN()) {
            val zone = java.time.ZoneId.of(zoneId)
            val zdt = java.time.Instant.ofEpochMilli(epoch).atZone(zone)
            selectedDate = zdt.toLocalDate()
            selectedTime = zdt.toLocalTime()
            name?.let { binding.nameInput.setText(it) }
            selectedCity = com.aakash.astro.geo.City("Custom", lat, lon)
            binding.placeInput.setText("Custom", false)
            binding.placeInput.setSelection(0)
            updateDateTimeSummary()
            updatePlaceCoords()
            generateChart()
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

    private fun hideKeyboard() {
        val imm = getSystemService(android.content.Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        currentFocus?.let { view ->
            imm?.hideSoftInputFromWindow(view.windowToken, 0)
        }
    }

    private fun scrollAndHighlightChart() {
        // Smooth scroll to the chart section
        binding.mainScroll.post {
            val y = (binding.sectionChart.top - dp(12)).coerceAtLeast(0)
            binding.mainScroll.smoothScrollTo(0, y)
        }

        // Briefly accent the section header color
        val original = binding.sectionChart.currentTextColor
        val accent = resources.getColor(R.color.accent_teal, theme)
        binding.sectionChart.setTextColor(accent)
        uiHandler.postDelayed({ binding.sectionChart.setTextColor(original) }, 1600)

        // Pulse the chart view for visibility
        binding.vedicChartView.animate()
            .scaleX(1.03f)
            .scaleY(1.03f)
            .setDuration(160)
            .withEndAction {
                binding.vedicChartView.animate().scaleX(1f).scaleY(1f).setDuration(160).start()
            }
            .start()
        android.animation.ObjectAnimator.ofFloat(binding.vedicChartView, "alpha", 0.6f, 1.0f).apply {
            duration = 700
            start()
        }
    }

    private fun dp(value: Int): Int {
        val density = resources.displayMetrics.density
        return (value * density).toInt()
    }
}
