package soko.ekibun.stitch

import kotlinx.coroutines.asCoroutineDispatcher
import soko.ekibun.stitch.interfaces.IBitmapCache
import soko.ekibun.stitch.interfaces.IProjectManager
import soko.ekibun.stitch.interfaces.IStitchNative
import soko.ekibun.stitch.service.StitchService
import soko.ekibun.stitch.ui.MainView
import java.io.File
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import nu.pattern.OpenCV

class AppContext(
    val dataDirPath: String,
    val bitmapCache: IBitmapCache,
    val projectManager: IProjectManager,
    val stitchNative: IStitchNative,
    val stitchService: StitchService,
) {
    val dispatcherIO = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
}


fun main() {
    // Load OpenCV native library before any other setup
    OpenCV.loadLocally()
    // Set up FlatLaf theme before any Swing components
    com.formdev.flatlaf.FlatLightLaf.setup()
    val font = java.awt.Font("Microsoft YaHei", java.awt.Font.PLAIN, 13)
    javax.swing.UIManager.put("defaultFont", font)

    val dataDirPath = System.getProperty("user.dir") + File.separator + "data"
    val bitmapCache = BitmapCacheImpl(dataDirPath) as IBitmapCache
    val projectManager = ProjectManagerImpl(dataDirPath) as IProjectManager
    val stitchNative = StitchNativeImpl(bitmapCache) as IStitchNative
    val stitchService = StitchService(stitchNative)
    val appContext = AppContext(
        dataDirPath = dataDirPath,
        bitmapCache = bitmapCache,
        projectManager = projectManager,
        stitchNative = stitchNative,
        stitchService = stitchService,
    )
    (projectManager as ProjectManagerImpl).appContext = appContext

    SwingUtilities.invokeLater {
        MainView(appContext).isVisible = true
    }
}
