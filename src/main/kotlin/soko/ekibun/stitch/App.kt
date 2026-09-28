package soko.ekibun.stitch

import kotlinx.coroutines.asCoroutineDispatcher
import soko.ekibun.stitch.interfaces.Dialogs
import soko.ekibun.stitch.interfaces.IBitmapCache
import soko.ekibun.stitch.interfaces.IProjectManager
import soko.ekibun.stitch.interfaces.IStitchNative
import soko.ekibun.stitch.interfaces.IStitchService
import soko.ekibun.stitch.service.StitchService
import soko.ekibun.stitch.ui.MainView
import soko.ekibun.stitch.ui.NativeFileDialogs
import java.io.File
import java.util.concurrent.Executors
import javax.swing.SwingUtilities
import nu.pattern.OpenCV

class AppContext(
    val dataDirPath: String,
    val bitmapCache: IBitmapCache,
    val projectManager: IProjectManager,
    val stitchNative: IStitchNative,
    val stitchService: IStitchService,
    val dialogs: Dialogs,
) {
    private val ioExecutor = Executors.newSingleThreadExecutor()
    val dispatcherIO = ioExecutor.asCoroutineDispatcher()

    init {
        Runtime.getRuntime().addShutdownHook(Thread { ioExecutor.shutdown() })
    }
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
    // provider 消除 ProjectManagerImpl.appContext 回填式双向依赖：manager 经 provider 懒取 AppContext
    lateinit var appContext: AppContext
    val projectManager = ProjectManagerImpl(dataDirPath) { appContext } as IProjectManager
    val stitchNative = StitchNativeImpl(bitmapCache) as IStitchNative
    val stitchService = StitchService(stitchNative) as IStitchService
    appContext = AppContext(
        dataDirPath = dataDirPath,
        bitmapCache = bitmapCache,
        projectManager = projectManager,
        stitchNative = stitchNative,
        stitchService = stitchService,
        dialogs = NativeFileDialogs(),
    )

    SwingUtilities.invokeLater {
        MainView(appContext).isVisible = true
    }
}
