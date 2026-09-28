package soko.ekibun.stitch.ui

import soko.ekibun.stitch.interfaces.Dialogs
import soko.ekibun.stitch.util.Strings
import java.io.File
import javax.swing.JFileChooser
import javax.swing.JOptionPane
import javax.swing.filechooser.FileNameExtensionFilter

/** Swing 实现的对话框/文件选择，线上默认使用；单测可 mock Dialogs。 */
class SwingDialogs : Dialogs {
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

    override fun pickOpenImages(): List<File>? {
        val chooser = JFileChooser()
        chooser.dialogTitle = Strings.get("dialog.selectImage")
        chooser.fileFilter = FileNameExtensionFilter(Strings.get("dialog.imageFiles"), "png", "jpg", "jpeg", "bmp")
        chooser.isMultiSelectionEnabled = true
        return if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFiles.toList() else null
    }

    override fun pickSaveFile(suggestedName: String): File? {
        val chooser = JFileChooser()
        chooser.dialogTitle = Strings.get("dialog.saveImage")
        chooser.fileFilter = FileNameExtensionFilter(Strings.get("dialog.pngImage"), "png")
        chooser.selectedFile = File(suggestedName)
        return if (chooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) chooser.selectedFile else null
    }
}
