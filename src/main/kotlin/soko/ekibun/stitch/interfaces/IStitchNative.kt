package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.StitchNative

interface IStitchNative {
    fun computeOffset(
        img0: Stitch.StitchInfo, img1: Stitch.StitchInfo,
        fullTransform: Boolean, edgeEnhance: Boolean
    ): StitchNative.OffsetResult
}
