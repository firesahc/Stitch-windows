package soko.ekibun.stitch

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import soko.ekibun.stitch.util.PointF
import soko.ekibun.stitch.util.Rect
import kotlin.math.*

object Stitch {

    private val gson = Gson()

    data class StitchProject(
        val projectKey: String
    ) {
        val file by lazy {
            App.getProjectFile(projectKey)
        }
        val stitchInfo by lazy {
            val list = mutableListOf<StitchInfo>()
            if (file.exists()) runBlocking(App.dispatcherIO) {
                try {
                    list.addAll(
                        gson.fromJson<ArrayList<StitchInfo>>(
                            file.readText(),
                            object : TypeToken<ArrayList<StitchInfo>>() {}.type
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            list
        }

        private val stitchInfoBak by lazy {
            mutableListOf<StitchInfo>().apply { addAll(stitchInfo.map { it.clone() }) }
        }
        private val selectedBak by lazy {
            mutableSetOf<String>().apply { addAll(selected) }
        }
        private var undoTag: Any? = null
        fun clearUndoTag() {
            undoTag = null
        }

        fun updateUndo(tag: Any? = System.currentTimeMillis(), immediateSave: Boolean = true, runBeforeSave: () -> Unit) {
            if (tag != null && tag != undoTag) {
                undoTag = tag
                selectedBak.clear()
                selectedBak.addAll(selected)
                stitchInfoBak.clear()
                stitchInfoBak.addAll(stitchInfo.map {
                    it.clone()
                })
            }
            runBeforeSave()
            if (immediateSave) save()
        }

        var job: Job? = null
        fun save() {
            runBlocking {
                job?.cancelAndJoin()
                job = launch(App.dispatcherIO) job@{
                    try {
                        val info = stitchInfo.toList()
                        if (!file.exists()) {
                            if (info.isNotEmpty()) file.parentFile?.mkdirs()
                            else return@job
                        }
                        file.writeText(gson.toJson(info))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }

        fun undo() {
            val last = stitchInfo.map { it.clone() }
            val lastSelect = selected.map { it }
            stitchInfo.clear()
            stitchInfo.addAll(stitchInfoBak)
            selected.clear()
            selected.addAll(selectedBak)
            selectedBak.clear()
            selectedBak.addAll(lastSelect)
            stitchInfoBak.clear()
            stitchInfoBak.addAll(last)
            save()
        }

        fun updateInfo(): Rect {
            var cx = 0f
            var cy = 0f
            var rot = 0f
            var scale_ = 1f
            var lastPoints = listOf<PointF>()
            var boundLeft = Float.MAX_VALUE
            var boundTop = Float.MAX_VALUE
            var boundRight = Float.MIN_VALUE
            var boundBottom = Float.MIN_VALUE
            var cos = 1f
            var sin = 0f
            stitchInfo.forEachIndexed { i, it ->
                var dx = (it.dx * cos - it.dy * sin) * scale_
                var dy = (it.dy * cos + it.dx * sin) * scale_
                cx = if (i == 0) 0f else cx + dx
                cy = if (i == 0) 0f else cy + dy
                rot += it.drot
                scale_ *= it.dscale
                it.rot = rot
                it.scale = scale_
                it.cx = cx
                it.cy = cy

                cos = cos(it.rot * Math.PI / 180).toFloat()
                sin = sin(it.rot * Math.PI / 180).toFloat()
                val l = it.width * (it.xa - 0.5f) * it.scale
                val t = it.height * (it.ya - 0.5f) * it.scale
                val r = it.width * (it.xb - 0.5f) * it.scale
                val b = it.height * (it.yb - 0.5f) * it.scale
                val points = listOf(
                    PointF(cx + l * cos - t * sin, cy + l * sin + t * cos),
                    PointF(cx + l * cos - b * sin, cy + l * sin + b * cos),
                    PointF(cx + r * cos - t * sin, cy + r * sin + t * cos),
                    PointF(cx + r * cos - b * sin, cy + r * sin + b * cos)
                )
                if (it.dx == 0f && it.dy == 0f) {
                    dx = -sin
                    dy = cos
                }
                val mag2 = dx * dx + dy * dy
                var minV = Float.MAX_VALUE
                var maxV = Float.MIN_VALUE
                for (p in points) {
                    val prod = (cx - p.x) * dx + (cy - p.y) * dy
                    minV = min(minV, prod)
                    maxV = max(maxV, prod)

                    if (p.x < boundLeft) boundLeft = p.x
                    if (p.y < boundTop) boundTop = p.y
                    if (p.x > boundRight) boundRight = p.x
                    if (p.y > boundBottom) boundBottom = p.y
                }
                var minO = Float.MAX_VALUE
                var maxO = Float.MIN_VALUE
                for (p in lastPoints) {
                    val prod = (cx - p.x) * dx + (cy - p.y) * dy
                    minO = min(minO, prod)
                    maxO = max(maxO, prod)
                }
                lastPoints = points

                minV = max(minV, minO)
                maxV = max(minV, max(maxV, maxO))

                val va = maxV - (maxV - minV) * it.a
                val vb = maxV - (maxV - minV) * it.b - 0.01f * sqrt(mag2)

                it.shaderPts = floatArrayOf(
                    cx - (dx * cos + dy * sin) * va / mag2,
                    cy - (-dx * sin + dy * cos) * va / mag2,
                    cx - (dx * cos + dy * sin) * vb / mag2,
                    cy - (-dx * sin + dy * cos) * vb / mag2
                )
            }
            return Rect(boundLeft, boundTop, boundRight, boundBottom)
        }

        val selected by lazy {
            mutableSetOf<String>()
        }

        fun drawToCanvas(
            g: java.awt.Graphics2D,
            drawMask: Boolean,
            maskColor: Int,
            overPaintColor: Int,
            gradientPaintColor: Int,
        ) {
            val srcRange = IntArray(4)
            for (i in 0 until stitchInfo.size) {
                val it = stitchInfo.getOrNull(i) ?: continue
                srcRange[0] = (it.width * it.xa).toInt()
                srcRange[1] = (it.height * it.ya).toInt()
                srcRange[2] = (it.width * it.xb).toInt()
                srcRange[3] = (it.height * it.yb).toInt()
                val dstLeft = it.cx + (srcRange[0] - it.width / 2f) * it.scale
                val dstTop = it.cy + (srcRange[1] - it.height / 2f) * it.scale
                val dstRight = it.cx + (srcRange[2] - it.width / 2f) * it.scale
                val dstBottom = it.cy + (srcRange[3] - it.height / 2f) * it.scale

                val bmp = App.bitmapCache.getBitmap(it.imageKey)

                // Guard: skip images with degenerate coordinates (NaN/Inf)
                if (!dstLeft.isFinite() || !dstTop.isFinite() ||
                    !dstRight.isFinite() || !dstBottom.isFinite()) continue
                val dw = (dstRight - dstLeft).toDouble()
                val dh = (dstBottom - dstTop).toDouble()
                // Guard: skip zero-size images
                if (dw <= 0.0 || dh <= 0.0) continue

                val oldTransform = g.getTransform()
                try {
                    if (it.rot != 0f) {
                        g.rotate(Math.toRadians(it.rot.toDouble()), it.cx.toDouble(), it.cy.toDouble())
                    }

                    if (bmp != null) {
                        // Step 1: DST_OUT - erase existing canvas at gradient region
                        // (matches Android: gradientPaint with DST_OUT xfermode)
                        it.shaderPts?.let { pts ->
                            try {
                                val shader = soko.ekibun.stitch.util.GraphicsHelper.makeLinearGradient(
                                    pts[0], pts[1], pts[2], pts[3],
                                    0x00000000.toInt(), 0xffffffff.toInt()
                                )
                                g.setComposite(java.awt.AlphaComposite.DstOut)
                                g.setPaint(shader)
                                g.fillRect(dstLeft.toInt(), dstTop.toInt(), dw.toInt(), dh.toInt())
                            } catch (_: Exception) {
                                // gradient render failure is non-fatal (matches Android)
                            }
                        }

                        // Step 2: DST_OVER - selection mask overlay
                        // (matches Android: overPaint with DST_OVER xfermode)
                        if (drawMask && selected.contains(it.imageKey)) {
                            g.setComposite(java.awt.AlphaComposite.DstOver)
                            g.setColor(soko.ekibun.stitch.util.GraphicsHelper.intToColor(maskColor))
                            g.fillRect(dstLeft.toInt(), dstTop.toInt(), dw.toInt(), dh.toInt())
                        }

                        // Step 3: DST_OVER - draw image UNDER existing content
                        // (matches Android: overPaint with DST_OVER xfermode, color=BLACK)
                        g.setComposite(java.awt.AlphaComposite.DstOver)
                        g.drawImage(bmp,
                            dstLeft.toInt(), dstTop.toInt(), (dstLeft + dw).toInt(), (dstTop + dh).toInt(),
                            srcRange[0], srcRange[1], srcRange[2], srcRange[3], null)
                    }
                } finally {
                    g.setTransform(oldTransform)
                    g.setComposite(java.awt.AlphaComposite.SrcOver)
                }
            }
        }
    }

    data class StitchInfo(
        val imageKey: String,
        val width: Int,
        val height: Int,
        var dx: Float = 0f,
        var dy: Float = height / 2f,
        var drot: Float = 0f,
        var dscale: Float = 1f,
        var a: Float = 0.4f,
        var b: Float = 0.6f,
        var xa: Float = 0f,
        var xb: Float = 1f,
        var ya: Float = 0f,
        var yb: Float = 1f,
    ) {
        @Transient
        var cx: Float = 0f
        @Transient
        var cy: Float = 0f
        @Transient
        var rot: Float = 0f
        @Transient
        var scale: Float = 1f
        @Transient
        var shaderPts: FloatArray? = null

        fun clone(): StitchInfo {
            return StitchInfo(
                imageKey, width, height, dx, dy, drot, dscale, a, b, xa, xb, ya, yb
            )
        }
    }

    fun combine(homo: Boolean, diff: Boolean, img0: StitchInfo, img1: StitchInfo): StitchInfo? {
        return try {
            val (dx, dy, drot, dscale) = StitchNative.computeOffset(img0, img1, homo, diff)
            if ((dx != 0f || dy != 0f) &&
                abs(dx) < (img1.width + img0.width) / 2 &&
                abs(dy) < (img1.height + img0.height) / 2
            ) {
                img1.clone().also {
                    it.dx = dx
                    it.dy = dy
                    it.drot = drot
                    it.dscale = dscale
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
