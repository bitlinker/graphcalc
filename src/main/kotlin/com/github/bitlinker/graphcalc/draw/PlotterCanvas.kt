package com.github.bitlinker.graphcalc.draw

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

internal class PlotterCanvas(
    width: Int, height: Int, numPlotters: Int
) {
    private val image = BufferedImage(width, height * numPlotters, BufferedImage.TYPE_INT_RGB)
    private val graphics = image.graphics

    init {
        graphics.color = Color.BLACK
        graphics.clearRect(0, 0, image.width, image.height)
    }

    val plotters = (0 until numPlotters).map { index ->
        Plotter(
            graphics = graphics,
            canvasY = height * index,
            canvasWidth = width,
            canvasHeight = height,
        )
    }

    fun drawHorizontalLine(y: Double, color: Color) {
        plotters.forEach {
            it.drawHorizontalLine(y, color)
        }
    }

    fun drawVerticalLine(x: Double, color: Color) {
        plotters.forEach {
            it.drawVerticalLine(x, color)
        }
    }

    fun save(filePath: String) {
        ImageIO.write(image, "png", File(filePath))
    }
}