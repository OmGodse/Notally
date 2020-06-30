package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R

class LabelsAdapter(private val context: Context) :
    ListAdapter<String, LabelsAdapter.ViewHolder>(DiffCallback()) {

    var onLabelClicked: ((position: Int) -> Unit)? = null
    var onLabelLongClicked: ((position: Int) -> Unit)? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val label = getItem(position)
        holder.displayLabel.text = label
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.label_item, parent, false)
        return ViewHolder(view)
    }


    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val displayLabel: MaterialTextView = view.findViewById(R.id.DisplayLabel)

        init {
            view.setOnClickListener {
                onLabelClicked?.invoke(adapterPosition)
            }

            view.setOnLongClickListener {
                onLabelLongClicked?.invoke(adapterPosition)
                return@setOnLongClickListener true
            }
        }
    }
}