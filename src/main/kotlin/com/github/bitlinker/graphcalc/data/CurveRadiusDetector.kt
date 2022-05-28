package com.github.bitlinker.graphcalc.data

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

internal class CurveRadiusDetector {
    fun detect(
        curve: Curve,
        startX: Double,
        endX: Double,
        radiusMoveFactor: Double,
        coordinatesMoveFactor: Double,
        radiusSteps: Int,
        coordinateSteps: Int,
    ): CircleEstimation {
        val estimatingCurve = curve.slice(startX, endX)

        val halfInterval = (endX - startX) * 0.5
        val middleX = startX + halfInterval
        val weightsCurve = estimatingCurve.modify {
            val pointMiddleCloseness = 1.0 - (abs(it.x - middleX) / halfInterval)
            it.copy(y = 0.8 + 0.2 * pointMiddleCloseness)
        }

        // First estimation: 3 points on the curve
        val p1 = estimatingCurve.points.first()
        val p2 = estimatingCurve.points[estimatingCurve.points.size / 2]
        val p3 = estimatingCurve.points.last()
        var curEstimation = circleFrom3Points(p1, p2, p3)

        // Move estimated circle in this area:
        val radiusMin = curEstimation.radius / radiusMoveFactor
        val radiusMax = curEstimation.radius * radiusMoveFactor
        val xDelta = abs(p1.x - p2.x)
        val yDelta = abs(p1.y - p2.y)
        val xMin = curEstimation.center.x - xDelta * coordinatesMoveFactor
        val xMax = curEstimation.center.x + xDelta * coordinatesMoveFactor
        val yMin = curEstimation.center.y - yDelta * coordinatesMoveFactor
        val yMax = curEstimation.center.y + yDelta * coordinatesMoveFactor

        var bestEstimation = curEstimation
        var bestError = calcEstimationAbsoluteError(estimatingCurve, weightsCurve, curEstimation, Double.MAX_VALUE)!!

        for (radiusStep in 0 until radiusSteps) {
            val radius = lerp(radiusMin, radiusMax, radiusStep.toDouble() / radiusSteps)
            for (xStep in 0 until coordinateSteps) {
                val x = lerp(xMin, xMax, xStep.toDouble() / coordinateSteps)
                for (yStep in 0 until coordinateSteps) {
                    val y = lerp(yMin, yMax, yStep.toDouble() / coordinateSteps)
                    curEstimation = CircleEstimation(radius, Point(x, y))
                    val error = calcEstimationAbsoluteError(estimatingCurve, weightsCurve, curEstimation, bestError)
                    if (error != null) {
                        bestError = error
                        bestEstimation = curEstimation
                    }
                }
            }
        }
        return bestEstimation
    }
}

private fun calcEstimationAbsoluteError(curve: Curve, weights: Curve, estimation: CircleEstimation, bestError: Double): Double? {
    var error = 0.0
    curve.points.forEachIndexed { index, point ->
        val dx = estimation.center.x - point.x
        val dy = estimation.center.y - point.y
        val pointError = sqrt(dx * dx + dy * dy) - estimation.radius
        error += (abs(pointError) * weights.points[index].y)
        if (error >= bestError) return null
    }
    return error
}

private fun circleFrom3Points(p1: Point, p2: Point, p3: Point): CircleEstimation {
    val offset = p2.x.pow(2) + p2.y.pow(2)
    val bc = (p1.x.pow(2) + p1.y.pow(2) - offset) / 2.0
    val cd = (offset - p3.x.pow(2) - p3.y.pow(2)) / 2.0
    val det = (p1.x - p2.x) * (p2.y - p3.y) - (p2.x - p3.x) * (p1.y - p2.y)
    if (abs(det) < 0.0000001) {
        throw IllegalArgumentException("Points are on the line")
    }
    val idet = 1.0 / det
    val cx = (bc * (p2.y - p3.y) - cd * (p1.y - p2.y)) * idet
    val cy = (cd * (p1.x - p2.x) - bc * (p2.x - p3.x)) * idet
    val radius = sqrt((p2.x - cx).pow(2) + (p2.y - cy).pow(2))
    return CircleEstimation(radius, Point(cx, cy))
}

private fun lerp(a: Double, b: Double, factor: Double): Double {
    return a * (1.0 - factor) + b * factor
}