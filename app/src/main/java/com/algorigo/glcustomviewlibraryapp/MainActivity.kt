package com.algorigo.glcustomviewlibraryapp

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.algorigo.glcustomviewlibrary.CustomGLView
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val data = Array(50) { FloatArray(50) }
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        customGlView.addColorMap(CustomGLView.ColorMap.RainbowColorMapRect(
                CustomGLView.Vec3D(0f, 0f, 0f),
                CustomGLView.Vec3D(0f, 22f, 0f),
                CustomGLView.Vec3D(22f, 0f, 0f),
                CustomGLView.Rotation.NORMAL,
                true,
                50,
                50
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
                    Single.create<Array<FloatArray>> {
                        for (y in 0 until data.size) {
                            for (x in 0 until data[y].size) {
                                data[y][x] = Math.random().toFloat()
                            }
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
        private val LOG_TAG = MainActivity::class.java.simpleName
    }
}
