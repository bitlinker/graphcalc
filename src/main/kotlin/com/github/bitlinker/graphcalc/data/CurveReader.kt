package com.github.bitlinker.graphcalc.data

import java.io.BufferedReader
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader

internal class CurveReader {
    fun read(path: String): Result<Curve> {
        return try {
            val points = mutableListOf<Point>()
            FileInputStream(path).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    BufferedReader(reader).use { bufferedReader ->
                        while (true) {
                            val line = bufferedReader.readLine() ?: break
                            val pointStrings = line.split(splitRegex)
                            if (pointStrings.size != 2) throw IOException("Wrong point string: $line")
                            points.add(
                                Point(
                                    x = pointStrings[0].parseDouble(),
                                    y = pointStrings[1].parseDouble(),
                                )
                            )
                        }
                    }
                }
            }
            Result.success(Curve(points = points))
        } catch (e: IOException) {
            Result.failure(e)
        } catch (e: NumberFormatException) {
            Result.failure(e)
        }
    }
}

private fun String.parseDouble() : Double {
    return replace(',','.').toDouble()
}

private val splitRegex = "\\s+".toRegex()