package com.appmarkets.firebasemlkit.activity

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity


class SplashActivity : AppCompatActivity() {

    private val SPLASH_TIME_OUT = 3000L
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.appmarkets.firebasemlkit.R.layout.activity_splash_screen)

        loadSplashScreen()

    }

    private fun loadSplashScreen() {
        Handler().postDelayed(
            {
                val i = Intent(this@SplashActivity, MainActivity::class.java)
                startActivity(i)
                finish()
            }, SPLASH_TIME_OUT)

    }


}
