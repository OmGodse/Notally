package com.omgodse.notally.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerImageBinding
import com.omgodse.notally.recyclerview.viewholder.ImageVH
import com.omgodse.notally.room.Image
import java.io.File

class ImageAdapter(private val mediaRoot: File?, val items: ArrayList<Image>) : RecyclerView.Adapter<ImageVH>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ImageVH, position: Int) {
        val image = items[position]
        val file = if (mediaRoot != null) File(mediaRoot, image.name) else null
        holder.bind(file)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerImageBinding.inflate(inflater, parent, false)
        return ImageVH(binding)
    }
}