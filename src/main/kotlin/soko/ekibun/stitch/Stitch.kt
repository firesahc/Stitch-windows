package soko.ekibun.stitch

import com.google.gson.Gson
import soko.ekibun.stitch.Renderer
import soko.ekibun.stitch.domain.GeometryCalculator
import soko.ekibun.stitch.domain.ProjectRepository
import soko.ekibun.stitch.service.UndoManager
import soko.ekibun.stitch.util.Rect

object Stitch {

    private val gson = Gson()

    class StitchProject(
        val projectKey: String,
        private val appContext: AppContext,
    ) {
        private val repository = ProjectRepository(gson, appContext.dispatcherIO)
        val file by lazy {
            appContext.projectManager.getProjectFile(projectKey)
        }
        val stitchInfo by lazy {
            repository.load(file)
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
        fun undo(): Boolean {
            val applied = undoManager.undo(stitchInfo, selected)
            if (applied) save()
            return applied
        }

        @Synchronized
        fun redo(): Boolean {
            val applied = undoManager.redo(stitchInfo, selected)
            if (applied) save()
            return applied
        }

        fun canUndo(): Boolean = undoManager.canUndo()
        fun canRedo(): Boolean = undoManager.canRedo()

        fun updateInfo(): Rect {
            // 薄委托：几何计算已抽至 domain.GeometryCalculator 纯函数
            return GeometryCalculator.updateInfo(stitchInfo)
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
