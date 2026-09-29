package soko.ekibun.stitch

import soko.ekibun.stitch.domain.ProjectRepository
import java.io.File

class ProjectManagerImpl(
    private val dataDirPath: String,
    private val appContextProvider: () -> AppContext
) : soko.ekibun.stitch.interfaces.IProjectManager {
    private val appCtx: AppContext
        get() = appContextProvider()

    val projects = java.util.concurrent.ConcurrentHashMap<String, Stitch.StitchProject>()

    override fun getProject(projectKey: String): Stitch.StitchProject {
        return projects.getOrPut(projectKey) { Stitch.StitchProject(projectKey, appCtx) }
    }

    override fun getProjects(): Array<File> {
        val file = File(dataDirPath)
        if (!file.exists()) return emptyArray()
        val dirs = file.listFiles { f -> f.isDirectory } ?: emptyArray()
        return dirs.sortedByDescending { it.lastModified() }.toTypedArray()
    }

    override fun getProjectFile(projectKey: String): File {
        return File(dataDirPath + File.separator + projectKey + File.separator + ".project")
    }

    // 时间戳 hex 前缀 + 短随机后缀：既保留日期可读性，又避免毫秒级碰撞。
    // formatProjectName() 解析 "-" 前的时间戳展示为日期，后缀仅用于区分同毫秒项目。
    override fun newProject(): String =
        System.currentTimeMillis().toString(16) + "-" + java.util.UUID.randomUUID().toString().take(8)

    override fun clearProjects() {
        val file = File(dataDirPath)
        file.deleteRecursively()
        projects.clear()
    }

    override fun deleteProject(projectKey: String) {
        val file = File(dataDirPath, projectKey)
        projects.remove(projectKey)
        file.deleteRecursively()
    }

    override fun cleanupOrphanBitmaps(): Int {
        var total = 0
        val repository = ProjectRepository()
        for (dir in getProjects()) {
            try {
                val keys = repository.load(getProjectFile(dir.name)).map { it.imageKey }.toSet()
                total += appCtx.bitmapCache.gcProjectFiles(dir.name, keys)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return total
    }
}
