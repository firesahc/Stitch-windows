package soko.ekibun.stitch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.launch
import soko.ekibun.stitch.interfaces.Dialogs
import soko.ekibun.stitch.interfaces.IBitmapCache
import soko.ekibun.stitch.interfaces.IProjectManager
import soko.ekibun.stitch.interfaces.IStitchNative
import soko.ekibun.stitch.interfaces.IStitchService
import soko.ekibun.stitch.service.StitchService
import soko.ekibun.stitch.ui.MainView
import soko.ekibun.stitch.ui.NativeFileDialogs
import soko.ekibun.stitch.util.Log
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
    Log.init(File(dataDirPath))
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

    // 启动时静默回收孤儿图片（此时无存活 undo 状态，安全）；失败不阻塞 UI。
    CoroutineScope(appContext.dispatcherIO).launch {
        try {
            appContext.projectManager.cleanupOrphanBitmaps()
        } catch (e: Exception) {
            Log.e("App", e)
        }
    }

    SwingUtilities.invokeLater {
        MainView(appContext).isVisible = true
    }
}
