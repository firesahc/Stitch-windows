package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.domain.StitchType

/** 编辑器状态最小契约：仅 stitchType/selectIndex，唯一源在 EditActivity。 */
interface EditorState {
    var stitchType: StitchType
    var selectIndex: String
}
