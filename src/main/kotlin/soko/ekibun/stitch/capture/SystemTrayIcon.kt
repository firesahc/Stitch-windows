package soko.ekibun.stitch.capture

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent

class SystemTrayIcon(
    private val onCapture: () -> Unit,
    private val onOpenEditor: () -> Unit,
    private val onExit: () -> Unit
) {
    private var trayIcon: TrayIcon? = null

    fun show() {
        if (!SystemTray.isSupported()) return

        val popup = PopupMenu().apply {
            add(MenuItem("截屏").apply {
                addActionListener { onCapture() }
            })
            add(MenuItem("打开编辑器").apply {
                addActionListener { onOpenEditor() }
            })
            addSeparator()
            add(MenuItem("退出").apply {
                addActionListener { onExit() }
            })
        }

        val image = Toolkit.getDefaultToolkit().getImage(
            javaClass.getResource("/icons/stitch_icon.png")
        ) ?: run {
            val buf = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val g = buf.createGraphics()
            g.color = java.awt.Color(66, 132, 243)
            g.fillRect(0, 0, 16, 16)
            g.color = java.awt.Color.WHITE
            g.fillRect(3, 3, 10, 10)
            g.dispose()
            buf
        }

        trayIcon = TrayIcon(image, "Stitch - 截图拼接工具", popup).apply {
            isImageAutoSize = true
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    if (e.clickCount >= 2) onOpenEditor()
                }
            })
            SystemTray.getSystemTray().add(this)
        }
    }

    fun hide() {
        trayIcon?.let {
            SystemTray.getSystemTray().remove(it)
            trayIcon = null
        }
    }
}
