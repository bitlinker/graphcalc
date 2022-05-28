package com.github.bitlinker.graphcalc.data

import kotlin.math.abs

internal class CurvePhaseDetector {
    fun detect(data: PreprocessorResult, derivativePeakDetectionMultiplier: Double, enlargeFactor: Double): Pair<Double, Double> {
        val info2ndDerivative = data.smooth2ndDerivative.info
        val limit2ndDerivative = abs(info2ndDerivative.deltaY * derivativePeakDetectionMultiplier)
        val phase0end = data.smooth2ndDerivative.points.find { it.y > limit2ndDerivative }!!

        val max2 = info2ndDerivative.maxY
        val d2maxPoint = data.smooth2ndDerivative.points.find { it.y == max2 }!!
        val phase0start = d2maxPoint.x

        val startX = phase0end.x
        @Suppress("UnnecessaryVariable") val endX = phase0start
        val enlarge = (endX - startX) * enlargeFactor
        return startX - enlarge to endX + enlarge
    }
}
