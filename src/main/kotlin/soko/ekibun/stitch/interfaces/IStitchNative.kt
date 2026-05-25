package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.OffsetResult
import soko.ekibun.stitch.Stitch

interface IStitchNative {
    fun computeOffset(
        img0: Stitch.StitchInfo, img1: Stitch.StitchInfo,
        fullTransform: Boolean, edgeEnhance: Boolean
    ): OffsetResult
}
