package com.algorigo.glcustomviewlibraryapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.algorigo.glcustomviewlibraryapp.databinding.ActivityIntroBinding

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rectBtn.setOnClickListener {
            Intent(this, RectActivity::class.java).also {
                startActivity(it)
            }
        }
        binding.customBtn.setOnClickListener {
            Intent(this, CustomActivity::class.java).also {
                startActivity(it)
            }
        }
    }
}
