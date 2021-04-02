package com.algorigo.glcustomviewlibrary.interpolator

import io.reactivex.rxjava3.core.Single

internal class SmoothInterpolator(val width: Int, val height: Int) : ImageProcessor {

    override val newWidth: Int
    override val newHeight: Int
    private val newDataArray: Array<FloatArray>

    init {
        newWidth = width
        newHeight = height
        newDataArray = Array(newHeight) { FloatArray(newWidth) }
    }

    override fun processSingle(input: Array<FloatArray>): Single<Array<FloatArray>> {
        return Single.fromCallable {
            val limitY = input.size - 1
            val limitX = input[0].size - 1

            newDataArray[0][0] = (input[0][0] + input[0][1] + input[1][0] + input[1][1]) / 4
            newDataArray[0][limitX] = (input[0][limitX-1] + input[0][limitX] + input[1][limitX-1] + input[1][limitX]) / 4
            newDataArray[limitY][0] = (input[limitY-1][0] + input[limitY-1][1] + input[limitY][0] + input[limitY][1]) / 4
            newDataArray[limitY][limitX] = (input[limitY-1][limitX-1] + input[limitY-1][limitX] + input[limitY][limitX-1] + input[limitY][limitX]) / 4

            for (i in 1 until limitY) {
                newDataArray[i][0] = (input[i-1][0] + input[i-1][1] + input[i][0] + input[i][1] + input[i+1][0] + input[i+1][1]) / 6
                newDataArray[i][limitX] = (input[i-1][limitX-1] + input[i-1][limitX] + input[i][limitX-1] + input[i][limitX] + input[i+1][limitX-1] + input[i+1][limitX]) / 6
            }

            for (j in 1 until limitX) {
                newDataArray[0][j] = (input[0][j-1] + input[0][j] + input[0][j+1] + input[1][j-1] + input[1][j] + input[1][j+1]) / 6
                newDataArray[limitY][j] = (input[limitY-1][j-1] + input[limitY-1][j] + input[limitY-1][j+1] + input[limitY][j-1] + input[limitY][j] + input[limitY][j+1]) / 6
            }

            for (i in 1 until limitY) {
                for (j in 1 until limitX) {
                    newDataArray[i][j] = (input[i - 1][j - 1] + input[i - 1][j] + input[i - 1][j + 1] +
                            input[i][j - 1] + input[i][j] + input[i][j + 1] +
                            input[i + 1][j - 1] + input[i + 1][j] + input[i + 1][j + 1]) / 9
                }
            }

            newDataArray
        }
    }
}