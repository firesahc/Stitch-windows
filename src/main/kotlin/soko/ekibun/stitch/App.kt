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
    val dispatcherIO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
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
