package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.NoteListener
import java.util.*

class LabelsAdapter(private val context: Context, var items: ArrayList<String>) :
    RecyclerView.Adapter<LabelsAdapter.LabelHolder>() {

    var noteListener: NoteListener? = null

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: LabelHolder, position: Int) {
        val label = items[position]
        holder.displayLabel.text = label
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.label_item, parent, false)
        return LabelHolder(view)
    }


    inner class LabelHolder(view: View) : RecyclerView.ViewHolder(view) {
        val displayLabel: MaterialTextView = view.findViewById(R.id.DisplayLabel)

        init {
            view.setOnLongClickListener {
                noteListener?.onNoteLongClicked(adapterPosition)
                return@setOnLongClickListener true
            }
            view.setOnClickListener {
                noteListener?.onNoteClicked(adapterPosition)
            }
        }
    }
}