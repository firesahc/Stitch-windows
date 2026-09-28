package soko.ekibun.stitch.interfaces

import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

/** 编辑器进度最小契约：Service 经此回调，不再直持具体 Panel。 */
interface EditorProgress {
    val progressLabel: JLabel
    val progressRow: JPanel
    val progressBar: JProgressBar
    fun updateSelectInfo()
}
