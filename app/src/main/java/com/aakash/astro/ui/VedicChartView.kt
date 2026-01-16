package com.aakash.astro.ui

import android.content.Context
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.aakash.astro.R
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.astrology.NakshatraCalc
import com.aakash.astro.databinding.ViewVedicChartBinding

class VedicChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVedicChartBinding.inflate(LayoutInflater.from(context), this, true)

    private val signViews: Map<ZodiacSign, TextView> = mapOf(
        ZodiacSign.PISCES to binding.house12,
        ZodiacSign.ARIES to binding.house1,
        ZodiacSign.TAURUS to binding.house2,
        ZodiacSign.GEMINI to binding.house3,
        ZodiacSign.CANCER to binding.house4,
        ZodiacSign.LEO to binding.house5,
        ZodiacSign.VIRGO to binding.house6,
        ZodiacSign.LIBRA to binding.house7,
        ZodiacSign.SCORPIO to binding.house8,
        ZodiacSign.SAGITTARIUS to binding.house9,
        ZodiacSign.CAPRICORN to binding.house10,
        ZodiacSign.AQUARIUS to binding.house11,
    )

    private val signAbbrev = mapOf(
        ZodiacSign.ARIES to "Ar", ZodiacSign.TAURUS to "Ta", ZodiacSign.GEMINI to "Ge",
        ZodiacSign.CANCER to "Cn", ZodiacSign.LEO to "Le", ZodiacSign.VIRGO to "Vi",
        ZodiacSign.LIBRA to "Li", ZodiacSign.SCORPIO to "Sc", ZodiacSign.SAGITTARIUS to "Sg",
        ZodiacSign.CAPRICORN to "Cp", ZodiacSign.AQUARIUS to "Aq", ZodiacSign.PISCES to "Pi"
    )

    private val planetAbbrev = mapOf(
        Planet.SUN to "Su", Planet.MOON to "Mo", Planet.MERCURY to "Me",
        Planet.VENUS to "Ve", Planet.MARS to "Ma", Planet.JUPITER to "Ju",
        Planet.SATURN to "Sa", Planet.RAHU to "Ra", Planet.KETU to "Ke"
    )

    fun setChart(chart: ChartResult?) {
        if (chart == null) { resetSigns(); return }

        val colorSecondary = ContextCompat.getColor(context, R.color.secondaryText)
        val colorPrimary = ContextCompat.getColor(context, R.color.primaryText)
        val colorTeal = ContextCompat.getColor(context, R.color.accent_teal)
        val colorGold = ContextCompat.getColor(context, R.color.accent_gold)
        val colorPurple = ContextCompat.getColor(context, R.color.accent_purple)
        val colorOrange = ContextCompat.getColor(context, R.color.accent_orange)

        // Update Center Summary Card
        val (nakName, pada) = NakshatraCalc.fromLongitude(chart.ascendantDegree)
        binding.chartSummaryLagna.text = context.getString(
            R.string.chart_summary_lagna,
            chart.ascendantSign.displayName
        )
        binding.chartSummaryNakshatra.text = context.getString(
            R.string.chart_summary_nakshatra,
            nakName,
            pada
        )

        // Animated entrance for summary
        binding.chartSummaryLagna.alpha = 0f
        binding.chartSummaryLagna.animate().alpha(1f).setDuration(800).start()
        binding.chartSummaryNakshatra.alpha = 0f
        binding.chartSummaryNakshatra.animate().alpha(1f).setStartDelay(200).setDuration(800).start()

        binding.shareChartButton.isEnabled = true
        binding.shareChartButton.setOnClickListener {
            // Intent to share summary
            val shareText = context.getString(
                R.string.chart_share_text,
                chart.ascendantSign.displayName,
                nakName,
                pada
            )
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, shareText)
            }
            context.startActivity(
                android.content.Intent.createChooser(
                    intent,
                    context.getString(R.string.chart_share_title)
                )
            )
        }

        signViews.forEach { (sign, textView) ->
            val planetsInSign = chart.planets.filter { it.sign == sign }
            val builder = SpannableStringBuilder()

            // 1. Sign Abbreviation (Subtle)
            val abbrev = signAbbrev[sign] ?: ""
            builder.append(abbrev)
            builder.setSpan(ForegroundColorSpan(colorSecondary), 0, abbrev.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.setSpan(RelativeSizeSpan(0.8f), 0, abbrev.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            builder.append("\n")

            // 2. Ascendant (Lagna) - "As"
            if (chart.ascendantSign == sign) {
                val start = builder.length
                builder.append("As ")
                builder.setSpan(ForegroundColorSpan(colorTeal), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                builder.setSpan(StyleSpan(android.graphics.Typeface.BOLD), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            }

            // 3. Planets
            planetsInSign.forEachIndexed { index, planetPos ->
                val start = builder.length
                val base = planetAbbrev[planetPos.planet] ?: planetPos.name.take(2)
                val text = if (planetPos.isRetrograde) base + "R" else base
                builder.append(text)
                
                val pColor = when (planetPos.planet) {
                    Planet.JUPITER, Planet.VENUS, Planet.MOON -> colorGold
                    Planet.SATURN, Planet.RAHU, Planet.KETU -> colorPurple
                    Planet.MARS, Planet.SUN -> colorOrange
                    else -> colorPrimary
                }
                
                builder.setSpan(ForegroundColorSpan(pColor), start, builder.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                if (index < planetsInSign.size - 1) {
                    builder.append(" ")
                }
            }

            textView.text = builder
            textView.contentDescription = resources.getString(
                R.string.house_content_description,
                0,
                sign.displayName,
                planetsInSign.joinToString { it.name }
            )
        }
    }

    private fun resetSigns() {
        binding.chartSummaryLagna.text = context.getString(R.string.chart_summary_placeholder_lagna)
        binding.chartSummaryNakshatra.text = context.getString(R.string.chart_summary_placeholder_nakshatra)
        binding.shareChartButton.isEnabled = false
        binding.shareChartButton.setOnClickListener(null)
        signViews.forEach { (sign, textView) ->
            textView.text = signAbbrev[sign] ?: ""
            textView.contentDescription = resources.getString(R.string.house_placeholder_content_description, 0)
        }
    }
}
