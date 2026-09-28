package soko.ekibun.stitch.ui

import java.awt.datatransfer.DataFlavor
import java.io.File
import javax.swing.TransferHandler

/**
 * 图片文件拖放接收器（纯 UI 基础设施）。
 * 职责仅：校验 javaFileListFlavor + 提取文件列表 + 转发。
 * 文件类型过滤、解码、入库归 [EditorService] 负责，保持低耦合可测试。
 */
class ImageDropHandler(
    private val onDrop: (List<File>) -> Unit
) : TransferHandler() {

    override fun canImport(support: TransferSupport): Boolean {
        if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) return false
        // 仅接受拖放，不劫持复制粘贴
        if (!support.isDrop) return false
        return true
    }

    override fun importData(support: TransferSupport): Boolean {
        if (!canImport(support)) return false
        return try {
            @Suppress("UNCHECKED_CAST")
            val files = support.transferable.getTransferData(DataFlavor.javaFileListFlavor) as? List<File>
                ?: return false
            if (files.isEmpty()) return false
            onDrop(files)
            true
        } catch (e: Exception) {
            false
        }
    }
}
