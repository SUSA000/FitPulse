@file:Suppress("DEPRECATION")
package com.example.wellnesstrack.loading

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.wellnesstrack.R
import com.example.wellnesstrack.auth.LoginActivity
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.MainActivity
import com.example.wellnesstrack.onboarding.OnboardingActivity

class LoadingActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)

        // Initialize preferences
        prefs = Prefs(this)

        // Show splash for 2 seconds, then check login state
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, 2000L)
    }

    private fun navigateToNextScreen() {
        // Always proceed to onboarding after loading screen
        val intent = Intent(this, OnboardingActivity::class.java)
        startActivity(intent)
        finish() // Prevent returning to loading screen on back press
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }
}
