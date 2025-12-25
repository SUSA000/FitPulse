# FitPulse - Wellness TrackApp 🧡

**FitPulse** is a comprehensive wellness application designed to help users manage their daily health routines through habit tracking, mood journaling, and hydration reminders. Built natively with Kotlin, it focuses on a clean UI/UX and operates entirely offline without the need for a backend database.

---

## 📱 Features

### 1. Daily Habit Tracker ✅
* **Track Habits:** Users can add, edit, and delete wellness habits (e.g., drinking water, meditation, exercise).
* **Visual Progress:** Each habit displays a visual completion progress indicator for the current day.
* **Auto-Reset:** The app automatically resets daily progress at midnight using date comparison logic.

### 2. Mood Journal 📝
* **Emoji Logging:** Log mood entries quickly by selecting from a collection of emojis.
* **Detailed Notes:** Add optional text notes to explain your feelings.
* **History & Filters:** View a chronological list of mood entries and filter them by day or week to track emotional patterns over time.

### 3. Hydration Reminder System 💧
* **Smart Notifications:** Uses `AlarmManager` to schedule recurring notifications.
* **Quick Actions:** Includes a "Drink" button directly in the notification, allowing users to log water intake without opening the app.
* **Custom Intervals:** Users can define their own reminder intervals.

### 4. Home Screen Widget (Advanced Feature) 🚀
* **Instant Overview:** A compact home screen widget displays today's overall habit completion percentage and total target count.
* **Quick Access:** Tapping the widget opens the Habits screen directly.
* **Real-time Updates:** The widget updates automatically as habits are marked complete.

---

## 🛠️ Tech Stack

* **Language:** Kotlin
* **Environment:** Android Studio
* **Architecture:** MVVM (Recommended pattern for modern Android apps)
* **Local Storage:** `SharedPreferences` (Used for data persistence, no backend required)
* **System Services:** `AlarmManager` (For scheduling notifications)
