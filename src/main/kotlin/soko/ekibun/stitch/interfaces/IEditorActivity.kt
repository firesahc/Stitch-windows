package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.ui.EditorSelectPanel
import soko.ekibun.stitch.ui.EditorView
import soko.ekibun.stitch.ui.StitchModePanel
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JProgressBar

interface IEditorActivity {
    val editView: EditorView
    val progressLabel: JLabel
    val progressRow: JPanel
    val progressBar: JProgressBar
    val selectPanel: EditorSelectPanel
    var stitchType: IEditorActivity.StitchType
    var selectIndex: String
    val modePanel: StitchModePanel
    fun updateSelectInfo()

    enum class StitchType(val label: String) {
        AUTO("\u81ea\u52a8"), TILE("\u5e73\u94fa"), MAN("\u624b\u52a8")
    }

    companion object {
        const val labelDx = "\u6c34\u5e73\u504f\u79fb"
        const val labelDy = "\u5782\u76f4\u504f\u79fb"
        const val labelTrim = "\u8fc7\u6e21"
        const val labelXrange = "\u6c34\u5e73\u8303\u56f4"
        const val labelYrange = "\u5782\u76f4\u8303\u56f4"
        const val labelScale = "\u7f29\u653e"
        const val labelRotate = "\u65cb\u8f6c"
    }
}
