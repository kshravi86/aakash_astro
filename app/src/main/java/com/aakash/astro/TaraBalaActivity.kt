package com.aakash.astro

import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.aakash.astro.astrology.AccurateCalculator
import com.aakash.astro.astrology.BirthDetails
import com.aakash.astro.astrology.TaraBalaCalc
import com.aakash.astro.databinding.ItemTaraRowBinding
import java.time.Instant
import java.time.ZoneId

class TaraBalaActivity : AppCompatActivity() {
    private val accurate = AccurateCalculator()

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

        val zdt = Instant.ofEpochMilli(epochMillis).atZone(zoneId)
        val chart = accurate.generateChart(BirthDetails(name, zdt, lat, lon))
        if (chart == null) {
            findViewById<android.widget.TextView>(R.id.engineNote).text = getString(R.string.transit_engine_missing)
            return
        }

        val rows = TaraBalaCalc.compute(chart)

        val inflater = LayoutInflater.from(this)
        val container = findViewById<android.widget.LinearLayout>(R.id.rowContainer)
        container.removeAllViews()
        rows.forEach { r ->
            val item = ItemTaraRowBinding.inflate(inflater, container, false)
            item.planetName.text = r.planet.displayName
            item.taraValue.text = "${r.tara} (${r.note})"
            item.resultValue.text = r.result
            container.addView(item.root)
        }

        findViewById<android.widget.TextView>(R.id.engineNote).text = "Reference: Moon of natal D-1"
    }

    // No longer needed; keeping for possible future use

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
