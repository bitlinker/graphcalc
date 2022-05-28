package com.github.bitlinker.graphcalc.draw

import com.github.bitlinker.graphcalc.data.CircleEstimation
import com.github.bitlinker.graphcalc.data.Curve
import com.github.bitlinker.graphcalc.data.Point
import java.awt.Color
import java.awt.Graphics

internal class Plotter(
    private val graphics: Graphics,
    private val canvasY: Int,
    private val canvasWidth: Int,
    private val canvasHeight: Int,
) {
    val mapper = PointMapper(canvasY, canvasWidth, canvasHeight)

    fun drawHorizontalLine(y: Double, color: Color) {
        graphics.color = color
        val screenY = mapper.mapPoint(Point(0.0, y)).y.toInt()
        graphics.drawLine(0, screenY, canvasWidth, screenY)
    }

    fun drawVerticalLine(x: Double, color: Color) {
        graphics.color = color
        val screenX = mapper.mapPoint(Point(x, 0.0)).x.toInt()
        graphics.drawLine(screenX, canvasY, screenX, canvasY + canvasHeight)
    }

    fun drawCurve(curve: Curve, color: Color) {
        graphics.color = color
        curve.points.scan(null) { prevMappedPoint: Point?, point ->
            val mappedPoint = mapper.mapPoint(point)
            if (prevMappedPoint != null) {
                graphics.drawLine(
                    prevMappedPoint.x.toInt(),
                    prevMappedPoint.y.toInt(),
                    mappedPoint.x.toInt(),
                    mappedPoint.y.toInt()
                )
            }
            mappedPoint
        }
    }

    fun drawCircle(circle: CircleEstimation) {
        val mappedCenter = mapper.mapPoint(circle.center)
        val radius = mapper.mapRadius(circle.radius)

        // Center
        graphics.color = Color.MAGENTA
        graphics.drawOval(
            mappedCenter.x.toInt(),
            mappedCenter.y.toInt(),
            1,
            1,
        )

        // Radius
        graphics.color = Color.ORANGE
        graphics.drawOval(
            (mappedCenter.x - radius).toInt(),
            (mappedCenter.y - radius).toInt(),
            (2 * radius).toInt(),
            (2 * radius).toInt()
        )
    }
}