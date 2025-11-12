package com.aakash.astro.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.aakash.astro.R
import com.aakash.astro.astrology.ChartResult
import com.aakash.astro.astrology.Planet
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ViewVedicChartBinding

class VedicChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVedicChartBinding.inflate(LayoutInflater.from(context), this, true)

    // Fixed-sign South Indian layout with Pisces at top-left, then clockwise.
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

    private val planetAbbrev = mapOf(
        Planet.SUN to "Su",
        Planet.MOON to "Mo",
        Planet.MERCURY to "Me",
        Planet.VENUS to "Ve",
        Planet.MARS to "Ma",
        Planet.JUPITER to "Ju",
        Planet.SATURN to "Sa",
        Planet.RAHU to "Ra",
        Planet.KETU to "Ke",
    )

    fun setChart(chart: ChartResult?) {
        if (chart == null) { resetSigns(); return }

        signViews.forEach { (sign, textView) ->
            val planetsInSign = chart.planets.filter { it.sign == sign }
            val planetsLabel = if (planetsInSign.isEmpty()) "" else planetsInSign.joinToString(" ") {
                val base = planetAbbrev[it.planet] ?: it.name.first().toString()
                if (it.isRetrograde) base + "(R)" else base
            }

            val label = buildString {
                // Hide sign name/abbrev per request; show only Lagna and planets.
                if (chart.ascendantSign == sign) {
                    append("Lagna")
                }
                if (planetsLabel.isNotEmpty()) {
                    if (isNotEmpty()) append('\n')
                    append(planetsLabel)
                }
            }
            textView.text = label
            textView.contentDescription = resources.getString(
                R.string.house_content_description,
                0,
                sign.displayName,
                planetsInSign.joinToString { it.name }
            )
        }
    }

    private fun resetSigns() {
        signViews.forEach { (_, textView) ->
            textView.text = ""
            textView.contentDescription = resources.getString(R.string.house_placeholder_content_description, 0)
        }
    }
}
