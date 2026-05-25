package soko.ekibun.stitch.ui

import kotlinx.coroutines.*
import soko.ekibun.stitch.App
import soko.ekibun.stitch.ProjectManager
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.StitchNative
import soko.ekibun.stitch.interfaces.IStitchNative
import soko.ekibun.stitch.service.StitchService
import java.io.File
import javax.imageio.ImageIO
import soko.ekibun.stitch.util.Strings
import javax.swing.*

class EditorService(
    private val projectKey: String,
    private val activity: EditActivity,
) {
    val project: Stitch.StitchProject
        get() = ProjectManager.getProject(projectKey)

    private val scope = CoroutineScope(SupervisorJob() + App.dispatcherIO)

    private val stitchService = StitchService(object : IStitchNative {
        override fun computeOffset(
            img0: Stitch.StitchInfo, img1: Stitch.StitchInfo,
            fullTransform: Boolean, edgeEnhance: Boolean
        ) = StitchNative.computeOffset(img0, img1, fullTransform, edgeEnhance)
    })

    fun stitch(fullTransform: Boolean, edgeEnhance: Boolean) {
        if (project.selected.isEmpty()) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.noSelection"), Strings.get("common.warning"), JOptionPane.WARNING_MESSAGE)
            return
        }
        val total = project.selected.size
        activity.modePanel.progressLabel.text = Strings.get("editor.progress", 0, total)
        activity.modePanel.progressBar.value = 0
        activity.modePanel.progressBar.maximum = total
        activity.modePanel.progressRow.isVisible = true

        val failedIndices = mutableListOf<Int>()
        scope.launch {
            synchronized(project) {
                var done = 0
                project.updateUndo {
                    project.stitchInfo.reduceOrNull { acc, it ->
                        if (project.selected.contains(it.imageKey)) {
                            val result = stitchService.combine(fullTransform, edgeEnhance, acc, it)
                            if (result != null) {
                                it.dx = result.dx; it.dy = result.dy
                                it.drot = result.drot; it.dscale = result.dscale
                            } else {
                                val idx = project.stitchInfo.indexOf(it)
                                if (idx >= 0) failedIndices.add(idx)
                            }
                            done++
                            val finalDone = done
                            SwingUtilities.invokeLater {
                                activity.modePanel.progressLabel.text = Strings.get("editor.progress", finalDone, total)
                                activity.modePanel.progressBar.value = finalDone
                            }
                        }
                        it
                    }
                }
            }
            SwingUtilities.invokeLater {
                activity.modePanel.progressRow.isVisible = false
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
            val i = selected.last().let {
                project.stitchInfo.indexOfFirst { info -> info.imageKey == it }
            }
            if (i < 0) return@updateUndo
            var a = project.stitchInfo[i]
            val adx = a.dx; val ady = a.dy; val adr = a.drot; val ads = a.dscale
            for (indic in 0 until selected.size - 1) {
                val j = selected[indic].let {
                    project.stitchInfo.indexOfFirst { info -> info.imageKey == it }
                }
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
                project.stitchInfo.removeAll { project.selected.contains(it.imageKey) }
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
            val key = App.bitmapCache.saveBitmap(projectKey, bufferedImage)
            val info = Stitch.StitchInfo(key, bufferedImage.width, bufferedImage.height)
            project.updateUndo { project.stitchInfo.add(info); project.selected.add(info.imageKey) }
            activity.updateSelectInfo()
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, Strings.get("dialog.imageLoadError", e.message), Strings.get("common.error"), JOptionPane.ERROR_MESSAGE)
            return false
        }
    }

    fun setNumber(a: Float? = null, b: Float? = null, relative: Boolean = false) {
        val selected = activity.selectPanel.selectedStitchInfo
        if (selected.isNotEmpty()) selected.forEach {
            if (activity.stitchType == EditActivity.StitchType.TILE) {
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
            } else when (activity.selectIndex) {
                EditActivity.labelDx -> if (a != null) it.dx = if (relative) (a * 2 - 1) * it.width else a
                EditActivity.labelDy -> if (a != null) it.dy = if (relative) (a * 2 - 1) * it.height else a
                EditActivity.labelTrim -> { if (a != null) it.a = a; if (b != null) it.b = b }
                EditActivity.labelXrange -> {
                    if (a != null) it.xa = if (!relative && it.width > 0) a / it.width else a
                    if (b != null) it.xb = if (!relative && it.width > 0) b / it.width else b
                }
                EditActivity.labelYrange -> {
                    if (a != null) it.ya = if (!relative && it.height > 0) a / it.height else a
                    if (b != null) it.yb = if (!relative && it.height > 0) b / it.height else b
                }
                EditActivity.labelScale -> if (a != null) it.dscale = if (relative) (a * 2) else a
                EditActivity.labelRotate -> if (a != null) it.drot = if (relative) (a * 2 - 1) * 180 else a
            }
        }
    }
}
