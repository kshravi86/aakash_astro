package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.databinding.ItemTaraRowBinding
import com.aakash.astro.databinding.ItemTransitTaraBinding
import java.time.Instant
import java.time.ZoneId

class TaraBalaActivity : AppCompatActivity() {
    private val accurate = AccurateCalculator()
    private var lastNatalChart: com.aakash.astro.astrology.ChartResult? = null
    private var lastTransitChart: com.aakash.astro.astrology.ChartResult? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tara_bala)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topBar)
            .setNavigationOnClickListener { finish() }

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val root = findViewById<android.view.View>(android.R.id.content)
        val startPaddingLeft = root.paddingLeft
        val startPaddingTop = root.paddingTop
        val startPaddingRight = root.paddingRight
        val startPaddingBottom = root.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(
                startPaddingLeft + systemBars.left,
                startPaddingTop + systemBars.top,
                startPaddingRight + systemBars.right,
                startPaddingBottom + systemBars.bottom
            )
            insets
        }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        // Generate natal chart
        val birthZdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val natalChart = accurate.generateChart(BirthDetails(name, birthZdt, lat, lon))
        if (natalChart == null) {
            findViewById<android.widget.TextView>(R.id.engineNote).text = getString(R.string.transit_engine_missing)
            return
        }

        // Generate transit chart (current time)
        val nowZdt = Instant.now().atZone(zoneId)
        val transitChart = accurate.generateChart(BirthDetails(name, nowZdt, lat, lon))
        lastNatalChart = natalChart
        lastTransitChart = transitChart

        // Display natal tara bala
        val rows = TaraBalaCalc.compute(natalChart)
        val inflater = LayoutInflater.from(this)
        val container = findViewById<android.widget.LinearLayout>(R.id.rowContainer)
        container.removeAllViews()
        rows.forEach { r ->
            val item = ItemTaraRowBinding.inflate(inflater, container, false)
            item.planetName.text = r.planet.displayName
            item.taraValue.text = "${r.tara} (${r.note})"
            item.resultValue.text = r.result
            // Apply red/green color for favorable/unfavorable (neutral is gray)
            val colorRes = when (r.result) {
                "Favorable" -> R.color.planet_favorable
                "Unfavorable" -> R.color.planet_unfavorable
                else -> R.color.planet_neutral
            }
            val c = androidx.core.content.ContextCompat.getColor(this, colorRes)
            item.resultValue.setTextColor(c)
            item.taraValue.setTextColor(c)
            container.addView(item.root)
        }

        findViewById<android.widget.TextView>(R.id.engineNote).text = "Natal Chart - Reference: Moon of natal D-1"

        // Display transit tara bala
        if (transitChart != null) {
            renderTransitTaraBala(transitChart, natalChart, inflater)
        }

        // Share buttons
        findViewById<com.google.android.material.button.MaterialButton>(R.id.shareTransit).setOnClickListener {
            shareTransitTara()
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.shareNatal).setOnClickListener {
            shareNatalTara()
        }
    }

    private fun renderTransitTaraBala(
        transitChart: com.aakash.astro.astrology.ChartResult,
        natalChart: com.aakash.astro.astrology.ChartResult,
        inflater: LayoutInflater
    ) {
        val transitContainer = findViewById<android.widget.LinearLayout>(R.id.transitRowContainer)
        transitContainer.removeAllViews()

        // Get natal moon's nakshatra for tara calculation
        val natalMoonDeg = natalChart.planets.find { it.planet == com.aakash.astro.astrology.Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)

        transitChart.planets.forEach { transitPlanet ->
            val item = ItemTransitTaraBinding.inflate(inflater, transitContainer, false)

            val nameWithRetro = if (transitPlanet.isRetrograde) "${transitPlanet.name} (R)" else transitPlanet.name
            item.planetName.text = nameWithRetro

            // Get nakshatra for transit planet
            val (nakName, pada) = NakshatraCalc.fromLongitude(transitPlanet.degree)
            item.nakshatraInfo.text = "$nakName (Pada $pada)"

            // Calculate tara bala
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

            // Apply red/green color for favorable/unfavorable (neutral is gray)
            val colorRes = when (taraResult) {
                "Favorable" -> R.color.planet_favorable
                "Unfavorable" -> R.color.planet_unfavorable
                else -> R.color.planet_neutral
            }
            val c = androidx.core.content.ContextCompat.getColor(this, colorRes)
            item.resultValue.setTextColor(c)
            item.taraValue.setTextColor(c)

            transitContainer.addView(item.root)
        }
    }

    private fun shareTransitTara() {
        val natal = lastNatalChart ?: return
        val transit = lastTransitChart ?: return
        val sb = StringBuilder()
        sb.appendLine("Transit Tara Bala")
        val natalMoonDeg = natal.planets.find { it.planet == com.aakash.astro.astrology.Planet.MOON }?.degree ?: 0.0
        val natalMoonNak = TaraBalaCalc.nakshatraNumber1Based(natalMoonDeg)
        transit.planets.forEach { tp ->
            val (nakName, pada) = com.aakash.astro.astrology.NakshatraCalc.fromLongitude(tp.degree)
            val transitNak = TaraBalaCalc.nakshatraNumber1Based(tp.degree)
            val tClass = TaraBalaCalc.tClass(natalMoonNak, transitNak)
            val tName = TaraBalaCalc.taraNames[tClass - 1]
            val tNote = TaraBalaCalc.taraNotes[tClass] ?: ""
            val result = when (tClass) {
                2, 4, 6, 8, 9 -> "Favorable"
                3, 5, 7 -> "Unfavorable"
                else -> "Neutral"
            }
            val nameWithRetro = if (tp.isRetrograde) "${tp.name} (R)" else tp.name
            sb.appendLine("$nameWithRetro — $nakName (Pada $pada) — $tName ($tNote) — $result")
        }
        shareText(sb.toString())
    }

    private fun shareNatalTara() {
        val natal = lastNatalChart ?: return
        val rows = TaraBalaCalc.compute(natal)
        val sb = StringBuilder()
        sb.appendLine("Natal Tara Bala (ref. Moon)")
        rows.forEach { r ->
            sb.appendLine("${r.planet.displayName} — ${r.tara} (${r.note}) — ${r.result}")
        }
        shareText(sb.toString())
    }

    private fun shareText(text: String) {
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
        startActivity(android.content.Intent.createChooser(intent, getString(R.string.share_result)))
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
