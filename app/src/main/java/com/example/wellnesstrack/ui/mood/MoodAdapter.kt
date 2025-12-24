package com.example.wellnesstrack.ui.mood

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesstrack.R
import com.example.wellnesstrack.data.MoodEntry

class MoodAdapter : RecyclerView.Adapter<MoodAdapter.VH>() {

    private var items: List<MoodEntry> = emptyList()

    fun submit(list: List<MoodEntry>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View): RecyclerView.ViewHolder(v) {
        val emoji: TextView = v.findViewById(R.id.tvEmoji)
        val time: TextView = v.findViewById(R.id.tvWhen)
        val note: TextView = v.findViewById(R.id.tvNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_mood, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val m = items[position]
        h.emoji.text = m.emoji
        h.time.text = DateFormat.format("MMM d, h:mm a", m.timestamp)
        h.note.text = m.note ?: ""
    }

    override fun getItemCount(): Int = items.size
}
