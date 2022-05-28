package com.github.bitlinker.graphcalc.data

import kotlin.math.PI
import kotlin.math.exp


internal data class Curve(
    val points: List<Point>
)

internal fun Curve.slice(startTime: Double, endTime: Double): Curve {
    return Curve(points.filter { it.x in startTime..endTime })
}

internal fun Curve.gaussSmooth(kernelRadius: Int): Curve {
    val core = gaussCore(kernelRadius)
    val xOffset = points[kernelRadius].x
    val filteredPoints = points.mapIndexed { index, point ->
        var averageY = 0.0
        for (i in core.indices) {
            val pointIndex = (index + i - kernelRadius).coerceIn(points.indices)
            averageY += (points[pointIndex].y * core[i])
        }
        Point(point.x - xOffset, averageY)
    }
    return Curve(filteredPoints)
}

internal fun Curve.derivative(): Curve {
    var prevPoint: Point? = null
    val newPoints = points.mapNotNull {
        val pp = prevPoint
        prevPoint = it
        if (pp != null) {
            Point(pp.x, it.y - pp.y)
        } else null
    }
    return Curve(newPoints)
}

internal fun Curve.modify(modifier: (Point) -> Point): Curve {
    return Curve(points.map(modifier))
}

internal data class CurveInfo(
    val maxX: Double,
    val minX: Double,
    val maxY: Double,
    val minY: Double,
    val deltaY: Double,
    val deltaX: Double,
)

internal val Curve.info: CurveInfo
    get() {
        val minX = points.minOf { it.x }
        val maxX = points.maxOf { it.x }
        val minY = points.minOf { it.y }
        val maxY = points.maxOf { it.y }
        return CurveInfo(
            minX = minX,
            maxX = maxX,
            minY = minY,
            maxY = maxY,
            deltaX = maxX - minX,
            deltaY = maxY - minY,
        )
    }


private fun gaussCore(kernelRadius: Int): DoubleArray {
    val sigma = kernelRadius / 2.0
    val s = 2.0 * sigma * sigma
    var sum = 0.0
    val core = DoubleArray(kernelRadius * 2 + 1)
    for (x in -kernelRadius..kernelRadius) {
        val value = (exp(-(x * x) / s)) / (PI * s)
        core[x + kernelRadius] = value
        sum += value
    }
    for (i in core.indices) {
        core[i] /= sum
    }
    return core
}