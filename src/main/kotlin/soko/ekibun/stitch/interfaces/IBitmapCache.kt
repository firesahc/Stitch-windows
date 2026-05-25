package soko.ekibun.stitch.interfaces

import java.awt.image.BufferedImage
import java.io.File

interface IBitmapCache {
    fun getBitmap(key: String): BufferedImage?
    fun saveBitmap(project: String, image: BufferedImage, saveToMemory: Boolean = true): String
    fun saveImageToPath(image: BufferedImage, file: File)
}
