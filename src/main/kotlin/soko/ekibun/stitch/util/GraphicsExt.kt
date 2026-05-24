package soko.ekibun.stitch.util

import java.awt.Color
import java.awt.LinearGradientPaint
import java.awt.MultipleGradientPaint.CycleMethod

val PRIMARY_COLOR = Color(66, 132, 243)

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

    fun createAppIcon(): java.awt.image.BufferedImage {
        val icon = java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = icon.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = PRIMARY_COLOR
        g.fillRoundRect(2, 2, 28, 28, 8, 8)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("Microsoft YaHei", java.awt.Font.BOLD, 20)
        val fm = g.fontMetrics
        val sw = fm.stringWidth("S")
        g.drawString("S", (32 - sw) / 2, 24)
        g.dispose()
        return icon
    }
}
