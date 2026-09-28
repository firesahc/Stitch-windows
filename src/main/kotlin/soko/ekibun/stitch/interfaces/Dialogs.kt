package soko.ekibun.stitch.interfaces

import java.awt.Component
import java.io.File

/**
 * 对话框/文件选择抽象。
 * EditorService 经此与具体 UI 解耦，便于单测；线上实现为 NativeFileDialogs（见 ui）。
 */
interface Dialogs {
    fun warn(message: String, title: String)
    fun error(message: String, title: String)
    fun info(message: String, title: String)
    fun confirm(message: String, title: String): Boolean
    fun pickOpenImages(owner: Component? = null): List<File>?
    fun pickSaveFile(suggestedName: String, owner: Component? = null): File?
}
