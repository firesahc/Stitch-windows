package soko.ekibun.stitch.ui

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.domain.ParamMapper
import soko.ekibun.stitch.domain.StitchType
import soko.ekibun.stitch.util.PRIMARY_COLOR
import java.awt.Color
import java.awt.FlowLayout
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JTextField
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

/**
 * 数值编辑面板，包含 numberA / numberB 文本框、分隔符、加减按钮，
 * 以及对应的 DocumentListener / ActionListener。
 */
class NumberEditPanel(
    private val stitchType: () -> StitchType,
    private val selectIndex: () -> String,
    private val selectItems: () -> Map<String, Pair<Int, Boolean>>,
    private val switchHorizon: () -> Boolean,
    private val onNumberChanged: ((Float?, Float?) -> Unit)? = null,
    private val onSeekbarUpdate: (() -> Unit)? = null,
    private val onEditViewUpdate: (() -> Unit)? = null
) {
    /** 0 = 第一个数值(numberA), 1 = 第二个数值(numberB) */
    var selectedHandle = 0

    internal val numberA: JTextField
    internal val numberB: JTextField
    private val numberDiv: JLabel
    private val numberDec: JButton
    private val numberInc: JButton
    private var suppressNumberListener = false

    val numberView: JPanel

    init {
        numberView = JPanel()
        numberView.layout = BoxLayout(numberView, BoxLayout.Y_AXIS)
        numberA = JTextField("0", 6)
        numberB = JTextField("0", 6)
        numberDiv = JLabel("-")
        numberDec = JButton("<")
        numberInc = JButton(">")

        numberA.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onNumberAChanged()
            override fun removeUpdate(e: DocumentEvent) = onNumberAChanged()
            override fun changedUpdate(e: DocumentEvent) = onNumberAChanged()

            private fun onNumberAChanged() {
                if (suppressNumberListener) return
                val num = numberA.text.toFloatOrNull() ?: return
                onNumberChanged?.invoke(num, null)
                onSeekbarUpdate?.invoke()
                onEditViewUpdate?.invoke()
            }
        })

        numberB.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = onNumberBChanged()
            override fun removeUpdate(e: DocumentEvent) = onNumberBChanged()
            override fun changedUpdate(e: DocumentEvent) = onNumberBChanged()

            private fun onNumberBChanged() {
                if (suppressNumberListener) return
                val num = numberB.text.toFloatOrNull() ?: return
                onNumberChanged?.invoke(null, num)
                onSeekbarUpdate?.invoke()
                onEditViewUpdate?.invoke()
            }
        })

        numberDec.addActionListener {
            val step = Math.pow(
                10.0,
                -(selectItems()[selectIndex()]?.first ?: 0).toDouble()
            ).toFloat()
            val num = (numberA.text.toFloatOrNull() ?: 0f) - step
            onNumberChanged?.invoke(num, null)
            updateNumberView(num)
            onSeekbarUpdate?.invoke()
            onEditViewUpdate?.invoke()
        }

        numberInc.addActionListener {
            val step = Math.pow(
                10.0,
                -(selectItems()[selectIndex()]?.first ?: 0).toDouble()
            ).toFloat()
            val num = (numberA.text.toFloatOrNull() ?: 0f) + step
            onNumberChanged?.invoke(num, null)
            updateNumberView(num)
            onSeekbarUpdate?.invoke()
            onEditViewUpdate?.invoke()
        }

        val numRow = JPanel(FlowLayout(FlowLayout.CENTER, 4, 0))
        numRow.add(numberDec)
        numRow.add(numberA)
        numRow.add(numberDiv)
        numRow.add(numberB)
        numRow.add(numberInc)
        numberView.add(numRow)
    }

    fun updateNumber(selectedStitchInfo: List<Stitch.StitchInfo>) {
        if (selectedStitchInfo.isNotEmpty()) {
            numberView.isVisible = true
            // 唯一映射源：ParamMapper.toNumber
            val (a, b) = ParamMapper.toNumber(stitchType(), selectIndex(), selectedStitchInfo, switchHorizon())
            updateNumberView(a, b)
        } else {
            numberView.isVisible = false
        }
    }

    fun updateNumberView(a: Float? = null, b: Float? = null) {
        val (roundOf, showB) = if (stitchType() == StitchType.MAN)
            selectItems()[selectIndex()] ?: (0 to false)
        else (2 to true)
        numberB.isVisible = showB
        numberDiv.isVisible = showB
        numberDec.isVisible = !showB
        numberInc.isVisible = !showB
        suppressNumberListener = true
        if (a != null) numberA.text = String.format("%.${roundOf}f", a)
        if (b != null) numberB.text = String.format("%.${roundOf}f", b)
        suppressNumberListener = false
        // 高亮选中的文本框
        numberA.border = if (selectedHandle == 0) BorderFactory.createLineBorder(PRIMARY_COLOR, 2)
        else BorderFactory.createLineBorder(Color(180, 180, 180))
        if (showB)
            numberB.border = if (selectedHandle == 1) BorderFactory.createLineBorder(PRIMARY_COLOR, 2)
            else BorderFactory.createLineBorder(Color(180, 180, 180))
    }
}
