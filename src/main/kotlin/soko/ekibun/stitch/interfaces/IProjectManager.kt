package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.Stitch
import java.io.File

interface IProjectManager {
    fun getProject(projectKey: String): Stitch.StitchProject
    fun getProjects(): Array<File>
    fun getProjectFile(projectKey: String): File
    fun newProject(): String
    fun clearProjects()
    fun deleteProject(projectKey: String)
    /** 清理各项目下未被引用的图片文件（删除图片后 undo 语义只存于内存，跨启动 GC 安全），返回删除数。 */
    fun cleanupOrphanBitmaps(): Int
    /** 项目目录名 -> 展示名。兼容三种 key：旧纯时间戳 hex、新时间戳-后缀、过渡期纯 UUID。 */
    fun formatProjectName(file: File): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS",
            java.util.Locale.getDefault())
        val raw = file.name
        // 1) 新/旧时间戳 key：取 "-" "_" 前缀解析
        val stampPart = raw.substringBefore("-").substringBefore("_")
        stampPart.toLongOrNull(16)?.let { ts ->
            val date = sdf.format(java.util.Date(ts))
            val suffix = raw.substringAfter("-", "").takeIf { it.isNotEmpty() }
            return if (suffix != null) "$date (${suffix.take(8)})" else date
        }
        // 2) 过渡期纯 UUID key（32 位 hex，toLong 溢出）：用目录修改时间 + 短 id，避免显示乱码
        if (file.isDirectory && file.lastModified() > 0) {
            return "${sdf.format(java.util.Date(file.lastModified()))} (${raw.take(8)})"
        }
        // 3) 其他：截断展示，避免超长乱码
        return raw.takeIf { it.length <= 12 } ?: raw.take(8)
    }
}
