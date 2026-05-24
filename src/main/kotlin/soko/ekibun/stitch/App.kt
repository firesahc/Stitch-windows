package soko.ekibun.stitch

import kotlinx.coroutines.asCoroutineDispatcher
import java.io.File
import java.util.concurrent.Executors
import soko.ekibun.stitch.ui.MainView
import javax.swing.SwingUtilities
import nu.pattern.OpenCV

object App {
    val dataDirPath: String by lazy {
        System.getProperty("user.dir") + File.separator + "data"
    }

    val bitmapCache by lazy { BitmapCache }
    val projects = HashMap<String, Stitch.StitchProject>()
    val preferencesStore = PreferencesStore()
    val dispatcherIO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    var captureProject: String? = null

    fun getProject(projectKey: String): Stitch.StitchProject {
        return projects.getOrPut(projectKey) { Stitch.StitchProject(projectKey) }
    }

    fun getProjects(): Array<File> {
        val file = File(dataDirPath)
        if (!file.exists()) return emptyArray()
        return file.listFiles { f -> f.isDirectory } ?: emptyArray()
    }

    fun getProjectFile(projectKey: String): File {
        return File(dataDirPath + File.separator + projectKey + File.separator + ".project")
    }

    fun newProject(): String = System.currentTimeMillis().toString(16)

    fun clearProjects() {
        val file = File(dataDirPath)
        file.deleteRecursively()
        projects.clear()
    }

    fun deleteProject(projectKey: String) {
        val file = File(dataDirPath, projectKey)
        projects.remove(projectKey)
        file.deleteRecursively()
    }
}

class PreferencesStore {
    private val store = mutableMapOf<String, Any>()

    fun getInt(key: String, default: Int): Int = (store[key] as? Int) ?: default
    fun putInt(key: String, value: Int) { store[key] = value }
    fun getString(key: String, default: String): String = (store[key] as? String) ?: default
    fun putString(key: String, value: String) { store[key] = value }
}

fun main() {
    // Load OpenCV native library before any other setup
    OpenCV.loadLocally()
    // Set up FlatLaf theme before any Swing components
    com.formdev.flatlaf.FlatLightLaf.setup()
    val font = java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 13)
    javax.swing.UIManager.put("defaultFont", font)

    SwingUtilities.invokeLater {
        MainView().isVisible = true
    }
}
