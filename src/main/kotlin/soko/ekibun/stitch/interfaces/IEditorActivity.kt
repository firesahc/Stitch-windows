package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.domain.StitchLabels
import soko.ekibun.stitch.domain.StitchType as DomainStitchType
import soko.ekibun.stitch.ui.EditorSelectPanel
import soko.ekibun.stitch.ui.EditorView
import soko.ekibun.stitch.ui.StitchModePanel

/**
 * 编辑器宿主旧契约（过渡期保留）。
 * 新代码请分别依赖 [EditorState] + [EditorProgress] 最小接口；
 * 本接口仅为兼容存量 EditActivity/EditorService/EditorView 而保留的组合。
 */
interface IEditorActivity : EditorState, EditorProgress {
    val editView: EditorView
    val selectPanel: EditorSelectPanel
    override var stitchType: DomainStitchType
    override var selectIndex: String
    val modePanel: StitchModePanel

    @Deprecated("迁移至 domain.StitchType", ReplaceWith("DomainStitchType", "soko.ekibun.stitch.domain.StitchType"))
    enum class StitchType(val label: String) {
        AUTO("自动"), TILE("平铺"), MAN("手动")
    }

    companion object {
        const val labelDx = StitchLabels.labelDx
        const val labelDy = StitchLabels.labelDy
        const val labelTrim = StitchLabels.labelTrim
        const val labelXrange = StitchLabels.labelXrange
        const val labelYrange = StitchLabels.labelYrange
        const val labelScale = StitchLabels.labelScale
        const val labelRotate = StitchLabels.labelRotate
    }
}
