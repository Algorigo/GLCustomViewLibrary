package com.algorigo.glcustomviewlibrary.interpolator

import com.jakewharton.rxrelay3.PublishRelay
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.schedulers.Schedulers

class ImageInterpolator private constructor() {

    class IllegalInputData : IllegalStateException("Input Data is Wrong")

    sealed class InterpolatorProcess {
        class INPUT_DATA(val width: Int, val height: Int) : InterpolatorProcess()
        class LINEAR_INTERPOLATE : InterpolatorProcess()
        class SMOOTH : InterpolatorProcess()
    }

    class Builder {

        private val interpolatorProcessList = mutableListOf<InterpolatorProcess>()

        fun addImageProcessor(interpolatorProcess: InterpolatorProcess): Builder {
            interpolatorProcessList.add(interpolatorProcess)
            return this
        }

        fun build(): ImageInterpolator {
            if (interpolatorProcessList.filter { it is InterpolatorProcess.INPUT_DATA }.size != 1 ||
                interpolatorProcessList.first() !is InterpolatorProcess.INPUT_DATA
            ) {
                throw IllegalInputData()
            }

            return ImageInterpolator().apply {
                var lastWidth = 0
                var lastHeight = 0
                for (interpolatorProcess in interpolatorProcessList) {
                    when (interpolatorProcess) {
                        is InterpolatorProcess.INPUT_DATA -> {
                            inputWidth = interpolatorProcess.width
                            inputHeight = interpolatorProcess.height
                            lastWidth = inputWidth
                            lastHeight = inputHeight
                        }
                        is InterpolatorProcess.LINEAR_INTERPOLATE -> {
                            val imageProcessor =
                                LinearInterpolator(
                                    lastWidth,
                                    lastHeight
                                )
                            imageProcessorList.add(imageProcessor)
                            lastWidth = imageProcessor.newWidth
                            lastHeight = imageProcessor.newHeight
                        }
                        is InterpolatorProcess.SMOOTH -> {
                            val imageProcessor =
                                SmoothInterpolator(
                                    lastWidth,
                                    lastHeight
                                )
                            imageProcessorList.add(imageProcessor)
                            lastWidth = imageProcessor.newWidth
                            lastHeight = imageProcessor.newHeight
                        }
                    }
                }
                width = lastWidth
                height = lastHeight
            }
        }
    }

    private val imageProcessorList = mutableListOf<ImageProcessor>()
    private var inputWidth: Int = 0
    private var inputHeight: Int = 0
    private var width: Int = 0
    private var height: Int = 0
    val outputWidth: Int
        get() = width
    val outputHeight: Int
        get() = height

    fun getSingle(from: Array<FloatArray>): Single<Array<FloatArray>> {
        val iter = imageProcessorList.iterator()
        var single = iter.next().processSingle(from)
        while (iter.hasNext()) {
            iter.next().also { imageProcessor ->
                single = single.flatMap { imageProcessor.processSingle(it) }
            }
        }
        return single.subscribeOn(Schedulers.io())
    }
}