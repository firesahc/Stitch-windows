package soko.ekibun.stitch.ui

import soko.ekibun.stitch.App
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.PRIMARY_COLOR
import java.awt.*
import java.awt.event.*
import java.io.File
import javax.imageio.ImageIO
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
    }

    private val projectKey: String
    val project: Stitch.StitchProject

    private lateinit var editView: EditorView
    private lateinit var selectInfo: JLabel
    private lateinit var seekbar: RangeSlider
    private lateinit var numberView: JPanel
    private lateinit var numberA: JTextField
    private lateinit var numberB: JTextField
    private lateinit var numberDiv: JLabel
    private lateinit var numberDec: JButton
    private lateinit var numberInc: JButton
    private lateinit var dropdown: JComboBox<String>
    private lateinit var panelAuto: JPanel
    private lateinit var panelSeekbar: JPanel
    private lateinit var switchHorizon: JCheckBox
    private lateinit var tabPane: JPanel
    private lateinit var tabviews: List<JLabel>
    private lateinit var progressRow: JPanel
    private lateinit var progressBar: JProgressBar
    private lateinit var progressLabel: JLabel
    private var suppressNumberListener = false

    enum class StitchType(val label: String) {
        AUTO("自动"), TILE("平铺"), MAN("手动")
    }

    private var stitchType = StitchType.AUTO

    private val labelDx = "水平偏移"
    private val labelDy = "垂直偏移"
    private val labelTrim = "过渡"
    private val labelXrange = "水平范围"
    private val labelYrange = "垂直范围"
    private val labelScale = "缩放"
    private val labelRotate = "旋转"

    private val selectItems = mapOf(
        labelDx to (0 to false),
        labelDy to (0 to false),
        labelTrim to (2 to true),
        labelXrange to (0 to true),
        labelYrange to (0 to true),
        labelScale to (2 to false),
        labelRotate to (0 to false)
    )
    private var selectIndex = labelDy

    constructor(projectKey: String) {
        this.projectKey = projectKey
        this.project = App.getProject(projectKey)
    }

    fun show() {
        val frame = JFrame("Stitch - 编辑")
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.minimumSize = Dimension(800, 600)
        frame.setSize(1000, 700)
        frame.setLocationRelativeTo(null)

        val icon = java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = icon.createGraphics()
        g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON)
        g.color = soko.ekibun.stitch.util.PRIMARY_COLOR
        g.fillRoundRect(2, 2, 28, 28, 8, 8)
        g.color = java.awt.Color.WHITE
        g.font = java.awt.Font("Microsoft YaHei", java.awt.Font.BOLD, 20)
        val fm = g.fontMetrics
        val sw = fm.stringWidth("S")
        g.drawString("S", (32 - sw) / 2, 24)
        g.dispose()
        frame.setIconImage(icon)

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
            override fun actionPerformed(e: java.awt.event.ActionEvent) { saveImage() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, Toolkit.getDefaultToolkit().menuShortcutKeyMask), "stitch")
        rootPane.actionMap.put("stitch", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { stitch(true, true) }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete")
        rootPane.actionMap.put("delete", object : AbstractAction() {
            override fun actionPerformed(e: java.awt.event.ActionEvent) { removeSelected() }
        })
        rootPane.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "delete")

        selectAll()
        frame.isVisible = true
        // 窗口显示后重新适配屏幕，确保缩放基于正确的窗口尺寸
        SwingUtilities.invokeLater { editView.update() }
    }

    private fun createTopBar(): JPanel {
        val undoBtn = JButton("撤销")
        undoBtn.addActionListener { project.undo(); updateSelectInfo() }

        val importBtn = JButton("添加图片")
        importBtn.addActionListener {
            val chooser = JFileChooser()
            chooser.dialogTitle = "选择图片"
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("图片文件", "png", "jpg", "jpeg", "bmp")
            chooser.isMultiSelectionEnabled = true
            val result = chooser.showOpenDialog(null)
            if (result == JFileChooser.APPROVE_OPTION) {
                chooser.selectedFiles.forEach { addImage(it) }
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
        selectInfo = JLabel("已选中 0/0")
        val selectAllBtn = JButton("全选").apply { addActionListener { selectAll() } }
        val selectClearBtn = JButton("取消选择").apply { addActionListener { selectClear() } }
        val swapBtn = JButton("交换").apply { addActionListener { swapSelected() } }
        val removeBtn = JButton("删除").apply { addActionListener { removeSelected() } }
        return JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            add(selectInfo); add(selectAllBtn); add(selectClearBtn); add(swapBtn); add(removeBtn)
        }
    }

    private fun createTabBar(): JPanel {
        tabPane = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        tabviews = StitchType.values().map { type ->
            JLabel(type.label).apply {
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
        val diffCb = JCheckBox("差异模式").apply { isSelected = true }
        val homoCb = JCheckBox("特征匹配")
        val stitchBtn = JButton("自动拼接").apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { stitch(homoCb.isSelected, diffCb.isSelected) }
        }
        panelAuto = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
            add(diffCb); add(homoCb); add(stitchBtn)
        }
        return panelAuto
    }

    private fun createSeekbarPanel(): JPanel {
        panelSeekbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            switchHorizon = JCheckBox("水平")
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
                project.updateUndo(seekbar) { setNumber(a, b, true) }
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
                    project.updateUndo(numberA) { setNumber(num) }
                    seekbar.draw()
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
                    project.updateUndo(numberB) { setNumber(null, num) }
                    seekbar.draw()
                    editView.update()
                }
            })

            numberDec.addActionListener {
                val num = (numberA.text.toFloatOrNull() ?: 0f) -
                    Math.pow(10.0, -(selectItems[selectIndex]?.first ?: 0).toDouble()).toFloat()
                project.updateUndo(this) { setNumber(num) }
                updateNumberView(num); seekbar.draw(); editView.update()
            }
            numberInc.addActionListener {
                val num = (numberA.text.toFloatOrNull() ?: 0f) +
                    Math.pow(10.0, -(selectItems[selectIndex]?.first ?: 0).toDouble()).toFloat()
                project.updateUndo(this) { setNumber(num) }
                updateNumberView(num); seekbar.draw(); editView.update()
            }

            val numRow = JPanel(FlowLayout(FlowLayout.CENTER, 4, 0))
            numRow.add(numberDec); numRow.add(numberA); numRow.add(numberDiv); numRow.add(numberB); numRow.add(numberInc)
            numberView.add(numRow)

            add(switchHorizon); add(dropdown); add(seekbar); add(numberView)
        }
        return panelSeekbar
    }

    private fun createActionRow(): JPanel {
        val saveBtn = JButton("保存").apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { saveImage() }
        }
        return JPanel(BorderLayout()).apply {
            add(JLabel(""), BorderLayout.CENTER)
            add(saveBtn, BorderLayout.EAST)
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
        selectInfo.text = "已选中 ${project.selected.size}/${project.stitchInfo.size}"
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

    private val selectedStitchInfo: List<Stitch.StitchInfo>
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
            seekbar.draw()
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

    fun setNumber(a: Float? = null, b: Float? = null, relative: Boolean = false) {
        val selected = selectedStitchInfo
        if (selected.isNotEmpty()) selected.forEach {
            if (stitchType == StitchType.TILE) {
                val aa = a ?: seekbar.a
                val bb = b ?: seekbar.b
                val rest = 1 - bb + aa
                it.a = if (rest > 0) aa / rest else 0f
                it.b = it.a
                if (switchHorizon.isSelected) {
                    it.dx = (bb - aa) * it.width
                    it.dy = 0f
                } else {
                    it.dy = (bb - aa) * it.height
                    it.dx = 0f
                }
            } else when (selectIndex) {
                labelDx -> if (a != null) it.dx = if (relative) (a * 2 - 1) * it.width else a
                labelDy -> if (a != null) it.dy = if (relative) (a * 2 - 1) * it.height else a
                labelTrim -> { if (a != null) it.a = a; if (b != null) it.b = b }
                labelXrange -> {
                    if (a != null) it.xa = if (!relative && it.width > 0) a / it.width else a
                    if (b != null) it.xb = if (!relative && it.width > 0) b / it.width else b
                }
                labelYrange -> {
                    if (a != null) it.ya = if (!relative && it.height > 0) a / it.height else a
                    if (b != null) it.yb = if (!relative && it.height > 0) b / it.height else b
                }
                labelScale -> if (a != null) it.dscale = if (relative) (a * 2) else a
                labelRotate -> if (a != null) it.drot = if (relative) (a * 2 - 1) * 180 else a
            }
        }
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

    private fun addImage(file: File) {
        try {
            val bufferedImage = ImageIO.read(file)
            if (bufferedImage == null) {
                JOptionPane.showMessageDialog(null, "无法读取图片：${file.name}", "错误", JOptionPane.ERROR_MESSAGE)
                return
            }
            val key = App.bitmapCache.saveBitmap(projectKey, bufferedImage)
            val info = Stitch.StitchInfo(key, bufferedImage.width, bufferedImage.height)
            project.updateUndo { project.stitchInfo.add(info); project.selected.add(info.imageKey) }
            updateSelectInfo()
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "加载图片失败：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
        }
    }

    private fun stitch(homo: Boolean, diff: Boolean) {
        if (project.selected.isEmpty()) {
            JOptionPane.showMessageDialog(null, "未选择图片", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        val total = project.selected.size
        progressLabel.text = "计算中 (0/$total)..."
        progressBar.value = 0
        progressBar.maximum = total
        progressRow.isVisible = true

        Thread {
            var done = 0
            project.updateUndo {
                project.stitchInfo.reduceOrNull { acc, it ->
                    if (project.selected.contains(it.imageKey)) {
                        Stitch.combine(homo, diff, acc, it)?.let { data ->
                            it.dx = data.dx
                            it.dy = data.dy
                            it.drot = data.drot
                            it.dscale = data.dscale
                        }
                        done++
                        val finalDone = done
                        SwingUtilities.invokeLater {
                            progressLabel.text = "计算中 ($finalDone/$total)..."
                            progressBar.value = finalDone
                        }
                    }
                    it
                }
            }
            SwingUtilities.invokeLater {
                progressRow.isVisible = false
                updateSelectInfo()
            }
        }.apply { isDaemon = true }.start()
    }

    private fun swapSelected() {
        if (project.selected.size < 2) {
            JOptionPane.showMessageDialog(null, "请选择至少两张图片进行交换", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        project.updateUndo {
            val selected = project.selected.toList()
            val i = selected.last().let {
                project.stitchInfo.indexOfFirst { info -> info.imageKey == it }
            }
            if (i < 0) return@updateUndo
            var a = project.stitchInfo[i]
            val adx = a.dx; val ady = a.dy; val adr = a.drot; val ads = a.dscale
            for (indic in 0 until selected.size - 1) {
                val j = selected[indic].let {
                    project.stitchInfo.indexOfFirst { info -> info.imageKey == it }
                }
                if (j < 0) return@updateUndo
                val b = project.stitchInfo.set(j, a)
                a.dx = b.dx; a.dy = b.dy; a.drot = b.drot; a.dscale = b.dscale
                a = b
            }
            project.stitchInfo[i] = a
            a.dx = adx; a.dy = ady; a.drot = adr; a.dscale = ads
        }
        updateSelectInfo()
    }

    private fun removeSelected() {
        if (project.selected.isEmpty()) {
            JOptionPane.showMessageDialog(null, "未选择图片", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        val result = JOptionPane.showConfirmDialog(null,
            "确定要删除已选中的 ${project.selected.size} 张图片吗？",
            "确认删除", JOptionPane.OK_CANCEL_OPTION)
        if (result == JOptionPane.OK_OPTION) {
            project.updateUndo {
                project.stitchInfo.removeAll { project.selected.contains(it.imageKey) }
                project.selected.clear()
            }
            updateSelectInfo()
        }
    }

    private fun saveImage() {
        if (project.stitchInfo.isEmpty()) {
            JOptionPane.showMessageDialog(null, "请先添加图片", "警告", JOptionPane.WARNING_MESSAGE)
            return
        }
        val chooser = JFileChooser()
        chooser.dialogTitle = "保存图片"
        chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter("PNG 图片", "png")
        chooser.selectedFile = File("Stitch$projectKey.png")
        val result = chooser.showSaveDialog(null)
        if (result != JFileChooser.APPROVE_OPTION) return

        try {
            val file = chooser.selectedFile
            val image = editView.drawToBitmap()
            javax.imageio.ImageIO.write(image, "png", file)
            JOptionPane.showMessageDialog(null, "图片已保存至 ${file.absolutePath}", "保存成功", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            JOptionPane.showMessageDialog(null, "保存失败：${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
        }
    }
}
