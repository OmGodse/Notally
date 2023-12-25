package com.omgodse.notally.recyclerview.viewholder

import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerColorBinding
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.Color

class ColorVH(private val binding: RecyclerColorBinding, listener: ItemListener) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.CardView.setOnClickListener {
            listener.onClick(adapterPosition)
        }
    }

    fun bind(color: Color) {
        val value = Operations.extractColor(color, binding.root.context)
        binding.CardView.setCardBackgroundColor(value)
    }
}