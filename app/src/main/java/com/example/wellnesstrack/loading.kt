@file:Suppress("DEPRECATION")
package com.example.wellnesstrack

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.wellnesstrack.auth.LoginActivity
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.onboarding.OnboardingActivity

/**
 * Preserved legacy loading page.
 * This is now the launcher activity in AndroidManifest, preserving your original
 * loading screen design and flow.
 */
class LegacyLoadingActivity : AppCompatActivity() {

	private lateinit var prefs: Prefs

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
		setContentView(R.layout.activity_loading)

		prefs = Prefs(this)

		// Small delay to display splash, then route based on onboarding/login
		Handler(Looper.getMainLooper()).postDelayed({
			navigateNext()
		}, 1500L)
	}

	private fun navigateNext() {
		// Always show onboarding after loading
		val next = Intent(this, OnboardingActivity::class.java)
		startActivity(next)
		finish()
		overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
	}
}

