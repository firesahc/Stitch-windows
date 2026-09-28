package soko.ekibun.stitch.domain

import soko.ekibun.stitch.Stitch
import kotlin.math.cos
import kotlin.math.sin

/**
 * 参数映射唯一源。
 *
 * 原先同一映射散落在三处：
 * - StitchModePanel.SeekbarHandler（模型均值 -> slider[0,1]）
 * - EditorService.NumberLabelHandler（number/slider -> 模型）
 * - NumberEditPanel.updateNumber（模型均值 -> number 文本）
 * - EditorView.onMouseDragged（拖拽增量 -> dx/dy）
 *
 * 本对象收敛 model <-> slider <-> number 三向换算 + 拖拽/TILE 换算，
 * UI 与 Service 均委托至此，消除重复与分叉。
 */
object ParamMapper {

    // ---------- slider -> model ----------

    /** slider[0,1] 相对值写入模型（seekbar 拖动路径，relative=true）。 */
    fun applyRelative(info: Stitch.StitchInfo, label: String, a: Float?, b: Float?) {
        when (label) {
            StitchLabels.labelDx -> if (a != null) info.dx = (a * 2 - 1) * info.width
            StitchLabels.labelDy -> if (a != null) info.dy = (a * 2 - 1) * info.height
            StitchLabels.labelTrim -> { if (a != null) info.a = a; if (b != null) info.b = b }
            StitchLabels.labelXrange -> { if (a != null) info.xa = a; if (b != null) info.xb = b }
            StitchLabels.labelYrange -> { if (a != null) info.ya = a; if (b != null) info.yb = b }
            StitchLabels.labelScale -> if (a != null) info.dscale = a * 2
            StitchLabels.labelRotate -> if (a != null) info.drot = (a * 2 - 1) * 180
        }
    }

    /** number 绝对值写入模型（数字框路径，relative=false）。 */
    fun applyAbsolute(info: Stitch.StitchInfo, label: String, a: Float?, b: Float?) {
        when (label) {
            StitchLabels.labelDx -> if (a != null) info.dx = a
            StitchLabels.labelDy -> if (a != null) info.dy = a
            StitchLabels.labelTrim -> { if (a != null) info.a = a; if (b != null) info.b = b }
            StitchLabels.labelXrange -> {
                if (a != null && info.width > 0) info.xa = a / info.width
                if (b != null && info.width > 0) info.xb = b / info.width
            }
            StitchLabels.labelYrange -> {
                if (a != null && info.height > 0) info.ya = a / info.height
                if (b != null && info.height > 0) info.yb = b / info.height
            }
            StitchLabels.labelScale -> if (a != null) info.dscale = a
            StitchLabels.labelRotate -> if (a != null) info.drot = a
        }
    }

    /** TILE 模式 slider(a,b)+方向写入模型。 */
    fun applyTile(info: Stitch.StitchInfo, a: Float, b: Float, horizontal: Boolean) {
        val rest = 1 - b + a
        info.a = if (rest > 0) a / rest else 0f
        info.b = info.a
        if (horizontal) {
            info.dx = (b - a) * info.width
            info.dy = 0f
        } else {
            info.dy = (b - a) * info.height
            info.dx = 0f
        }
    }

    /** 画布拖拽增量换算为 dx/dy（原 EditorView.onMouseDragged 内联数学）。 */
    fun applyDrag(info: Stitch.StitchInfo, ddx: Float, ddy: Float) {
        val rad = (info.rot - info.drot) * Math.PI / 180
        val cos = cos(rad).toFloat()
        val sin = sin(rad).toFloat()
        val s = if (info.dscale == 0f) 0f else info.scale / info.dscale
        if (s == 0f) return
        info.dx += (ddx * cos + ddy * sin) / s
        info.dy += (-ddx * sin + ddy * cos) / s
    }

    // ---------- model -> slider[0,1] ----------

    /** 选中集均值映射到 slider，返回 Triple(a, b, sliderType)。type: 0=RANGE 1=GRADIENT 2=CENTER。 */
    fun toSlider(type: StitchType, label: String, infos: List<Stitch.StitchInfo>, horizontal: Boolean): Triple<Float, Float, Int> {
        if (infos.isEmpty()) return Triple(0f, 1f, 0)
        if (type == StitchType.TILE) {
            val (a, b) = if (horizontal) {
                infos.map { (1 - it.dx / it.width) * it.a }.average().toFloat() to
                    infos.map { it.a + (1 - it.a) * (it.dx / it.width) }.average().toFloat()
            } else {
                infos.map { (1 - it.dy / it.height) * it.a }.average().toFloat() to
                    infos.map { it.a + (1 - it.a) * (it.dy / it.height) }.average().toFloat()
            }
            return Triple(a, b, 0)
        }
        return when (label) {
            StitchLabels.labelDx -> Triple(infos.map { (it.dx / it.width + 1) / 2 }.average().toFloat(), 1f, 2)
            StitchLabels.labelDy -> Triple(infos.map { (it.dy / it.height + 1) / 2 }.average().toFloat(), 1f, 2)
            StitchLabels.labelTrim -> Triple(
                infos.map { it.a }.average().toFloat(),
                infos.map { it.b }.average().toFloat(), 1
            )
            StitchLabels.labelXrange -> Triple(
                infos.map { it.xa }.average().toFloat(),
                infos.map { it.xb }.average().toFloat(), 0
            )
            StitchLabels.labelYrange -> Triple(
                infos.map { it.ya }.average().toFloat(),
                infos.map { it.yb }.average().toFloat(), 0
            )
            StitchLabels.labelScale -> Triple(infos.map { it.dscale / 2f }.average().toFloat(), 1f, 2)
            StitchLabels.labelRotate -> Triple(infos.map { (it.drot / 360) + 0.5f }.average().toFloat(), 1f, 2)
            else -> Triple(0f, 1f, 0)
        }
    }

    // ---------- model -> number ----------

    /** 选中集均值映射到数字框，返回 Pair(a, b?)，b 为 null 表示单值。 */
    fun toNumber(type: StitchType, label: String, infos: List<Stitch.StitchInfo>, horizontal: Boolean): Pair<Float, Float?> {
        if (type == StitchType.TILE) {
            return if (horizontal) {
                infos.map { (1 - it.dx / it.width) * it.a }.average().toFloat() to
                    infos.map { it.a + (1 - it.a) * (it.dx / it.width) }.average().toFloat()
            } else {
                infos.map { (1 - it.dy / it.height) * it.a }.average().toFloat() to
                    infos.map { it.a + (1 - it.a) * (it.dy / it.height) }.average().toFloat()
            }
        }
        return when (label) {
            StitchLabels.labelDx -> infos.map { it.dx }.average().toFloat() to null
            StitchLabels.labelDy -> infos.map { it.dy }.average().toFloat() to null
            StitchLabels.labelTrim -> infos.map { it.a }.average().toFloat() to infos.map { it.b }.average().toFloat()
            StitchLabels.labelXrange -> infos.map { it.xa * it.width }.average().toFloat() to infos.map { it.xb * it.width }.average().toFloat()
            StitchLabels.labelYrange -> infos.map { it.ya * it.height }.average().toFloat() to infos.map { it.yb * it.height }.average().toFloat()
            StitchLabels.labelScale -> infos.map { it.dscale }.average().toFloat() to null
            StitchLabels.labelRotate -> infos.map { it.drot }.average().toFloat() to null
            else -> 0f to null
        }
    }
}
