package soko.ekibun.stitch.domain

import soko.ekibun.stitch.Stitch
import soko.ekibun.stitch.util.PointF
import soko.ekibun.stitch.util.Rect
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 几何链式计算纯函数。
 *
 * 原为 Stitch.StitchProject.updateInfo() 百行就地变异方法，
 * 抽出后无 IO、无全局状态、可单测。StitchProject 仅做薄委托。
 */
object GeometryCalculator {

    fun updateInfo(stitchInfo: List<Stitch.StitchInfo>): Rect {
        var cx = 0f
        var cy = 0f
        var rot = 0f
        var scale_ = 1f
        var lastPoints = listOf<PointF>()
        var boundLeft = Float.MAX_VALUE
        var boundTop = Float.MAX_VALUE
        var boundRight = Float.MIN_VALUE
        var boundBottom = Float.MIN_VALUE
        var cos = 1f
        var sin = 0f
        stitchInfo.forEachIndexed { i, it ->
            var dx = (it.dx * cos - it.dy * sin) * scale_
            var dy = (it.dy * cos + it.dx * sin) * scale_
            cx = if (i == 0) 0f else cx + dx
            cy = if (i == 0) 0f else cy + dy
            rot += it.drot
            scale_ *= it.dscale
            it.rot = rot
            it.scale = scale_
            it.cx = cx
            it.cy = cy

            cos = cos(it.rot * Math.PI / 180).toFloat()
            sin = sin(it.rot * Math.PI / 180).toFloat()
            val l = it.width * (it.xa - 0.5f) * it.scale
            val t = it.height * (it.ya - 0.5f) * it.scale
            val r = it.width * (it.xb - 0.5f) * it.scale
            val b = it.height * (it.yb - 0.5f) * it.scale
            val points = listOf(
                PointF(cx + l * cos - t * sin, cy + l * sin + t * cos),
                PointF(cx + l * cos - b * sin, cy + l * sin + b * cos),
                PointF(cx + r * cos - t * sin, cy + r * sin + t * cos),
                PointF(cx + r * cos - b * sin, cy + r * sin + b * cos)
            )
            if (it.dx == 0f && it.dy == 0f) {
                dx = -sin
                dy = cos
            }
            val mag2 = dx * dx + dy * dy
            var minV = Float.MAX_VALUE
            var maxV = Float.MIN_VALUE
            for (p in points) {
                val prod = (cx - p.x) * dx + (cy - p.y) * dy
                minV = min(minV, prod)
                maxV = max(maxV, prod)

                if (p.x < boundLeft) boundLeft = p.x
                if (p.y < boundTop) boundTop = p.y
                if (p.x > boundRight) boundRight = p.x
                if (p.y > boundBottom) boundBottom = p.y
            }
            var minO = Float.MAX_VALUE
            var maxO = Float.MIN_VALUE
            for (p in lastPoints) {
                val prod = (cx - p.x) * dx + (cy - p.y) * dy
                minO = min(minO, prod)
                maxO = max(maxO, prod)
            }
            lastPoints = points

            minV = maxOf(minV, minO)
            maxV = maxOf(minV, minOf(maxV, maxO))

            val va = maxV - (maxV - minV) * it.a
            val vb = maxV - (maxV - minV) * it.b - 0.01f * sqrt(mag2)

            it.shaderPts = floatArrayOf(
                cx - (dx * cos + dy * sin) * va / mag2,
                cy - (-dx * sin + dy * cos) * va / mag2,
                cx - (dx * cos + dy * sin) * vb / mag2,
                cy - (-dx * sin + dy * cos) * vb / mag2
            )
        }
        return Rect(boundLeft, boundTop, boundRight, boundBottom)
    }
}
