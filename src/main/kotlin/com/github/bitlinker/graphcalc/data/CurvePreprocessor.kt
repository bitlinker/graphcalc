package com.github.bitlinker.graphcalc.data

internal data class PreprocessorResult(
    val curve: Curve,
    val smoothCurve: Curve,
    val smooth1stDerivative: Curve,
    val smooth2ndDerivative: Curve,
)

internal class CurvePreprocessor {
    fun preprocess(
        curve: Curve,
        smoothSamples: Int,
        derivativeSmoothSamples: Int,
    ): PreprocessorResult {
        val smoothedCurve = curve.gaussSmooth(smoothSamples / 2)
        val smooth1stDerivative = smoothedCurve.derivative().gaussSmooth(derivativeSmoothSamples / 2)
        val smooth2stDerivative = smooth1stDerivative.derivative().gaussSmooth(derivativeSmoothSamples / 2)

        return PreprocessorResult(
            curve = curve,
            smoothCurve = smoothedCurve,
            smooth1stDerivative = smooth1stDerivative,
            smooth2ndDerivative = smooth2stDerivative,
        )
    }
}


