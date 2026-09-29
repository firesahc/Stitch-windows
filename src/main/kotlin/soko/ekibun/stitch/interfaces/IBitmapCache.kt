package soko.ekibun.stitch.interfaces

import java.awt.image.BufferedImage
import java.io.File

interface IBitmapCache {
    fun getBitmap(key: String): BufferedImage?
    fun saveBitmap(project: String, image: BufferedImage, saveToMemory: Boolean = true): String
    fun saveImageToPath(image: BufferedImage, file: File)
    /** 删除项目下未被引用的图片文件（含内存驱逐），返回删除数；跳过 .project 描述文件。 */
    fun gcProjectFiles(projectKey: String, referencedKeys: Set<String>): Int
}
