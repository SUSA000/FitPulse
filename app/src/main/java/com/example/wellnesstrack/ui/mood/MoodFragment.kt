package com.example.wellnesstrack.ui.mood

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.GridLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wellnesstrack.R
import com.example.wellnesstrack.data.MoodEntry
import com.example.wellnesstrack.data.Prefs
import com.example.wellnesstrack.databinding.FragmentMoodBinding
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TreeMap

class MoodFragment : Fragment() {

    private var _binding: FragmentMoodBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: Prefs
    private lateinit var adapter: MoodAdapter

    private val emojis = listOf("😞","😐","🙂","😊","😄","🤩")

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMoodBinding.inflate(inflater, container, false)
        prefs = Prefs(requireContext())

        // Customize header
        binding.header.ivAvatar.visibility = View.GONE
        binding.header.tvHeaderSubtitle.visibility = View.GONE
        binding.header.tvHeaderTitle.text = "Mood Journal"

        // Recycler
        adapter = MoodAdapter()
        binding.rvMoods.layoutManager = LinearLayoutManager(requireContext())
        binding.rvMoods.adapter = adapter
        adapter.submit(prefs.getMoods().reversed())

        // Emoji picker and add
        binding.btnPickEmoji.setOnClickListener { pickEmoji() }
        binding.btnAddMood.setOnClickListener { addMood() }

        // Share summary of last 7 emojis
        binding.btnShare.setOnClickListener { shareSummary() }

        updateChart()
        setupCalendar()
        return binding.root
    }

    // --- Calendar implementation (simple) ---
    private var visibleCal = Calendar.getInstance()

    private fun setupCalendar() {
        // Populate headers S M T W T F S
        val headers = DateFormatSymbols.getInstance(Locale.getDefault()).shortWeekdays
        val glHeader = binding.glDayHeaders
        glHeader.removeAllViews()
        val days = listOf("S","M","T","W","T","F","S")
        for (d in days) {
            val tv = TextView(requireContext()).apply {
                text = d
                gravity = Gravity.CENTER
                setTextColor(resources.getColor(R.color.text_secondary, null))
                textSize = 12f
                layoutParams = GridLayout.LayoutParams().apply {
                    width = 0
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                }
            }
            glHeader.addView(tv)
        }

        // wire month nav
        binding.btnPrevMonth.setOnClickListener { visibleCal.add(Calendar.MONTH, -1); renderCalendar() }
        binding.btnNextMonth.setOnClickListener { visibleCal.add(Calendar.MONTH, 1); renderCalendar() }
        binding.btnToday.setOnClickListener { visibleCal = Calendar.getInstance(); renderCalendar() }

        renderCalendar()
    }

    private fun renderCalendar() {
        val monthTitle = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(visibleCal.time)
        binding.tvCalendarMonth.text = monthTitle

        val grid = binding.glCalendarGrid
        grid.removeAllViews()

        val cal = visibleCal.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK) // 1=Sun
        val offset = firstDow - Calendar.SUNDAY

        // number of cells: 6 rows x 7 cols = 42
        val cells = 42
        val moodsByDate = prefs.getMoods().groupBy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) }

        for (i in 0 until cells) {
            val cellIndex = i - offset + 1
            val cell = LayoutInflater.from(requireContext()).inflate(R.layout.item_calendar_day, null)
            val lp = GridLayout.LayoutParams().apply {
                width = 0
                height = ViewGroup.LayoutParams.WRAP_CONTENT
                columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            }
            cell.layoutParams = lp

            val tvDay = cell.findViewById<TextView>(R.id.tvDayNumber)
            val tvEmoji = cell.findViewById<TextView>(R.id.tvDayEmoji)

            if (cellIndex < 1 || cellIndex > cal.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                // empty cell (before or after month)
                tvDay.text = ""
                tvEmoji.text = ""
                tvDay.setTextColor(resources.getColor(R.color.text_secondary, null))
                cell.isEnabled = false
            } else {
                tvDay.text = cellIndex.toString()
                val dateCal = visibleCal.clone() as Calendar
                dateCal.set(Calendar.DAY_OF_MONTH, cellIndex)
                val key = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(dateCal.time)
                val list = moodsByDate[key]
                if (!list.isNullOrEmpty()) {
                    val latest = list.maxByOrNull { it.timestamp }
                    tvEmoji.text = latest?.emoji ?: ""
                } else {
                    tvEmoji.text = ""
                }

                // today highlight
                val today = Calendar.getInstance()
                if (dateCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) && dateCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)) {
                    cell.setBackgroundResource(R.drawable.bg_today_border)
                }

                cell.setOnClickListener {
                    showDaySheet(key)
                }
            }

            grid.addView(cell)
        }
    }

    private fun showDaySheet(dateKey: String) {
        val sheet = BottomSheetDialog(requireContext())
        val view = layoutInflater.inflate(R.layout.mood_day_sheet, null)
        val tvDate = view.findViewById<TextView>(R.id.tvSheetDate)
        val rv = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvDayMoods)
        tvDate.text = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault()).format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateKey)!!)
        val list = prefs.getMoods().filter { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(it.timestamp)) == dateKey }
        val a = MoodDayAdapter(list)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = a
        sheet.setContentView(view)
        sheet.show()
    }

    private fun pickEmoji() {
        val ad = AlertDialog.Builder(requireContext())
            .setTitle("Pick mood")
            .setAdapter(ArrayAdapter(requireContext(), android.R.layout.simple_list_item_1, emojis)) { d, which ->
                binding.btnPickEmoji.text = emojis[which]
                d.dismiss()
            }.create()
        ad.show()
    }

    private fun addMood() {
        val emoji = binding.btnPickEmoji.text.toString().ifBlank { "🙂" }
        val note = binding.etNote.text.toString()
        val entry = MoodEntry(System.currentTimeMillis(), emoji, note)
        val list = prefs.getMoods().toMutableList().apply { add(entry) }
        prefs.saveMoods(list)
        binding.etNote.text?.clear()

        // Refresh list newest-first
        adapter.submit(list.reversed())

        // Refresh chart
        updateChart()
    }

    private fun shareSummary() {
        val last7 = prefs.getMoods().takeLast(7)
        val text = if (last7.isEmpty()) {
            "My week moods: (no entries yet)"
        } else {
            "My week moods: " + last7.joinToString(" ") { it.emoji }
        }
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        startActivity(Intent.createChooser(share, "Share mood"))
    }

    private fun updateChart() {
        // average per day for last 7 days
        val map = TreeMap<String, MutableList<Int>>() // yyyy-MM-dd -> scores
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val score: (String) -> Int = { e -> (emojis.indexOf(e).takeIf { it >= 0 } ?: 2) + 1 }

        val cal = Calendar.getInstance()
        repeat(7) {
            val date = fmt.format(cal.time)
            map[date] = mutableListOf()
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        prefs.getMoods().forEach {
            val d = fmt.format(Date(it.timestamp))
            map[d]?.add(score(it.emoji))
        }

        val entries = mutableListOf<Entry>()
        var x = 0f
        map.entries.reversed().forEach { (_, list) ->
            val avg = if (list.isEmpty()) 0f else list.average().toFloat()
            entries.add(Entry(x, avg))
            x += 1f
        }

        val ds = LineDataSet(entries, "Weekly mood").apply {
            setDrawCircles(true)
            mode = LineDataSet.Mode.CUBIC_BEZIER
        }
        binding.chart.data = LineData(ds)
        binding.chart.axisLeft.axisMinimum = 0f
        binding.chart.axisLeft.axisMaximum = 6f
        binding.chart.axisRight.isEnabled = false
        binding.chart.description.isEnabled = false
        binding.chart.invalidate()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
