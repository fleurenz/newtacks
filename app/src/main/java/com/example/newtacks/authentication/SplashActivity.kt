package com.example.newtacks.authentication

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Handler(Looper.getMainLooper()).postDelayed({

            val user = FirebaseAuth.getInstance().currentUser

            if (user != null) {
                // Already logged in → go login routing later
                startActivity(Intent(this, OnboardingActivity::class.java))
            } else {
                startActivity(Intent(this, OnboardingActivity::class.java))
            }

            finish()

        }, 2000)
    }
}