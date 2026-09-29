package soko.ekibun.stitch.ui

import java.awt.*
import javax.swing.*
import soko.ekibun.stitch.util.Strings

class AboutDialog {

    companion object {
        fun show(owner: Frame) {
            val dialog = JDialog(owner, Strings.get("about.title"), true)

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

            val repoUrl = Strings.get("about.repoUrl")
            val repo = JLabel("<html><a href=''>$repoUrl</a></html>")
            repo.alignmentX = Component.CENTER_ALIGNMENT
            repo.horizontalAlignment = SwingConstants.CENTER
            repo.maximumSize = Dimension(Int.MAX_VALUE, repo.preferredSize.height)
            repo.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            repo.toolTipText = repoUrl
            repo.addMouseListener(object : java.awt.event.MouseAdapter() {
                override fun mouseClicked(e: java.awt.event.MouseEvent) {
                    openRepo(repoUrl)
                }
            })

            val info = JLabel(Strings.get("about.info"))
            info.alignmentX = Component.CENTER_ALIGNMENT

            val closeBtn = JButton(Strings.get("about.ok"))
            closeBtn.alignmentX = Component.CENTER_ALIGNMENT
            closeBtn.border = BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 20, 5, 20)
            )
            closeBtn.addActionListener { dialog.dispose() }

            panel.add(title)
            panel.add(Box.createVerticalStrut(5))
            panel.add(desc)
            panel.add(Box.createVerticalStrut(5))
            panel.add(version)
            panel.add(Box.createVerticalStrut(5))
            panel.add(repo)
            panel.add(Box.createVerticalStrut(15))
            panel.add(JSeparator().apply {
                maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
                alignmentX = Component.CENTER_ALIGNMENT
            })
            panel.add(Box.createVerticalStrut(15))
            panel.add(info)
            panel.add(Box.createVerticalStrut(15))
            panel.add(closeBtn)

            dialog.contentPane.add(panel)
            dialog.pack()
            dialog.setLocationRelativeTo(owner)
            dialog.isVisible = true
        }

        private fun openRepo(url: String) {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(java.net.URI(url))
                    return
                }
            } catch (_: Exception) {
            }
            // 兜底：打不开浏览器则复制地址到剪贴板
            try {
                val sel = java.awt.datatransfer.StringSelection(url)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(sel, sel)
            } catch (_: Exception) {
            }
        }
    }
}
