package com.aakash.astro

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.aakash.astro.astrology.*
import com.aakash.astro.databinding.ActivityIshtaDevataBinding
import java.time.Instant
import java.time.ZoneId

class IshtaDevataActivity : AppCompatActivity() {
    private lateinit var binding: ActivityIshtaDevataBinding
    private val accurate = AccurateCalculator()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIshtaDevataBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.topBar.setNavigationOnClickListener { finish() }

        EphemerisPreparer.prepare(this)?.let { accurate.setEphePath(it.absolutePath) }

        val name = intent.getStringExtra(EXTRA_NAME)
        val epochMillis = intent.getLongExtra(EXTRA_EPOCH_MILLIS, 0L)
        val zoneId = intent.getStringExtra(EXTRA_ZONE_ID)?.let { ZoneId.of(it) } ?: ZoneId.systemDefault()
        val lat = intent.getDoubleExtra(EXTRA_LAT, 0.0)
        val lon = intent.getDoubleExtra(EXTRA_LON, 0.0)

        binding.title.text = getString(R.string.ishta_title)
        binding.subtitle.text = getString(R.string.ishta_subtitle)
        name?.let { binding.subtitle.append("\n" + getString(R.string.chart_generated_for, it)) }

        val natal = accurate.generateChart(BirthDetails(name, Instant.ofEpochMilli(epochMillis).atZone(zoneId), lat, lon))
        if (natal == null) {
            binding.subtitle.append("\n" + getString(R.string.transit_engine_missing))
            return
        }

        val res = IshtaDevataCalc.compute(natal)
        if (res == null) {
            binding.details.text = getString(R.string.ishta_noresult)
            return
        }

        fun addRow(table: android.widget.TableLayout, label: String, value: String, highlight: Boolean = false) {
            val row = android.widget.TableRow(this)
            val tvLabel = TextView(this)
            val tvValue = TextView(this)
            tvLabel.text = label
            tvValue.text = value
            tvLabel.setPadding(8, 8, 16, 8)
            tvValue.setPadding(8, 8, 8, 8)
            if (highlight) {
                tvValue.setTypeface(tvValue.typeface, android.graphics.Typeface.BOLD)
                tvValue.setBackgroundColor(getColor(com.aakash.astro.R.color.surface_variant))
            }
            row.addView(tvLabel)
            row.addView(tvValue)
            table.addView(row)
        }

        binding.ishtaTable.removeAllViews()
        run {
            val header = android.widget.TableRow(this)
            val h1 = TextView(this); h1.text = "Field"; h1.setTypeface(h1.typeface, android.graphics.Typeface.BOLD)
            val h2 = TextView(this); h2.text = "Value"; h2.setTypeface(h2.typeface, android.graphics.Typeface.BOLD)
            header.addView(h1); header.addView(h2); binding.ishtaTable.addView(header)
        }
        addRow(binding.ishtaTable, "Atmakaraka", "${res.atmakaraka.displayName} (Rasi: ${res.akRasiSign.displayName})")
        addRow(binding.ishtaTable, "AK in Navamsha", res.akNavamsaSign.displayName)
        addRow(binding.ishtaTable, "12th from AK (D9)", res.twelfthFromAKNavamsaSign.displayName)
        addRow(binding.ishtaTable, "12th Lord", res.twelfthLord.displayName)
        addRow(binding.ishtaTable, "Occupant in 12th (D9)", res.twelfthOccupant?.displayName ?: getString(R.string.none))
        addRow(
            binding.ishtaTable,
            "Ishta Devata (by ${if (res.twelfthOccupant!=null) "occupant" else "lord"})",
            res.deity,
            highlight = true
        )
        addRow(binding.ishtaTable, "Practice", res.suggestion)

        binding.palanaTable.removeAllViews()
        run {
            val header = android.widget.TableRow(this)
            val h1 = TextView(this); h1.text = "Field"; h1.setTypeface(h1.typeface, android.graphics.Typeface.BOLD)
            val h2 = TextView(this); h2.text = "Value"; h2.setTypeface(h2.typeface, android.graphics.Typeface.BOLD)
            header.addView(h1); header.addView(h2); binding.palanaTable.addView(header)
        }
        addRow(binding.palanaTable, "Amatyakaraka", "${res.amatyakaraka.displayName} (Rasi: ${res.amkRasiSign.displayName})")
        addRow(binding.palanaTable, "AMK in Navamsha", res.amkNavamsaSign.displayName)
        addRow(binding.palanaTable, "6th from AMK (D9)", res.sixthFromAMKNavamsaSign.displayName)
        addRow(binding.palanaTable, "6th Lord", res.sixthLord.displayName)
        addRow(binding.palanaTable, "Occupant in 6th (D9)", res.sixthOccupant?.displayName ?: getString(R.string.none))
        addRow(
            binding.palanaTable,
            "Palana Devata (by ${if (res.sixthOccupant!=null) "occupant" else "lord"})",
            res.palanaDeity,
            highlight = true
        )
        addRow(binding.palanaTable, "Practice", res.palanaSuggestion)

        binding.details.text = "" // optional legacy text; keep empty now
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
