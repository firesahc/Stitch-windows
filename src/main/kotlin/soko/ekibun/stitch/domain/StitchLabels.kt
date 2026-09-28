package soko.ekibun.stitch.domain

/** 编辑器拼接模式。原位于 IEditorActivity.StitchType，迁移至领域层以解除 interfaces->ui 反向依赖。 */
enum class StitchType(val label: String) {
    AUTO("自动"),
    TILE("平铺"),
    MAN("手动")
}

/** 参数标签常量。原位于 IEditorActivity.Companion，迁移至此统一国际化前的稳定 key。 */
object StitchLabels {
    const val labelDx = "水平偏移"
    const val labelDy = "垂直偏移"
    const val labelTrim = "过渡"
    const val labelXrange = "水平范围"
    const val labelYrange = "垂直范围"
    const val labelScale = "缩放"
    const val labelRotate = "旋转"
}
