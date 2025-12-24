package com.example.wellnesstrack.ui.home

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.wellnesstrack.R
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentHomeBinding
import com.example.wellnesstrack.ui.components.CircularProgressView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import java.time.LocalDate
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Button

/**
 * A simple [Fragment] subclass.
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private var sensorManager: SensorManager? = null
    private var accelListener: SensorEventListener? = null
    private var lastShakeTimestamp = 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

    sensorManager = requireContext().getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager

    // update app header greeting (time-based)
    (activity as? com.example.wellnesstrack.MainActivity)?.setHeaderSubtitle(computeTimeBasedGreeting())

        // Hero UI bindings
        val cpvHero = binding.root.findViewById<CircularProgressView>(R.id.cpvHero)
        val tvHeroPercent = binding.root.findViewById<android.widget.TextView>(R.id.tvHeroPercent)
        val tvHeroSubtitle = binding.root.findViewById<android.widget.TextView>(R.id.tvHeroSubtitle)

        updateHydrationCard()

        binding.btnDrinkNow.setOnClickListener {
            var waterGlasses = prefs.getWaterGlasses()
            waterGlasses++
            prefs.setWaterGlasses(waterGlasses)
            updateWaterCard()
            prefs.saveLong("last_hydration_time", System.currentTimeMillis())
            updateHydrationCard()
        }

        binding.root.findViewById<View>(R.id.btnNotifications).setOnClickListener {
            Toast.makeText(requireContext(), "Notifications clicked", Toast.LENGTH_SHORT).show()
        }

        // show quick summaries
    val sleepHours = prefs.getSleepHours()
    val sleepGoal = prefs.getSleepGoal()
    binding.tvSleepHours.text = "$sleepHours / $sleepGoal Hours"
    val sleepFraction = if (sleepGoal <= 0f) 0f else (sleepHours / sleepGoal)
    binding.cpvSleep.setStroke(3f)
    binding.cpvSleep.setColors(Color.parseColor("#FF8A00"), Color.parseColor("#222222"))
    binding.cpvSleep.setProgressFraction(sleepFraction)

    updateWaterCard()

        // Removed click listeners for old cards

        // populate weekly sleep chart
        val last7 = prefs.getLastNDaysSleep(7) // oldest -> newest
        val entries = last7.mapIndexed { idx, v -> Entry(idx.toFloat(), v) }
        val ds = LineDataSet(entries, "Sleep last 7 days").apply {
            color = Color.parseColor("#FF8A00")
            setDrawCircles(true); circleRadius = 3f
            setDrawValues(false)
        }
        binding.chartSleepWeekly.data = LineData(ds)
        binding.chartSleepWeekly.description.isEnabled = false
        binding.chartSleepWeekly.invalidate()

        // wire chips
        val chipToday = binding.root.findViewById<com.google.android.material.chip.Chip>(R.id.chipToday)
        val chipWeek = binding.root.findViewById<com.google.android.material.chip.Chip>(R.id.chipWeek)
        val chipMonth = binding.root.findViewById<com.google.android.material.chip.Chip>(R.id.chipMonth)

        chipToday.setOnClickListener { loadMoodData("today") }
        chipWeek.setOnClickListener { loadMoodData("week") }
        chipMonth.setOnClickListener { loadMoodData("month") }

        // initial load (week)
        loadMoodData("week")

        // compute and animate hero progress
        fun updateHero(animated: Boolean = true) {
            val frac = computeOverallProgress()
            val pct = (frac * 100).toInt()
            tvHeroSubtitle.text = computeSubtitleText()
            if (animated) {
                val anim = ValueAnimator.ofFloat(0f, frac)
                anim.duration = 420
                anim.interpolator = DecelerateInterpolator()
                anim.addUpdateListener { v ->
                    val valf = v.animatedValue as Float
                    cpvHero.setProgressFraction(valf)
                    tvHeroPercent.text = "${(valf * 100).toInt()}%"
                }
                anim.start()
            } else {
                cpvHero.setProgressFraction(frac)
                tvHeroPercent.text = "$pct%"
            }
            // color coding
            val color = when {
                frac >= 0.8f -> Color.parseColor("#2ECC71") // green
                frac >= 0.5f -> Color.parseColor("#F1C40F") // yellow
                else -> Color.parseColor("#FF8A00") // orange
            }
            cpvHero.setColors(color, Color.parseColor("#EEEEEE"))
        }

        updateHero(true)
    }

    override fun onResume() {
        super.onResume()
        // refresh hero when returning
        val cpvHero = binding.root.findViewById<CircularProgressView>(R.id.cpvHero)
        val tvHeroPercent = binding.root.findViewById<android.widget.TextView>(R.id.tvHeroPercent)
        val tvHeroSubtitle = binding.root.findViewById<android.widget.TextView>(R.id.tvHeroSubtitle)
        // simple non-animated refresh
        val frac = computeOverallProgress()
        cpvHero.setProgressFraction(frac)
        tvHeroPercent.text = "${(frac * 100).toInt()}%"
        tvHeroSubtitle.text = computeSubtitleText()
    updateWaterCard()
    // refresh greeting when returning to the fragment
    (activity as? com.example.wellnesstrack.MainActivity)?.setHeaderSubtitle(computeTimeBasedGreeting())

        // Mood shake allow button
        val btnAllow = binding.root.findViewById<Button>(R.id.btnAllowShake)
        fun updateAllowButtonText() {
            btnAllow.text = if (prefs.isMoodSensorAllowed()) "Disable Shake Logging" else "Allow Shake Logging"
        }
        updateAllowButtonText()
        btnAllow.setOnClickListener {
            val allowed = !prefs.isMoodSensorAllowed()
            prefs.setMoodSensorAllowed(allowed)
            updateAllowButtonText()
            if (allowed) startShakeListener() else stopShakeListener()
        }

        if (prefs.isMoodSensorAllowed()) startShakeListener()
    }

    private fun loadMoodData(range: String) {
        // range: "today", "week", "month"
        val moods = prefs.getMoods()
        val now = java.util.Calendar.getInstance()
        val pie = binding.root.findViewById<PieChart>(R.id.pieChartMood)
        val tvMoodEmpty = binding.root.findViewById<android.widget.TextView>(R.id.tvMoodEmpty)
        val llLegend = binding.root.findViewById<android.widget.LinearLayout>(R.id.llMoodLegend)

        val cutoff = when (range) {
            "today" -> {
                now.apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0) }
                now.timeInMillis
            }
            "week" -> { now.add(java.util.Calendar.DAY_OF_YEAR, -6); now.timeInMillis }
            "month" -> { now.add(java.util.Calendar.MONTH, -1); now.timeInMillis }
            else -> { now.add(java.util.Calendar.DAY_OF_YEAR, -6); now.timeInMillis }
        }

        val filtered = moods.filter { it.timestamp >= cutoff }
        if (filtered.isEmpty()) {
            pie.visibility = View.GONE
            tvMoodEmpty.visibility = View.VISIBLE
            llLegend.removeAllViews()
            return
        } else {
            pie.visibility = View.VISIBLE
            tvMoodEmpty.visibility = View.GONE
        }

        // categories and colors map
        val categories = listOf("😞","😐","🙂","😊","😄","🤩", "😠")
        val colors = listOf(
            android.graphics.Color.parseColor("#D32F2F"), // sad
            android.graphics.Color.parseColor("#9E9E9E"), // neutral
            android.graphics.Color.parseColor("#4FC3F7"), // okay
            android.graphics.Color.parseColor("#2ECC71"), // good
            android.graphics.Color.parseColor("#CDDC39"), // happy
            android.graphics.Color.parseColor("#9C27B0"),  // great
            android.graphics.Color.parseColor("#B71C1C")   // angry

        )

        val counts = categories.associateWith { emoji -> filtered.count { it.emoji == emoji } }
        val total = filtered.size.toFloat()

        val entries = mutableListOf<PieEntry>()
        val entryColors = mutableListOf<Int>()
        categories.forEachIndexed { idx, emoji ->
            val cnt = counts[emoji] ?: 0
            if (cnt > 0) {
                entries.add(PieEntry(cnt.toFloat(), emoji))
                entryColors.add(colors[idx])
            }
        }

        val dsPie = PieDataSet(entries, "Moods").apply {
            setColors(entryColors)
            sliceSpace = 2f
            valueTextSize = 12f
        }
        pie.data = PieData(dsPie)
        pie.description.isEnabled = false
        pie.invalidate()

        // build legend: emoji + label (count/percentage)
        llLegend.removeAllViews()
        val ctx = requireContext()
        categories.forEachIndexed { idx, emoji ->
            val cnt = counts[emoji] ?: 0
            if (cnt > 0) {
                val perc = (cnt / total * 100).toInt()
                val row = android.widget.LinearLayout(ctx).apply {
                    orientation = android.widget.LinearLayout.HORIZONTAL
                    val dot = android.view.View(ctx).apply {
                        setBackgroundColor(colors[idx])
                        layoutParams = android.widget.LinearLayout.LayoutParams(24, 24).apply { setMargins(0,0,12,0) }
                    }
                    addView(dot)
                    addView(android.widget.TextView(ctx).apply { text = "$emoji  $cnt ($perc%)" })
                }
                llLegend.addView(row)
            }
        }
    }

    private fun computeSubtitleText(): String {
        // show remaining items summary
        val habits = prefs.getHabits()
        val totalHabits = habits.size
        val doneHabits = habits.count { it.isDoneToday }
        val remaining = (totalHabits - doneHabits).coerceAtLeast(0)
        val sleepLogged = prefs.getSleepHours() > 0f
        val water = prefs.getWaterGlasses()
        val waterGoal = prefs.getWaterGoal()
        val moodLogged = prefs.getMoods().any { LocalDate.ofEpochDay(it.timestamp / (24*60*60)).isAfter(LocalDate.now().minusDays(1)) }
        return when {
            totalHabits == 0 && !sleepLogged && water == 0 && !moodLogged -> "Start tracking to see progress"
            else -> "$remaining tasks left"
        }
    }

    private fun computeOverallProgress(): Float {
        // Habits score: fraction done
        val habits = prefs.getHabits()
        val habitsScore = if (habits.isEmpty()) 0f else (habits.count { it.isDoneToday }.toFloat() / habits.size)

        // Sleep score: logged/goal capped at 1
        val sleepScore = (prefs.getSleepHours() / prefs.getSleepGoal()).coerceIn(0f, 1f)

        // Water score: glasses/goal capped at 1
        val waterScore = (prefs.getWaterGlasses().toFloat() / prefs.getWaterGoal()).coerceIn(0f, 1f)

        // Mood score: 1 if mood logged today else 0
        val moods = prefs.getMoods()
        val moodScore = if (moods.isNotEmpty()) 1f else 0f

        val avg = (habitsScore + sleepScore + waterScore + moodScore) / 4f
        return avg.coerceIn(0f, 1f)
    }

    private fun updateHydrationCard() {
        if (prefs.isHydrationReminderEnabled()) {
            binding.cardHydration.visibility = View.VISIBLE
            val intervalMillis = prefs.getHydrationReminderInterval() * 60 * 1000
            val lastHydrationTime = prefs.getLong("last_hydration_time", 0)
            val nextHydrationTime = lastHydrationTime + intervalMillis
            val remainingTime = nextHydrationTime - System.currentTimeMillis()
            val remainingMinutes = (remainingTime / (60 * 1000)).coerceAtLeast(0)
            binding.tvNextHydration.text = "Next in: $remainingMinutes minutes"
        } else {
            binding.cardHydration.visibility = View.GONE
        }
    }

    private fun startShakeListener() {
        try {
            if (accelListener != null) return
            val sm = sensorManager ?: return
            val sensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return
            accelListener = object : SensorEventListener {
                private var lastX = 0f
                private var lastY = 0f
                private var lastZ = 0f
                override fun onSensorChanged(event: SensorEvent?) {
                    event ?: return
                    val x = event.values[0]
                    val y = event.values[1]
                    val z = event.values[2]
                    val now = System.currentTimeMillis()
                    val delta = Math.abs(x - lastX) + Math.abs(y - lastY) + Math.abs(z - lastZ)
                    if (delta > 20) { // heuristic threshold
                        // debounce shakes
                        if (now - lastShakeTimestamp > 1500) {
                            lastShakeTimestamp = now
                            onShakeDetected()
                        }
                    }
                    lastX = x; lastY = y; lastZ = z
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }
            sm.registerListener(accelListener, sensor, SensorManager.SENSOR_DELAY_UI)
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun stopShakeListener() {
        try {
            val sm = sensorManager ?: return
            accelListener?.let { sm.unregisterListener(it) }
            accelListener = null
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun onShakeDetected() {
        try {
            // Quick mood log: angry emoji with note indicating shake
            val ts = System.currentTimeMillis()
            prefs.addMoodEntry(ts, "😠", "Logged by shake")
            // update UI: refresh moods view
            loadMoodData("week")
            // show a small confirmation
            Toast.makeText(requireContext(), "Angry mood logged by shake", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun updateWaterCard() {
        val water = prefs.getWaterGlasses()
        val waterGoal = prefs.getWaterGoal()
        binding.tvWaterGlasses.text = "$water / $waterGoal Glasses"
        binding.cpvWater.setStroke(3f)
        binding.cpvWater.setColors(Color.parseColor("#2F9BFF"), Color.parseColor("#DDDDDD"))
        binding.cpvWater.setProgressFraction(if (waterGoal <= 0) 0f else water.toFloat() / waterGoal)
    }

    private fun computeTimeBasedGreeting(): String {
        return try {
            val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
            when (hour) {
                in 5..11 -> "Good morning"
                in 12..16 -> "Good afternoon"
                in 17..20 -> "Good evening"
                else -> "Good night"
            }
        } catch (e: Exception) {
            "Hello"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
