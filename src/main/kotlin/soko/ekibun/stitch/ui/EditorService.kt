package soko.ekibun.stitch.ui

import soko.ekibun.stitch.App
import soko.ekibun.stitch.ProjectManager
import soko.ekibun.stitch.Stitch
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*

class EditorService(
    private val projectKey: String,
    private val activity: EditActivity,
) {
    val project: Stitch.StitchProject
        get() = ProjectManager.getProject(projectKey)

    fun stitch(homo: Boolean, diff: Boolean) {
        if (project.selected.isEmpty()) {
            JOptionPane.showMessageDialog(null, "未选择图片", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        val total = project.selected.size
        activity.progressLabel.text = "计算中 (0/$total)..."
        activity.progressBar.value = 0
        activity.progressBar.maximum = total
        activity.progressRow.isVisible = true

        Thread {
            var done = 0
            project.updateUndo {
                project.stitchInfo.reduceOrNull { acc, it ->
                    if (project.selected.contains(it.imageKey)) {
                        Stitch.combine(homo, diff, acc, it)?.let { data ->
                            it.dx = data.dx; it.dy = data.dy
                            it.drot = data.drot; it.dscale = data.dscale
                        }
                        done++
                        val finalDone = done
                        SwingUtilities.invokeLater {
                            activity.progressLabel.text = "计算中 ($finalDone/$total)..."
                            activity.progressBar.value = finalDone
                        }
                    }
                    it
                }
            }
            SwingUtilities.invokeLater {
                activity.progressRow.isVisible = false
                activity.updateSelectInfo()
            }
        }.apply { isDaemon = true }.start()
    }

    fun swapSelected() {
        if (project.selected.size < 2) {
            JOptionPane.showMessageDialog(null, "请选择至少两张图片进行交换", "警告", JOptionPane.WARNING_MESSAGE)
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
            JOptionPane.showMessageDialog(null, "未选择图片", "警告", JOptionPane.WARNING_MESSAGE)
            return false
        }
        val result = JOptionPane.showConfirmDialog(null,
            "确定要删除已选中的 ${project.selected.size} 张图片吗？",
            "确认删除", JOptionPane.OK_CANCEL_OPTION)
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
            JOptionPane.showMessageDialog(null, "请先添加图片", "警告", JOptionPane.WARNING_MESSAGE)
            return false
        }
        val chooser = JFileChooser()
        chooser.dialogTitle = "保存图片"
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("PNG 图片", "png")
        chooser.selectedFile = File("Stitch$projectKey.png")
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return false

        try {
            val file = chooser.selectedFile
            val image = activity.editView.drawToBitmap()
            ImageIO.write(image, "png", file)
            JOptionPane.showMessageDialog(null, "图片已保存至 ${file.absolutePath}", "保存成功", JOptionPane.INFORMATION_MESSAGE)
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "保存失败：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            return false
        }
    }

    fun addImage(file: File): Boolean {
        try {
            val bufferedImage = ImageIO.read(file)
            if (bufferedImage == null) {
                JOptionPane.showMessageDialog(null, "无法读取图片：${file.name}", "错误", JOptionPane.ERROR_MESSAGE)
                return false
            }
            val key = App.bitmapCache.saveBitmap(projectKey, bufferedImage)
            val info = Stitch.StitchInfo(key, bufferedImage.width, bufferedImage.height)
            project.updateUndo { project.stitchInfo.add(info); project.selected.add(info.imageKey) }
            activity.updateSelectInfo()
            return true
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "加载图片失败：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            return false
        }
    }

    fun setNumber(a: Float? = null, b: Float? = null, relative: Boolean = false) {
        val selected = activity.selectedStitchInfo
        if (selected.isNotEmpty()) selected.forEach {
            if (activity.stitchType == EditActivity.StitchType.TILE) {
                val aa = a ?: activity.seekbar.a
                val bb = b ?: activity.seekbar.b
                val rest = 1 - bb + aa
                it.a = if (rest > 0) aa / rest else 0f
                it.b = it.a
                if (activity.switchHorizon.isSelected) {
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
