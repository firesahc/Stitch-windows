package soko.ekibun.stitch.ui

import soko.ekibun.stitch.AppContext
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.domain.StitchLabels
import soko.ekibun.stitch.domain.StitchType
import soko.ekibun.stitch.interfaces.IEditorActivity
import soko.ekibun.stitch.util.GraphicsHelper
import soko.ekibun.stitch.util.Strings
import java.awt.*
import java.awt.event.*
import javax.swing.*

class EditActivity : IEditorActivity {

    companion object {
        fun open(appContext: AppContext, owner: JFrame, projectKey: String) {
            SwingUtilities.invokeLater {
                val editor = EditActivity(appContext, projectKey)
                editor.show()
            }
        }
    }

    private val appContext: AppContext
    private val projectKey: String
    val project: Stitch.StitchProject

    private lateinit var editorService: EditorService
    override val selectPanel: EditorSelectPanel

    override lateinit var editView: EditorView
    override lateinit var modePanel: StitchModePanel
    lateinit var numberEditPanel: NumberEditPanel
    private lateinit var undoBtn: JButton
    private lateinit var redoBtn: JButton

    override val progressLabel: JLabel get() = modePanel.progressLabel
    override val progressRow: JPanel get() = modePanel.progressRow
    override val progressBar: JProgressBar get() = modePanel.progressBar

    override var stitchType = StitchType.AUTO

    val selectItems = mapOf(
        StitchLabels.labelDx to (0 to false),
        StitchLabels.labelDy to (0 to false),
        StitchLabels.labelTrim to (2 to true),
        StitchLabels.labelXrange to (0 to true),
        StitchLabels.labelYrange to (0 to true),
        StitchLabels.labelScale to (2 to false),
        StitchLabels.labelRotate to (0 to false)
    )
    override var selectIndex = StitchLabels.labelDx

    /** 唯一事务入口：所有参数变更经此做 updateUndo + 扇出，避免三入口各自拼装。 */
    private fun commit(tag: Any?, save: Boolean = true, block: () -> Unit) {
        project.updateUndo(tag, save) { block() }
        updateSelectInfo()
    }

    constructor(appContext: AppContext, projectKey: String) {
        this.appContext = appContext
        this.projectKey = projectKey
        this.project = appContext.projectManager.getProject(projectKey)
        this.editorService = EditorService(appContext, projectKey, this as IEditorActivity)
        this.selectPanel = EditorSelectPanel(
            project = project,
            stitchType = { stitchType },
            selectIndex = { selectIndex },
            onSwap = { editorService.swapSelected() },
            onRemove = { editorService.removeSelected() },
            onSelectionChanged = { updateSelectInfo() }
        )
    }

    fun show() {
        val frame = JFrame(Strings.get("edit.title"))
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.minimumSize = Dimension(800, 600)
        frame.setSize(1000, 700)
        frame.setLocationRelativeTo(null)

        frame.setIconImage(GraphicsHelper.createAppIcon())

        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosed(e: WindowEvent) {
                editorService.cancel()
            }
        })

        editView = EditorView(this)
        frame.add(editView, BorderLayout.CENTER)

        modePanel = StitchModePanel(
            selectItems = selectItems,
            onStitch = { fullTransform, edgeEnhance -> editorService.stitch(fullTransform, edgeEnhance) },
            onTabChanged = { type: StitchType -> stitchType = type; updateSelectInfo() },
            onSeekbarChange = { a, b ->
                // TILE 显式传入 slider+方向；非 TILE 走 relative 路径，Service 不再直读 modePanel
                // 拖动中只改内存，松手再落盘，避免高频全量序列化
                if (stitchType == StitchType.TILE) {
                    commit(modePanel.seekbar, save = false) {
                        editorService.setNumber(a, b, true, tileSlider = a to b, horizontal = modePanel.switchHorizon.isSelected)
                    }
                } else {
                    commit(modePanel.seekbar, save = false) { editorService.setNumber(a, b, true) }
                }
                updateNumber()
                editView.update()
            },
            onSave = { editorService.saveImage() },
            onSeekbarTouchUp = { project.clearUndoTag(); project.save() },
            onDropdownChanged = { newVal -> selectIndex = newVal; updateSelectInfo() }
        )

        numberEditPanel = NumberEditPanel(
            stitchType = { stitchType },
            selectIndex = { selectIndex },
            selectItems = { selectItems },
            switchHorizon = { modePanel.switchHorizon.isSelected },
            onNumberChanged = { a, b ->
                commit(numberEditPanel) { editorService.setNumber(a, b) }
            },
            onSeekbarUpdate = {
                modePanel.updateSeekbar(stitchType, selectIndex, selectPanel.selectedStitchInfo, modePanel.switchHorizon.isSelected)
            },
            onEditViewUpdate = { editView.update() }
        )
        modePanel.addNumberView(numberEditPanel.numberView)
        numberEditPanel.numberA.addActionListener { modePanel.seekbar.requestFocusInWindow() }
        numberEditPanel.numberB.addActionListener { modePanel.seekbar.requestFocusInWindow() }

        val bottomPanel = createBottomPanel()
        val topBar = createTopBar()
        frame.add(bottomPanel, BorderLayout.SOUTH)
        frame.add(topBar, BorderLayout.NORTH)

        val dropHandler = ImageDropHandler { files -> editorService.addImages(files) }
        installDropHandler(frame.rootPane, dropHandler)

        ShortcutManager(frame.rootPane, createShortcutActions())

        selectPanel.selectAll()
        frame.isVisible = true
        SwingUtilities.invokeLater { editView.update() }
    }

    private fun createShortcutActions(): Map<String, () -> Unit> = mapOf(
        "undo" to { project.undo(); updateSelectInfo() },
        "redo" to { project.redo(); updateSelectInfo() },
        "selectAll" to { selectPanel.selectAll() },
        "selectClear" to { selectPanel.selectClear() },
        "save" to { editorService.saveImage() },
        "stitch" to { editorService.stitch(modePanel.radioTransformFull.isSelected, modePanel.checkEdgeEnhance.isSelected) },
        "delete" to { editorService.removeSelected() },
        "selHandleB" to {
            if (!modePanel.panelSeekbar.isVisible) return@to
            val showB = stitchType != StitchType.AUTO && (
                stitchType == StitchType.TILE || (selectItems[selectIndex]?.second == true))
            if (!showB) return@to
            numberEditPanel.selectedHandle = 1
            numberEditPanel.updateNumberView()
        },
        "selHandleA" to {
            if (!modePanel.panelSeekbar.isVisible) return@to
            numberEditPanel.selectedHandle = 0
            numberEditPanel.updateNumberView()
        },
        "decValue" to {
            if (!modePanel.panelSeekbar.isVisible) return@to
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner is JTextField) return@to
            val rounding = if (stitchType == StitchType.TILE) 2
                else (selectItems[selectIndex]?.first ?: 0)
            val step = Math.pow(10.0, -rounding.toDouble()).toFloat()
            if (numberEditPanel.selectedHandle == 0) {
                val num = (numberEditPanel.numberA.text.toFloatOrNull() ?: 0f) - step
                project.updateUndo("decValue") { editorService.setNumber(num) }
                numberEditPanel.updateNumberView(num, null)
            } else {
                val num = (numberEditPanel.numberB.text.toFloatOrNull() ?: 0f) - step
                project.updateUndo("decValue") { editorService.setNumber(null, num) }
                numberEditPanel.updateNumberView(null, num)
            }
            modePanel.updateSeekbar(stitchType, selectIndex, selectPanel.selectedStitchInfo, modePanel.switchHorizon.isSelected)
            editView.update()
        },
        "incValue" to {
            if (!modePanel.panelSeekbar.isVisible) return@to
            if (KeyboardFocusManager.getCurrentKeyboardFocusManager().focusOwner is JTextField) return@to
            val rounding = if (stitchType == StitchType.TILE) 2
                else (selectItems[selectIndex]?.first ?: 0)
            val step = Math.pow(10.0, -rounding.toDouble()).toFloat()
            if (numberEditPanel.selectedHandle == 0) {
                val num = (numberEditPanel.numberA.text.toFloatOrNull() ?: 0f) + step
                project.updateUndo("incValue") { editorService.setNumber(num) }
                numberEditPanel.updateNumberView(num, null)
            } else {
                val num = (numberEditPanel.numberB.text.toFloatOrNull() ?: 0f) + step
                project.updateUndo("incValue") { editorService.setNumber(null, num) }
                numberEditPanel.updateNumberView(null, num)
            }
            modePanel.updateSeekbar(stitchType, selectIndex, selectPanel.selectedStitchInfo, modePanel.switchHorizon.isSelected)
            editView.update()
        }
    )

    private fun installDropHandler(comp: Component, handler: ImageDropHandler) {
        // 保留文本框默认传输行为，其余容器/按钮统一接受文件拖放
        if (comp is JTextField) return
        if (comp is JComponent && comp.transferHandler == null) {
            comp.transferHandler = handler
        }
        if (comp is Container) {
            comp.components.forEach { installDropHandler(it, handler) }
        }
    }

    private fun createTopBar(): JPanel {
        undoBtn = JButton(Strings.get("edit.undo"))
        undoBtn.addActionListener { project.undo(); updateSelectInfo() }

        redoBtn = JButton(Strings.get("edit.redo"))
        redoBtn.addActionListener { project.redo(); updateSelectInfo() }

        val importBtn = JButton(Strings.get("edit.addImage"))
        importBtn.addActionListener {
            val files = appContext.dialogs.pickOpenImages(SwingUtilities.getWindowAncestor(importBtn)) ?: return@addActionListener
            editorService.addImages(files)
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(undoBtn); add(redoBtn); add(importBtn)
            background = Color(220, 220, 220)
        }
    }

    private fun createBottomPanel(): JPanel {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        return JPanel(GridBagLayout()).apply {
            gbc.gridy = 0; add(selectPanel.createSelectRow(), gbc)
            gbc.gridy = 1; add(modePanel.tabPane, gbc)
            gbc.gridy = 2; add(modePanel.panelAuto, gbc)
            gbc.gridy = 3; add(modePanel.panelSeekbar, gbc)
            gbc.gridy = 4; add(modePanel.progressRow, gbc)
            gbc.gridy = 5; add(modePanel.createActionRow(), gbc)
            background = Color(235, 235, 235)
        }
    }

    override fun updateSelectInfo() {
        numberEditPanel.selectedHandle = 0
        editView.update()
        selectPanel.updateSelectInfo()
        modePanel.updateTab(stitchType)
        modePanel.updateSeekbar(stitchType, selectIndex, selectPanel.selectedStitchInfo, modePanel.switchHorizon.isSelected)
        numberEditPanel.updateNumber(selectPanel.selectedStitchInfo)
        if (::undoBtn.isInitialized) {
            undoBtn.isEnabled = project.canUndo()
            redoBtn.isEnabled = project.canRedo()
        }
    }

    fun updateNumber() {
        numberEditPanel.updateNumber(selectPanel.selectedStitchInfo)
    }
}
