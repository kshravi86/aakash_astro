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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityD60Binding.inflate(layoutInflater)
        setContentView(binding.root)

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

        // Fill planet table with D60 sign, house, amsha number and range within the sign
        val table = binding.d60Table
        fun addRow(c1: String, c2: String, c3: String, c4: String, c5: String, c6: String, c7: String) {
            val row = TableRow(this)
            fun cell(text: String) = TextView(this).apply { this.text = text; setPadding(8,8,8,8) }
            row.addView(cell(c1))
            row.addView(cell(c2))
            row.addView(cell(c3))
            row.addView(cell(c4))
            row.addView(cell(c5))
            row.addView(cell(c6))
            val natureView = cell(c7)
            val isBenefic = c7.equals(getString(R.string.benefic), ignoreCase = true)
            val color = if (isBenefic) 0xFF2E7D32.toInt() else 0xFFC62828.toInt()
            natureView.setTextColor(color)
            row.addView(natureView)
            table.addView(row)
        }

        val natalByPlanet = natal.planets.associateBy { it.planet }
        d60.planets.forEach { p ->
            val natalPos = natalByPlanet[p.planet]
            val natalDegree = natalPos?.degree ?: p.degree
            val natalSignIndex = natalPos?.sign?.ordinal ?: (natalDegree / 30.0).toInt()
            val inSign = ((natalDegree - natalSignIndex * 30.0) % 30.0 + 30.0) % 30.0
            val size = 30.0 / 60.0 // 0.5 degree per amsha
            val amsha = floor(inSign / size).toInt() + 1
            val start = floor(inSign / size) * size
            val end = start + size
            val range = String.format("%05.2f°–%05.2f°", start, end)
            val (amshaName, isBenefic) = amshaInfo(amsha)
            val nature = if (isBenefic) getString(R.string.benefic) else getString(R.string.malefic)
            addRow(p.name, p.sign.displayName, p.house.toString(), amsha.toString(), range, amshaName, nature)
        }
    }

    private fun amshaInfo(amsha: Int): Pair<String, Boolean> {
        // Names and nature parsed from Shashtyamsha table (Wikipedia)
        val list = arrayOf(
            "Ghorānsh" to false,
            "Rākshasa" to false,
            "Deva" to true,
            "Kuber" to true,
            "Rakshogana" to true,
            "Kinnar" to true,
            "Bhrshta" to false,
            "Kulaghna" to false,
            "Garala" to false,
            "Agni" to false,
            "Māyā" to false,
            "Yama" to false,
            "Varuna" to true,
            "Indra" to true,
            "Kāla" to false,
            "Ahibhāga" to true,
            "Amrtānshu" to true,
            "Chandra" to true,
            "Madu" to true,
            "Komala" to true,
            "Padma" to true,
            "Vishnu" to true,
            "Vāgāsh" to true,
            "Dingchar" to true,
            "Dava" to true,
            "Ārdra" to true,
            "Kālanāsha" to false,
            "Kshitish" to true,
            "Kamalākar" to true,
            "Manda" to false,
            "Mrtyu" to false,
            "Kāla" to false,
            "Dāvagni" to false,
            "Ghora" to false,
            "Maya" to false,
            "Kantaka" to false,
            "Sudhā" to true,
            "Amrta" to true,
            "Poornachandra" to true,
            "Vishpradimā" to false,
            "Kulanāsha" to false,
            "Mukhya" to true,
            "Vanshakshya" to false,
            "Utpāta" to false,
            "Kālroopa" to true,
            "Saomaya" to true,
            "Mrdu" to true,
            "Sheetala" to true,
            "Danshtrākarā" to false,
            "Indumukha" to false,
            "Praveena" to true,
            "Kālagni" to false,
            "Dandayudha" to true,
            "Nirmala" to true,
            "Shubha" to true,
            "Ashubha" to true,
            "Atisheeta" to true,
            "Sudhāyoga" to true,
            "Bhramana" to false,
            "Indurekhā" to true,
        )
        val idx = (amsha - 1).coerceIn(0, list.size - 1)
        return list[idx]
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
