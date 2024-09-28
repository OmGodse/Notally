package com.omgodse.notally.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerSelectableLabelBinding

class SelectableLabelVH(
    private val binding: RecyclerSelectableLabelBinding,
    private val onChecked: (position: Int, checked: Boolean) -> Unit
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnCheckedChangeListener { _, isChecked ->
            onChecked(adapterPosition, isChecked)
        }
    }

    fun bind(value: String, checked: Boolean) {
        binding.root.text = value
        binding.root.isChecked = checked
    }
}