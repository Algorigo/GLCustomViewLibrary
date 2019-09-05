package com.algorigo.glcustomviewlibraryapp

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import kotlinx.android.synthetic.main.activity_intro.*

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        rectBtn.setOnClickListener {
            Intent(this, RectActivity::class.java).also {
                startActivity(it)
            }
        }
        customBtn.setOnClickListener {
            Intent(this, CustomActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}
