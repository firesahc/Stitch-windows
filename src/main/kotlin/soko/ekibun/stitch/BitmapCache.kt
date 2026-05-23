package soko.ekibun.stitch

import java.awt.image.BufferedImage
import java.io.File
import java.util.*
import javax.imageio.ImageIO

object BitmapCache {
    private const val MAX_MEMORY_ENTRIES = 256

    private val memoryCache = object : LinkedHashMap<String, BufferedImage>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, BufferedImage>): Boolean {
            return size > MAX_MEMORY_ENTRIES
        }
    }

    private fun addToMemoryCache(key: String, image: BufferedImage) {
        synchronized(memoryCache) { memoryCache[key] = image }
    }

    fun getBitmap(key: String): BufferedImage? {
        synchronized(memoryCache) { memoryCache[key]?.let { return it } }
        return getBitmapFromDisk(key)?.also { addToMemoryCache(key, it) }
    }

    private fun getBitmapFromDisk(key: String): BufferedImage? {
        try {
            val file = File(App.dataDirPath, key)
            if (!file.exists()) return null
            return ImageIO.read(file)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    private var lastKey: String = ""

    fun saveBitmap(project: String, image: BufferedImage, saveToMemory: Boolean = true): String {
        var key = project + File.separator + System.currentTimeMillis().toString(16)
        if (lastKey == key) {
            Thread.sleep(1)
            return saveBitmap(project, image, saveToMemory)
        }
        lastKey = key

        if (saveToMemory) addToMemoryCache(key, image)

        try {
            val file = File(App.dataDirPath, key)
            if (!file.exists()) {
                file.parentFile?.mkdirs()
                file.createNewFile()
            }
            ImageIO.write(image, "png", file)
        } catch (e: Throwable) {
            e.printStackTrace()
        }
        return key
    }

    fun saveImageToPath(image: BufferedImage, file: File) {
        ImageIO.write(image, "png", file)
    }
}
