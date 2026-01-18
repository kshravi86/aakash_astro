package com.aakash.astro

import android.os.Bundle
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityD60Binding
import java.time.Instant
import java.time.ZoneId
import kotlin.math.floor

class D60Activity : AppCompatActivity() {
    private lateinit var binding: ActivityD60Binding
    private val accurate = AccurateCalculator()

    private fun renderD60(natal: ChartResult, d60: ChartResult) {
        val table = binding.d60Table
        
        fun createCell(text: String, isHeader: Boolean = false, textColor: Int? = null): TextView {
            return TextView(this).apply {
                this.text = text
                this.setPadding(16, 12, 16, 12)
                setTextAppearance(if (isHeader) {
                    com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
                } else {
                    com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
                })
                this.setTextColor(textColor ?: if (isHeader) getColor(R.color.accent_blue) else getColor(R.color.primaryText))
                if (isHeader) {
                    this.letterSpacing = 0.1f
                    this.text = text.uppercase()
                    this.setTypeface(this.typeface, android.graphics.Typeface.BOLD)
                }
            }
        }

        fun addRow(c1: String, c2: String, c3: String, c4: String, c5: String, isBenefic: Boolean) {
            val row = TableRow(this)
            row.addView(createCell(c1))
            row.addView(createCell(c2))
            row.addView(createCell(c3))
            row.addView(createCell(c4))
            
            val nature = if (isBenefic) getString(R.string.benefic) else getString(R.string.malefic)
            val natureColor = if (isBenefic) getColor(R.color.planet_favorable) else getColor(R.color.planet_unfavorable)
            row.addView(createCell(nature, false, natureColor))
            
            table.addView(row)
        }

        table.removeAllViews()
        val header = TableRow(this)
        header.addView(createCell("PLANET", true))
        header.addView(createCell("SIGN", true))
        header.addView(createCell("AMSHA", true))
        header.addView(createCell("NAME", true))
        header.addView(createCell("NATURE", true))
        table.addView(header)

        val natalByPlanet = natal.planets.associateBy { it.planet }
        d60.planets.forEach { p ->
            val natalPos = natalByPlanet[p.planet]
            val natalDegree = natalPos?.degree ?: p.degree
            val natalSignIndex = natalPos?.sign?.ordinal ?: (natalDegree / 30.0).toInt()
            val inSign = ((natalDegree - natalSignIndex * 30.0) % 30.0 + 30.0) % 30.0

            val amsha = D60Shashtiamsa.amshaNumber(inSign)
            val isOddSign = (natalSignIndex % 2 == 0) 
            val amshaName = D60Shashtiamsa.amshaName(amsha, isOddSign)
            val isBenefic = D60Shashtiamsa.isBenefic(amsha, isOddSign)
            
            addRow(p.name, p.sign.displayName, amsha.toString(), amshaName, "", isBenefic)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityD60Binding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.d60_title)
        binding.subtitle.text = getString(R.string.d60_subtitle)
        name?.let { binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it)) }

        val natal = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        if (natal == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        val d60 = VargaCalculator.computeVargaChart(natal, Varga.D60, natal.ascendantDegree)
        binding.vedicChartView.setChart(d60)
        
        renderD60(natal, d60)

        // Entrance animation
        binding.contentContainer.alpha = 0f
        binding.contentContainer.translationY = 30f
        binding.contentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .start()
    }

    // amshaInfo no longer needed; naming and nature now use Phaladipika-style rules from D60Shashtiamsa

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
