package com.omgodse.notally.recyclerview.viewholders

import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerLabelBinding
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.Label

class LabelsViewHolder(private val binding: RecyclerLabelBinding, itemListener: ItemListener) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            itemListener.onClick(adapterPosition)
        }

        binding.root.setOnLongClickListener {
            itemListener.onLongClick(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(label: Label) {
        binding.root.text = label.value
    }
}