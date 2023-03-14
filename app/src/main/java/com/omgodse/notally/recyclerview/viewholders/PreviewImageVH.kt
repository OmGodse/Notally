package com.omgodse.notally.recyclerview.viewholders

import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.omgodse.notally.room.Image
import java.io.File

class PreviewImageVH(private val view: ImageView, onClick: ((position: Int) -> Unit)?) : RecyclerView.ViewHolder(view) {

    init {
        if (onClick != null) {
            view.setOnClickListener { onClick(adapterPosition) }
        }
    }

    fun bind(root: File, image: Image) {
        val file = File(root, image.name)

        Glide.with(view)
            .load(file)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(view)
    }
}