package soko.ekibun.stitch.interfaces

import soko.ekibun.stitch.Stitch

/** 配准校验门面接口。原 StitchService 具体类直接暴露，AppContext 改依赖此接口。 */
interface IStitchService {
    fun combine(
        fullTransform: Boolean,
        edgeEnhance: Boolean,
        img0: Stitch.StitchInfo,
        img1: Stitch.StitchInfo
    ): Stitch.StitchInfo?
}
