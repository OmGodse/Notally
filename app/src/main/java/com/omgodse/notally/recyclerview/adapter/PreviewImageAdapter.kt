package com.omgodse.notally.recyclerview.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.RecyclerPreviewImageBinding
import com.omgodse.notally.recyclerview.viewholder.PreviewImageVH
import com.omgodse.notally.room.Image
import java.io.File

class PreviewImageAdapter(private val imageRoot: File?, private val onClick: (position: Int) -> Unit) :
    ListAdapter<Image, PreviewImageVH>(DiffCallback) {

    override fun onBindViewHolder(holder: PreviewImageVH, position: Int) {
        val image = getItem(position)
        val file = if (imageRoot != null) File(imageRoot, image.name) else null
        holder.bind(file)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewImageVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerPreviewImageBinding.inflate(inflater, parent, false)
        return PreviewImageVH(binding, onClick)
    }

    private object DiffCallback : DiffUtil.ItemCallback<Image>() {

        override fun areItemsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Image, newItem: Image): Boolean {
            return oldItem == newItem
        }
    }
}