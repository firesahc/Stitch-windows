package soko.ekibun.stitch

import java.io.File

class ProjectManagerImpl(
    private val dataDirPath: String,
    private val appContextProvider: (() -> AppContext)? = null
) : soko.ekibun.stitch.interfaces.IProjectManager {
    // 兼容旧构造：App.kt 完成 AppContext 组装前暂存，组装后经 provider 读取，避免双向 lateinit 回填。
    // 新构造优先使用 appContextProvider；为空时回退到 legacyAppContext（过渡期）。
    var legacyAppContext: AppContext? = null

    @Deprecated("过渡兼容：新代码请使用构造注入的 appContextProvider", ReplaceWith("appContextProvider"))
    var appContext: AppContext
        get() = appContextProvider?.invoke() ?: legacyAppContext
            ?: error("ProjectManagerImpl.appContext 未初始化")
        set(value) { legacyAppContext = value }

    private val appCtx: AppContext
        get() = appContextProvider?.invoke() ?: legacyAppContext
            ?: error("ProjectManagerImpl.appContext 未初始化")

    val projects = java.util.concurrent.ConcurrentHashMap<String, Stitch.StitchProject>()

    override fun getProject(projectKey: String): Stitch.StitchProject {
        return projects.getOrPut(projectKey) { Stitch.StitchProject(projectKey, appCtx) }
    }

    override fun getProjects(): Array<File> {
        val file = File(dataDirPath)
        if (!file.exists()) return emptyArray()
        return file.listFiles { f -> f.isDirectory } ?: emptyArray()
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
}
