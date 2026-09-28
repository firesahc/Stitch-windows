package soko.ekibun.stitch.ui

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

data class DecodedImage(val image: BufferedImage, val source: File)

data class DecodeResult(
    val decoded: List<DecodedImage>,
    val failedNames: List<String>,
    val unsupportedNames: List<String>,
)

/**
 * 文件→图片解码（纯函数，不碰 UI、不弹框；提示由调用方经 Dialogs 处理）。
 * [EditorService.addImages] 与 [MainView] 导入共用，消除两份重复解码逻辑。
 */
object ImageImport {
    fun decode(files: List<File>): DecodeResult {
        val supported = EditorService.SUPPORTED_EXTENSIONS
        val imageFiles = files.filter { it.isFile && supported.contains(it.extension.lowercase()) }
        val unsupportedNames = files
            .filter { !it.isFile || !supported.contains(it.extension.lowercase()) }
            .map { it.name }
        val decoded = mutableListOf<DecodedImage>()
        val failed = mutableListOf<String>()
        for (f in imageFiles) {
            try {
                val bufferedImage = ImageIO.read(f)
                if (bufferedImage == null) failed.add(f.name)
                else decoded.add(DecodedImage(bufferedImage, f))
            } catch (e: Exception) {
                failed.add(f.name)
            }
        }
        return DecodeResult(decoded, failed, unsupportedNames)
    }
}
