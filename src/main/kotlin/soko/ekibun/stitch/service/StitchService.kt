package soko.ekibun.stitch.service

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.interfaces.IStitchNative
import kotlin.math.abs

class StitchService(private val stitchNative: IStitchNative) {
    fun combine(
        fullTransform: Boolean, edgeEnhance: Boolean,
        img0: Stitch.StitchInfo, img1: Stitch.StitchInfo
    ): Stitch.StitchInfo? {
        return try {
            val (dx, dy, drot, dscale) = stitchNative.computeOffset(img0, img1, fullTransform, edgeEnhance)
            if ((dx != 0f || dy != 0f || drot != 0f || dscale != 1f) &&
                abs(dx) < (img1.width + img0.width) / 2 &&
                abs(dy) < (img1.height + img0.height) / 2
            ) {
                img1.clone().also {
                    it.dx = dx
                    it.dy = dy
                    it.drot = drot
                    it.dscale = dscale
                }
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
