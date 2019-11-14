package com.algorigo.glcustomviewlibrary.interpolator

import io.reactivex.Single

internal interface ImageProcessor {

    val newWidth: Int
    val newHeight: Int

    fun processSingle(input: Array<FloatArray>): Single<Array<FloatArray>>

}