package com.aakash.astro



import android.content.Intent
import android.content.res.ColorStateList
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.aakash.astro.BirthIntentPayload
import com.aakash.astro.putBirthPayload
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.AstrologyCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.databinding.ActivityMainBinding
import com.aakash.astro.databinding.ItemPlanetPositionBinding
import com.aakash.astro.geo.City
import com.aakash.astro.geo.CityDatabase
import com.aakash.astro.ui.ActionGridItem
import com.aakash.astro.ui.ActionTile
import com.aakash.astro.ui.ActionTileAdapter
import com.google.android.material.chip.Chip
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.floor
import kotlin.math.roundToInt



class MainActivity : AppCompatActivity() {



    private lateinit var binding: ActivityMainBinding

    private val fallbackCalculator = AstrologyCalculator()

    private val accurateCalculator = AccurateCalculator()

    private val uiHandler = Handler(Looper.getMainLooper())

    private var placeSearchRunnable: Runnable? = null

    private val dynamicCityMap: MutableMap<String, City> = mutableMapOf()

    private fun normalizedCityKey(name: String): String = name.trim().lowercase(Locale.ROOT)

    private fun rememberDynamicCity(city: City) {
        dynamicCityMap[normalizedCityKey(city.name)] = city
    }

    private fun resolveCityByName(rawName: String, allowGeocoder: Boolean = false): City? {
        val normalized = normalizedCityKey(rawName)
        if (normalized.isEmpty()) return null
        dynamicCityMap[normalized]?.let { return it }
        CityDatabase.findByName(rawName)?.let { return it }
        return if (allowGeocoder) geocodeFirstIndia(rawName) else null
    }

    private var suppressPlaceSuggestions: Boolean = false

    private var suppressDateChipCallback = false

    private var suppressTimeChipCallback = false

    private var lastQuickNowTime: LocalTime? = null

    private val morningPreset: LocalTime = LocalTime.of(6, 0)

    private val eveningPreset: LocalTime = LocalTime.of(18, 0)



    private var selectedDate: LocalDate? = null
    private var selectedTime: LocalTime? = null
    private var selectedCity: City? = null
    private val birthPrefs by lazy { getSharedPreferences("birth_defaults", MODE_PRIVATE) }
    private val recentActionPrefs by lazy { getSharedPreferences("recent_actions", MODE_PRIVATE) }
    private val defaultCityFallback = City("Bengaluru", 12.9716, 77.5946)
    private val deviceZone: ZoneId by lazy { ZoneId.systemDefault() }
    private val dateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    private val timeFormatter24H: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val timeFormatter12H: DateTimeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
    private val actionTileLookup: MutableMap<String, ActionTile> = linkedMapOf()

    private data class BirthContext(
        val date: LocalDate,
        val time: LocalTime,
        val city: City,
        val zone: ZoneId,
        val birthDetails: BirthDetails,
        val rawName: String?
    ) {
        val epochMillis: Long = birthDetails.dateTime.toInstant().toEpochMilli()
    }


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
        setupDateInput()
        setupTimeInput()
        setupSmartInputHelpers()
        binding.generateButton.setOnClickListener { generateChart() }

        setupActionGrid()

        prepareEphemeris()



        // Defaults: current date/time and Bengaluru as birthplace

        initializeDefaultsAndGenerate()

        applyPrefillFromIntent(intent)

    }

    private fun setupActionGrid() {

        val foundationsLabel = getString(R.string.category_foundations)
        val predictiveLabel = getString(R.string.category_predictive)
        val utilitiesLabel = getString(R.string.category_utilities)

        val foundations = listOf(
            ActionTile("dasha", "Vimshottari Dasha", "Mahadasha/Antar periods", R.drawable.ic_clock_outline_24, R.color.accent_teal, foundationsLabel),
            ActionTile("panchanga", "Panchanga", "Tithi, Vara, Nakshatra, Yoga, Karana", R.drawable.ic_calendar_event_24, R.color.accent_orange, foundationsLabel),
            ActionTile("today_panchanga", "Today's Panchanga", "For current date", R.drawable.ic_calendar_event_24, R.color.accent_blue, foundationsLabel),
            ActionTile("yogas", "Yogas", "Detected yogas", R.drawable.ic_star_24, R.color.accent_purple, foundationsLabel)
        )

        val predictive = listOf(
            ActionTile("sav", "Sarva Ashtakavarga", "Total bindus", R.drawable.ic_dashboard_black_24dp, R.color.accent_teal, predictiveLabel),
            ActionTile("bav", "Ashtakavarga Details (BAV)", "Bhinnashtakavarga", R.drawable.ic_dashboard_black_24dp, R.color.accent_orange, predictiveLabel),
            ActionTile("karakas", "Jaimini Karakas", "Atmakaraka → Darakaraka", R.drawable.ic_dashboard_black_24dp, R.color.accent_purple, predictiveLabel),
            ActionTile("arudha", "Arudha Padas", "Padas for all houses", R.drawable.ic_dashboard_black_24dp, R.color.accent_blue, predictiveLabel),
            ActionTile("special_lagna", "Special Lagnas", "Arudha, Ghatika, Hora", R.drawable.ic_dashboard_black_24dp, R.color.accent_teal, predictiveLabel),
            ActionTile("tara", "Tara Bala", "Favorable by nakshatra", R.drawable.ic_chip_star, R.color.accent_orange, predictiveLabel),
            ActionTile("tara_any", "Tara Bala (Any Date)", "Transit tara for chosen instant", R.drawable.ic_chip_star, R.color.accent_purple, predictiveLabel),
            ActionTile("transit", "Transit Chart", "Current transits", R.drawable.ic_clock_outline_24, R.color.accent_blue, predictiveLabel),
            ActionTile("transit_any", "Transit (Any Date)", "Use selected date/time/place", R.drawable.ic_clock_outline_24, R.color.accent_teal, predictiveLabel),
            ActionTile("transit_combo_any", "Transit + Tara (Any Date)", "Verdicts + Tara Bala", R.drawable.ic_clock_outline_24, R.color.accent_orange, predictiveLabel),
            ActionTile("overlay_sa_ju", "Transit Overlay (Sa/Ju)", "Overlay on natal houses", R.drawable.ic_clock_outline_24, R.color.accent_purple, predictiveLabel),
            ActionTile("overlay_nodes", "Transit Overlay (Ra/Ke)", "Overlay on natal houses", R.drawable.ic_clock_outline_24, R.color.accent_blue, predictiveLabel)
        )

        val utilities = listOf(
            ActionTile("pushkara", "Pushkara Navamsha", "Elemental pushkara bands", R.drawable.ic_location_pin_24, R.color.accent_teal, utilitiesLabel),
            ActionTile("yogi", "Yogi / Sahayogi / Avayogi", "Yogi point and lords", R.drawable.ic_star_24, R.color.accent_orange, utilitiesLabel),
            ActionTile("ishta", "Ishta Devata", "Karakamsa based", R.drawable.ic_star_24, R.color.accent_purple, utilitiesLabel),
            ActionTile("sbc", "Sarvatobhadra Chakra", "28-star vedha map", R.drawable.ic_notifications_black_24dp, R.color.accent_blue, utilitiesLabel),
            ActionTile("kundali_match", "Kundali Matching", "Ashta-koota (36 gun)", R.drawable.ic_star_24, R.color.accent_blue, utilitiesLabel),
            ActionTile("sixtyfour_twenty_two", "64th D9 & 22nd D3", "From Lagna and Moon", R.drawable.ic_history_24, R.color.accent_teal, utilitiesLabel)
        )

        val categories = listOf(
            foundationsLabel to foundations,
            predictiveLabel to predictive,
            utilitiesLabel to utilities
        )

        val entries = mutableListOf<ActionGridItem>()
        actionTileLookup.clear()
        categories.forEach { (header, tiles) ->
            if (tiles.isEmpty()) return@forEach
            entries.add(ActionGridItem.Header(header))
            tiles.forEach { tile ->
                actionTileLookup[tile.id] = tile
                entries.add(ActionGridItem.Tile(tile))
            }
        }

        val layoutManager = GridLayoutManager(this, 2)
        val adapter = ActionTileAdapter(entries) { handleActionTileClick(it) }
        layoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (entries[position] is ActionGridItem.Header) layoutManager.spanCount else 1
            }
        }

        binding.actionGrid.layoutManager = layoutManager
        binding.actionGrid.adapter = adapter

        renderRecentActionsFromStorage()
    }

    private fun handleActionTileClick(item: ActionTile) {
        recordRecentAction(item.id)
        when (item.id) {
            "dasha" -> openDasha()
            "panchanga" -> openPanchanga()
            "today_panchanga" -> openTodayPanchanga()
            "transit" -> openTransit()
            "transit_any" -> openTransitAny()
            "overlay_sa_ju" -> openTransitOverlay()
            "overlay_nodes" -> openTransitOverlayNodes()
            "yogas" -> openYogas()
            "pushkara" -> openPushkara()
            "yogi" -> openYogi()
            "sav" -> openSAV()
            "bav" -> openAshtakavargaBAV()
            "karakas" -> openJaiminiKarakas()
            "arudha" -> openArudha()
            "special_lagna" -> openSpecialLagnas()
            "ishta" -> openIshtaDevata()
            "sbc" -> openSBC()
            "tara" -> openTaraBala()
            "tara_any" -> openTaraBalaAny()
            "transit_combo_any" -> openTransitComboAny()
            "kundali_match" -> openKundaliMatching()
            "sixtyfour_twenty_two" -> openSixtyFourTwentyTwo()
        }
    }

    private fun recordRecentAction(actionId: String) {
        val ids = readRecentActionIds()
        ids.remove(actionId)
        ids.add(0, actionId)
        val trimmed = ids.distinct().take(4)
        recentActionPrefs.edit().putString(RECENT_ACTION_KEY, trimmed.joinToString(",")).apply()
        renderRecentActionChips(trimmed.mapNotNull { actionTileLookup[it] })
    }

    private fun renderRecentActionsFromStorage() {
        val tiles = readRecentActionIds().mapNotNull { actionTileLookup[it] }
        renderRecentActionChips(tiles)
    }

    private fun renderRecentActionChips(tiles: List<ActionTile>) {
        val container = binding.recentActionsContainer
        val chipGroup = binding.recentActionChips
        chipGroup.removeAllViews()
        if (tiles.isEmpty()) {
            container.visibility = View.GONE
            return
        }
        container.visibility = View.VISIBLE
        tiles.forEach { tile ->
            val chip = Chip(this).apply {
                text = tile.title
                isCheckable = false
                setEnsureMinTouchTargetSize(false)
                val accent = ContextCompat.getColor(context, tile.accentColor)
                chipBackgroundColor = ColorStateList.valueOf(ColorUtils.setAlphaComponent(accent, 60))
                chipIcon = ContextCompat.getDrawable(context, tile.iconRes)
                chipIconTint = ColorStateList.valueOf(accent)
            }
            chip.setOnClickListener { handleActionTileClick(tile) }
            chipGroup.addView(chip)
        }
    }

    private fun readRecentActionIds(): MutableList<String> =
        recentActionPrefs.getString(RECENT_ACTION_KEY, "")
            ?.split(",")
            ?.mapNotNull { it.trim().takeIf(String::isNotEmpty) }
            ?.toMutableList()
            ?: mutableListOf()

    private fun openSBC() = launchBirthActivity<SarvatobhadraActivity>()

    private fun openTaraBala() = launchBirthActivity<TaraBalaActivity>()

    private fun openTaraBalaAny() {

        // Pass natal context for Moon reference; user picks transit inputs on the next page

        val context = buildBirthContext()

        val intent = android.content.Intent(this, TaraBalaAnyActivity::class.java).apply {

            context?.let { ctx ->

                putExtra(TaraBalaActivity.EXTRA_NAME, ctx.rawName)

                putExtra(TaraBalaActivity.EXTRA_EPOCH_MILLIS, ctx.epochMillis)

                putExtra(TaraBalaActivity.EXTRA_ZONE_ID, ctx.zone.id)

                putExtra(TaraBalaActivity.EXTRA_LAT, ctx.city.latitude)

                putExtra(TaraBalaActivity.EXTRA_LON, ctx.city.longitude)

            }

        }

        startActivity(intent)

    }



    private fun openTransitComboAny() {

        // Combined page: pass natal context; user picks transit inputs on the next page

        val context = buildBirthContext()

        val intent = android.content.Intent(this, TransitComboAnyActivity::class.java).apply {

            context?.let { ctx ->

                putExtra(TransitActivity.EXTRA_NAME, ctx.rawName)

                putExtra(TransitActivity.EXTRA_EPOCH_MILLIS, ctx.epochMillis)

                putExtra(TransitActivity.EXTRA_ZONE_ID, ctx.zone.id)

                putExtra(TransitActivity.EXTRA_LAT, ctx.city.latitude)

                putExtra(TransitActivity.EXTRA_LON, ctx.city.longitude)

            }

        }

        startActivity(intent)

    }

    private fun openKundaliMatching() {
        startActivity(android.content.Intent(this, KundaliMatchingActivity::class.java))
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

            R.id.action_privacy -> {

                startActivity(android.content.Intent(this, PrivacyActivity::class.java))

                true

            }

            else -> super.onOptionsItemSelected(item)

        }

    }



    private fun saveCurrentHoroscope() {

        withBirthContext(notifyOnMissing = true) { ctx ->
            val saved = com.aakash.astro.storage.SavedStore.save(
                this,
                ctx.rawName,
                ctx.epochMillis,
                ctx.zone.id,
                ctx.city.latitude,
                ctx.city.longitude
            )
            Snackbar.make(
                binding.root,
                getString(R.string.saved_chart_confirmation, saved.id),
                Snackbar.LENGTH_LONG
            ).show()
        }

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
            val resolved = resolveCityByName(name.orEmpty())

            if (resolved != null) {
                setSelectedCity(resolved, updateInput = false)
            } else {
                selectedCity = null
                updatePlaceCoords()
            }

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
                            rememberDynamicCity(city)
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

                val textValue = binding.placeInput.text?.toString()?.trim().orEmpty()
                val resolved = resolveCityByName(textValue, allowGeocoder = true)

                if (resolved != null) {
                    setSelectedCity(resolved, updateInput = false)
                } else {
                    selectedCity = null
                    updatePlaceCoords()
                }

            }

        }

    }

    private fun setupDateInput() {

        binding.dateInputLayout.setEndIconOnClickListener { showDatePicker() }

        binding.dateInput.setOnClickListener { showDatePicker() }

        binding.chipDateCustom.setOnClickListener {

            updateDateQuickChipSelection(View.NO_ID)

            showDatePicker()

        }

        binding.dateQuickChips.setOnCheckedChangeListener { _, checkedId ->

            if (suppressDateChipCallback) return@setOnCheckedChangeListener

            when (checkedId) {

                R.id.chipDateToday -> setSelectedDate(LocalDate.now(), R.id.chipDateToday)

                R.id.chipDateYesterday -> setSelectedDate(LocalDate.now().minusDays(1), R.id.chipDateYesterday)

            }

        }

    }



    private fun setupTimeInput() {

        binding.timeInputLayout.setEndIconOnClickListener { showTimePicker() }

        binding.timeInput.setOnClickListener { showTimePicker() }

        binding.chipTimeCustom.setOnClickListener {

            updateTimeQuickChipSelection(View.NO_ID)

            showTimePicker()

        }

        binding.timeQuickChips.setOnCheckedChangeListener { _, checkedId ->

            if (suppressTimeChipCallback) return@setOnCheckedChangeListener

            when (checkedId) {

                R.id.chipTimeNow -> {

                    val now = LocalTime.now().truncatedTo(ChronoUnit.MINUTES)

                    setSelectedTime(now, R.id.chipTimeNow)

                }

                R.id.chipTimeMorning -> setSelectedTime(morningPreset, R.id.chipTimeMorning)

                R.id.chipTimeNoon -> setSelectedTime(LocalTime.NOON, R.id.chipTimeNoon)

                R.id.chipTimeEvening -> setSelectedTime(eveningPreset, R.id.chipTimeEvening)

            }

        }

    }



    private fun setupSmartInputHelpers() {
        binding.chipSmartNow.setOnClickListener { applySmartNow() }
        binding.chipSmartLast.setOnClickListener { applySmartLast() }
        binding.chipSmartSample.setOnClickListener { applySampleBirth() }
        binding.chipPlaceBengaluru.setOnClickListener { applyPlaceShortcut("Bengaluru") }
        binding.chipPlaceDelhi.setOnClickListener { applyPlaceShortcut("Delhi") }
        binding.chipPlaceMumbai.setOnClickListener { applyPlaceShortcut("Mumbai") }
        binding.chipPlaceChennai.setOnClickListener { applyPlaceShortcut("Chennai") }
    }

    private fun setSelectedDate(date: LocalDate?, preferredChipId: Int? = null) {

        selectedDate = date

        updateDateTimeSummary()

        updateDateQuickChipSelection(preferredChipId)

    }



    private fun updateDateQuickChipSelection(preferredChipId: Int? = null) {

        suppressDateChipCallback = true

        val targetId = preferredChipId?.takeUnless { it == View.NO_ID } ?: when (selectedDate) {

            LocalDate.now() -> R.id.chipDateToday

            LocalDate.now().minusDays(1) -> R.id.chipDateYesterday

            else -> View.NO_ID

        }

        val current = binding.dateQuickChips.checkedChipId

        if (targetId == View.NO_ID) {

            binding.dateQuickChips.clearCheck()

        } else if (current != targetId) {

            binding.dateQuickChips.check(targetId)

        }

        suppressDateChipCallback = false

    }



    private fun setSelectedTime(time: LocalTime?, preferredChipId: Int? = null) {

        selectedTime = time

        lastQuickNowTime = if (preferredChipId == R.id.chipTimeNow) time else null

        updateDateTimeSummary()

        updateTimeQuickChipSelection(preferredChipId)

    }



    private fun updateTimeQuickChipSelection(preferredChipId: Int? = null) {

        suppressTimeChipCallback = true

        val targetId = preferredChipId?.takeUnless { it == View.NO_ID } ?: when (selectedTime) {

            lastQuickNowTime -> R.id.chipTimeNow

            morningPreset -> R.id.chipTimeMorning

            LocalTime.NOON -> R.id.chipTimeNoon

            eveningPreset -> R.id.chipTimeEvening

            else -> View.NO_ID

        }

        val current = binding.timeQuickChips.checkedChipId

        if (targetId == View.NO_ID) {

            binding.timeQuickChips.clearCheck()

        } else if (current != targetId) {

            binding.timeQuickChips.check(targetId)

        }

        suppressTimeChipCallback = false

    }



    private fun fetchIndiaSuggestions(query: String): List<City>? {

        val addresses = geocodeInIndia(query, 10) ?: return null

        return addresses.mapNotNull { addr ->

            val name = resolveDisplayName(addr) ?: return@mapNotNull null

            City(name, addr.latitude, addr.longitude)

        }.distinctBy { it.name }

    }



    private fun geocodeFirstIndia(query: String): City? {

        val address = geocodeInIndia(query, 1)?.firstOrNull() ?: return null

        val name = resolveDisplayName(address) ?: query

        return City(name, address.latitude, address.longitude)

    }

    private fun geocodeInIndia(query: String, maxResults: Int): List<Address>? {

        if (!Geocoder.isPresent()) return null

        return runCatching {

            val geocoder = Geocoder(this)

            geocoder.getFromLocationName(

                "$query, India",

                maxResults,

                INDIA_SOUTH_BOUND,

                INDIA_WEST_BOUND,

                INDIA_NORTH_BOUND,

                INDIA_EAST_BOUND

            )

        }.getOrNull()

    }

    private fun resolveDisplayName(address: Address): String? {

        val locality = listOfNotNull(address.locality, address.subAdminArea, address.adminArea).joinToString(", ")

        return if (locality.isNotBlank()) locality else address.featureName

    }



    private fun showDatePicker() {

        val zone = deviceZone

        val selection = selectedDate?.let { it.atStartOfDay(zone).toInstant().toEpochMilli() }

            ?: MaterialDatePicker.todayInUtcMilliseconds()



        val picker = MaterialDatePicker.Builder.datePicker()

            .setTitleText(getString(R.string.label_birth_date))

            .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)

            .setSelection(selection)

            .build()



        picker.addOnPositiveButtonClickListener { millis ->

            val instant = Instant.ofEpochMilli(millis)

            val date = instant.atZone(zone).toLocalDate()

            setSelectedDate(date)

        }

        picker.show(supportFragmentManager, "birth-date-picker")

    }



    private fun showTimePicker() {

        val initial = selectedTime ?: LocalTime.now().truncatedTo(ChronoUnit.MINUTES)

        val picker = MaterialTimePicker.Builder()

            .setTimeFormat(if (DateFormat.is24HourFormat(this)) TimeFormat.CLOCK_24H else TimeFormat.CLOCK_12H)

            .setHour(initial.hour)

            .setMinute(initial.minute)

            .setTitleText(getString(R.string.label_birth_time))

            .build()

        picker.addOnPositiveButtonClickListener {

            val time = LocalTime.of(picker.hour, picker.minute)

            setSelectedTime(time)

        }

        picker.show(supportFragmentManager, "birth-time-picker")

    }



    private fun updateDateTimeSummary() {

        val date = selectedDate

        val time = selectedTime

        val dateText = date?.format(dateFormatter) ?: ""

        val timeFormatter = if (DateFormat.is24HourFormat(this)) timeFormatter24H else timeFormatter12H

        val timeText = time?.format(timeFormatter) ?: ""



        // Reflect selection in the input fields

        binding.dateInput.setText(dateText)

        binding.timeInput.setText(timeText)



        // Keep a compact summary below

        binding.dateTimeValue.text = when {

            date == null && time == null -> getString(R.string.date_time_summary_empty)

            date != null && time == null -> "${getString(R.string.label_birth_date)}: $dateText"

            date == null && time != null -> "${getString(R.string.label_birth_time)}: $timeText"

            else -> "$dateText | $timeText"

        }

    }

    private inline fun withBirthContext(
        notifyOnMissing: Boolean = false,
        block: (BirthContext) -> Unit
    ) {
        val context = buildBirthContext(notifyOnMissing) ?: return
        block(context)
    }

    private inline fun <reified A : AppCompatActivity> launchBirthActivity(
        notifyOnMissing: Boolean = true,
        crossinline configure: Intent.(BirthContext) -> Unit = {}
    ) {
        withBirthContext(notifyOnMissing) { ctx ->
            val intent = Intent(this, A::class.java).apply {
                putBirthPayload(ctx.toIntentPayload())
                configure(ctx)
            }
            startActivity(intent)
        }
    }

    private fun BirthContext.toIntentPayload(): BirthIntentPayload =
        BirthIntentPayload(rawName, epochMillis, zone, city.latitude, city.longitude)

    private fun buildBirthContext(notifyOnMissing: Boolean = false): BirthContext? {
        val date = selectedDate
        val time = selectedTime
        val city = resolveCityInput()

        if (date == null || time == null || city == null) {
            if (notifyOnMissing) {
                showMissingBirthDetailsMessage()
            }
            return null
        }

        val rawName = binding.nameInput.text?.toString()
        val birthDateTime = LocalDateTime.of(date, time).atZone(deviceZone)
        val birthDetails = BirthDetails(
            name = rawName?.takeIf { it.isNotBlank() },
            dateTime = birthDateTime,
            latitude = city.latitude,
            longitude = city.longitude
        )
        return BirthContext(date, time, city, deviceZone, birthDetails, rawName)
    }

    private fun resolveCityInput(): City? {
        val typed = binding.placeInput.text?.toString().orEmpty()
        return selectedCity ?: resolveCityByName(typed)
    }

    private fun showMissingBirthDetailsMessage() {
        Snackbar.make(binding.root, getString(R.string.missing_birth_details), Snackbar.LENGTH_LONG).show()
    }



    private fun generateChart() {

        // Hide keyboard and clear focus

        hideKeyboard()

        binding.root.clearFocus()

        withBirthContext(notifyOnMissing = true) { ctx ->
            val accurate = accurateCalculator.generateChart(ctx.birthDetails)
            val chart = accurate ?: run {
                Snackbar.make(
                    binding.root,
                    getString(R.string.swiss_ephemeris_missing_generic),
                    Snackbar.LENGTH_LONG
                ).show()
                fallbackCalculator.generateChart(ctx.birthDetails)
            }

            persistBirthDefaults(ctx.date, ctx.time, ctx.city)
            renderPlanets(chart)
            binding.engineIndicator.text = if (accurate != null) {
                getString(R.string.engine_label_swiss)
            } else {
                getString(R.string.engine_label_builtin)
            }
            ctx.birthDetails.name?.let {
                binding.subtitleText.text = getString(R.string.chart_generated_for, it)
            }
        }

    }



    private fun openDasha() = launchBirthActivity<DashaActivity>()

    private fun openYoginiDasha() = launchBirthActivity<YoginiDashaActivity>()

    private fun openPushkara() = launchBirthActivity<PushkaraNavamshaActivity>()

    private fun openIKH() = launchBirthActivity<IshtaKashtaHarshaActivity>()

    private fun openCharaDasha() = launchBirthActivity<CharaDashaActivity>()

    private fun openYogi() = launchBirthActivity<YogiActivity>()

    private fun openTransit() = launchBirthActivity<TransitActivity>()



    private fun openTransitAny() {

        // Open a clean page that asks for date/time/place and computes transit only after user input.

        // Pass natal details so the page can judge houses against the natal ascendant.

        val context = buildBirthContext()

        val intent = android.content.Intent(this, TransitAnyActivity::class.java).apply {

            context?.let { ctx ->

                putExtra(TransitActivity.EXTRA_NAME, ctx.rawName)

                putExtra(TransitActivity.EXTRA_EPOCH_MILLIS, ctx.epochMillis)

                putExtra(TransitActivity.EXTRA_ZONE_ID, ctx.zone.id)

                putExtra(TransitActivity.EXTRA_LAT, ctx.city.latitude)

                putExtra(TransitActivity.EXTRA_LON, ctx.city.longitude)

            }

        }

        startActivity(intent)

    }



    private fun openTransitOverlay() = launchBirthActivity<OverlayActivity>()

    private fun openTransitOverlayNodes() = launchBirthActivity<OverlayNodesActivity>()



    private fun openVargas() = launchBirthActivity<DivisionalChartsActivity>()

    private fun openD60() = launchBirthActivity<D60Activity>()

    private fun openShadbala() = launchBirthActivity<ShadbalaActivity>()

    private fun openSAV() = launchBirthActivity<SarvaAshtakavargaActivity>()

    private fun openAshtakavargaBAV() = launchBirthActivity<AshtakavargaBavActivity>()

    private fun openJaiminiKarakas() = launchBirthActivity<JaiminiKarakasActivity>()

    private fun openArudha() = launchBirthActivity<ArudhaPadasActivity>()

    private fun openSpecialLagnas() = launchBirthActivity<SpecialLagnasActivity>()

    private fun openIshtaDevata() = launchBirthActivity<IshtaDevataActivity>()

    private fun openYogas() = launchBirthActivity<YogasActivity>()

    private fun openPanchanga() = launchBirthActivity<PanchangaActivity>()

    private fun openSixtyFourTwentyTwo() = launchBirthActivity<SixtyFourTwentyTwoActivity>()



    private fun openTodayPanchanga() {

        val city = resolveCityInput() ?: run {

            showMissingBirthDetailsMessage()

            return

        }

        val now = Instant.now().atZone(deviceZone)

        val intent = android.content.Intent(this, PanchangaActivity::class.java).apply {

            putExtra(PanchangaActivity.EXTRA_NAME, binding.nameInput.text?.toString())

            putExtra(PanchangaActivity.EXTRA_EPOCH_MILLIS, now.toInstant().toEpochMilli())

            putExtra(PanchangaActivity.EXTRA_ZONE_ID, deviceZone.id)

            putExtra(PanchangaActivity.EXTRA_LAT, city.latitude)

            putExtra(PanchangaActivity.EXTRA_LON, city.longitude)

            putExtra(PanchangaActivity.EXTRA_IS_TODAY, true)

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



        // Special lagnas now live on their dedicated screen


        // Finally, render the Vedic chart (South Indian style) at the end

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

        return String.format(Locale.US, "%02d\u00B0 %02d'", degrees, minutes)

    }



    private fun formatDegreeWithSign(value: Double): String {

        val absText = formatDegree(value)

        val inSign = ((value % 30.0) + 30.0) % 30.0

        val inSignText = formatDegree(inSign)

        return "$absText ($inSignText)"

    }







    private fun initializeDefaultsAndGenerate() {

        val effectiveDate = selectedDate ?: LocalDate.of(1993, 5, 18)
        val effectiveTime = selectedTime ?: LocalTime.of(22, 30)
        setSelectedDate(effectiveDate)
        setSelectedTime(effectiveTime)

        if (selectedCity == null) {
            val fallback = CityDatabase.findByName("Bengaluru")
                ?: CityDatabase.findByName("Bangalore")
                ?: defaultCityFallback
            setSelectedCity(fallback)
        } else {
            selectedCity?.let { setSelectedCity(it) }
        }

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

            setSelectedDate(zdt.toLocalDate())

            setSelectedTime(zdt.toLocalTime())

            name?.let { binding.nameInput.setText(it) }

            setSelectedCity(com.aakash.astro.geo.City("Custom", lat, lon))

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



    private fun applySmartNow() {
        val now = ZonedDateTime.now()
        setSelectedDate(now.toLocalDate(), R.id.chipDateToday)
        setSelectedTime(now.toLocalTime().truncatedTo(ChronoUnit.MINUTES), R.id.chipTimeNow)
        if (binding.placeInput.text.isNullOrBlank()) {
            val city = CityDatabase.findByName(defaultCityFallback.name) ?: defaultCityFallback
            setSelectedCity(city)
        }
    }

    private fun applySmartLast() {
        val dateStr = birthPrefs.getString(PREF_LAST_DATE, null)
        val timeStr = birthPrefs.getString(PREF_LAST_TIME, null)
        val cityName = birthPrefs.getString(PREF_LAST_CITY_NAME, null)
        if (dateStr == null || timeStr == null || cityName == null) {
            Snackbar.make(binding.root, getString(R.string.birth_action_last_missing), Snackbar.LENGTH_SHORT).show()
            return
        }

        runCatching { LocalDate.parse(dateStr) }.getOrNull()?.let { setSelectedDate(it) }
        runCatching { LocalTime.parse(timeStr) }.getOrNull()?.let { setSelectedTime(it) }

        val lat = readStoredDouble(PREF_LAST_CITY_LAT)
        val lon = readStoredDouble(PREF_LAST_CITY_LON)
        val city = CityDatabase.findByName(cityName)
            ?: if (lat != null && lon != null) City(cityName, lat, lon) else null

        city?.let { setSelectedCity(it) }
    }

    private fun applySampleBirth() {
        setSelectedDate(LocalDate.of(1993, 5, 18))
        setSelectedTime(LocalTime.of(22, 30))
        val city = CityDatabase.findByName("Mumbai") ?: City("Mumbai", 19.0760, 72.8777)
        setSelectedCity(city)
    }

    private fun applyPlaceShortcut(name: String) {
        val city = CityDatabase.findByName(name)
        if (city != null) {
            setSelectedCity(city)
        } else {
            Snackbar.make(binding.root, getString(R.string.place_shortcut_missing, name), Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun persistBirthDefaults(date: LocalDate, time: LocalTime, city: City) {
        birthPrefs.edit()
            .putString(PREF_LAST_DATE, date.toString())
            .putString(PREF_LAST_TIME, time.toString())
            .putString(PREF_LAST_CITY_NAME, city.name)
            .putLong(PREF_LAST_CITY_LAT, java.lang.Double.doubleToRawLongBits(city.latitude))
            .putLong(PREF_LAST_CITY_LON, java.lang.Double.doubleToRawLongBits(city.longitude))
            .apply()
    }

    private fun readStoredDouble(key: String): Double? {
        val bits = birthPrefs.getLong(key, java.lang.Double.doubleToRawLongBits(Double.NaN))
        val value = java.lang.Double.longBitsToDouble(bits)
        return if (value.isNaN()) null else value
    }

    private fun setSelectedCity(city: City, updateInput: Boolean = true) {
        selectedCity = city
        if (updateInput) {
            suppressPlaceSuggestions = true
            binding.placeInput.setText(city.name, false)
            binding.placeInput.setSelection(0)
        }
        updatePlaceCoords()
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

    companion object {
        private const val INDIA_SOUTH_BOUND = 6.0
        private const val INDIA_NORTH_BOUND = 37.0
        private const val INDIA_WEST_BOUND = 68.0
        private const val INDIA_EAST_BOUND = 97.0
        private const val PREF_LAST_DATE = "birth_last_date"
        private const val PREF_LAST_TIME = "birth_last_time"
        private const val PREF_LAST_CITY_NAME = "birth_last_city_name"
        private const val PREF_LAST_CITY_LAT = "birth_last_city_lat"
        private const val PREF_LAST_CITY_LON = "birth_last_city_lon"
        private const val RECENT_ACTION_KEY = "recent.action.ids"
    }
}




