package soko.ekibun.stitch.util

import java.awt.Color
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint.CycleMethod

data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun width(): Float = if (right > left && isFinite()) right - left else 0f
    fun height(): Float = if (bottom > top && isFinite()) bottom - top else 0f
    private fun isFinite(): Boolean =
        left.isFinite() && top.isFinite() && right.isFinite() && bottom.isFinite()
}

data class PointF(val x: Float, val y: Float)

object GraphicsHelper {
    fun intToColor(color: Int): Color {
        val a = (color ushr 24) and 0xff
        val r = (color ushr 16) and 0xff
        val g = (color ushr 8) and 0xff
        val b = color and 0xff
        return Color(r, g, b, a)
    }

    fun colorToInt(color: Color): Int {
        return (color.alpha shl 24) or (color.red shl 16) or (color.green shl 8) or color.blue
    }

    fun makeLinearGradient(x0: Float, y0: Float, x1: Float, y1: Float,
                           color0: Int, color1: Int): LinearGradientPaint {
        return LinearGradientPaint(x0, y0, x1, y1,
            floatArrayOf(0f, 1f),
            arrayOf(intToColor(color0), intToColor(color1)),
            CycleMethod.NO_CYCLE)
    }
}
