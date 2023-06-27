package com.omgodse.notally.recyclerview.viewholders

/*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.omgodse.notally.databinding.RecyclerPreviewImageBinding
import com.omgodse.notally.room.Image
import java.io.File

class PreviewImageVH(private val binding: RecyclerPreviewImageBinding, onClick: ((position: Int) -> Unit)) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.root.setOnClickListener {
            onClick(adapterPosition)
        }
    }

    fun bind(root: File, image: Image) {
        val file = File(root, image.name)

        Glide.with(binding.ImageView)
            .load(file)
            .centerCrop()
            .transition(DrawableTransitionOptions.withCrossFade())
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(binding.ImageView)
    }
}
*/