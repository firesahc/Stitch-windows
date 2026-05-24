package soko.ekibun.stitch.ui

import java.awt.*
import javax.swing.*
import soko.ekibun.stitch.util.Strings

class AboutDialog {

    companion object {
        fun show(owner: Frame) {
            val dialog = JDialog(owner, Strings.get("about.title"), true)
            dialog.setSize(420, 380)
            dialog.setLocationRelativeTo(owner)

            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            panel.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)

            val title = JLabel(Strings.get("main.title"))
            title.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
            title.foreground = soko.ekibun.stitch.util.PRIMARY_COLOR
            title.alignmentX = Component.CENTER_ALIGNMENT

            val desc = JLabel(Strings.get("about.desc"))
            desc.alignmentX = Component.CENTER_ALIGNMENT

            val version = JLabel(Strings.get("about.version"))
            version.alignmentX = Component.CENTER_ALIGNMENT

            val info = JLabel(Strings.get("about.info"))
            info.alignmentX = Component.CENTER_ALIGNMENT

            val closeBtn = JButton(Strings.get("about.ok"))
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
