package com.example.wellnesstrack.ui.habits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wellnesstrack.data.Habit
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentHabitsBinding

class HabitsFragment : Fragment() {

    private var _binding: FragmentHabitsBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var adapter: HabitsAdapter
    private lateinit var habits: MutableList<Habit>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
        prefs.resetIfNewDay()
        habits = prefs.getHabits() // returns MutableList<Habit>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHabitsBinding.inflate(inflater, container, false)

        // Customize header
        binding.header.ivAvatar.visibility = View.GONE
        binding.header.tvHeaderSubtitle.visibility = View.GONE
        binding.header.tvHeaderTitle.text = "Habits"

        // Adapter with callbacks: toggle, delete, edit->persist+progress
        adapter = HabitsAdapter(
            items = habits,
            onToggle = { persistAndUpdate() },
            onDelete = { idx ->
                habits.removeAt(idx)
                adapter.notifyItemRemoved(idx)
                persistAndUpdate()
            },
            onEdited = { persistAndUpdate() }
        )

        binding.rvHabits.layoutManager = LinearLayoutManager(requireContext())
        binding.rvHabits.adapter = adapter

        binding.btnAdd.setOnClickListener {
            val title = binding.etHabit.text.toString().trim()
            if (title.isNotEmpty()) {
                habits.add(Habit(title = title, isDoneToday = false))
                adapter.notifyItemInserted(habits.lastIndex)
                binding.etHabit.setText("")
                persistAndUpdate()
            }
        }

        // Initialize health metrics UI
        fun refreshHealthUi() {
            val sleep = prefs.getSleepHours()
            val sleepGoal = prefs.getSleepGoal()
            binding.tvSleepSummary.text = "${sleep} / ${sleepGoal} Hours"

            val water = prefs.getWaterGlasses()
            val waterGoal = prefs.getWaterGoal()
            binding.tvWaterSummary.text = "$water / $waterGoal Glasses"
        }

        binding.btnLogSleep.setOnClickListener {
            // simple input dialog for hours (float)
            val input = EditText(requireContext())
            input.hint = "e.g. 7.5"
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("How many hours did you sleep?")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val vstr = input.text.toString().trim()
                    val hours = vstr.toFloatOrNull() ?: 0f
                    prefs.setSleepHours(hours)
                    refreshHealthUi()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        binding.btnAddGlass.setOnClickListener {
            val cur = prefs.getWaterGlasses()
            prefs.setWaterGlasses(cur + 1)
            refreshHealthUi()
        }

        binding.btnResetWater.setOnClickListener {
            prefs.setWaterGlasses(0)
            refreshHealthUi()
        }

        refreshHealthUi()

        persistAndUpdate()
        return binding.root
    }

    private fun persistAndUpdate() {
        prefs.saveHabits(habits)
        val total = habits.size.coerceAtLeast(1)
        val done = habits.count { it.isDoneToday }
        val pct = (done * 100) / total
        binding.progress.progress = pct
        binding.tvProgress.text = "Progress: $pct%"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
