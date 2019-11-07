package com.algorigo.glcustomviewlibrary.interpolator

import io.reactivex.Single

internal class LinearInterpolator(val width: Int, val height: Int) : ImageProcessor {

    override val newWidth: Int
    override val newHeight: Int
    private val newDataArray: Array<FloatArray>

    init {
        newWidth = width * 2 - 1
        newHeight = height * 2 - 1
        newDataArray = Array(newHeight) { FloatArray(newWidth) }
    }

    override fun processSingle(input: Array<FloatArray>): Single<Array<FloatArray>> {
        return Single.fromCallable {
            val limitY = input.size - 1
            for (y in input.indices) {
                val limitX = input[y].size - 1
                for (x in input[y].indices) {
                    newDataArray[2 * y][2 * x] = input[y][x]

                    if (y < limitY && x < limitX) {
                        newDataArray[2 * y][2 * x + 1] = (input[y][x] + input[y][x + 1]) / 2
                        newDataArray[2 * y + 1][2 * x] = (input[y][x] + input[y + 1][x]) / 2
                        newDataArray[2 * y + 1][2 * x + 1] = (input[y][x] + input[y][x + 1] + input[y + 1][x] + input[y + 1][x + 1]) / 4
                    } else if (y == limitY && x < limitX) {
                        newDataArray[2 * y][2 * x + 1] = (input[y][x] + input[y][x + 1]) / 2
                    } else if (y < limitY && x == limitX) {
                        newDataArray[2 * y + 1][2 * x] = (input[y][x] + input[y + 1][x]) / 2
                    }
                }
            }

            newDataArray
        }
    }
}