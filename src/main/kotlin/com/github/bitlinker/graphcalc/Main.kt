package com.github.bitlinker.graphcalc

import com.github.bitlinker.graphcalc.data.*
import com.github.bitlinker.graphcalc.draw.PlotterCanvas
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.required
import java.awt.Color
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val parser = ArgParser("graphcalc")
    val dataInputFilePath by parser.option(ArgType.String, fullName = "data", description = "Входные данные: текстовый CSV файл, точки (время, напряжение)").required()
    val dataManualStartTime by parser.option(
        ArgType.Double,
        fullName = "dataManualStartTime",
        description = "Входные данные: Заданное вручную время старта определяемого участка, единицы времени"
    )
    val dataManualEndTime by parser.option(
        ArgType.Double,
        fullName = "dataManualEndTime",
        description = "Входные данные: Заданное вручную время конца определяемого участка, единицы времени"
    )
    val preprocessorSmoothSamples by parser.option(
        ArgType.Int,
        fullName = "preprocessorSmoothSamples",
        description = "Препроцессор: Число сэмплов для сглаживания исходной кривой, шт."
    ).default(80)
    val preprocessorDerivativeSmoothSamples by parser.option(
        ArgType.Int,
        fullName = "preprocessorDerivativeSmoothSamples",
        description = "Препроцессор: Число сэмплов для сглаживания производных кривой, шт."
    ).default(20)

    val phaseDetectorDerivativePeakDetectionMultiplier by parser.option(
        ArgType.Double,
        fullName = "phaseDetectorDerivativePeakDetectionMultiplier",
        description = "Детектор фазы: значение для определения пика 2-й производной, доля от максимального значения, [0..1]"
    ).default(0.1)
    val phaseDetectorTimeEnlargeMultiplier by parser.option(
        ArgType.Double,
        fullName = "phaseDetectorTimeEnlargeMultiplier",
        description = "Детектор фазы: множитель для увеличения найденного диапазона времени (0 - без увеличения, 1 - увеличение в 2 раза) [0..бесконечность]"
    ).default(0.3)
    val radiusDetectorRadiusChangeFactor by parser.option(
        ArgType.Double,
        fullName = "radiusDetectorRadiusChangeFactor",
        description = "Детектор радиуса: множитель для диапазона изменения радиуса исходной окружности [исходный радиус / значение .. исходный радиус * значение]"
    )
        .default(2.0)
    val radiusDetectorCoordinateChangeFactor by parser.option(
        ArgType.Double,
        fullName = "radiusDetectorRadiusCoordinateFactor",
        description = "Детектор радиуса: множитель для диапазона изменения координат исходной окружности [исходная координата / значение .. исходная координата  * значение]"
    )
        .default(2.0)
    val radiusDetectorRadiusSteps by parser.option(ArgType.Int, fullName = "radiusDetectorRadiusSteps", description = "Детектор радиуса: число шагов для изменения радиуса, шт.")
        .default(200)
    val radiusDetectorCoordinateSteps by parser.option(
        ArgType.Int,
        fullName = "radiusDetectorCoordinateSteps",
        description = "Детектор радиуса: число шагов для изменения координат, шт."
    )
        .default(200)
    val imageWidth by parser.option(ArgType.Int, fullName = "imageWidth", description = "График: Ширина картинки, пикс").default(1024)
    val imageFileName by parser.option(ArgType.String, fullName = "imageFileName", description = "График: Имя файла картинки (png), файл").default("out.png")
    parser.parse(args)

    val curveReader = CurveReader()
    val curve = curveReader.read(dataInputFilePath).getOrElse {
        fail("Не могу прочитать исходные данные: $dataInputFilePath", it)
    }

    val curvePreprocessor = CurvePreprocessor()
    val curvePhaseDetector = CurvePhaseDetector()
    val curveRadiusDetector = CurveRadiusDetector()

    val preprocessorResult = curvePreprocessor.preprocess(
        curve = curve,
        smoothSamples = preprocessorSmoothSamples,
        derivativeSmoothSamples = preprocessorDerivativeSmoothSamples
    )

    val curveInfo = curve.info
    val estimationTimeRange = if (dataManualStartTime != null && dataManualEndTime != null) {
        dataManualStartTime!! to dataManualEndTime!!
    } else {
        curvePhaseDetector.detect(
            data = preprocessorResult,
            derivativePeakDetectionMultiplier = phaseDetectorDerivativePeakDetectionMultiplier,
            enlargeFactor = phaseDetectorTimeEnlargeMultiplier,
        )
    }

    val estimationCurve = curve.slice(estimationTimeRange.first, estimationTimeRange.second)
    val estimation = curveRadiusDetector.detect(
        curve = estimationCurve,
        startX = estimationTimeRange.first,
        endX = estimationTimeRange.second,
        radiusMoveFactor = radiusDetectorRadiusChangeFactor,
        coordinatesMoveFactor = radiusDetectorCoordinateChangeFactor,
        radiusSteps = radiusDetectorRadiusSteps,
        coordinateSteps = radiusDetectorCoordinateSteps,
    )

    // Draw & print result
    println("Участок для определения радиуса: [${estimationTimeRange.first}..${estimationTimeRange.second}]")
    println("Радиус усредненной кривой: ${estimation.radius}")

    val plotterCanvas = PlotterCanvas(width = imageWidth, height = imageWidth, numPlotters = 3)
    val curvePlotter = plotterCanvas.plotters[0]
    val derivative1stCanvas = plotterCanvas.plotters[1]
    val derivative2ndCanvas = plotterCanvas.plotters[2]
    curvePlotter.mapper.setUniformScale(
        minX = curveInfo.minX,
        maxX = curveInfo.maxX,
        minY = curveInfo.minY,
        maxY = curveInfo.maxY,
    )
    val smooth1stDerivativeInfo = preprocessorResult.smooth1stDerivative.info
    val smooth2ndDerivativeInfo = preprocessorResult.smooth2ndDerivative.info
    derivative1stCanvas.mapper.apply {
        copyScaleAndOffsetX(curvePlotter.mapper)
        calcScaleAndCenterY(smooth1stDerivativeInfo.minY, smooth1stDerivativeInfo.maxY)
    }
    derivative2ndCanvas.mapper.apply {
        copyScaleAndOffsetX(curvePlotter.mapper)
        calcScaleAndCenterY(smooth2ndDerivativeInfo.minY, smooth2ndDerivativeInfo.maxY)
    }
    plotterCanvas.drawHorizontalLine(0.0, Color.GRAY)
    plotterCanvas.drawVerticalLine(estimationTimeRange.first, Color.DARK_GRAY)
    plotterCanvas.drawVerticalLine(estimationTimeRange.second, Color.DARK_GRAY)
    curvePlotter.drawCurve(curve, Color.YELLOW)
    curvePlotter.drawCurve(preprocessorResult.smoothCurve, Color.GREEN)
    curvePlotter.drawCurve(estimationCurve, Color.RED)
    curvePlotter.drawCircle(estimation)
    derivative1stCanvas.drawCurve(preprocessorResult.smooth1stDerivative, Color.BLUE)
    derivative2ndCanvas.drawCurve(preprocessorResult.smooth2ndDerivative, Color.MAGENTA)
    plotterCanvas.save(imageFileName)
}

private fun fail(message: String, t: Throwable? = null): Nothing {
    println(message)
    t?.printStackTrace()
    exitProcess(-1)
}