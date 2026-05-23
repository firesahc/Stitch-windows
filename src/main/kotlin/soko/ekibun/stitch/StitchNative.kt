package soko.ekibun.stitch

import kotlin.math.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import org.opencv.features2d.*
import org.opencv.calib3d.Calib3d
import java.awt.image.BufferedImage

object StitchNative {

    data class OffsetResult(val dx: Float, val dy: Float, val drot: Float, val dscale: Float)

    private fun logError(e: Exception) {
        System.err.println("[StitchNative] ${e.message}")
        e.printStackTrace()
    }

    fun computeOffset(img0: Stitch.StitchInfo, img1: Stitch.StitchInfo,
                      homo: Boolean, diff: Boolean): OffsetResult {
        return if (homo) {
            val bmp0 = getClipBitmap(img0) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val bmp1 = getClipBitmap(img1) ?: return OffsetResult(0f, 0f, 0f, 1f)
            computeHomography(bmp0, bmp1, diff) ?: OffsetResult(0f, 0f, 0f, 1f)
        } else {
            val bmp0 = getClipBitmap(img0, img1) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val bmp1 = getClipBitmap(img1, img0) ?: return OffsetResult(0f, 0f, 0f, 1f)
            val (dx, dy) = computePhaseCorrelate(bmp0, bmp1, diff)
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

    private fun computeHomography(img0: BufferedImage, img1: BufferedImage, diff: Boolean): OffsetResult? {
        return try {
            val mat0 = bufferedImageToMat(img0)
            val mat1 = bufferedImageToMat(img1)

            val gray0 = Mat()
            val gray1 = Mat()
            Imgproc.cvtColor(mat0, gray0, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_RGBA2GRAY)

            val proc0: Mat
            val proc1: Mat

            if (diff && mat0.size() == mat1.size()) {
                val kernelData = intArrayOf(1, 1, 1, 1, -8, 1, 1, 1, 1)
                val kernel = Mat(3, 3, CvType.CV_32S)
                kernel.put(0, 0, kernelData)

                val lap0 = Mat()
                val lap1 = Mat()
                Imgproc.filter2D(gray0, lap0, CvType.CV_32F, kernel)
                Imgproc.filter2D(gray1, lap1, CvType.CV_32F, kernel)

                val diffMat = Mat()
                Core.absdiff(lap0, lap1, diffMat)

                val mask = Mat()
                Core.compare(diffMat, Scalar(0.0), mask, Core.CMP_GT)

                proc0 = Mat()
                proc1 = Mat()
                lap0.copyTo(proc0, mask)
                lap1.copyTo(proc1, mask)
            } else {
                proc0 = gray0
                proc1 = gray1
            }

            val sift = SIFT.create()
            val keypoints0 = MatOfKeyPoint()
            val keypoints1 = MatOfKeyPoint()
            val desc0 = Mat()
            val desc1 = Mat()
            sift.detectAndCompute(proc0, Mat(), keypoints0, desc0)
            sift.detectAndCompute(proc1, Mat(), keypoints1, desc1)

            val kp0 = keypoints0.toArray()
            val kp1 = keypoints1.toArray()
            if (kp0.size < 10 || kp1.size < 10) return null

            val matcher = FlannBasedMatcher.create()
            val knnMatches = mutableListOf<MatOfDMatch>()
            matcher.knnMatch(desc0, desc1, knnMatches, 2)

            val goodMatches = mutableListOf<DMatch>()
            for (matchGroup in knnMatches) {
                val matches = matchGroup.toArray()
                if (matches.size >= 2 && matches[0].distance < 0.7 * matches[1].distance) {
                    goodMatches.add(matches[0])
                }
            }

            if (goodMatches.size < 10) return null

            val srcPoints = mutableListOf<Point>()
            val dstPoints = mutableListOf<Point>()
            for (match in goodMatches) {
                srcPoints.add(kp0[match.queryIdx].pt)
                dstPoints.add(kp1[match.trainIdx].pt)
            }

            val srcMat = MatOfPoint2f()
            val dstMat = MatOfPoint2f()
            srcMat.fromArray(*srcPoints.toTypedArray())
            dstMat.fromArray(*dstPoints.toTypedArray())

            val homoMask = Mat()
            val homo = Calib3d.findHomography(srcMat, dstMat, Calib3d.RHO, 5.0, homoMask)

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
        }
    }

    private fun computePhaseCorrelate(img0: BufferedImage, img1: BufferedImage, diff: Boolean): Pair<Float, Float> {
        return try {
            val mat0 = bufferedImageToMat(img0)
            val mat1 = bufferedImageToMat(img1)

            if (mat0.size() != mat1.size()) return 0f to 0f

            val gray0 = Mat()
            val gray1 = Mat()
            Imgproc.cvtColor(mat0, gray0, Imgproc.COLOR_RGBA2GRAY)
            Imgproc.cvtColor(mat1, gray1, Imgproc.COLOR_RGBA2GRAY)

            val f64_0 = Mat()
            val f64_1 = Mat()
            gray0.convertTo(f64_0, CvType.CV_64F)
            gray1.convertTo(f64_1, CvType.CV_64F)

            val proc0: Mat
            val proc1: Mat

            if (diff) {
                val kernelData = intArrayOf(1, 1, 1, 1, -8, 1, 1, 1, 1)
                val kernel = Mat(3, 3, CvType.CV_32S)
                kernel.put(0, 0, kernelData)

                val lap0 = Mat()
                val lap1 = Mat()
                Imgproc.filter2D(f64_0, lap0, CvType.CV_64F, kernel)
                Imgproc.filter2D(f64_1, lap1, CvType.CV_64F, kernel)

                val diffMat = Mat()
                Core.absdiff(lap0, lap1, diffMat)

                val mask = Mat()
                Core.compare(diffMat, Scalar(0.0), mask, Core.CMP_GT)

                proc0 = Mat()
                proc1 = Mat()
                lap0.copyTo(proc0, mask)
                lap1.copyTo(proc1, mask)
            } else {
                proc0 = f64_0
                proc1 = f64_1
            }

            val shift = Imgproc.phaseCorrelate(proc1, proc0)
            shift.x.toFloat() to shift.y.toFloat()
        } catch (e: Exception) {
            logError(e)
            0f to 0f
        }
    }
}
