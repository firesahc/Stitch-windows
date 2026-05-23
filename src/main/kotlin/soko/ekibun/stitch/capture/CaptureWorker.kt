package soko.ekibun.stitch.capture

import soko.ekibun.stitch.App
import soko.ekibun.stitch.Stitch

class CaptureWorker(private val projectKey: String) {
    private val screenCapture = ScreenCapture()
    @Volatile var running = false

    fun start() {
        running = true
    }

    fun stop() {
        running = false
    }

    fun capture() {
        try {
            val image = screenCapture.captureFullScreen()
            val project = App.getProject(projectKey)
            val key = App.bitmapCache.saveBitmap(projectKey, image)
            val info = Stitch.StitchInfo(key, image.width, image.height)
            project.updateUndo { project.stitchInfo.add(info) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
