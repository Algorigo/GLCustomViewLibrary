package com.algorigo.glcustomviewlibraryapp

import android.graphics.PointF
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.algorigo.glcustomviewlibrary.CustomGLView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_custom.*
import java.util.concurrent.TimeUnit

class CustomActivity : AppCompatActivity() {

    private val data = FloatArray(4) { 0f }
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_custom)

        val pointList = mutableListOf<PointF>().apply {
            add(PointF(0.0f, 0.0f))
            add(PointF(1.0f, 0.0f))
            add(PointF(1.0f, 1.0f))
            add(PointF(0.0f, 1.0f))
        }
        customGlView.addColorMap(CustomGLView.ColorMap.RainbowColorMapCustom(
            pointList,
            intArrayOf(0, 1, 2, 1, 3, 0),
            CustomGLView.Vec3D(0f, 0f, 0f),
            CustomGLView.Vec3D(0f, 22f, 0f),
            CustomGLView.Vec3D(22f, 0f, 0f)
        ))
        button.setOnClickListener {
            if (it.isSelected) {
                button.text = "Start"
                button.isSelected = false
                stop()
            } else {
                button.text = "Stop"
                button.isSelected = true
                start()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        button.text = "Stop"
        button.isSelected = true
        stop()
    }

    private fun start() {
        if (disposable == null) {
            disposable = Observable.interval(1000, TimeUnit.MILLISECONDS)
                .concatMapSingle {
                    Single.create<FloatArray> {
                        for (index in 0 until data.size) {
                            data[index] = Math.random().toFloat()
                        }
                        it.onSuccess(data)
                    }
                }
                .subscribeOn(AndroidSchedulers.mainThread())
                .subscribe({
                    customGlView.setData(it)
                }, {
                    Log.e(LOG_TAG, "", it)
                })
        }
    }

    private fun stop() {
        disposable?.dispose()
        disposable = null
    }

    companion object {
        private val LOG_TAG = CustomActivity::class.java.simpleName
    }
}
