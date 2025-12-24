package com.example.wellnesstrack.ui.mood

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesstrack.R
import com.example.wellnesstrack.data.MoodEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MoodDayAdapter(private val items: List<MoodEntry>): RecyclerView.Adapter<MoodDayAdapter.VH>() {
    class VH(v: View): RecyclerView.ViewHolder(v) {
        val tvTime: TextView = v.findViewById(R.id.tvTime)
        val tvEmoji: TextView = v.findViewById(R.id.tvEmoji)
        val tvNote: TextView = v.findViewById(R.id.tvNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood_day_entry, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val fmt = SimpleDateFormat("h:mm a", Locale.getDefault())
        holder.tvTime.text = fmt.format(Date(m.timestamp))
        holder.tvEmoji.text = m.emoji
        holder.tvNote.text = m.note
    }

    override fun getItemCount(): Int = items.size
}
