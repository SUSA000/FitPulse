package com.example.wellnesstrack.ui.habits

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.recyclerview.widget.RecyclerView
import com.example.wellnesstrack.data.Habit
import com.example.wellnesstrack.databinding.ItemHabitBinding

class HabitsAdapter(
    private val items: MutableList<Habit>,
    private val onToggle: () -> Unit,            // called when a checkbox is toggled
    private val onDelete: (Int) -> Unit,         // called with adapter index to delete
    private val onEdited: () -> Unit             // called after an edit to persist + refresh progress
) : RecyclerView.Adapter<HabitsAdapter.VH>() {

    inner class VH(val b: ItemHabitBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHabitBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val h = items[position]

        // Bind text and checkbox
        holder.b.tvTitle.text = h.title
        holder.b.cbDone.setOnCheckedChangeListener(null)
        holder.b.cbDone.isChecked = h.isDoneToday
        holder.b.cbDone.setOnCheckedChangeListener { _, checked ->
            h.isDoneToday = checked
            onToggle()
        }

        // Edit habit title
        holder.b.btnEdit.setOnClickListener {
            val ctx = holder.itemView.context
            val input = EditText(ctx).apply { setText(h.title) }
            AlertDialog.Builder(ctx)
                .setTitle("Edit habit")
                .setView(input)
                .setPositiveButton("Save") { _, _ ->
                    val newTitle = input.text.toString().trim()
                    if (newTitle.isNotEmpty()) {
                        h.title = newTitle
                        notifyItemChanged(holder.adapterPosition)
                        onEdited()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        // Delete
        holder.b.btnDelete.setOnClickListener {
            val idx = holder.adapterPosition
            if (idx != RecyclerView.NO_POSITION) onDelete(idx)
        }
    }

    override fun getItemCount() = items.size
}
