package com.omgodse.notally.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.NoteListener

class LabelHolder(view: View, noteListener: NoteListener?) : RecyclerView.ViewHolder(view) {
    val displayLabel: MaterialTextView = view.findViewById(R.id.DisplayLabel)

    init {
        if (noteListener != null){
            view.setOnLongClickListener {
                noteListener.onNoteLongClicked(adapterPosition)
                return@setOnLongClickListener true
            }
            view.setOnClickListener {
                noteListener.onNoteClicked(adapterPosition)
            }
        }
    }
}