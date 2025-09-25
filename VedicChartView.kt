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

    private val houseViews: Map<Int, TextView> = mapOf(
        1 to binding.house1,
        2 to binding.house2,
        3 to binding.house3,
        4 to binding.house4,
        5 to binding.house5,
        6 to binding.house6,
        7 to binding.house7,
        8 to binding.house8,
        9 to binding.house9,
        10 to binding.house10,
        11 to binding.house11,
        12 to binding.house12
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

    private val signAbbrev = mapOf(
        ZodiacSign.ARIES to "Ar",
        ZodiacSign.TAURUS to "Ta",
        ZodiacSign.GEMINI to "Ge",
        ZodiacSign.CANCER to "Cn",
        ZodiacSign.LEO to "Le",
        ZodiacSign.VIRGO to "Vi",
        ZodiacSign.LIBRA to "Li",
        ZodiacSign.SCORPIO to "Sc",
        ZodiacSign.SAGITTARIUS to "Sg",
        ZodiacSign.CAPRICORN to "Cp",
        ZodiacSign.AQUARIUS to "Aq",
        ZodiacSign.PISCES to "Pi",
    )

    fun setChart(chart: ChartResult?) {
        if (chart == null) {
            resetHouses()
            return
        }

        val houseData = chart.houses.associateBy { it.number }

        houseViews.forEach { (number, textView) ->
            val info = houseData[number]
            val sign = info?.sign
            val signSymbol = sign?.symbol ?: ""
            val signShort = sign?.let { signAbbrev[it] } ?: ""
            val planetsInHouse = chart.planets.filter { it.house == number }

            val planetsLabel = if (planetsInHouse.isEmpty()) "" else planetsInHouse.joinToString(" ") {
                planetAbbrev[it.planet] ?: it.name.first().toString()
            }

            val label = buildString {
                // South Indian style cell content: sign + Lagna tag + planets
                if (signSymbol.isNotEmpty()) {
                    append(signSymbol)
                } else if (signShort.isNotEmpty()) {
                    append(signShort)
                }
                if (number == 1) {
                    if (isNotEmpty()) append(' ')
                    append("Lagna")
                }
                if (planetsLabel.isNotEmpty()) {
                    append('\n')
                    append(planetsLabel)
                }
            }

            textView.text = label.ifEmpty { if (number == 1) "Lagna" else signShort }
            textView.contentDescription = resources.getString(
                R.string.house_content_description,
                number,
                sign?.displayName ?: "",
                planetsInHouse.joinToString { it.name }
            )
        }
    }

    private fun resetHouses() {
        houseViews.forEach { (number, textView) ->
            textView.text = if (number == 1) "Lagna" else ""
            textView.contentDescription = resources.getString(R.string.house_placeholder_content_description, number)
        }
    }
}
