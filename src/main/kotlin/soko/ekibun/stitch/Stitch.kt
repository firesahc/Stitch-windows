package soko.ekibun.stitch

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import soko.ekibun.stitch.Renderer
import soko.ekibun.stitch.service.UndoManager
import soko.ekibun.stitch.util.PointF
import soko.ekibun.stitch.util.Rect
import kotlin.math.*

object Stitch {

    private val gson = Gson()

    class StitchProject(
        val projectKey: String,
        private val appContext: AppContext,
    ) {
        val file by lazy {
            appContext.projectManager.getProjectFile(projectKey)
        }
        val stitchInfo by lazy {
            val list = mutableListOf<StitchInfo>()
            if (file.exists()) runBlocking(appContext.dispatcherIO) {
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

        val undoManager = UndoManager()
        fun clearUndoTag() {
            undoManager.clearUndoTag()
        }

        fun updateUndo(tag: Any? = System.currentTimeMillis(), immediateSave: Boolean = true, runBeforeSave: () -> Unit) {
            undoManager.updateUndo(tag, stitchInfo, selected, runBeforeSave)
            if (immediateSave) save()
        }

        @Synchronized
        fun save() {
            undoManager.save(file, stitchInfo, gson, appContext.dispatcherIO)
        }

        @Synchronized
        fun undo() {
            undoManager.undo(stitchInfo, selected)
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
                maxV = max(minV, min(maxV, maxO))

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
            Renderer.drawToCanvas(
                g = g,
                drawMask = drawMask,
                maskColor = maskColor,
                overPaintColor = overPaintColor,
                gradientPaintColor = gradientPaintColor,
                stitchInfo = stitchInfo,
                selected = selected,
                bitmapCache = appContext.bitmapCache,
            )
        }

        /** Facade: check if an image key is selected */
        fun isSelected(imageKey: String): Boolean = selected.contains(imageKey)

        /** Facade: find index of a stitch info by image key */
        fun indexOfInfo(imageKey: String): Int = stitchInfo.indexOfFirst { it?.imageKey == imageKey }

        /** Facade: get stitch info at index (null-safe) */
        fun getStitchInfo(index: Int): StitchInfo? = stitchInfo.getOrNull(index)

        /** Facade: get all selected stitch infos */
        fun getSelectedInfos(): List<StitchInfo> = stitchInfo.filter { it != null && selected.contains(it.imageKey) }
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

}
