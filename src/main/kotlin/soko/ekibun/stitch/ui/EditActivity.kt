package soko.ekibun.stitch.ui

import soko.ekibun.stitch.ProjectManager
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.GraphicsHelper
import soko.ekibun.stitch.util.PRIMARY_COLOR
import soko.ekibun.stitch.util.Strings
import java.awt.*
import java.awt.event.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class EditActivity {

    companion object {
        fun open(owner: JFrame, projectKey: String) {
            SwingUtilities.invokeLater {
                val editor = EditActivity(projectKey)
                editor.show()
            }
        }

        val labelDx = "水平偏移"
        val labelDy = "垂直偏移"
        val labelTrim = "过渡"
        val labelXrange = "水平范围"
        val labelYrange = "垂直范围"
        val labelScale = "缩放"
        val labelRotate = "旋转"
    }

    private val projectKey: String
    val project: Stitch.StitchProject

    private lateinit var editorService: EditorService

    lateinit var editView: EditorView
    private lateinit var selectInfo: JLabel
    lateinit var seekbar: RangeSlider
    private lateinit var numberView: JPanel
    private lateinit var numberA: JTextField
    private lateinit var numberB: JTextField
    private lateinit var numberDiv: JLabel
    private lateinit var numberDec: JButton
    private lateinit var numberInc: JButton
    private lateinit var dropdown: JComboBox<String>
    private lateinit var panelAuto: JPanel
    private lateinit var panelSeekbar: JPanel
    lateinit var switchHorizon: JCheckBox
    private lateinit var tabPane: JPanel
    private lateinit var tabviews: List<JLabel>
    lateinit var progressRow: JPanel
    lateinit var progressBar: JProgressBar
    lateinit var progressLabel: JLabel
    private var suppressNumberListener = false

    enum class StitchType(val label: String) {
        AUTO("自动"), TILE("平铺"), MAN("手动")
    }

    var stitchType = StitchType.AUTO

    val selectItems = mapOf(
        labelDx to (0 to false),
        labelDy to (0 to false),
        labelTrim to (2 to true),
        labelXrange to (0 to true),
        labelYrange to (0 to true),
        labelScale to (2 to false),
        labelRotate to (0 to false)
    )
    var selectIndex = labelDy

    constructor(projectKey: String) {
        this.projectKey = projectKey
        this.project = ProjectManager.getProject(projectKey)
        this.editorService = EditorService(projectKey, this)
    }

    fun show() {
        val frame = JFrame(Strings.get("edit.title"))
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.minimumSize = Dimension(800, 600)
        frame.setSize(1000, 700)
        frame.setLocationRelativeTo(null)

        frame.setIconImage(GraphicsHelper.createAppIcon())

        editView = EditorView(this)
        frame.add(editView, BorderLayout.CENTER)

        val bottomPanel = createBottomPanel()
        frame.add(bottomPanel, BorderLayout.SOUTH)

        val topBar = createTopBar()
        frame.add(topBar, BorderLayout.NORTH)

        val rootPane = frame.rootPane
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().menuShortcutKeyMask), "undo")
        rootPane.actionMap.put("undo", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) {
                project.undo(); updateSelectInfo()
            }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, Toolkit.getDefaultToolkit().menuShortcutKeyMask), "selectAll")
        rootPane.actionMap.put("selectAll", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { selectAll() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "selectClear")
        rootPane.actionMap.put("selectClear", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { selectClear() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().menuShortcutKeyMask), "save")
        rootPane.actionMap.put("save", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { editorService.saveImage() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().menuShortcutKeyMask), "stitch")
        rootPane.actionMap.put("stitch", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { editorService.stitch(true, true) }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete")
        rootPane.actionMap.put("delete", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { editorService.removeSelected() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete")

        selectAll()
        frame.isVisible = true
        // 窗口显示后重新适配屏幕，确保缩放基于正确的窗口尺寸
        SwingUtilities.invokeLater { editView.update() }
    }

    private fun createTopBar(): JPanel {
        val undoBtn = JButton(Strings.get("edit.undo"))
        undoBtn.addActionListener { project.undo(); updateSelectInfo() }

        val importBtn = JButton(Strings.get("edit.addImage"))
        importBtn.addActionListener {
            val chooser = JFileChooser()
            chooser.dialogTitle = Strings.get("dialog.selectImage")
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(Strings.get("dialog.imageFiles"), "png", "jpg", "jpeg", "bmp")
            chooser.isMultiSelectionEnabled = true
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.forEach { editorService.addImage(it) }
            }
        }

        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(undoBtn); add(importBtn)
            background = Color(220, 220, 220)
        }
    }

    private fun createBottomPanel(): JPanel {
        val gbc = GridBagConstraints()
        gbc.gridx = 0
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0
        return JPanel(GridBagLayout()).apply {
            gbc.gridy = 0; add(createSelectRow(), gbc)
            gbc.gridy = 1; add(createTabBar(), gbc)
            gbc.gridy = 2; add(createAutoPanel(), gbc)
            gbc.gridy = 3; add(createSeekbarPanel(), gbc)
            gbc.gridy = 4; add(createProgressRow(), gbc)
            gbc.gridy = 5; add(createActionRow(), gbc)
            background = Color(235, 235, 235)
        }
    }

    private fun createSelectRow(): JPanel {
        selectInfo = JLabel(Strings.get("edit.selected", 0, 0))
        val selectAllBtn = JButton(Strings.get("edit.selectAll")).apply { addActionListener { selectAll() } }
        val selectClearBtn = JButton(Strings.get("edit.deselect")).apply { addActionListener { selectClear() } }
        val swapBtn = JButton(Strings.get("edit.swap")).apply { addActionListener { editorService.swapSelected() } }
        val removeBtn = JButton(Strings.get("edit.delete")).apply { addActionListener { editorService.removeSelected() } }
        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(selectInfo); add(selectAllBtn); add(selectClearBtn); add(swapBtn); add(removeBtn)
        }
    }

    private fun createTabBar(): JPanel {
        tabPane = JPanel(FlowLayout(FlowLayout.LEFT, 15, 4))
        tabviews = StitchType.values().map { type ->
            JLabel(type.label).apply {
                border = javax.swing.border.EmptyBorder(4, 12, 4, 12)
                toolTipText = type.label
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        stitchType = type
                        updateSelectInfo()
                    }
                })
                tabPane.add(this)
            }
        }
        return tabPane
    }

    private fun createAutoPanel(): JPanel {
        val diffCb = JCheckBox(Strings.get("edit.diffMode")).apply { isSelected = true }
        val homoCb = JCheckBox(Strings.get("edit.featureMatch"))
        val stitchBtn = JButton(Strings.get("edit.stitch")).apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { editorService.stitch(homoCb.isSelected, diffCb.isSelected) }
        }
        panelAuto = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
            add(diffCb); add(homoCb); add(stitchBtn)
        }
        return panelAuto
    }

    private fun createSeekbarPanel(): JPanel {
        panelSeekbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            switchHorizon = JCheckBox(Strings.get("edit.horizon"))
            dropdown = JComboBox<String>().apply {
                selectItems.keys.forEach { addItem(it) }
                selectedItem = selectIndex
                preferredSize = Dimension(100, 25)
                addActionListener {
                    val newVal = selectedItem as? String
                    if (newVal != null && newVal != selectIndex) {
                        selectIndex = newVal
                        updateSelectInfo()
                    }
                }
            }
            seekbar = RangeSlider().apply {
                preferredSize = Dimension(200, 30)
            }
            seekbar.onRangeChange = { a, b ->
                project.updateUndo(seekbar) { editorService.setNumber(a, b, true) }
                updateNumber()
                editView.update()
            }
            seekbar.onTouchUp = { project.clearUndoTag() }

            numberView = JPanel()
            numberView.layout = BoxLayout(numberView, BoxLayout.Y_AXIS)
            numberA = JTextField("0", 6)
            numberB = JTextField("0", 6)
            numberDiv = JLabel("-")
            numberDec = JButton("<")
            numberInc = JButton(">")

            numberA.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) { onNumberAChanged() }
                override fun removeUpdate(e: DocumentEvent) { onNumberAChanged() }
                override fun changedUpdate(e: DocumentEvent) { onNumberAChanged() }
                private fun onNumberAChanged() {
                    if (suppressNumberListener) return
                    val num = numberA.text.toFloatOrNull() ?: return
                    project.updateUndo(numberA) { editorService.setNumber(num) }
                    seekbar.repaint()
                    editView.update()
                }
            })

            numberB.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent) { onNumberBChanged() }
                override fun removeUpdate(e: DocumentEvent) { onNumberBChanged() }
                override fun changedUpdate(e: DocumentEvent) { onNumberBChanged() }
                private fun onNumberBChanged() {
                    if (suppressNumberListener) return
                    val num = numberB.text.toFloatOrNull() ?: return
                    project.updateUndo(numberB) { editorService.setNumber(null, num) }
                    seekbar.repaint()
                    editView.update()
                }
            })

            numberDec.addActionListener {
                val num = (numberA.text.toFloatOrNull() ?: 0f) -
                    Math.pow(10.0, -(selectItems[selectIndex]?.first ?: 0).toDouble()).toFloat()
                project.updateUndo(this) { editorService.setNumber(num) }
                updateNumberView(num); seekbar.repaint(); editView.update()
            }
            numberInc.addActionListener {
                val num = (numberA.text.toFloatOrNull() ?: 0f) +
                    Math.pow(10.0, -(selectItems[selectIndex]?.first ?: 0).toDouble()).toFloat()
                project.updateUndo(this) { editorService.setNumber(num) }
                updateNumberView(num); seekbar.repaint(); editView.update()
            }

            val numRow = JPanel(FlowLayout(FlowLayout.CENTER, 4, 0))
            numRow.add(numberDec); numRow.add(numberA); numRow.add(numberDiv); numRow.add(numberB); numRow.add(numberInc)
            numberView.add(numRow)

            add(switchHorizon); add(dropdown); add(seekbar); add(numberView)
        }
        return panelSeekbar
    }

    private fun createActionRow(): JPanel {
        val saveBtn = JButton(Strings.get("edit.save")).apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { editorService.saveImage() }
        }
        val rightPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 0, 0)).apply {
            add(saveBtn)
            border = javax.swing.border.EmptyBorder(0, 0, 0, 20)
        }
        return JPanel(BorderLayout()).apply {
            add(JLabel(""), BorderLayout.CENTER)
            add(rightPanel, BorderLayout.EAST)
            border = javax.swing.border.EmptyBorder(0, 0, 8, 0)
        }
    }

    private fun createProgressRow(): JPanel {
        progressBar = JProgressBar()
        progressLabel = JLabel()
        progressRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
            add(progressLabel); add(progressBar)
            isVisible = false
        }
        return progressRow
    }

    fun updateSelectInfo() {
        editView.update()
        selectInfo.text = Strings.get("edit.selected", project.selected.size, project.stitchInfo.size)
        updateTab()
        updateSeekbar()
        updateNumber()
    }

    private fun updateTab() {
        tabviews.forEach { label ->
            val idx = tabviews.indexOf(label)
            val type = StitchType.values()[idx]
            label.foreground = if (type == stitchType) PRIMARY_COLOR else Color.BLACK
            label.font = if (type == stitchType) label.font.deriveFont(Font.BOLD) else label.font.deriveFont(Font.PLAIN)
        }

        panelAuto.isVisible = stitchType == StitchType.AUTO
        panelSeekbar.isVisible = stitchType != StitchType.AUTO
        switchHorizon.isVisible = stitchType == StitchType.TILE
        dropdown.isVisible = stitchType == StitchType.MAN
    }

    val selectedStitchInfo: List<Stitch.StitchInfo>
        get() = when {
            stitchType == StitchType.MAN && selectIndex in listOf(labelDx, labelDy, labelTrim) ->
                project.stitchInfo.filterIndexed { i, v ->
                    i > 0 && project.selected.contains(v.imageKey)
                }
            else -> project.stitchInfo.filter { project.selected.contains(it.imageKey) }
        }

    fun updateSeekbar() {
        val selected = selectedStitchInfo
        if (selected.isNotEmpty()) {
            seekbar.isEnabled = true
            when {
                stitchType == StitchType.TILE -> {
                    seekbar.type = RangeSlider.TYPE_RANGE
                    if (switchHorizon.isSelected) {
                        seekbar.a = selected.map { (1 - it.dx / it.width) * it.a }.average().toFloat()
                        seekbar.b = selected.map { it.a + (1 - it.a) * (it.dx / it.width) }.average().toFloat()
                    } else {
                        seekbar.a = selected.map { (1 - it.dy / it.height) * it.a }.average().toFloat()
                        seekbar.b = selected.map { it.a + (1 - it.a) * (it.dy / it.height) }.average().toFloat()
                    }
                }
                selectIndex == labelDx -> {
                    seekbar.type = RangeSlider.TYPE_CENTER
                    seekbar.a = selected.map { (it.dx / it.width + 1) / 2 }.average().toFloat()
                }
                selectIndex == labelDy -> {
                    seekbar.type = RangeSlider.TYPE_CENTER
                    seekbar.a = selected.map { (it.dy / it.height + 1) / 2 }.average().toFloat()
                }
                selectIndex == labelTrim -> {
                    seekbar.type = RangeSlider.TYPE_GRADIENT
                    seekbar.a = selected.map { it.a }.average().toFloat()
                    seekbar.b = selected.map { it.b }.average().toFloat()
                }
                selectIndex == labelXrange -> {
                    seekbar.type = RangeSlider.TYPE_RANGE
                    seekbar.a = selected.map { it.xa }.average().toFloat()
                    seekbar.b = selected.map { it.xb }.average().toFloat()
                }
                selectIndex == labelYrange -> {
                    seekbar.type = RangeSlider.TYPE_RANGE
                    seekbar.a = selected.map { it.ya }.average().toFloat()
                    seekbar.b = selected.map { it.yb }.average().toFloat()
                }
                selectIndex == labelScale -> {
                    seekbar.type = RangeSlider.TYPE_CENTER
                    seekbar.a = selected.map { it.dscale / 2f }.average().toFloat()
                }
                selectIndex == labelRotate -> {
                    seekbar.type = RangeSlider.TYPE_CENTER
                    seekbar.a = selected.map { (it.drot / 360) + 0.5f }.average().toFloat()
                }
            }
            seekbar.repaint()
        } else {
            seekbar.isEnabled = false
        }
    }

    fun updateNumber() {
        val selected = selectedStitchInfo
        if (selected.isNotEmpty()) {
            numberView.isVisible = true
            when {
                stitchType == StitchType.TILE -> {
                    if (switchHorizon.isSelected) {
                        updateNumberView(
                            selected.map { (1 - it.dx / it.width) * it.a }.average().toFloat(),
                            selected.map { it.a + (1 - it.a) * (it.dx / it.width) }.average().toFloat()
                        )
                    } else {
                        updateNumberView(
                            selected.map { (1 - it.dy / it.height) * it.a }.average().toFloat(),
                            selected.map { it.a + (1 - it.a) * (it.dy / it.height) }.average().toFloat()
                        )
                    }
                }
                selectIndex == labelDx -> updateNumberView(selected.map { it.dx }.average().toFloat())
                selectIndex == labelDy -> updateNumberView(selected.map { it.dy }.average().toFloat())
                selectIndex == labelTrim -> updateNumberView(
                    selected.map { it.a }.average().toFloat(),
                    selected.map { it.b }.average().toFloat()
                )
                selectIndex == labelXrange -> updateNumberView(
                    selected.map { it.xa * it.width }.average().toFloat(),
                    selected.map { it.xb * it.width }.average().toFloat()
                )
                selectIndex == labelYrange -> updateNumberView(
                    selected.map { it.ya * it.height }.average().toFloat(),
                    selected.map { it.yb * it.height }.average().toFloat()
                )
                selectIndex == labelScale -> updateNumberView(selected.map { it.dscale }.average().toFloat())
                selectIndex == labelRotate -> updateNumberView(selected.map { it.drot }.average().toFloat())
            }
        } else {
            numberView.isVisible = false
        }
    }

    private fun updateNumberView(a: Float? = null, b: Float? = null) {
        val (roundOf, showB) = if (stitchType == StitchType.MAN)
            selectItems[selectIndex] ?: (0 to false)
        else (2 to true)
        numberB.isVisible = showB
        numberDiv.isVisible = showB
        numberDec.isVisible = !showB
        numberInc.isVisible = !showB
        suppressNumberListener = true
        if (a != null) numberA.text = String.format("%.${roundOf}f", a)
        if (b != null) numberB.text = String.format("%.${roundOf}f", b)
        suppressNumberListener = false
    }

    fun selectToggle(info: Stitch.StitchInfo) {
        if (!project.selected.remove(info.imageKey)) project.selected.add(info.imageKey)
        updateSelectInfo()
    }

    private fun selectAll() {
        project.selected.clear()
        project.selected.addAll(project.stitchInfo.map { it.imageKey })
        updateSelectInfo()
    }

    private fun selectClear() {
        project.selected.clear()
        updateSelectInfo()
    }
}
