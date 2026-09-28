package soko.ekibun.stitch.interfaces

import java.io.File

/**
 * 对话框/文件选择抽象。
 * EditorService 经此与 Swing 解耦，便于单测；线上实现为 SwingDialogs（见 ui）。
 */
interface Dialogs {
    fun warn(message: String, title: String)
    fun error(message: String, title: String)
    fun info(message: String, title: String)
    fun confirm(message: String, title: String): Boolean
    fun pickOpenImages(): List<File>?
    fun pickSaveFile(suggestedName: String): File?
}
