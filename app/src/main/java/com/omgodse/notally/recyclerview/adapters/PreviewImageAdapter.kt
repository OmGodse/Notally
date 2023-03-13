package com.omgodse.notally.recyclerview.adapters

import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.recyclerview.viewholders.PreviewImageVH
import com.omgodse.notally.room.Image
import java.io.File

class PreviewImageAdapter(private val root: File) : ListAdapter<Image, PreviewImageVH>(DiffCallback) {

    var onClick: ((position: Int) -> Unit)? = null

    override fun onBindViewHolder(holder: PreviewImageVH, position: Int) {
        val image = getItem(position)
        holder.bind(root, image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PreviewImageVH {
        val view = ImageView(parent.context)
        view.layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        return PreviewImageVH(view, onClick)
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