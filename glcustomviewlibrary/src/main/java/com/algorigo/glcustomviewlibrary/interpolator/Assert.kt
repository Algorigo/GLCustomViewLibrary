package com.algorigo.glcustomviewlibrary.interpolator

import android.util.Log
import com.algorigo.glcustomviewlibrary.BuildConfig

internal object Assert {
    fun check(condition: Boolean, tag: String, e: Exception) {
        if (condition) {
            return
        }

        Log.e(tag, e.localizedMessage, e)
        if (BuildConfig.DEBUG) {
            throw e
        }
    }
}