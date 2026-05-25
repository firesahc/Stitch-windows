package soko.ekibun.stitch

import java.io.File

class ProjectManagerImpl(private val dataDirPath: String) : soko.ekibun.stitch.interfaces.IProjectManager {
    lateinit var appContext: AppContext

    val projects = HashMap<String, Stitch.StitchProject>()

    override fun getProject(projectKey: String): Stitch.StitchProject {
        return projects.getOrPut(projectKey) { Stitch.StitchProject(projectKey, appContext) }
    }

    override fun getProjects(): Array<File> {
        val file = File(dataDirPath)
        if (!file.exists()) return emptyArray()
        return file.listFiles { f -> f.isDirectory } ?: emptyArray()
    }

    override fun getProjectFile(projectKey: String): File {
        return File(dataDirPath + File.separator + projectKey + File.separator + ".project")
    }

    override fun newProject(): String = System.currentTimeMillis().toString(16)

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
