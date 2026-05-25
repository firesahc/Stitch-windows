package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.Stitch
import java.io.File

interface IProjectManager {
    fun getProject(projectKey: String): Stitch.StitchProject
    fun getProjects(): Array<File>
    fun newProject(): String
    fun clearProjects()
    fun deleteProject(projectKey: String)
}
