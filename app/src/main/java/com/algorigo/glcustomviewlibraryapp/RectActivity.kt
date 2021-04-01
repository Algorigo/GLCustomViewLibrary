package com.algorigo.glcustomviewlibraryapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.algorigo.glcustomviewlibrary.CustomGLView
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.disposables.Disposable
import kotlinx.android.synthetic.main.activity_rect.*
import java.util.concurrent.TimeUnit

class RectActivity : AppCompatActivity() {

    private val data = FloatArray(2500) { 0f }
    private var disposable: Disposable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rect)

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
        private val LOG_TAG = RectActivity::class.java.simpleName
    }
}
