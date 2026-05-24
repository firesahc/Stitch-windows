package soko.ekibun.stitch.ui

import soko.ekibun.stitch.App
import soko.ekibun.stitch.ProjectManager
import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.GraphicsHelper
import soko.ekibun.stitch.util.Strings
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

class MainView : JFrame() {

    private val projectList = JList<File>()
    private val defaultListModel = DefaultListModel<File>()

    init {
        title = Strings.get("main.title")
        defaultCloseOperation = EXIT_ON_CLOSE
        minimumSize = Dimension(500, 400)
        setSize(600, 500)
        setLocationRelativeTo(null)

        setIconImage(GraphicsHelper.createAppIcon())

        layout = BorderLayout()

        // Header
        val header = createHeader()
        add(header, BorderLayout.NORTH)

        // Project list
        projectList.model = defaultListModel
        projectList.cellRenderer = ProjectListRenderer()
        val popupMenu = JPopupMenu()
        val deleteItem = JMenuItem(Strings.get("main.deleteProject")).apply {
            addActionListener {
                val selected = projectList.selectedValue
                if (selected != null) {
                    val result = JOptionPane.showConfirmDialog(
                        this@MainView,
                        Strings.get("main.deleteConfirm", formatProjectName(selected)),
                        Strings.get("dialog.confirmTitle"),
                        JOptionPane.OK_CANCEL_OPTION
                    )
                    if (result == JOptionPane.OK_OPTION) {
                        ProjectManager.deleteProject(selected.name)
                        loadProjects()
                    }
                }
            }
        }
        popupMenu.add(deleteItem)

        projectList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = projectList.selectedValue
                    if (selected != null) {
                        EditActivity.open(this@MainView, selected.name)
                    }
                }
            }

            override fun mousePressed(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                if (e.isPopupTrigger) showPopup(e)
            }

            private fun showPopup(e: MouseEvent) {
                val index = projectList.locationToIndex(e.point)
                if (index >= 0) {
                    projectList.selectedIndex = index
                    popupMenu.show(e.component, e.x, e.y)
                }
            }
        })
        add(JScrollPane(projectList), BorderLayout.CENTER)

        // Bottom bar
        val bottomBar = JPanel(FlowLayout(FlowLayout.RIGHT, 10, 10))
        val aboutBtn = JButton(Strings.get("main.about"))
        aboutBtn.addActionListener { AboutDialog.show(this) }
        aboutBtn.border = EmptyBorder(5, 10, 5, 10)
        bottomBar.add(aboutBtn)
        add(bottomBar, BorderLayout.SOUTH)

        // 窗口激活时刷新项目列表（关闭编辑器后自动更新）
        addWindowListener(object : WindowAdapter() {
            override fun windowActivated(e: WindowEvent) {
                loadProjects()
            }
        })

        loadProjects()
    }

    private fun createHeader(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.border = EmptyBorder(20, 20, 20, 20)
        panel.background = Color(220, 220, 220)

        val label = JLabel(Strings.get("main.title"))
        label.font = Font(Font.SANS_SERIF, Font.BOLD, 24)
        label.alignmentX = Component.CENTER_ALIGNMENT

        val subLabel = JLabel(Strings.get("main.subtitle"))
        subLabel.font = Font(Font.SANS_SERIF, Font.PLAIN, 13)
        subLabel.foreground = Color.GRAY
        subLabel.alignmentX = Component.CENTER_ALIGNMENT

        val fromGalleryBtn = JButton(Strings.get("main.import"))
        fromGalleryBtn.alignmentX = Component.CENTER_ALIGNMENT
        fromGalleryBtn.maximumSize = Dimension(300, fromGalleryBtn.preferredSize.height)
        fromGalleryBtn.addActionListener {
            val chooser = JFileChooser()
            chooser.dialogTitle = Strings.get("dialog.selectImage")
            chooser.fileFilter = javax.swing.filechooser.FileNameExtensionFilter(Strings.get("dialog.imageFiles"), "png", "jpg", "jpeg", "bmp")
            chooser.isMultiSelectionEnabled = true
            val result = chooser.showOpenDialog(this)
            if (result == JFileChooser.APPROVE_OPTION) {
                val key = ProjectManager.newProject()
                chooser.selectedFiles.forEach { file ->
                    try {
                        val img = ImageIO.read(file)
                        if (img != null) {
                            val imgKey = App.bitmapCache.saveBitmap(key, img)
                            val project = ProjectManager.getProject(key)
                            project.updateUndo {
                                project.stitchInfo.add(
                                    Stitch.StitchInfo(imgKey, img.width, img.height)
                                )
                            }
                        }
                    } catch (ex: Exception) {
                        JOptionPane.showMessageDialog(null, Strings.get("dialog.operationFailed", ex.message), Strings.get("common.error"), JOptionPane.ERROR_MESSAGE)
                    }
                }
                EditActivity.open(this, key)
                loadProjects()
            }
        }

        val openBtn = JButton(Strings.get("main.newProject"))
        openBtn.alignmentX = Component.CENTER_ALIGNMENT
        openBtn.maximumSize = Dimension(300, openBtn.preferredSize.height)
        openBtn.addActionListener {
            val key = ProjectManager.newProject()
            EditActivity.open(this, key)
            loadProjects()
        }

        val clearBtn = JButton(Strings.get("main.clearHistory"))
        clearBtn.alignmentX = Component.CENTER_ALIGNMENT
        clearBtn.maximumSize = Dimension(300, clearBtn.preferredSize.height)
        clearBtn.addActionListener {
            ProjectManager.clearProjects()
            loadProjects()
        }

        panel.add(label)
        panel.add(Box.createVerticalStrut(5))
        panel.add(subLabel)
        panel.add(Box.createVerticalStrut(15))
        panel.add(fromGalleryBtn)
        panel.add(Box.createVerticalStrut(5))
        panel.add(openBtn)
        panel.add(Box.createVerticalStrut(5))
        panel.add(clearBtn)
        return panel
    }

    fun loadProjects() {
        defaultListModel.clear()
        ProjectManager.getProjects().forEach { defaultListModel.addElement(it) }
    }

    private fun formatProjectName(file: File): String {
        return file.name.toLongOrNull(16)?.let {
            java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
                java.util.Locale.getDefault()).format(java.util.Date(it))
        } ?: file.name
    }

    private inner class ProjectListRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<out Any>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus) as JLabel
            if (value is File) {
                text = formatProjectName(value)
            }
            return comp
        }
    }
}
