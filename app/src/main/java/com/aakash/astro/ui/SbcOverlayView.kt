package com.aakash.astro.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

// Coordinates are in grid units (0..9). Integers represent cell boundaries;
// use "+0.5" to target the center of a cell.
data class SbcLine(val r1: Float, val c1: Float, val r2: Float, val c2: Float, val color: Int)

class SbcOverlayView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val lines = mutableListOf<SbcLine>()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    fun setLines(newLines: List<SbcLine>) {
        lines.clear()
        lines.addAll(newLines)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val cw = width / 9f
        val ch = height / 9f
        lines.forEach { l ->
            paint.color = l.color
            val x1 = (l.c1) * cw
            val y1 = (l.r1) * ch
            val x2 = (l.c2) * cw
            val y2 = (l.r2) * ch
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
    }
}
