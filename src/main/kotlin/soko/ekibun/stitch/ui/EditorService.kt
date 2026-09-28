package soko.ekibun.stitch.ui

import kotlinx.coroutines.*
import soko.ekibun.stitch.AppContext
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.domain.ParamMapper
import soko.ekibun.stitch.domain.StitchLabels
import soko.ekibun.stitch.domain.StitchType
import soko.ekibun.stitch.interfaces.Dialogs
import soko.ekibun.stitch.interfaces.IEditorActivity
import java.io.File
import soko.ekibun.stitch.util.Strings
import javax.swing.*

/**
 * 编辑器业务执行者。
 *
 * 边界：不再直持 Swing 具体对话框，不再直读 modePanel.seekbar/switch；
 * 进度经 IEditorActivity（EditorProgress）回调，确认/文件选择经 Dialogs，
 * 参数换算委托 ParamMapper。TILE 所需的 slider(a,b)+方向由调用方（EditActivity）传入。
 */
class EditorService(
    private val appContext: AppContext,
    private val projectKey: String,
    private val activity: IEditorActivity,
    private val dialogs: Dialogs = appContext.dialogs
) {
    val project: Stitch.StitchProject
        get() = appContext.projectManager.getProject(projectKey)

    private val scope = CoroutineScope(SupervisorJob() + appContext.dispatcherIO)

    fun stitch(fullTransform: Boolean, edgeEnhance: Boolean) {
        if (project.selected.isEmpty()) {
            dialogs.warn(Strings.get("dialog.noSelection"), Strings.get("common.warning"))
            return
        }
        val total = project.selected.size
        activity.progressLabel.text = Strings.get("editor.progress", 0, total)
        activity.progressBar.value = 0
        activity.progressBar.maximum = total
        activity.progressRow.isVisible = true

        val failedIndices = mutableListOf<Int>()
        scope.launch {
            synchronized(project) {
                var done = 0
                project.updateUndo {
                    project.stitchInfo.reduceOrNull { acc, it ->
                        if (project.isSelected(it.imageKey)) {
                            val result = appContext.stitchService.combine(fullTransform, edgeEnhance, acc, it)
                            if (result != null) {
                                it.dx = result.dx; it.dy = result.dy
                                it.drot = result.drot; it.dscale = result.dscale
                            } else {
                                val idx = project.indexOfInfo(it.imageKey)
                                if (idx >= 0) failedIndices.add(idx)
                            }
                            done++
                            val finalDone = done
                            SwingUtilities.invokeLater {
                                activity.progressLabel.text = Strings.get("editor.progress", finalDone, total)
                                activity.progressBar.value = finalDone
                            }
                        }
                        it
                    }
                }
            }
            SwingUtilities.invokeLater {
                activity.progressRow.isVisible = false
                activity.updateSelectInfo()
                if (failedIndices.isNotEmpty()) {
                    dialogs.warn(
                        Strings.get("editor.stitchFailed", failedIndices.size, failedIndices.joinToString(", ")),
                        Strings.get("editor.stitchFailedTitle")
                    )
                }
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }

    fun swapSelected() {
        if (project.selected.size < 2) {
            dialogs.warn(Strings.get("dialog.selectTwoImages"), Strings.get("common.warning"))
            return
        }
        project.updateUndo {
            val selected = project.selected.toList()
            val i = project.indexOfInfo(selected.last())
            if (i < 0) return@updateUndo
            var a = project.getStitchInfo(i) ?: return@updateUndo
            val adx = a.dx; val ady = a.dy; val adr = a.drot; val ads = a.dscale
            for (indic in 0 until selected.size - 1) {
                val j = project.indexOfInfo(selected[indic])
                if (j < 0) return@updateUndo
                val b = project.stitchInfo.set(j, a)
                a.dx = b.dx; a.dy = b.dy; a.drot = b.drot; a.dscale = b.dscale
                a = b
            }
            project.stitchInfo[i] = a
            a.dx = adx; a.dy = ady; a.drot = adr; a.dscale = ads
        }
        activity.updateSelectInfo()
    }

    fun removeSelected(): Boolean {
        if (project.selected.isEmpty()) {
            dialogs.warn(Strings.get("dialog.noSelection"), Strings.get("common.warning"))
            return false
        }
        val result = dialogs.confirm(
            Strings.get("dialog.confirmDelete", project.selected.size),
            Strings.get("dialog.confirmTitle")
        )
        if (result) {
            project.updateUndo {
                project.stitchInfo.removeAll { project.isSelected(it.imageKey) }
                project.selected.clear()
            }
            activity.updateSelectInfo()
            return true
        }
        return false
    }

    fun saveImage(bitmapProvider: (() -> java.awt.image.BufferedImage)? = null): Boolean {
        if (project.stitchInfo.isEmpty()) {
            dialogs.warn(Strings.get("dialog.noImages"), Strings.get("common.warning"))
            return false
        }
        val file = dialogs.pickSaveFile("Stitch$projectKey.png") ?: return false

        try {
            // 默认经 activity.editView 取图（过渡兼容）；新调用方可直接传入 bitmap 以彻底解耦 View。
            val image = bitmapProvider?.invoke() ?: activity.editView.drawToBitmap()
            appContext.bitmapCache.saveImageToPath(image, file)
            dialogs.info(Strings.get("dialog.saved", file.absolutePath), Strings.get("common.success"))
            return true
        } catch (e: Exception) {
            dialogs.error(Strings.get("dialog.saveFail", e.message), Strings.get("common.error"))
            return false
        }
    }

    fun addImages(files: List<File>): Int {
        if (files.isEmpty()) return 0
        val result = ImageImport.decode(files)
        if (result.decoded.isNotEmpty()) {
            project.updateUndo("dropImages") {
                result.decoded.forEach { d ->
                    val key = appContext.bitmapCache.saveBitmap(projectKey, d.image)
                    val info = Stitch.StitchInfo(key, d.image.width, d.image.height)
                    project.stitchInfo.add(info)
                    project.selected.add(info.imageKey)
                }
            }
            activity.updateSelectInfo()
        }
        val skippedTotal = result.unsupportedNames.size + result.failedNames.size
        if (result.decoded.isEmpty()) {
            dialogs.warn(Strings.get("dialog.dropNoSupported"), Strings.get("common.warning"))
        } else if (skippedTotal > 0) {
            val detail = (result.unsupportedNames + result.failedNames).take(5).joinToString(", ")
            dialogs.warn(Strings.get("dialog.dropSkipped", result.decoded.size, skippedTotal, detail), Strings.get("common.warning"))
        }
        return result.decoded.size
    }

    companion object {
        val SUPPORTED_EXTENSIONS = setOf("png", "jpg", "jpeg", "bmp")
        @Deprecated("已收敛至 domain.ParamMapper", ReplaceWith("ParamMapper", "soko.ekibun.stitch.domain.ParamMapper"))
        sealed class NumberLabelHandler {
            abstract fun apply(
                info: Stitch.StitchInfo,
                a: Float?,
                b: Float?,
                relative: Boolean,
                width: Int,
                height: Int
            )

            object Dx : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    ParamMapper.applyAbsolute(info, StitchLabels.labelDx, a, null)
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelDx, a, null)
                }
            }

            object Dy : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelDy, a, null)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelDy, a, null)
                }
            }

            object Trim : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelTrim, a, b)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelTrim, a, b)
                }
            }

            object Xrange : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelXrange, a, b)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelXrange, a, b)
                }
            }

            object Yrange : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelYrange, a, b)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelYrange, a, b)
                }
            }

            object Scale : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelScale, a, null)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelScale, a, null)
                }
            }

            object Rotate : NumberLabelHandler() {
                override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                    if (relative) ParamMapper.applyRelative(info, StitchLabels.labelRotate, a, null)
                    else ParamMapper.applyAbsolute(info, StitchLabels.labelRotate, a, null)
                }
            }
        }
    }

    /**
     * 统一参数入口。
     *
     * @param tileSlider 当 stitchType==TILE 时由调用方传入当前 slider(a,b)；非 TILE 传 null。
     * @param horizontal TILE 方向（switchHorizon），非 TILE 忽略。
     */
    fun setNumber(
        a: Float? = null,
        b: Float? = null,
        relative: Boolean = false,
        tileSlider: Pair<Float, Float>? = null,
        horizontal: Boolean = false
    ) {
        val selected = activity.selectPanel.selectedStitchInfo
        if (selected.isEmpty()) return
        val type = activity.stitchType
        if (type == StitchType.TILE) {
            // 兼容旧调用：未传 tileSlider 时回退读 modePanel（过渡期），新调用方应显式传入。
            val (aa, bb) = tileSlider ?: (a?.let { it to (b ?: it) } ?: (activity.modePanel.seekbar.a to activity.modePanel.seekbar.b))
            val h = if (tileSlider != null) horizontal else activity.modePanel.switchHorizon.isSelected
            selected.forEach { ParamMapper.applyTile(it, aa, bb, h) }
        } else {
            val label = activity.selectIndex
            selected.forEach {
                if (relative) ParamMapper.applyRelative(it, label, a, b)
                else ParamMapper.applyAbsolute(it, label, a, b)
            }
        }
    }
}
