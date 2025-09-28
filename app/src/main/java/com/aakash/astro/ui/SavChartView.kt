package com.aakash.astro.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.TextView
import com.aakash.astro.R
import com.aakash.astro.astrology.ZodiacSign
import com.aakash.astro.databinding.ViewVedicChartBinding

/**
 * Displays Sarva Ashtakavarga (SAV) values inside a fixed-sign South Indian chart grid.
 * Expects values[0..11] ordered Aries..Pisces.
 */
class SavChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = ViewVedicChartBinding.inflate(LayoutInflater.from(context), this, true)

    // Same mapping as VedicChartView: Pisces at top-left, then Aries..Aquarius clockwise.
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

    private var highlighted: ZodiacSign? = null

    fun setSav(values: IntArray?) {
        if (values == null || values.size != 12) { reset(); return }
        ZodiacSign.entries.forEachIndexed { index, sign ->
            val tv = signViews[sign]
            val v = values[index]
            if (tv != null) {
                tv.text = v.toString()
                tv.contentDescription = resources.getString(
                    R.string.house_content_description,
                    index + 1,
                    sign.displayName,
                    resources.getString(R.string.sav_value_content_desc, v)
                )
            }
        }
    }

    fun setHighlight(sign: ZodiacSign?) {
        // Clear previous highlight
        signViews.values.forEach { tv -> tv.setBackgroundResource(0) }
        highlighted = sign
        if (sign != null) {
            val tv = signViews[sign]
            tv?.setBackgroundResource(R.drawable.bg_chart_highlight)
        }
    }

    private fun reset() {
        signViews.values.forEach { tv ->
            tv.text = ""
            tv.contentDescription = resources.getString(R.string.house_placeholder_content_description, 0)
            tv.setBackgroundResource(0)
        }
    }
}
