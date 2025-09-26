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

        val sb = StringBuilder()
        sb.appendLine("Atmakaraka: ${res.atmakaraka.displayName} (Rasi: ${res.akRasiSign.displayName})")
        sb.appendLine("AK in Navamsha: ${res.akNavamsaSign.displayName}")
        sb.appendLine("12th from AK (Navamsha): ${res.twelfthFromAKNavamsaSign.displayName}")
        res.twelfthOccupant?.let { sb.appendLine("Planet in 12th from AK (Navamsha): ${it.displayName}") }
        sb.appendLine("12th Lord: ${res.twelfthLord.displayName}")
        sb.appendLine("Ishta Devata (by ${if (res.twelfthOccupant!=null) "occupant" else "lord"}): ${res.deity}")
        sb.appendLine("Practice: ${res.suggestion}")
        sb.appendLine("")
        sb.appendLine("Amatyakaraka: ${res.amatyakaraka.displayName} (Rasi: ${res.amkRasiSign.displayName})")
        sb.appendLine("AMK in Navamsha: ${res.amkNavamsaSign.displayName}")
        sb.appendLine("6th from AMK (Navamsha): ${res.sixthFromAMKNavamsaSign.displayName}")
        res.sixthOccupant?.let { sb.appendLine("Planet in 6th from AMK (Navamsha): ${it.displayName}") }
        sb.appendLine("6th Lord: ${res.sixthLord.displayName}")
        sb.appendLine("Palana Devata (by ${if (res.sixthOccupant!=null) "occupant" else "lord"}): ${res.palanaDeity}")
        sb.appendLine("Practice: ${res.palanaSuggestion}")
        binding.details.text = sb.toString()
    }

    companion object {
        const val EXTRA_NAME = "name"
        const val EXTRA_EPOCH_MILLIS = "epochMillis"
        const val EXTRA_ZONE_ID = "zoneId"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }
}
