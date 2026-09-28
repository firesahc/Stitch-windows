package soko.ekibun.stitch.ui

import com.formdev.flatlaf.util.SystemFileChooser
import soko.ekibun.stitch.interfaces.Dialogs
import soko.ekibun.stitch.util.Strings
import java.awt.Component
import java.io.File
import javax.swing.JOptionPane

/**
 * 系统原生文件对话框（FlatLaf SystemFileChooser，Windows 下为 IFileOpenDialog/IFileSaveDialog）。
 * 视图切换/搜索/预览窗格/目录记忆/保存覆盖确认均为系统级。
 */
class NativeFileDialogs : Dialogs {
    private var lastDirectory: File? = null

    override fun warn(message: String, title: String) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.WARNING_MESSAGE)
    }

    override fun error(message: String, title: String) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
    }

    override fun info(message: String, title: String) {
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.INFORMATION_MESSAGE)
    }

    override fun confirm(message: String, title: String): Boolean {
        return JOptionPane.showConfirmDialog(null, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION
    }

    override fun pickOpenImages(owner: Component?): List<File>? {
        val chooser = SystemFileChooser()
        chooser.dialogTitle = Strings.get("dialog.selectImage")
        chooser.fileFilter = SystemFileChooser.PatternFilter(
            Strings.get("dialog.imageFiles"),
            *EditorService.SUPPORTED_EXTENSIONS.map { "*.$it" }.toTypedArray()
        )
        chooser.isMultiSelectionEnabled = true
        lastDirectory?.let { chooser.currentDirectory = it }
        if (chooser.showOpenDialog(owner) != SystemFileChooser.APPROVE_OPTION) return null
        lastDirectory = chooser.currentDirectory
        return chooser.selectedFiles.toList().sortedBy { it.name.lowercase() }
    }

    override fun pickSaveFile(suggestedName: String, owner: Component?): File? {
        val chooser = SystemFileChooser()
        chooser.dialogTitle = Strings.get("dialog.saveImage")
        chooser.fileFilter = SystemFileChooser.PatternFilter(Strings.get("dialog.pngImage"), "*.png")
        lastDirectory?.let { chooser.currentDirectory = it }
        chooser.selectedFile = File(lastDirectory, suggestedName)
        if (chooser.showSaveDialog(owner) != SystemFileChooser.APPROVE_OPTION) return null
        lastDirectory = chooser.currentDirectory
        var file = chooser.selectedFile ?: return null
        if (file.extension.isEmpty()) file = File(file.parent, "${file.name}.png")
        return file
    }
}
