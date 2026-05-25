package soko.ekibun.stitch.ui

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.interfaces.IEditorActivity
import soko.ekibun.stitch.interfaces.IEditorActivity.StitchType
import soko.ekibun.stitch.util.Strings
import java.awt.FlowLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel

class EditorSelectPanel(
    private val project: Stitch.StitchProject,
    private val stitchType: () -> StitchType,
    private val selectIndex: () -> String,
    private val onSwap: () -> Unit,
    private val onRemove: () -> Unit,
    private val onSelectionChanged: (() -> Unit)?
) {
    val selectInfo: JLabel = JLabel(Strings.get("edit.selected", 0, 0))

    val selectedStitchInfo: List<Stitch.StitchInfo>
        get() = when {
            stitchType() == StitchType.MAN && selectIndex() in listOf(IEditorActivity.labelDx, IEditorActivity.labelDy, IEditorActivity.labelTrim) ->
                project.stitchInfo.filterIndexed { i, v ->
                    i > 0 && project.isSelected(v.imageKey)
                }
            else -> project.getSelectedInfos()
        }

    fun createSelectRow(): JPanel {
        val selectAllBtn = JButton(Strings.get("edit.selectAll")).apply { addActionListener { selectAll() } }
        val selectClearBtn = JButton(Strings.get("edit.deselect")).apply { addActionListener { selectClear() } }
        val swapBtn = JButton(Strings.get("edit.swap")).apply { addActionListener { onSwap() } }
        val removeBtn = JButton(Strings.get("edit.delete")).apply { addActionListener { onRemove() } }
        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(selectInfo); add(selectAllBtn); add(selectClearBtn); add(swapBtn); add(removeBtn)
        }
    }

    fun selectToggle(info: Stitch.StitchInfo) {
        if (!project.selected.remove(info.imageKey)) project.selected.add(info.imageKey)
        updateSelectInfo()
        onSelectionChanged?.invoke()
    }

    fun selectAll() {
        project.selected.clear()
        project.selected.addAll(project.stitchInfo.map { it.imageKey })
        updateSelectInfo()
        onSelectionChanged?.invoke()
    }

    fun selectClear() {
        project.selected.clear()
        updateSelectInfo()
        onSelectionChanged?.invoke()
    }

    fun updateSelectInfo() {
        selectInfo.text = Strings.get("edit.selected", project.selected.size, project.stitchInfo.size)
    }
}
