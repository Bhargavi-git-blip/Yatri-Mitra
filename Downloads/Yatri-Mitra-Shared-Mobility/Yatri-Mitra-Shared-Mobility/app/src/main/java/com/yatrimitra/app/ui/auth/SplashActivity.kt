package com.yatrimitra.app.ui.auth

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.yatrimitra.app.R
import com.yatrimitra.app.network.SessionManager
import com.yatrimitra.app.ui.MainActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        SessionManager.init(this)
        Handler(Looper.getMainLooper()).postDelayed({
            val dest = if (SessionManager.isLoggedIn()) MainActivity::class.java
                       else LoginActivity::class.java
            startActivity(Intent(this, dest))
            finish()
        }, 1500)
    }
}
