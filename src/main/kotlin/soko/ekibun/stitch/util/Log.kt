package soko.ekibun.stitch.util

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

/**
 * 统一日志：控制台保持输出，同时落盘 data/stitch.log（1MB×3 轮转）。
 * App 启动时最先初始化；各模块不再直接 printStackTrace/System.err。
 */
object Log {
    private val logger: Logger = Logger.getLogger("Stitch")

    fun init(dataDir: File) {
        try {
            if (logger.handlers.any { it is FileHandler }) return
            dataDir.mkdirs()
            val handler = FileHandler(File(dataDir, "stitch.log").absolutePath, 1024 * 1024, 3, true)
            handler.formatter = SimpleFormatter()
            handler.level = Level.ALL
            logger.addHandler(handler)
            logger.level = Level.ALL
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun e(tag: String, e: Throwable) {
        logger.log(Level.SEVERE, "[$tag] ${e.message}", e)
    }

    fun w(tag: String, msg: String) {
        logger.log(Level.WARNING, "[$tag] $msg")
    }
}
