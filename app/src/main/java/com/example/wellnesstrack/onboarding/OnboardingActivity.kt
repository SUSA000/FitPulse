package com.example.wellnesstrack.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.wellnesstrack.R
import com.example.wellnesstrack.auth.LoginActivity
import com.example.wellnesstrack.data.Prefs

class OnboardingActivity : AppCompatActivity() {
    private lateinit var pager: ViewPager2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding)

        pager = findViewById(R.id.pager)
        pager.adapter = OnboardingPagerAdapter(this)

        findViewById<View>(R.id.btnSkip).setOnClickListener { finishOnboarding() }
        findViewById<View>(R.id.btnNext).setOnClickListener {
            if (pager.currentItem < 2) pager.currentItem = pager.currentItem + 1 else finishOnboarding()
        }

        pager.registerOnPageChangeCallback(object: ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                // update indicators and button text
                val dots = listOf(
                    findViewById<View>(R.id.dot1),
                    findViewById<View>(R.id.dot2),
                    findViewById<View>(R.id.dot3),
                )
                dots.forEachIndexed { idx, v -> v.alpha = if (idx == position) 1f else 0.35f }
                val btnNext = findViewById<android.widget.Button>(R.id.btnNext)
                btnNext.text = if (position == 2) "Get Started" else "Next"
            }
        })
    }

    private fun finishOnboarding() {
        Prefs(this).setOnboardingCompleted(true)
        val i = Intent(this, LoginActivity::class.java)
        i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(i)
        finish()
    }
}
