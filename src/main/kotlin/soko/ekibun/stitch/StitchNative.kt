package soko.ekibun.stitch

import kotlin.math.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.features2d.*
import org.opencv.calib3d.Calib3d
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

object StitchNative {

    data class OffsetResult(val dx: Float, val dy: Float, val drot: Float, val dscale: Float)

    /** Cache of Hanning windows keyed by Size to avoid recomputing identical windows. */
    private val hanningCache = ConcurrentHashMap<Size, Mat>()

    /** Release all cached Hanning windows and clear the cache. */
    fun clearHanningWindowCache() {
        hanningCache.values.forEach { it.release() }
        hanningCache.clear()
    }

    private fun logError(e: Exception) {
        System.err.println("[StitchNative] ${e.message}")
        e.printStackTrace()
    }

    fun computeOffset(img0: Stitch.StitchInfo, img1: Stitch.StitchInfo,
                      fullTransform: Boolean, edgeEnhance: Boolean): OffsetResult {
        return if (fullTransform) {
            val bmp0 = getClipBitmap(img0) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val bmp1 = getClipBitmap(img1) ?: return OffsetResult(0f, 0f, 0f, 1f)
            computeHomography(bmp0, bmp1, edgeEnhance) ?: OffsetResult(0f, 0f, 0f, 1f)
        } else {
            val bmp0 = getClipBitmap(img0, img1) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val bmp1 = getClipBitmap(img1, img0) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val (dx, dy) = computePhaseCorrelate(bmp0, bmp1, edgeEnhance)
            OffsetResult(dx, dy, 0f, 1f)
        }
    }

    private fun getClipBitmap(info: Stitch.StitchInfo): BufferedImage? {
        val left = (info.xa * info.width).toInt()
        val top = (info.ya * info.height).toInt()
        val right = (info.xb * info.width).toInt()
        val bottom = (info.yb * info.height).toInt()
        if (left >= right || top >= bottom) return null
        return App.bitmapCache.getBitmap(info.imageKey)?.let { img ->
            if (left == 0 && right == info.width && top == 0 && bottom == info.height) img
            else {
                val pw = right - left; val ph = bottom - top
                img.getSubimage(left, top, pw, ph)
            }
        }
    }

    private fun getClipBitmap(info0: Stitch.StitchInfo, info1: Stitch.StitchInfo): BufferedImage? {
        val left = (info0.xa * info0.width).toInt()
        val top = (info0.ya * info0.height).toInt()
        val right = (info0.xb * info0.width).toInt()
        val bottom = (info0.yb * info0.height).toInt()
        if (left >= right || top >= bottom) return null
        val w1 = (info1.xb * info1.width).toInt() - (info1.xa * info1.width).toInt()
        val h1 = (info1.yb * info1.height).toInt() - (info1.ya * info1.height).toInt()
        return App.bitmapCache.getBitmap(info0.imageKey)?.let { img ->
            if (left == 0 && right == info0.width && top == 0 && bottom == info0.height
                && info0.width == w1 && info0.height == h1) img
            else {
                val pw = right - left; val ph = bottom - top
                val canvas = BufferedImage(max(info0.width, w1), max(info0.height, h1), BufferedImage.TYPE_INT_ARGB)
                val g = canvas.createGraphics()
                g.drawImage(img, 0, 0, pw, ph, left, top, right, bottom, null)
                g.dispose()
                canvas
            }
        }
    }

    private fun bufferedImageToMat(img: BufferedImage): Mat {
        val w = img.width; val h = img.height
        val mat = Mat(h, w, CvType.CV_8UC4)
        val pixels = IntArray(w * h)
        img.getRGB(0, 0, w, h, pixels, 0, w)
        val data = ByteArray(w * h * 4)
        for (i in pixels.indices) {
            val argb = pixels[i]
            data[i * 4] = ((argb shr 16) and 0xFF).toByte()      // R
            data[i * 4 + 1] = ((argb shr 8) and 0xFF).toByte()   // G
            data[i * 4 + 2] = (argb and 0xFF).toByte()           // B
            data[i * 4 + 3] = ((argb shr 24) and 0xFF).toByte()  // A
        }
        mat.put(0, 0, data)
        return mat
    }

    /** Create a 2D Hanning window for suppressing FFT boundary artifacts.
     *  Results are cached by [Size] to avoid recomputing identical windows. */
    private fun createHanningWindow(size: Size): Mat {
        hanningCache[size]?.let { return it.clone() }
        val cols = size.width.toInt()
        val rows = size.height.toInt()
        val rowWindow = Mat(rows, 1, CvType.CV_64F)
        val colWindow = Mat(1, cols, CvType.CV_64F)
        for (i in 0 until rows) {
            rowWindow.put(i, 0, 0.5 * (1.0 - cos(2.0 * PI * i / (rows - 1))))
        }
        for (j in 0 until cols) {
            colWindow.put(0, j, 0.5 * (1.0 - cos(2.0 * PI * j / (cols - 1))))
        }
        // outer product: window = rowWindow * colWindow  (rows×1 × 1×cols = rows×cols)
        val window = Mat(rows, cols, CvType.CV_64F)
        Core.gemm(rowWindow, colWindow, 1.0, Mat(), 0.0, window)
        rowWindow.release()
        colWindow.release()
        hanningCache[size] = window
        return window.clone()
    }

    private fun computeHomography(img0: BufferedImage, img1: BufferedImage, edgeEnhance: Boolean): OffsetResult? {
        val mat0 = bufferedImageToMat(img0)
        val mat1 = bufferedImageToMat(img1)
        val gray0 = Mat()
        val gray1 = Mat()
        val keypoints0 = MatOfKeyPoint()
        val keypoints1 = MatOfKeyPoint()
        val desc0 = Mat()
        val desc1 = Mat()
        val srcMat = MatOfPoint2f()
        val dstMat = MatOfPoint2f()
        val homoMask = Mat()
        val knnMatches = mutableListOf<MatOfDMatch>()
        var kernel: Mat? = null
        var float0: Mat? = null
        var float1: Mat? = null
        var extraProc0: Mat? = null
        var extraProc1: Mat? = null
        var homo: Mat? = null

        return try {
            Imgproc.cvtColor(mat0, gray0, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_RGBA2GRAY)

            if (edgeEnhance) {
                val kernelData = intArrayOf(1, 1, 1, 1, -8, 1, 1, 1, 1)
                kernel = Mat(3, 3, CvType.CV_32S)
                kernel.put(0, 0, kernelData)

                float0 = Mat()
                float1 = Mat()
                Imgproc.filter2D(gray0, float0, CvType.CV_32F, kernel)
                Imgproc.filter2D(gray1, float1, CvType.CV_32F, kernel)
                // Normalize Laplacian result to full 0-255 range as CV_8U for SIFT,
                // preserving relative edge strength (convertScaleAbs would clip).
                extraProc0 = Mat()
                extraProc1 = Mat()
                Core.normalize(float0, extraProc0, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
                Core.normalize(float1, extraProc1, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8U)
            }

            val proc0 = extraProc0 ?: gray0
            val proc1 = extraProc1 ?: gray1

            // Adaptive thresholds based on geometric mean of image resolution
            val geomRes = sqrt((img0.width * img0.height).toDouble())
            val minKeypoints = max(8, (geomRes / 200.0).toInt())
            val ransacThreshold = max(3.0, geomRes / 400.0)

            val sift = SIFT.create()
            sift.detectAndCompute(proc0, Mat(), keypoints0, desc0)
            sift.detectAndCompute(proc1, Mat(), keypoints1, desc1)

            val kp0 = keypoints0.toArray()
            val kp1 = keypoints1.toArray()
            if (kp0.size < minKeypoints || kp1.size < minKeypoints) return null

            val matcher = FlannBasedMatcher.create()
            matcher.knnMatch(desc0, desc1, knnMatches, 2)

            val goodMatches = mutableListOf<DMatch>()
            for (matchGroup in knnMatches) {
                val matches = matchGroup.toArray()
                if (matches.size >= 2 && matches[0].distance < 0.7 * matches[1].distance) {
                    goodMatches.add(matches[0])
                }
            }

            if (goodMatches.size < minKeypoints) return null

            val srcPoints = mutableListOf<Point>()
            val dstPoints = mutableListOf<Point>()
            for (match in goodMatches) {
                srcPoints.add(kp0[match.queryIdx].pt)
                dstPoints.add(kp1[match.trainIdx].pt)
            }

            srcMat.fromArray(*srcPoints.toTypedArray())
            dstMat.fromArray(*dstPoints.toTypedArray())

            homo = Calib3d.findHomography(srcMat, dstMat, Calib3d.RHO, ransacThreshold, homoMask)

            val hData = DoubleArray(9)
            homo.get(0, 0, hData)

            val dx = hData[2].toFloat()
            val dy = hData[5].toFloat()
            val scale = sqrt(hData[0] * hData[0] + hData[3] * hData[3]).toFloat()
            val rot = atan2(hData[3], hData[0]).toFloat() * (180f / PI.toFloat())

            OffsetResult(dx, dy, rot, scale)
        } catch (e: Exception) {
            logError(e)
            null
        } finally {
            mat0.release()
            mat1.release()
            gray0.release()
            gray1.release()
            keypoints0.release()
            keypoints1.release()
            desc0.release()
            desc1.release()
            srcMat.release()
            dstMat.release()
            homoMask.release()
            kernel?.release()
            float0?.release()
            float1?.release()
            extraProc0?.release()
            extraProc1?.release()
            homo?.release()
            for (m in knnMatches) m.release()
        }
    }

    private fun computePhaseCorrelate(img0: BufferedImage, img1: BufferedImage, edgeEnhance: Boolean): Pair<Float, Float> {
        val mat0 = bufferedImageToMat(img0)
        val mat1 = bufferedImageToMat(img1)
        val gray0 = Mat()
        val gray1 = Mat()
        val f64_0 = Mat()
        val f64_1 = Mat()
        var kernel: Mat? = null
        var extraProc0: Mat? = null
        var extraProc1: Mat? = null
        var window: Mat? = null

        return try {
            if (mat0.size() != mat1.size()) return 0f to 0f

            Imgproc.cvtColor(mat0, gray0, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_RGBA2GRAY)

            gray0.convertTo(f64_0, CvType.CV_64F)
            gray1.convertTo(f64_1, CvType.CV_64F)

            if (edgeEnhance) {
                val kernelData = intArrayOf(1, 1, 1, 1, -8, 1, 1, 1, 1)
                kernel = Mat(3, 3, CvType.CV_32S)
                kernel.put(0, 0, kernelData)

                extraProc0 = Mat()
                extraProc1 = Mat()
                Imgproc.filter2D(f64_0, extraProc0, CvType.CV_64F, kernel)
                Imgproc.filter2D(f64_1, extraProc1, CvType.CV_64F, kernel)
            }

            val proc0 = extraProc0 ?: f64_0
            val proc1 = extraProc1 ?: f64_1

            // Hanning window suppresses FFT boundary artifacts from padding
            window = createHanningWindow(proc0.size())
            Core.multiply(proc0, window, proc0)
            Core.multiply(proc1, window, proc1)

            val shift = Imgproc.phaseCorrelate(proc1, proc0)
            shift.x.toFloat() to shift.y.toFloat()
        } catch (e: Exception) {
            logError(e)
            0f to 0f
        } finally {
            mat0.release()
            mat1.release()
            gray0.release()
            gray1.release()
            f64_0.release()
            f64_1.release()
            kernel?.release()
            extraProc0?.release()
            extraProc1?.release()
            window?.release()
        }
    }
}
