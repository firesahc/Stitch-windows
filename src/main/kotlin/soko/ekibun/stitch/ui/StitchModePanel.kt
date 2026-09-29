package soko.ekibun.stitch.ui

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.domain.ParamMapper
import soko.ekibun.stitch.domain.StitchLabels
import soko.ekibun.stitch.domain.StitchType
import soko.ekibun.stitch.util.PRIMARY_COLOR
import soko.ekibun.stitch.util.Strings
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

/**
 * Panel containing all stitch mode UI: tabs, auto panel, seekbar panel,
 * progress row, and action row.
 *
 * Constructor callbacks bridge operations that require EditActivity (project, editorService, etc.).
 *
 * Note: NumberEditPanel is NOT owned here – it is created by EditActivity and its
 * numberView is added to [panelSeekbar] via [addNumberView].
 */
class StitchModePanel(
    val selectItems: Map<String, Pair<Int, Boolean>>,
    private val onStitch: ((fullTransform: Boolean, edgeEnhance: Boolean) -> Unit)? = null,
    private val onTabChanged: ((stitchType: StitchType) -> Unit)? = null,
    private val onSeekbarChange: ((a: Float, b: Float) -> Unit)? = null,
    private val onSave: (() -> Unit)? = null,
    private val onSeekbarTouchUp: (() -> Unit)? = null,
    private val onDropdownChanged: ((newVal: String) -> Unit)? = null
) {
    /** Current stitch type – updated externally by EditActivity */
    var stitchType: StitchType = StitchType.AUTO

    /** Current select index – updated externally by EditActivity */
    var selectIndex: String = StitchLabels.labelDx

    // ---- Exposed components ----

    lateinit var panelAuto: JPanel
    lateinit var panelSeekbar: JPanel
    lateinit var tabPane: JPanel
    lateinit var seekbar: RangeSlider
    lateinit var dropdown: JComboBox<String>
    lateinit var switchHorizon: JCheckBox
    lateinit var radioTransformTrans: JRadioButton
    lateinit var radioTransformFull: JRadioButton
    lateinit var checkEdgeEnhance: JCheckBox
    lateinit var progressRow: JPanel
    lateinit var progressBar: JProgressBar
    lateinit var progressLabel: JLabel
    lateinit var tabviews: List<JLabel>

    init {
        createTabBar()
        createAutoPanel()
        createSeekbarPanel()
        createProgressRow()
        // createActionRow() is called by EditActivity.createBottomPanel()
    }

    // ---- Tab bar ----

    private fun createTabBar() {
        tabPane = JPanel(FlowLayout(FlowLayout.LEFT, 15, 4))
        tabviews = StitchType.values().map { type ->
            JLabel(type.label).apply {
                border = javax.swing.border.EmptyBorder(4, 12, 4, 12)
                toolTipText = type.label
                addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        onTabChanged?.invoke(type)
                    }
                })
                tabPane.add(this)
            }
        }
    }

    // ---- Auto panel ----

    private fun createAutoPanel() {
        radioTransformTrans = JRadioButton(Strings.get("edit.transformTrans")).apply { isSelected = true }
        radioTransformFull = JRadioButton(Strings.get("edit.transformFull"))
        ButtonGroup().apply { add(radioTransformTrans); add(radioTransformFull) }
        checkEdgeEnhance = JCheckBox(Strings.get("edit.edgeEnhance"))
        val stitchBtn = JButton(Strings.get("edit.stitch")).apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { onStitch?.invoke(radioTransformFull.isSelected, checkEdgeEnhance.isSelected) }
        }
        panelAuto = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
            add(radioTransformTrans); add(radioTransformFull); add(checkEdgeEnhance); add(stitchBtn)
        }
    }

    // ---- Seekbar panel (numberView added by EditActivity via addNumberView) ----

    private fun createSeekbarPanel() {
        panelSeekbar = JPanel(FlowLayout(FlowLayout.LEFT, 8, 5)).apply {
            switchHorizon = JCheckBox(Strings.get("edit.horizon"))
            dropdown = JComboBox<String>().apply {
                selectItems.keys.forEach { addItem(it) }
                selectedItem = selectIndex
                preferredSize = Dimension(100, 25)
                isFocusable = false // 防止 ↑/↓ 被下拉框拦截用于切换选项
                addActionListener {
                    val newVal = selectedItem as? String
                    if (newVal != null && newVal != selectIndex) {
                        selectIndex = newVal
                        onDropdownChanged?.invoke(newVal)
                    }
                }
            }
            seekbar = RangeSlider().apply {
                preferredSize = Dimension(200, 30)
                onRangeChange = { a, b -> onSeekbarChange?.invoke(a, b) }
                onTouchUp = { onSeekbarTouchUp?.invoke() }
            }
            add(switchHorizon); add(dropdown); add(seekbar)
            // numberView added later by EditActivity via addNumberView()
        }
    }

    /** Add the NumberEditPanel's numberView to the seekbar panel (called by EditActivity). */
    fun addNumberView(numberView: JPanel) {
        panelSeekbar.add(numberView)
    }

    // ---- Action row ----

    fun createActionRow(): JPanel {
        val saveBtn = JButton(Strings.get("edit.save")).apply {
            foreground = Color.WHITE
            background = PRIMARY_COLOR
            addActionListener { onSave?.invoke() }
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

    // ---- Progress row ----

    private fun createProgressRow(): JPanel {
        progressBar = JProgressBar()
        progressLabel = JLabel()
        progressRow = JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)).apply {
            add(progressLabel); add(progressBar)
            isVisible = false
        }
        return progressRow
    }

    // ---- Tab highlighting & panel visibility ----

    fun updateTab(stitchType: StitchType) {
        this.stitchType = stitchType
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

    // ---- Seekbar value calculation ----

    fun updateSeekbar(
        stitchType: StitchType,
        selectIndex: String,
        selectedStitchInfo: List<Stitch.StitchInfo>,
        switchHorizonSelected: Boolean
    ) {
        this.stitchType = stitchType
        this.selectIndex = selectIndex
        if (selectedStitchInfo.isNotEmpty()) {
            seekbar.isEnabled = true
            seekbar.constrainHandles = stitchType != StitchType.TILE
            // 唯一映射源：ParamMapper.toSlider（原 7 个 SeekbarHandler 已收敛）
            val (a, b, type) = ParamMapper.toSlider(stitchType, selectIndex, selectedStitchInfo, switchHorizonSelected)
            seekbar.type = type
            seekbar.a = a
            seekbar.b = b
            seekbar.repaint()
        } else {
            seekbar.isEnabled = false
        }
    }
}
