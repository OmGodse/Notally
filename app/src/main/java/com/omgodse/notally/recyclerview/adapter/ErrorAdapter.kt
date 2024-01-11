package com.omgodse.notally.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.ErrorBinding
import com.omgodse.notally.image.ImageError
import com.omgodse.notally.recyclerview.viewholder.ErrorVH

class ErrorAdapter(private val items: List<ImageError>) : RecyclerView.Adapter<ErrorVH>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ErrorVH, position: Int) {
        val error = items[position]
        holder.bind(error)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ErrorVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ErrorBinding.inflate(inflater, parent, false)
        return ErrorVH(binding)
    }
}