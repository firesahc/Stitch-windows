package soko.ekibun.stitch.ui

import java.awt.*
import javax.swing.*

class AboutDialog {

    companion object {
        fun show(owner: Frame) {
            val dialog = JDialog(owner, "关于 Stitch", true)
            dialog.setSize(420, 380)
            dialog.setLocationRelativeTo(owner)

            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

            val title = JLabel("Stitch")
            title.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
            title.foreground = soko.ekibun.stitch.util.PRIMARY_COLOR
            title.alignmentX = Component.CENTER_ALIGNMENT

            val desc = JLabel("截图拼接助手")
            desc.alignmentX = Component.CENTER_ALIGNMENT

            val version = JLabel("v1.0.0 (Windows 桌面版)")
            version.alignmentX = Component.CENTER_ALIGNMENT

            val info = JLabel("<html><div style='text-align:center; line-height:1.8;'>1. 点击「导入/截屏」添加图片。<br>2. 点击圆圈选中图片以修改拼接参数，或拖动圆圈移动图片。<br>3. 自动拼接可用于拼接地图，请确保有足够的重叠区域。<br>4. 重启后拼接数据将被清除。</div></html>")
            info.alignmentX = Component.CENTER_ALIGNMENT

            val closeBtn = JButton("确定")
            closeBtn.alignmentX = Component.CENTER_ALIGNMENT
            closeBtn.border = BorderFactory.createEmptyBorder(5, 20, 5, 20)
            closeBtn.addActionListener { dialog.dispose() }

            panel.add(title)
            panel.add(Box.createVerticalStrut(5))
            panel.add(desc)
            panel.add(Box.createVerticalStrut(5))
            panel.add(version)
            panel.add(Box.createVerticalStrut(20))
            panel.add(Box.createVerticalStrut(5))
            panel.add(JSeparator())
            panel.add(Box.createVerticalGlue())
            panel.add(info)
            panel.add(Box.createVerticalStrut(15))
            panel.add(closeBtn)

            dialog.contentPane.add(panel)
            dialog.isVisible = true
        }
    }
}
