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

        // Populate summary cards
        binding.ishtaResult.text = res.deity
        binding.ishtaPractice.text = getString(R.string.practice_prefix, res.suggestion)
        binding.palanaResult.text = res.palanaDeity
        binding.palanaPractice.text = getString(R.string.practice_prefix, res.palanaSuggestion)

        // Share actions
        fun shareText(text: String) {
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND)
            intent.type = "text/plain"
            intent.putExtra(android.content.Intent.EXTRA_TEXT, text)
            startActivity(android.content.Intent.createChooser(intent, getString(R.string.share_result)))
        }
        binding.shareIshta.setOnClickListener {
            val text = buildString {
                appendLine(getString(R.string.ishta_title))
                appendLine("Ishta Devata: ${res.deity}")
                appendLine("Practice: ${res.suggestion}")
            }
            shareText(text)
        }
        binding.sharePalana.setOnClickListener {
            val text = buildString {
                appendLine("Palana Devata")
                appendLine("Deity: ${res.palanaDeity}")
                appendLine("Practice: ${res.palanaSuggestion}")
            }
            shareText(text)
        }

        fun addRow(table: android.widget.TableLayout, label: String, value: String, highlight: Boolean = false) {
            val row = android.widget.TableRow(this)
            val tvLabel = TextView(this)
            val tvValue = TextView(this)
            tvLabel.text = label
            tvValue.text = value
            tvLabel.setPadding(16, 12, 32, 12)
            tvValue.setPadding(16, 12, 16, 12)
            
            tvLabel.setTextColor(getColor(R.color.secondaryText))
            tvLabel.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_LabelMedium
            
            tvValue.setTextColor(if (highlight) getColor(R.color.accent_teal) else getColor(R.color.primaryText))
            tvValue.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_BodyMedium
            
            if (highlight) {
                tvValue.setTypeface(tvValue.typeface, android.graphics.Typeface.BOLD)
            }
            row.addView(tvLabel)
            row.addView(tvValue)
            table.addView(row)
        }

        binding.ishtaTable.removeAllViews()
        run {
            val header = android.widget.TableRow(this)
            val h1 = TextView(this); h1.text = "CALCULATION FIELD"; h1.setTextColor(getColor(R.color.accent_gold))
            h1.setPadding(16, 8, 16, 8)
            h1.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
            h1.letterSpacing = 0.1f
            val h2 = TextView(this); h2.text = "RESULT"; h2.setTextColor(getColor(R.color.accent_gold))
            h2.setPadding(16, 8, 16, 8)
            h2.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
            h2.letterSpacing = 0.1f
            header.addView(h1); header.addView(h2); binding.ishtaTable.addView(header)
        }
        addRow(binding.ishtaTable, "Atmakaraka", "${res.atmakaraka.displayName} (Rasi: ${res.akRasiSign.displayName})")
        addRow(binding.ishtaTable, "AK in Navamsha", res.akNavamsaSign.displayName)
        addRow(binding.ishtaTable, "12th from AK (D9)", res.twelfthFromAKNavamsaSign.displayName)
        addRow(binding.ishtaTable, "12th Lord", res.twelfthLord.displayName)
        addRow(binding.ishtaTable, "Occupant in 12th (D9)", res.twelfthOccupant?.displayName ?: getString(R.string.none))
        addRow(
            binding.ishtaTable,
            "Determining Factor",
            if (res.twelfthOccupant!=null) "Occupant" else "Lord",
            highlight = true
        )

        binding.palanaTable.removeAllViews()
        run {
            val header = android.widget.TableRow(this)
            val h1 = TextView(this); h1.text = "CALCULATION FIELD"; h1.setTextColor(getColor(R.color.accent_purple))
            h1.setPadding(16, 8, 16, 8)
            h1.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
            h1.letterSpacing = 0.1f
            val h2 = TextView(this); h2.text = "RESULT"; h2.setTextColor(getColor(R.color.accent_purple))
            h2.setPadding(16, 8, 16, 8)
            h2.textAppearance = com.google.android.material.R.style.TextAppearance_Material3_LabelSmall
            h2.letterSpacing = 0.1f
            header.addView(h1); header.addView(h2); binding.palanaTable.addView(header)
        }
        addRow(binding.palanaTable, "Amatyakaraka", "${res.amatyakaraka.displayName} (Rasi: ${res.amkRasiSign.displayName})")
        addRow(binding.palanaTable, "AMK in Navamsha", res.amkNavamsaSign.displayName)
        addRow(binding.palanaTable, "6th from AMK (D9)", res.sixthFromAMKNavamsaSign.displayName)
        addRow(binding.palanaTable, "6th Lord", res.sixthLord.displayName)
        addRow(binding.palanaTable, "Occupant in 6th (D9)", res.sixthOccupant?.displayName ?: getString(R.string.none))
        addRow(
            binding.palanaTable,
            "Determining Factor",
            if (res.sixthOccupant!=null) "Occupant" else "Lord",
            highlight = true
        )

        // Staggered entrance animation
        binding.contentContainer.alpha = 0f
        binding.contentContainer.translationY = 30f
        binding.contentContainer.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(600)
            .setInterpolator(android.view.animation.DecelerateInterpolator())
            .start()

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
