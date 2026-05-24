package soko.ekibun.stitch

import java.io.File

object ProjectManager {
    val projects = HashMap<String, Stitch.StitchProject>()

    fun getProject(projectKey: String): Stitch.StitchProject {
        return projects.getOrPut(projectKey) { Stitch.StitchProject(projectKey) }
    }

    fun getProjects(): Array<File> {
        val file = File(App.dataDirPath)
        if (!file.exists()) return emptyArray()
        return file.listFiles { f -> f.isDirectory } ?: emptyArray()
    }

    fun getProjectFile(projectKey: String): File {
        return File(App.dataDirPath + File.separator + projectKey + File.separator + ".project")
    }

    fun newProject(): String = System.currentTimeMillis().toString(16)

    fun clearProjects() {
        val file = File(App.dataDirPath)
        file.deleteRecursively()
        projects.clear()
    }

    fun deleteProject(projectKey: String) {
        val file = File(App.dataDirPath, projectKey)
        projects.remove(projectKey)
        file.deleteRecursively()
    }
}
