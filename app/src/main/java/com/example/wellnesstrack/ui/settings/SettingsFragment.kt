package com.example.wellnesstrack.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentSettingsBinding
import com.example.wellnesstrack.reminders.HydrationWorker
import com.example.wellnesstrack.reminders.MedicineScheduler
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesstrack.R
import java.util.Calendar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.wellnesstrack.reminders.AlarmHelper
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.TimeFormat
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.wellnesstrack.reminders.HabitReminderWorker
import com.example.wellnesstrack.reminders.SleepReminderWorker
import java.util.concurrent.TimeUnit

class SettingsFragment : Fragment() {

    private lateinit var prefs: Prefs
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    companion object {
        const val SLEEP_REMINDER_WORKER_TAG = "sleep_reminder_worker"
        const val HABIT_REMINDER_WORKER_TAG = "habit_reminder_worker"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val view = binding.root

        val headerTitle = view.findViewById<TextView?>(R.id.tvHeaderTitle)
        val headerSubtitle = view.findViewById<TextView?>(R.id.tvHeaderSubtitle)

        headerTitle?.text = "Settings"
        headerSubtitle?.visibility = View.GONE

        loadSettings()
        setupClickListeners()
        setupHydrationSpinner()
        setupMedicineList()

        return view
    }

    private fun loadSettings() {
        binding.swSleepReminder.isChecked = prefs.isSleepReminderEnabled()
        binding.tvSleepTime.text = prefs.getSleepReminderTime()

        binding.swHydrationReminder.isChecked = prefs.isHydrationReminderEnabled()

        binding.swHabitReminder.isChecked = prefs.isHabitReminderEnabled()
        binding.tvHabitTime.text = prefs.getHabitReminderTime()

        binding.swMedicineReminder.isChecked = prefs.isMedicineRemindersEnabled()

        binding.rvMedicineTimes.visibility = View.VISIBLE
        binding.btnAddMedicineTime.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.rlSleep.setOnClickListener {
            showTimePickerDialog(binding.tvSleepTime) { hour, minute ->
                val newTime = String.format("%02d:%02d", hour, minute)
                prefs.setSleepReminderTime(newTime)
                scheduleSleepReminder()
            }
        }
        binding.swSleepReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.setSleepReminderEnabled(isChecked)
            if (isChecked) scheduleSleepReminder() else cancelSleepReminder()
        }

        binding.swHydrationReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.setHydrationReminderEnabled(isChecked)
            if (isChecked) {
                // record last hydration time as now so user enabling doesn't immediately trigger a notification
                prefs.saveLong("last_hydration_time", System.currentTimeMillis())
                HydrationWorker.reschedule(requireContext())
            } else HydrationWorker.cancel(requireContext())
        }

        binding.rlHabit.setOnClickListener {
            showTimePickerDialog(binding.tvHabitTime) { hour, minute ->
                val newTime = String.format("%02d:%02d", hour, minute)
                prefs.setHabitReminderTime(newTime)
                scheduleHabitReminder()
            }
        }
        binding.swHabitReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.setHabitReminderEnabled(isChecked)
            if (isChecked) scheduleHabitReminder() else cancelHabitReminder()
        }

        binding.swMedicineReminder.setOnCheckedChangeListener { _, isChecked ->
            prefs.setMedicineRemindersEnabled(isChecked)
            if (isChecked) {
                MedicineScheduler.reschedule(requireContext())
                checkAndPromptNotificationPermission()
            } else {
                MedicineScheduler.cancel(requireContext())
            }
            binding.rvMedicineTimes.visibility = View.VISIBLE
            binding.btnAddMedicineTime.visibility = View.VISIBLE
        }

        binding.btnAddMedicineTime.setOnClickListener {
             val now = Calendar.getInstance()
            val picker = MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(now.get(Calendar.HOUR_OF_DAY))
                .setMinute(now.get(Calendar.MINUTE))
                .setTitleText("Add Medicine Reminder")
                .build()

            picker.addOnPositiveButtonClickListener { 
                val hour = picker.hour
                val minute = picker.minute
                val time = String.format("%02d:%02d", hour, minute)
                prefs.addMedicineReminder(time)
                refreshMedicineList()
                MedicineScheduler.reschedule(requireContext()) 
            }
            picker.show(childFragmentManager, "medicineTimePicker")
        }
    }

    private fun setupMedicineList() {
        binding.rvMedicineTimes.layoutManager = LinearLayoutManager(requireContext())
        refreshMedicineList()
    }

    private fun refreshMedicineList() {
        val list = prefs.getMedicineReminders() ?: emptyList()
        val canAdd = list.size < 5
        binding.btnAddMedicineTime.isEnabled = canAdd
        binding.btnAddMedicineTime.alpha = if (canAdd) 1f else 0.55f
        binding.rvMedicineTimes.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
                val v = LayoutInflater.from(parent.context).inflate(R.layout.item_medicine_time, parent, false)
                return object : RecyclerView.ViewHolder(v) {}
            }

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val time = list[position]
                val parts = time.split(":")
                val h = parts.getOrNull(0)?.toIntOrNull() ?: 0
                val m = parts.getOrNull(1)?.toIntOrNull() ?: 0
                val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, h); set(Calendar.MINUTE, m) }
                val display = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(cal.time)
                holder.itemView.findViewById<TextView>(R.id.tvMedicineTime).text = display
                holder.itemView.findViewById<ImageView>(R.id.ivDeleteMedicine).setOnClickListener {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Remove reminder")
                        .setMessage("Remove reminder at $display ?")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Remove") { _, _ ->
                            prefs.removeMedicineReminder(time)
                            refreshMedicineList()
                            MedicineScheduler.reschedule(requireContext())
                        }.show()
                }
            }

            override fun getItemCount(): Int = list.size
        }
    }

    private fun setupHydrationSpinner() {
        val intervals = arrayOf(15, 30, 60, 90, 120)
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, intervals.map { "$it min" })
        binding.spinnerHydrationInterval.adapter = adapter

        val savedInterval = prefs.getHydrationReminderInterval()
        val selection = intervals.indexOf(savedInterval)
        if (selection != -1) {
            binding.spinnerHydrationInterval.setSelection(selection)
        }

        binding.spinnerHydrationInterval.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                prefs.setHydrationReminderInterval(intervals[position])
                if (prefs.isHydrationReminderEnabled()) HydrationWorker.reschedule(requireContext())
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun showTimePickerDialog(timeTextView: TextView, onTimeSet: (hour: Int, minute: Int) -> Unit) {
        val timeParts = timeTextView.text.toString().split(":")
        val initialHour = timeParts.getOrNull(0)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val initialMinute = timeParts.getOrNull(1)?.toIntOrNull() ?: Calendar.getInstance().get(Calendar.MINUTE)

        val picker = MaterialTimePicker.Builder()
            .setTimeFormat(TimeFormat.CLOCK_24H)
            .setHour(initialHour)
            .setMinute(initialMinute)
            .setTitleText("Select Reminder Time")
            .build()

        picker.addOnPositiveButtonClickListener {
            val selectedHour = picker.hour
            val selectedMinute = picker.minute
            val timeString = String.format("%02d:%02d", selectedHour, selectedMinute)
            timeTextView.text = timeString
            onTimeSet(selectedHour, selectedMinute)
        }

        picker.show(childFragmentManager, "timePicker")
    }

    private fun scheduleSleepReminder() {
        if (!prefs.isSleepReminderEnabled()) return

        val time = prefs.getSleepReminderTime().split(":")
        val hour = time.getOrNull(0)?.toIntOrNull() ?: 22
        val minute = time.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DATE, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<SleepReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            SLEEP_REMINDER_WORKER_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelSleepReminder() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(SLEEP_REMINDER_WORKER_TAG)
    }

    private fun scheduleHabitReminder() {
        if (!prefs.isHabitReminderEnabled()) return

        val time = prefs.getHabitReminderTime().split(":")
        val hour = time.getOrNull(0)?.toIntOrNull() ?: 9
        val minute = time.getOrNull(1)?.toIntOrNull() ?: 0

        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }
        if (target.before(now)) {
            target.add(Calendar.DATE, 1)
        }

        val initialDelay = target.timeInMillis - now.timeInMillis

        val workRequest = PeriodicWorkRequestBuilder<HabitReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            HABIT_REMINDER_WORKER_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun cancelHabitReminder() {
        WorkManager.getInstance(requireContext()).cancelUniqueWork(HABIT_REMINDER_WORKER_TAG)
    }

    private fun areNotificationsEnabled(): Boolean {
        val nm = requireContext().getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        return nm.areNotificationsEnabled()
    }

    private fun checkAndPromptNotificationPermission() {
        if (!areNotificationsEnabled()) {
            AlertDialog.Builder(requireContext())
                .setTitle("Enable notifications")
                .setMessage("This device is blocking notifications. To receive reminders, please enable notifications in system settings.")
                .setPositiveButton("Open Settings") { _, _ ->
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, requireContext().packageName)
                    }
                    startActivity(intent)
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
