package com.github.bitlinker.graphcalc.draw

import com.github.bitlinker.graphcalc.data.Point
import java.lang.Double.min

internal class PointMapper(
    private val canvasY: Int,
    private val canvasWidth: Int,
    private val canvasHeight: Int
) {
    private var offsetX = 0.0
    private var offsetY = 0.0

    private var scaleX = 1.0
    private var scaleY = 1.0

    fun setUniformScale(minX: Double, maxX: Double, minY: Double, maxY: Double) {
        scaleX = min(canvasWidth / (maxX - minX), canvasHeight / (maxY - minY))
        scaleY = scaleX
        offsetX = minX
        offsetY = maxY
    }

    fun copyScaleAndOffsetX(other: PointMapper) {
        scaleX = other.scaleX
        offsetX = other.offsetX
    }

    fun calcScaleAndCenterY(minY: Double, maxY: Double) {
        scaleY = canvasHeight / (maxY - minY)
        offsetY = maxY
    }

    fun mapPoint(point: Point): Point {
        return Point(
            x = (point.x - offsetX) * scaleX,
            y = canvasY + (point.y - offsetY) * -scaleY,
        )
    }

    fun mapRadius(radius: Double): Double {
        require(scaleX == scaleY)
        return radius * scaleX
    }
}