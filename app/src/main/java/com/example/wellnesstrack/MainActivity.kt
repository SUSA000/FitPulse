package com.example.wellnesstrack

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.enableEdgeToEdge
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.wellnesstrack.databinding.ActivityMainBinding
import android.view.View
import android.widget.TextView
import android.widget.ImageButton
import android.widget.PopupMenu
import androidx.appcompat.app.AlertDialog
import androidx.navigation.findNavController
import com.example.wellnesstrack.data.Prefs
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.provider.Settings
import android.content.Intent
import android.net.Uri

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        createNotificationChannelIfNeeded()
        requestNotificationPermissionIfNeeded()
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    // Try to attach header controls immediately and with a small delay to cover first render
    attachHeaderControls()
    binding.root.post { attachHeaderControls() }
    binding.root.postDelayed({ attachHeaderControls() }, 120)

        val navHost = supportFragmentManager.findFragmentById(
            R.id.nav_host_fragment_container
        ) as NavHostFragment? ?: NavHostFragment.create(R.navigation.nav_graph).also {
            supportFragmentManager.beginTransaction()
                .replace(R.id.nav_host_fragment_container, it).setPrimaryNavigationFragment(it)
                .commitNow()
        }
        binding.bottomNav.setupWithNavController(navHost.navController)

        navHost.navController.addOnDestinationChangedListener { _, _, _ ->
            attachHeaderControls()
        }
    }

    private fun attachHeaderControls() {
        // When destination changes, find the header controls in the fragment view and wire them.
        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)?.view?.post {
            try {
                val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)
                val rootView: View? = host?.view
                val overflowId = resources.getIdentifier("btnOverflow", "id", packageName)
                val notifId = resources.getIdentifier("btnNotifications", "id", packageName)
                val badgeId = resources.getIdentifier("tvNotifBadge", "id", packageName)
                val overflow = rootView?.findViewById<ImageButton>(overflowId)
                val notifBtn = rootView?.findViewById<ImageButton>(notifId)
                val notifBadge = rootView?.findViewById<TextView>(badgeId)
                val avatarViewId = resources.getIdentifier("ivAvatar", "id", packageName)
                val avatarView = rootView?.findViewById<android.widget.ImageView>(avatarViewId)

                overflow?.setOnClickListener { v -> showHeaderMenu(v) }

                // update badge count
                try {
                    val count = Prefs(this).getNotifications().size
                    if (count > 0) {
                        notifBadge?.visibility = View.VISIBLE
                        notifBadge?.text = count.coerceAtMost(99).toString()
                    } else notifBadge?.visibility = View.GONE
                } catch (_: Exception) {}

                notifBtn?.setOnClickListener {
                    try {
                        val prefs = Prefs(this)
                        val notifs = prefs.getNotifications()
                        if (notifs.isEmpty()) {
                            AlertDialog.Builder(this).setTitle("Notifications").setMessage("No notifications").setPositiveButton("OK", null).show()
                        } else {
                            val msgs = notifs.joinToString("\n\n") { n ->
                                val t = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault()).format(java.util.Date(n.ts))
                                "${t}: ${n.title}\n${n.body}"
                            }
                            AlertDialog.Builder(this).setTitle("Notifications").setMessage(msgs).setPositiveButton("OK", null).setNeutralButton("Clear") { _, _ -> prefs.clearNotifications(); notifBadge?.visibility = View.GONE }.show()
                        }
                    } catch (_: Exception) { }
                }

                // Load avatar from prefs if available
                try {
                    val avatarUri = Prefs(this).getString("profile_avatar_uri", "")
                    if (avatarUri.isNotBlank()) {
                        avatarView?.setImageURI(android.net.Uri.parse(avatarUri))
                    } else {
                        avatarView?.setImageResource(R.drawable.ic_user_placeholder)
                    }
                    // Tap avatar to open Profile
                    avatarView?.setOnClickListener {
                        try { findNavController(R.id.nav_host_fragment_container).navigate(R.id.profileFragment) } catch (_: Exception) {}
                    }
                } catch (_: Exception) {}
            } catch (_: Exception) { }
        }
    }

    private fun createNotificationChannelIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Medicine Reminders"
            val descriptionText = "Notifications for medicine reminders"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("medicine_channel", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            // habit
            val habit = NotificationChannel("habit_channel", "Habit Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Habit reminders" }
            notificationManager.createNotificationChannel(habit)
            // sleep
            val sleep = NotificationChannel("sleep_channel", "Sleep Reminders", NotificationManager.IMPORTANCE_DEFAULT).apply { description = "Sleep reminders" }
            notificationManager.createNotificationChannel(sleep)
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val perm = android.Manifest.permission.POST_NOTIFICATIONS
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(perm), 1010)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1010) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ok
            } else {
                // guide user to settings to enable notifications
                AlertDialog.Builder(this)
                    .setTitle("Enable notifications")
                    .setMessage("Notifications are disabled. To see medicine reminders, enable notifications for this app in system settings.")
                    .setPositiveButton("Open Settings") { _, _ ->
                        val intent = Intent().apply {
                            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
                        }
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        }
    }

    /**
     * Convenience: open app notification settings when notifications are blocked.
     */
    fun openAppNotificationSettings() {
        val intent = Intent().apply {
            action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startActivity(intent)
    }

    /**
     * Update the shared header subtitle if present in the currently shown fragment's layout.
     */
    fun setHeaderSubtitle(text: String) {
        // nav host contains fragment root; try to find the header subtitle TextView
        val host = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_container)
        val rootView: View? = host?.view
        val tv = rootView?.findViewById<TextView?>(resources.getIdentifier("tvHeaderSubtitle", "id", packageName))
        tv?.text = text
    }

    private fun showHeaderMenu(anchor: View) {
        val popup = PopupMenu(this, anchor)
        val navController = findNavController(R.id.nav_host_fragment_container)

        when (navController.currentDestination?.id) {
            R.id.habitsFragment -> popup.menuInflater.inflate(R.menu.habits_overflow_menu, popup.menu)
            R.id.settingsFragment -> popup.menuInflater.inflate(R.menu.settings_overflow_menu, popup.menu)
            R.id.moodFragment -> popup.menuInflater.inflate(R.menu.mood_overflow_menu, popup.menu)
            else -> popup.menuInflater.inflate(R.menu.overflow_menu, popup.menu)
        }

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_home -> {
                    navController.navigate(R.id.homeFragment)
                    true
                }
                R.id.menu_habit -> {
                    navController.navigate(R.id.habitsFragment)
                    true
                }
                R.id.menu_help -> {
                    AlertDialog.Builder(this)
                        .setTitle("Help & Tips")
                        .setMessage("Open onboarding tour or help dialog here.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_profile -> {
                    navController.navigate(R.id.profileFragment)
                    true
                }
                R.id.menu_about -> {
                    AlertDialog.Builder(this)
                        .setTitle("About This App")
                        .setMessage("App version and other details.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_set_daily_goal -> {
                    AlertDialog.Builder(this)
                        .setTitle("Set Daily Goal")
                        .setMessage("Dialog to set target number of habits.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_view_habit_history -> {
                    AlertDialog.Builder(this)
                        .setTitle("View Habit History")
                        .setMessage("Shows calendar view of past completions.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_reset_progress -> {
                    AlertDialog.Builder(this)
                        .setTitle("Reset Today's Progress")
                        .setMessage("Are you sure you want to uncheck all habits for today?")
                        .setPositiveButton("Reset", null)
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                R.id.menu_logout -> {
                    AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Logout") { _, _ ->
                            val prefs = Prefs(this)
                            prefs.setLoggedIn("")
                            val i = Intent(this, com.example.wellnesstrack.auth.LoginActivity::class.java)
                            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(i)
                            finish()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                    true
                }
                R.id.menu_mood -> {
                    navController.navigate(R.id.moodFragment)
                    true
                }
                R.id.menu_view_mood_calendar -> {
                    AlertDialog.Builder(this)
                        .setTitle("View Mood Calendar")
                        .setMessage("Shows calendar view of past moods.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_share_weekly_summary -> {
                    AlertDialog.Builder(this)
                        .setTitle("Share Weekly Summary")
                        .setMessage("Share a summary of the week's moods.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                R.id.menu_export_mood_report -> {
                    AlertDialog.Builder(this)
                        .setTitle("Export Mood Report")
                        .setMessage("Export a report of mood history.")
                        .setPositiveButton("OK", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_container)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
