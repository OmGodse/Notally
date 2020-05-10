package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.viewholders.LabelHolder
import java.util.*

class LabelsAdapter(private val context: Context, var items: ArrayList<String>) :
    RecyclerView.Adapter<LabelHolder>() {

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
        return LabelHolder(view, noteListener)
    }
}