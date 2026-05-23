package soko.ekibun.stitch.ui

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseMotionAdapter
import javax.swing.JComponent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class RangeSlider : JComponent() {
    companion object {
        const val TYPE_RANGE = 0
        const val TYPE_GRADIENT = 1
        const val TYPE_CENTER = 2
    }

    var type = TYPE_RANGE

    var a = 0f
        set(value) { field = max(0f, min(1f, value)) }
    var b = 1f
        set(value) { field = max(0f, min(1f, value)) }

    var onRangeChange: ((Float, Float) -> Unit)? = null
    var onTouchUp: (() -> Unit)? = null

    private val primaryColor = Color(66, 132, 243)
    private val opaqueColor = Color(136, 136, 136, 136)

    private var downObj = 0

    init {
        preferredSize = Dimension(200, 30)
        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val w = width.toDouble()
                val r = 7.0
                val ax = r + (w - 2 * r) * a
                val bx = r + (w - 2 * r) * b
                val ar = abs(e.x - r - (w - 2 * r) * a)
                val br_ = abs(e.x - r - (w - 2 * r) * b)
                downObj = when {
                    (type == TYPE_CENTER || ar <= br_) && ar < 2 * r -> 1
                    type != TYPE_CENTER && ar >= br_ && br_ < 2 * r -> 2
                    else -> 0
                }
            }

            override fun mouseReleased(e: MouseEvent) {
                downObj = 0
                onTouchUp?.invoke()
            }
        })

        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                if (downObj == 0) return
                val w = width.toDouble()
                val r = 7.0
                val newValue = ((e.x - r) / max(1.0, (w - 2 * r))).coerceIn(0.0, 1.0)
                when (downObj) {
                    1 -> {
                        a = newValue.toFloat()
                        if (type == TYPE_CENTER) {
                            if (abs(a - 0.5f) < 0.03f) a = 0.5f
                        } else if (type == TYPE_GRADIENT && a > b) {
                            b = a
                        }
                    }
                    2 -> b = max(a, newValue.toFloat())
                }
                onRangeChange?.invoke(a, b)
                repaint()
            }
        })
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        val w = width.toDouble()
        val h = height.toDouble()
        if (w <= 0 || h <= 0) return

        val radius = 7.0
        val thick = 1.0

        val ax = radius + (w - 2 * radius) * a
        val bx = radius + (w - 2 * radius) * b

        when (type) {
            TYPE_GRADIENT -> drawGradient(g2d, ax, bx, w, h)
            TYPE_RANGE -> drawRange(g2d, ax, bx, w, h)
            TYPE_CENTER -> drawCenter(g2d, ax, w, h)
        }

        g2d.color = primaryColor
        g2d.fillOval((ax - radius).toInt(), ((h / 2 - radius).toInt()), (radius * 2).toInt(), (radius * 2).toInt())

        if (type != TYPE_CENTER) {
            g2d.fillOval((bx - radius).toInt(), ((h / 2 - radius).toInt()), (radius * 2).toInt(), (radius * 2).toInt())
        }
    }

    private fun drawGradient(g2d: Graphics2D, ax: Double, bx: Double, w: Double, h: Double) {
        val radius = 7.0
        val thick = 1.0
        if (ax != bx) {
            val grad = LinearGradientPaint(
                ax.toFloat(), 0f, bx.toFloat(), 0f,
                floatArrayOf(0f, 1f),
                arrayOf(opaqueColor, primaryColor),
                MultipleGradientPaint.CycleMethod.NO_CYCLE
            )
            g2d.paint = grad
            g2d.fillRect(radius.toInt(), (h / 2 - thick).toInt(), (w - 2 * radius).toInt(), (thick * 2).toInt())
        } else {
            g2d.color = opaqueColor
            g2d.fillRect(radius.toInt(), (h / 2 - thick).toInt(), (w - 2 * radius).toInt(), (thick * 2).toInt())
        }
    }

    private fun drawRange(g2d: Graphics2D, ax: Double, bx: Double, w: Double, h: Double) {
        val radius = 7.0
        val thick = 1.0
        g2d.color = opaqueColor
        g2d.fillRect(radius.toInt(), (h / 2 - thick).toInt(), (w - 2 * radius).toInt(), (thick * 2).toInt())
        g2d.color = primaryColor
        g2d.fillRect(ax.toInt(), (h / 2 - thick).toInt(), (bx - ax).toInt(), (thick * 2).toInt())
    }

    private fun drawCenter(g2d: Graphics2D, ax: Double, w: Double, h: Double) {
        val radius = 7.0
        val thick = 1.0
        g2d.color = opaqueColor
        g2d.fillRect(radius.toInt(), (h / 2 - thick).toInt(), (w - 2 * radius).toInt(), (thick * 2).toInt())
        g2d.color = primaryColor
        g2d.fillRect((w / 2).toInt(), (h / 2 - thick).toInt(), (ax - w / 2).toInt(), (thick * 2).toInt())
    }

    fun draw() {
        repaint()
    }
}
