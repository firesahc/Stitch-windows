package soko.ekibun.stitch.ui

import kotlinx.coroutines.*
import soko.ekibun.stitch.AppContext
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.interfaces.IEditorActivity
import soko.ekibun.stitch.interfaces.IEditorActivity.StitchType
import java.io.File
import javax.imageio.ImageIO
import soko.ekibun.stitch.util.Strings
import javax.swing.*

class EditorService(
    private val appContext: AppContext,
    private val projectKey: String,
    private val activity: IEditorActivity,
) {
    val project: Stitch.StitchProject
        get() = appContext.projectManager.getProject(projectKey)

    private val scope = CoroutineScope(SupervisorJob() + appContext.dispatcherIO)

    fun stitch(fullTransform: Boolean, edgeEnhance: Boolean) {
        if (project.selected.isEmpty()) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.noSelection"), Strings.get("common.warning"), JOptionPane.WARNING_MESSAGE)
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
                    val msg = "以下 ${failedIndices.size} 张图片拼接失败（编号从 0 开始）：\n" +
                            failedIndices.joinToString(", ")
                    JOptionPane.showMessageDialog(null, msg, "拼接失败",
                        JOptionPane.WARNING_MESSAGE)
                }
            }
        }
    }

    fun cancel() {
        scope.cancel()
    }

    fun swapSelected() {
        if (project.selected.size < 2) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.selectTwoImages"), Strings.get("common.warning"), JOptionPane.WARNING_MESSAGE)
            return
        }
        project.updateUndo {
            val selected = project.selected.toList()
            val i = project.indexOfInfo(selected.last())
            if (i < 0) return@updateUndo
            var a = project.getStitchInfo(i)!!
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
            JOptionPane.showMessageDialog(null, Strings.get("dialog.noSelection"), Strings.get("common.warning"), JOptionPane.WARNING_MESSAGE)
            return false
        }
        val result = JOptionPane.showConfirmDialog(null,
            Strings.get("dialog.confirmDelete", project.selected.size),
            Strings.get("dialog.confirmTitle"), JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            project.updateUndo {
                project.stitchInfo.removeAll { project.isSelected(it.imageKey) }
                project.selected.clear()
            }
            activity.updateSelectInfo()
            return true
        }
        return false
    }

    fun saveImage(): Boolean {
        if (project.stitchInfo.isEmpty()) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.noImages"), Strings.get("common.warning"), JOptionPane.WARNING_MESSAGE)
            return false
        }
        val chooser = JFileChooser()
        chooser.dialogTitle = Strings.get("dialog.saveImage")
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(Strings.get("dialog.pngImage"), "png")
        chooser.selectedFile = File("Stitch$projectKey.png")
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return false

        try {
            val file = chooser.selectedFile
            val image = activity.editView.drawToBitmap()
            ImageIO.write(image, "png", file)
            JOptionPane.showMessageDialog(null, Strings.get("dialog.saved", file.absolutePath), Strings.get("common.success"), JOptionPane.INFORMATION_MESSAGE)
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.saveFail", e.message), Strings.get("common.error"), JOptionPane.ERROR_MESSAGE)
            return false
        }
    }

    fun addImage(file: File): Boolean {
        try {
            val bufferedImage = ImageIO.read(file)
            if (bufferedImage == null) {
                JOptionPane.showMessageDialog(null, Strings.get("dialog.imageLoadFailed", file.name), Strings.get("common.error"), JOptionPane.ERROR_MESSAGE)
                return false
            }
            val key = appContext.bitmapCache.saveBitmap(projectKey, bufferedImage)
            val info = Stitch.StitchInfo(key, bufferedImage.width, bufferedImage.height)
            project.updateUndo { project.stitchInfo.add(info); project.selected.add(info.imageKey) }
            activity.updateSelectInfo()
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.imageLoadError", e.message), Strings.get("common.error"), JOptionPane.ERROR_MESSAGE)
            return false
        }
    }

    companion object {
    private val numberHandlers = mapOf(
        IEditorActivity.labelDx to NumberLabelHandler.Dx,
        IEditorActivity.labelDy to NumberLabelHandler.Dy,
        IEditorActivity.labelTrim to NumberLabelHandler.Trim,
        IEditorActivity.labelXrange to NumberLabelHandler.Xrange,
        IEditorActivity.labelYrange to NumberLabelHandler.Yrange,
        IEditorActivity.labelScale to NumberLabelHandler.Scale,
        IEditorActivity.labelRotate to NumberLabelHandler.Rotate,
    )

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
                if (a != null) info.dx = if (relative) (a * 2 - 1) * width else a
            }
        }

        object Dy : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.dy = if (relative) (a * 2 - 1) * height else a
            }
        }

        object Trim : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.a = a
                if (b != null) info.b = b
            }
        }

        object Xrange : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.xa = if (!relative && width > 0) a / width else a
                if (b != null) info.xb = if (!relative && width > 0) b / width else b
            }
        }

        object Yrange : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.ya = if (!relative && height > 0) a / height else a
                if (b != null) info.yb = if (!relative && height > 0) b / height else b
            }
        }

        object Scale : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.dscale = if (relative) (a * 2) else a
            }
        }

        object Rotate : NumberLabelHandler() {
            override fun apply(info: Stitch.StitchInfo, a: Float?, b: Float?, relative: Boolean, width: Int, height: Int) {
                if (a != null) info.drot = if (relative) (a * 2 - 1) * 180 else a
            }
        }
    }
}

    fun setNumber(a: Float? = null, b: Float? = null, relative: Boolean = false) {
        val selected = activity.selectPanel.selectedStitchInfo
        if (selected.isNotEmpty()) selected.forEach {
            if (activity.stitchType == StitchType.TILE) {
                val aa = a ?: activity.modePanel.seekbar.a
                val bb = b ?: activity.modePanel.seekbar.b
                val rest = 1 - bb + aa
                it.a = if (rest > 0) aa / rest else 0f
                it.b = it.a
                if (activity.modePanel.switchHorizon.isSelected) {
                    it.dx = (bb - aa) * it.width; it.dy = 0f
                } else {
                    it.dy = (bb - aa) * it.height; it.dx = 0f
                }
            } else {
                numberHandlers[activity.selectIndex]?.apply(it, a, b, relative, it.width, it.height)
            }
        }
    }
}
