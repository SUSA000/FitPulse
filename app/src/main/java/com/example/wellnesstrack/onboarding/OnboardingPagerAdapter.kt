package com.example.wellnesstrack.onboarding

import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.wellnesstrack.R

class OnboardingPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3
    override fun createFragment(position: Int) =
        when (position) {
            0 -> OnboardingPageFragment.newInstance(
                position,
                R.drawable.gymph,
                "Track Your Goals",
                "Easily Log and track your workouts to stay motivated."
            )
            1 -> OnboardingPageFragment.newInstance(
                position,
                R.drawable.gym1,
                "Smart Reminders",
                "Never miss your medicine, hydration, or habit check-ins."
            )
            else -> OnboardingPageFragment.newInstance(
                position,
                R.drawable.gym2,
                "Insights & Progress",
                "See your weekly trends and stay motivated."
            )
        }
}
