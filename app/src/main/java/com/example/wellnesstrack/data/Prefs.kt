package com.example.wellnesstrack.data

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("wellness_prefs", Context.MODE_PRIVATE)

    // Simple representation stored in prefs for UI (recent notifications)
    data class AppNotification(val ts: Long, val type: String, val title: String, val body: String)

    fun resetIfNewDay() {
        val today = LocalDate.now().toString()
        val lastReset = sp.getString("last_reset", "") ?: ""
        if (lastReset != today) {
            val habits = getHabits()
            habits.forEach { it.isDoneToday = false }
            saveHabits(habits)
            // reset daily counters
            sp.edit {
                putString("last_reset", today)
                putInt("water_$today", 0)
            }
        }
    }

    // Habits stored as JSON array of {name:String, done:Boolean}
    fun saveHabits(list: List<Habit>) {
        val arr = JSONArray()
        list.forEach { h ->
            arr.put(JSONObject().apply {
                put("name", h.title)
                put("done", h.isDoneToday)
            })
        }
        sp.edit { putString("habits", arr.toString()) }
    }

    fun getHabits(): MutableList<Habit> {
        val raw = sp.getString("habits", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<Habit>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += Habit(o.getString("name"), o.getBoolean("done"))
        }
        return out
    }

    fun getHabitGoal(): Int = try {
        sp.getInt("habit_goal", 5)
    } catch (e: ClassCastException) {
        sp.edit { putInt("habit_goal", 5) }
        5
    }
    fun setHabitGoal(goal: Int) = sp.edit { putInt("habit_goal", goal) }

    // Mood entries as JSON array of {ts:Long, emoji:String, note:String}
    fun saveMoods(list: List<MoodEntry>) {
        val arr = JSONArray()
        list.forEach {
            arr.put(JSONObject().apply {
                put("ts", it.timestamp)
                put("emoji", it.emoji)
                put("note", it.note)
            })
        }
        sp.edit { putString("moods", arr.toString()) }
    }

    fun getMoods(): MutableList<MoodEntry> {
        val raw = sp.getString("moods", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<MoodEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += MoodEntry(o.getLong("ts"), o.getString("emoji"), o.optString("note"))
        }
        return out
    }

    // Generic helpers
    fun saveLong(key: String, value: Long) = sp.edit { putLong(key, value) }
    fun getLong(key: String, default: Long): Long {
        return try {
            sp.getLong(key, default)
        } catch (e: ClassCastException) {
            saveLong(key, default) // Reset to default
            default
        }
    }

    // Generic string/int helpers (used for Profile storage)
    fun getString(key: String, default: String = ""): String = try {
        sp.getString(key, default) ?: default
    } catch (e: ClassCastException) {
        sp.edit { putString(key, default) }
        default
    }
    fun putString(key: String, value: String) = sp.edit { putString(key, value) }

    fun getIntSafe(key: String, default: Int = 0): Int = try {
        sp.getInt(key, default)
    } catch (e: ClassCastException) {
        sp.edit { putInt(key, default) }
        default
    }
    fun putIntSafe(key: String, value: Int) = sp.edit { putInt(key, value) }

    // --- Sleep tracking (per-day, date-stamped key) ---
    private fun todayKey(prefix: String): String = "${prefix}_${LocalDate.now()}"

    fun getSleepHours(): Float {
        val key = todayKey("sleep")
        return try {
            sp.getFloat(key, 0f)
        } catch (e: ClassCastException) {
            sp.edit { putFloat(key, 0f) }
            0f
        }
    }

    fun setSleepHours(hours: Float) {
        val key = todayKey("sleep")
        sp.edit { putFloat(key, hours) }
    }

    fun getSleepGoal(): Float = try {
        sp.getFloat("sleep_goal", 8.0f)
    } catch (e: ClassCastException) {
        sp.edit { putFloat("sleep_goal", 8.0f) }
        8.0f
    }

    fun setSleepGoal(goal: Float) = sp.edit { putFloat("sleep_goal", goal) }

    // --- Water tracking (per-day) ---
    fun getWaterGlasses(): Int {
        val key = todayKey("water")
        return try {
            sp.getInt(key, 0)
        } catch (e: ClassCastException) {
            sp.edit { putInt(key, 0) }
            0
        }
    }

    fun setWaterGlasses(count: Int) {
        val key = todayKey("water")
        sp.edit { putInt(key, count) }
    }

    fun getWaterGoal(): Int = try {
        sp.getInt("water_goal", 8)
    } catch (e: ClassCastException) {
        sp.edit { putInt("water_goal", 8) }
        8
    }
    fun setWaterGoal(goal: Int) = sp.edit { putInt("water_goal", goal) }

    // --- Notification preferences ---
    // Sleep reminders
    fun setSleepReminderEnabled(enabled: Boolean) = sp.edit { putBoolean("notif_sleep_enabled", enabled) }
    fun isSleepReminderEnabled(): Boolean = sp.getBoolean("notif_sleep_enabled", false)
    fun setSleepReminderTime(time: String) = sp.edit { putString("notif_sleep_time", time) }
    fun getSleepReminderTime(): String = sp.getString("notif_sleep_time", "22:30") ?: "22:30"
    
    // Hydration reminders
    fun setHydrationReminderEnabled(enabled: Boolean) = sp.edit { putBoolean("notif_hydration_enabled", enabled) }
    fun isHydrationReminderEnabled(): Boolean = sp.getBoolean("notif_hydration_enabled", false)
    fun setHydrationReminderInterval(minutes: Int) = sp.edit { putInt("notif_hydration_interval", minutes) }
    fun getHydrationReminderInterval(): Int = sp.getInt("notif_hydration_interval", 60)

    // Habit check-in
    fun setHabitReminderEnabled(enabled: Boolean) = sp.edit { putBoolean("notif_habit_enabled", enabled) }
    fun isHabitReminderEnabled(): Boolean = sp.getBoolean("notif_habit_enabled", false)
    fun setHabitReminderTime(time: String) = sp.edit { putString("notif_habit_time", time) }
    fun getHabitReminderTime(): String = sp.getString("notif_habit_time", "08:00") ?: "08:00"

    // Read sleep value for an arbitrary date string (yyyy-MM-dd)
    fun getSleepForDate(date: String): Float {
        val key = "sleep_$date"
        return try {
            sp.getFloat(key, 0f)
        } catch (e: ClassCastException) {
            0f
        }
    }

    // Return last n days sleep hours (oldest first)
    fun getLastNDaysSleep(n: Int): List<Float> {
        val out = mutableListOf<Float>()
        for (i in (n - 1) downTo 0) {
            val d = LocalDate.now().minusDays(i.toLong()).toString()
            out.add(getSleepForDate(d))
        }
        return out
    }

    // Clear simple user/session data (used for logout)
    fun clearUserSession() {
        sp.edit {
            // goals
            remove("sleep_goal")
            remove("water_goal")
            remove("habit_goal")
            // daily data
            remove("habits")
            remove("moods")
            // session
            remove("last_reset")
            remove("is_logged_in")
            remove("current_user")
        }
    }

    // --- Notifications stored for header UI ---
    fun addNotification(n: AppNotification) {
        val raw = sp.getString("notifications", "[]") ?: "[]"
        val arr = JSONArray(raw)
        arr.put(JSONObject().apply {
            put("ts", n.ts)
            put("type", n.type)
            put("title", n.title)
            put("body", n.body)
        })
        // keep only latest 50
        val out = JSONArray()
        val start = (arr.length() - 50).coerceAtLeast(0)
        for (i in start until arr.length()) out.put(arr.getJSONObject(i))
        sp.edit { putString("notifications", out.toString()) }
    }

    fun getNotifications(): List<AppNotification> {
        val raw = sp.getString("notifications", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<AppNotification>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += AppNotification(o.optLong("ts", 0L), o.optString("type", ""), o.optString("title", ""), o.optString("body", ""))
        }
        return out.reversed()
    }

    fun clearNotifications() = sp.edit { remove("notifications") }

    // Mood sensor permission (app-level toggle)
    fun setMoodSensorAllowed(allowed: Boolean) = sp.edit { putBoolean("mood_sensor_allowed", allowed) }
    fun isMoodSensorAllowed(): Boolean = sp.getBoolean("mood_sensor_allowed", false)

    // Convenience to add a simple mood entry (ts, emoji, note)
    fun addMoodEntry(ts: Long, emoji: String, note: String = "") {
        val moods = getMoods()
        val entry = MoodEntry(ts, emoji, note)
        val out = moods.toMutableList()
        out.add(entry)
        // save back
        saveMoods(out)
    }

    // --- Simple user storage for Login/SignUp (demo only) ---
    data class User(val username: String, val email: String, val password: String)

    fun getUsers(): MutableList<User> {
        val raw = sp.getString("users", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<User>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            out += User(o.optString("username"), o.optString("email"), o.optString("password"))
        }
        return out
    }

    fun saveUsers(list: List<User>) {
        val arr = JSONArray()
        list.forEach { u ->
            arr.put(JSONObject().apply {
                put("username", u.username)
                put("email", u.email)
                put("password", u.password)
            })
        }
        sp.edit { putString("users", arr.toString()) }
    }

    fun findUserByIdentifier(id: String): User? {
        val norm = id.trim().lowercase()
        return getUsers().firstOrNull { it.email.equals(norm, true) || it.username.equals(norm, true) }
    }

    fun registerUser(user: User): Boolean {
        val users = getUsers()
        if (users.any { it.email.equals(user.email, true) || it.username.equals(user.username, true) }) return false
        users.add(user)
        saveUsers(users)
        return true
    }

    fun setLoggedIn(username: String) = sp.edit {
        putBoolean("is_logged_in", username.isNotEmpty())
        putString("current_user", username)
    }
    fun isLoggedIn(): Boolean = sp.getBoolean("is_logged_in", false)
    fun currentUser(): String? = sp.getString("current_user", null)

    // Onboarding flag
    fun setOnboardingCompleted(value: Boolean) = sp.edit { putBoolean("onboarding_completed", value) }
    fun isOnboardingCompleted(): Boolean = sp.getBoolean("onboarding_completed", false)

    // --- Medicine reminders storage (times as "HH:mm") ---
    fun getMedicineReminders(): MutableList<String> {
        val raw = sp.getString("medicine_reminders", "[]") ?: "[]"
        val arr = JSONArray(raw)
        val out = mutableListOf<String>()
        for (i in 0 until arr.length()) out += arr.getString(i)
        return out
    }

    fun saveMedicineReminders(list: List<String>) {
        val arr = JSONArray()
        list.forEach { arr.put(it) }
        sp.edit { putString("medicine_reminders", arr.toString()) }
    }

    fun addMedicineReminder(time: String) {
        val list = getMedicineReminders()
        if (!list.contains(time)) {
            list += time
            saveMedicineReminders(list.sorted())
        }
    }

    fun removeMedicineReminder(time: String) {
        val list = getMedicineReminders()
        if (list.remove(time)) saveMedicineReminders(list.sorted())
    }

    fun setMedicineRemindersEnabled(enabled: Boolean) = sp.edit { putBoolean("medicine_reminders_enabled", enabled) }
    fun isMedicineRemindersEnabled(): Boolean = sp.getBoolean("medicine_reminders_enabled", false)
}

data class Habit(var title: String, var isDoneToday: Boolean = false)
data class MoodEntry(val timestamp: Long, val emoji: String, val note: String = "")
