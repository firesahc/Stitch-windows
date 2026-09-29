package soko.ekibun.stitch

import soko.ekibun.stitch.interfaces.IBitmapCache
import soko.ekibun.stitch.util.Log
import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

class BitmapCacheImpl(private val dataDirPath: String) : IBitmapCache {
    private companion object {
        private const val MAX_MEMORY_ENTRIES = 256
    }

    private val memoryCache = object : LinkedHashMap<String, BufferedImage>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BufferedImage>): Boolean {
            return size > MAX_MEMORY_ENTRIES
        }
    }

    private fun addToMemoryCache(key: String, image: BufferedImage) {
        synchronized(memoryCache) { memoryCache[key] = image }
    }

    override fun getBitmap(key: String): BufferedImage? {
        synchronized(memoryCache) { memoryCache[key]?.let { return it } }
        return getBitmapFromDisk(key)?.also { addToMemoryCache(key, it) }
    }

    private fun getBitmapFromDisk(key: String): BufferedImage? {
        try {
            val file = File(dataDirPath, key)
            if (!file.exists()) return null
            return ImageIO.read(file)
        } catch (e: Exception) {
            Log.e("BitmapCache", e)
        }
        return null
    }

    override fun saveBitmap(project: String, image: BufferedImage, saveToMemory: Boolean): String {
        // 统一使用 "/" 分隔，避免 File.separator 泄漏进 key 导致跨平台/清理困难。
        // 读取侧 File(dataDirPath, key) 同时兼容新旧 key（Windows 下 "/" 与 "\" 均可解析）。
        val key = "$project/${UUID.randomUUID()}"

        if (saveToMemory) addToMemoryCache(key, image)

        try {
            val file = File(dataDirPath, key)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            ImageIO.write(image, "png", file)
        } catch (e: Throwable) {
            Log.e("BitmapCache", e)
        }
        return key
    }

    override fun saveImageToPath(image: BufferedImage, file: File) {
        ImageIO.write(image, "png", file)
    }

    override fun gcProjectFiles(projectKey: String, referencedKeys: Set<String>): Int {
        val referencedNames = referencedKeys.map { it.substringAfterLast('/') }.toSet()
        val dir = File(dataDirPath, projectKey)
        val files = dir.listFiles() ?: return 0
        var deleted = 0
        for (f in files) {
            if (!f.isFile || f.name == ".project" || f.name in referencedNames) continue
            try {
                if (f.delete()) deleted++
            } catch (e: Exception) {
                Log.e("BitmapCache", e)
            }
        }
        synchronized(memoryCache) {
            memoryCache.keys.removeAll { it.startsWith("$projectKey/") && it.substringAfterLast('/') !in referencedNames }
        }
        return deleted
    }
}
