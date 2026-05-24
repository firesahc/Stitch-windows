package soko.ekibun.stitch

import soko.ekibun.stitch.util.GraphicsHelper
import java.awt.AlphaComposite
import java.awt.Graphics2D

object Renderer {
    fun drawToCanvas(
        g: Graphics2D,
        drawMask: Boolean,
        maskColor: Int,
        overPaintColor: Int,
        gradientPaintColor: Int,
        stitchInfo: List<Stitch.StitchInfo>,
        selected: Set<String>,
        bitmapCache: BitmapCache,
    ) {
        val srcRange = IntArray(4)
        for (i in 0 until stitchInfo.size) {
            val it = stitchInfo.getOrNull(i) ?: continue
            srcRange[0] = (it.width * it.xa).toInt()
            srcRange[1] = (it.height * it.ya).toInt()
            srcRange[2] = (it.width * it.xb).toInt()
            srcRange[3] = (it.height * it.yb).toInt()
            val dstLeft = it.cx + (srcRange[0] - it.width / 2f) * it.scale
            val dstTop = it.cy + (srcRange[1] - it.height / 2f) * it.scale
            val dstRight = it.cx + (srcRange[2] - it.width / 2f) * it.scale
            val dstBottom = it.cy + (srcRange[3] - it.height / 2f) * it.scale

            val bmp = bitmapCache.getBitmap(it.imageKey)

            // Guard: skip images with degenerate coordinates (NaN/Inf)
            if (!dstLeft.isFinite() || !dstTop.isFinite() ||
                !dstRight.isFinite() || !dstBottom.isFinite()) continue
            val dw = (dstRight - dstLeft).toDouble()
            val dh = (dstBottom - dstTop).toDouble()
            // Guard: skip zero-size images
            if (dw <= 0.0 || dh <= 0.0) continue

            val oldTransform = g.transform
            try {
                if (it.rot != 0f) {
                    g.rotate(Math.toRadians(it.rot.toDouble()), it.cx.toDouble(), it.cy.toDouble())
                }

                if (bmp != null) {
                    // Step 1: DST_OUT - erase existing canvas at gradient region
                    it.shaderPts?.let { pts ->
                        try {
                            val shader = GraphicsHelper.makeLinearGradient(
                                pts[0], pts[1], pts[2], pts[3],
                                0x00000000.toInt(), 0xffffffff.toInt()
                            )
                            g.setComposite(AlphaComposite.DstOut)
                            g.setPaint(shader)
                            g.fillRect(dstLeft.toInt(), dstTop.toInt(), dw.toInt(), dh.toInt())
                        } catch (_: Exception) {
                            // gradient render failure is non-fatal
                        }
                    }

                    // Step 2: DST_OVER - selection mask overlay
                    if (drawMask && selected.contains(it.imageKey)) {
                        g.setComposite(AlphaComposite.DstOver)
                        g.setColor(GraphicsHelper.intToColor(maskColor))
                        g.fillRect(dstLeft.toInt(), dstTop.toInt(), dw.toInt(), dh.toInt())
                    }

                    // Step 3: DST_OVER - draw image UNDER existing content
                    g.setComposite(AlphaComposite.DstOver)
                    g.drawImage(bmp,
                        dstLeft.toInt(), dstTop.toInt(), (dstLeft + dw).toInt(), (dstTop + dh).toInt(),
                        srcRange[0], srcRange[1], srcRange[2], srcRange[3], null)
                }
            } finally {
                g.setTransform(oldTransform)
                g.setComposite(AlphaComposite.SrcOver)
            }
        }
    }
}
