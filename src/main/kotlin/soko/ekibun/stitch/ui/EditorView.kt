package soko.ekibun.stitch.ui

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.GraphicsHelper
import soko.ekibun.stitch.util.Rect
import java.awt.*
import java.awt.event.*
import javax.swing.JPanel
import kotlin.math.*

class EditorView(private val editActivity: EditActivity) : JPanel() {

    var scale = 0.8
    var scrollX = 0.0
    var scrollY = 0.0
    var bound = Rect(0f, 0f, 0f, 0f)

    private var initScrollDone = false

    private var touching: Stitch.StitchInfo? = null
    private var dragging = false
    private var initialTouchX = 0.0
    private var initialTouchY = 0.0
    private var lastTouchX = 0.0
    private var lastTouchY = 0.0
    private var downOffsetX = 0.0
    private var downOffsetY = 0.0
    private var dragDirty = false

    private val colorPrimary = soko.ekibun.stitch.util.PRIMARY_COLOR
    private val colorUnselected = Color(136, 136, 136, 136)

    init {
        isOpaque = true
        background = Color.WHITE

        addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                onMousePressed(e)
            }
            override fun mouseReleased(e: MouseEvent) {
                onMouseReleased(e)
            }
        })
        addMouseMotionListener(object : MouseMotionAdapter() {
            override fun mouseDragged(e: MouseEvent) {
                onMouseDragged(e)
            }
        })
        addMouseWheelListener { e -> onScroll(e) }
    }

    val project: Stitch.StitchProject? get() = editActivity.project

    fun update() {
        bound = project?.updateInfo() ?: Rect(0f, 0f, 0f, 0f)
        if (!initScrollDone && width > 0 && height > 0) {
            initScrollDone = true
            if (bound.width() > 0f && bound.height() > 0f) {
                scale = min(width.toDouble() / bound.width().toDouble(), height.toDouble() / bound.height().toDouble()) * 0.95
                scale = scale.coerceIn(0.2, 5.0)
            }
            scrollX = (minScrollX + maxScrollX) / 2
            scrollY = (minScrollY + maxScrollY) / 2
        }
        clampScroll()
        repaint()
    }

    private val minScrollY: Double get() = bound.top.toDouble() * scale
    private val maxScrollY: Double get() = max(minScrollY, bound.bottom.toDouble() * scale - height)
    private val minScrollX: Double get() = bound.left.toDouble() * scale
    private val maxScrollX: Double get() = max(minScrollX, bound.right.toDouble() * scale - width)

    private fun clampScroll() {
        scrollX = max(minScrollX, min(maxScrollX, scrollX))
        scrollY = max(minScrollY, min(maxScrollY, scrollY))
    }

    fun redraw() { repaint() }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val g2d = g as Graphics2D

        val w = width
        val h = height
        if (w <= 0 || h <= 0) return

        if (bound.width() <= 0f || bound.height() <= 0f) return

        val transX = max(0.0, (w - bound.width().toDouble() * scale) / 2) - scrollX
        val transY = max(0.0, (h - bound.height().toDouble() * scale) / 2) - scrollY

        // Render to transparent offscreen buffer so DST_OVER compositing works correctly
        // (the panel's white opaque background would block DST_OVER)
        val buffer = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val bg = buffer.createGraphics()
        try {
            bg.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            bg.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)

            bg.translate(transX, transY)
            bg.scale(scale, scale)

            val maskColor = GraphicsHelper.colorToInt(Color(136, 136, 136, 136))
            project?.drawToCanvas(bg, true, maskColor, GraphicsHelper.colorToInt(Color.BLACK), 0)

            // Draw UI overlay (circles, numbers, connection lines)
            val radius = 20.0 / scale
            val textSize = (15.0 / scale).coerceIn(8.0, 48.0)
            val baseline = textSize / 2

            bg.font = Font(Font.SANS_SERIF, Font.PLAIN, textSize.toInt())

            project?.stitchInfo?.let { infoList ->
                infoList.firstOrNull()?.let { info ->
                    val sel = project!!.selected.contains(info.imageKey)
                    bg.color = if (sel) colorPrimary else colorUnselected
                    bg.fillOval((info.cx - radius).toInt(), (info.cy - radius).toInt(), (radius * 2).toInt(), (radius * 2).toInt())
                    bg.color = Color.WHITE
                    val fm = bg.fontMetrics
                    val text = "0"
                    val sw = fm.stringWidth(text)
                    bg.drawString(text, (info.cx - sw / 2f).toInt(), (info.cy + baseline / 2f).toInt())
                }

                infoList.reduceIndexedOrNull { i, acc, info ->
                    val sel = project!!.selected.contains(info.imageKey)
                    bg.color = if (sel) colorPrimary else colorUnselected
                    bg.fillOval((info.cx - radius).toInt(), (info.cy - radius).toInt(), (radius * 2).toInt(), (radius * 2).toInt())

                    val dx = info.cx - acc.cx
                    val dy = info.cy - acc.cy
                    val lineOffset = sqrt(dx.toDouble() * dx + dy.toDouble() * dy).toFloat() / radius.toFloat()
                    if (lineOffset > 2) {
                        bg.color = if (sel) colorPrimary else colorUnselected
                        bg.stroke = BasicStroke((3.0 / scale).toFloat())
                        bg.drawLine(
                            (acc.cx + dx / lineOffset).toInt(),
                            (acc.cy + dy / lineOffset).toInt(),
                            (info.cx - dx / lineOffset).toInt(),
                            (info.cy - dy / lineOffset).toInt()
                        )
                    }

                    bg.color = Color.WHITE
                    val fm = bg.fontMetrics
                    val text = i.toString()
                    val sw = fm.stringWidth(text)
                    bg.drawString(text, (info.cx - sw / 2f).toInt(), (info.cy + baseline / 2f).toInt())
                    info
                }
            }
        } finally {
            bg.dispose()
        }

        // Composite offscreen buffer onto the white panel background
        g2d.drawImage(buffer, 0, 0, null)
    }

    private fun onMousePressed(e: MouseEvent) {
        touching = null
        dragging = false
        val (transX, transY) = getTranslate()
        val x = (e.x - transX) / scale
        val y = (e.y - transY) / scale
        val radius = 20.0 / scale

        project?.stitchInfo?.lastOrNull {
            abs(it.cx - x) < radius && abs(it.cy - y) < radius
        }?.let { hit ->
            touching = hit
            downOffsetX = x - hit.cx
            downOffsetY = y - hit.cy
        }
        initialTouchX = e.x.toDouble()
        lastTouchX = e.x.toDouble()
        initialTouchY = e.y.toDouble()
        lastTouchY = e.y.toDouble()
    }

    private fun onMouseDragged(e: MouseEvent) {
        val touching = touching
        if (touching != null) {
            val (transX, transY) = getTranslate()
            val x = (e.x - transX) / scale - downOffsetX
            val y = (e.y - transY) / scale - downOffsetY

            if (abs(initialTouchX - e.x) > 10 || abs(initialTouchY - e.y) > 10) {
                dragging = true
            }
            if (dragging) {
                project?.updateUndo(touching, immediateSave = false) {
                    val ddx = x - touching.cx
                    val ddy = y - touching.cy
                    val cos = cos((touching.rot - touching.drot) * Math.PI / 180).toFloat()
                    val sin = sin((touching.rot - touching.drot) * Math.PI / 180).toFloat()
                    val s = if (touching.dscale == 0f) 0f else touching.scale / touching.dscale
                    touching.dx += if (s == 0f) 0f else ((ddx * cos + ddy * sin) / s).toFloat()
                    touching.dy += if (s == 0f) 0f else (((-ddx * sin + ddy * cos) / s)).toFloat()
                }
                dragDirty = true
                editActivity.updateSelectInfo()
            }
        } else {
            val dx = lastTouchX - e.x
            val dy = lastTouchY - e.y
            scrollX += dx; scrollY += dy
            clampScroll()
            lastTouchX = e.x.toDouble(); lastTouchY = e.y.toDouble()
            repaint()
        }
    }

    private fun onMouseReleased(e: MouseEvent) {
        val touching = this.touching
        this.touching = null
        if (dragDirty) {
            dragDirty = false
            project?.save()
        }
        if (!dragging && touching != null) {
            editActivity.selectToggle(touching)
        }
    }

    private fun onScroll(e: MouseWheelEvent) {
        if (e.isControlDown) {
            val oldScale = scale
            scale = (scale * (1 + e.preciseWheelRotation / 4.0)).coerceIn(0.2, 5.0)
            scrollX = e.x - (e.x - scrollX) * scale / oldScale
            scrollY = e.y - (e.y - scrollY) * scale / oldScale
            clampScroll()
            repaint()
        } else {
            scrollY -= e.wheelRotation * 20
            if (e.isShiftDown) scrollX -= e.wheelRotation * 20
            clampScroll()
            repaint()
        }
    }

    private fun getTranslate(): Pair<Double, Double> {
        clampScroll()
        val transX = max(0.0, (width - bound.width().toDouble() * scale) / 2) - scrollX
        val transY = max(0.0, (height - bound.height().toDouble() * scale) / 2) - scrollY
        return transX to transY
    }

    fun drawToBitmap(): java.awt.image.BufferedImage {
        bound = project?.updateInfo() ?: Rect(0f, 0f, 0f, 0f)
        val w = max(1, bound.width().toInt())
        val h = max(1, bound.height().toInt())
        val bmp = java.awt.image.BufferedImage(w, h, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g2d = bmp.createGraphics()

        try {
            g2d.translate(-bound.left.toDouble(), -bound.top.toDouble())

            val maskColor = GraphicsHelper.colorToInt(Color(136, 136, 136, 136))
            project?.drawToCanvas(g2d, false, maskColor, GraphicsHelper.colorToInt(Color.BLACK), 0)
        } finally {
            g2d.dispose()
        }
        return bmp
    }
}
