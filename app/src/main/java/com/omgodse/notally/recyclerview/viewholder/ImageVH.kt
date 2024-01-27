package com.omgodse.notally.recyclerview.viewholder

import android.net.Uri
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView.DefaultOnImageEventListener
import com.omgodse.notally.databinding.RecyclerImageBinding
import java.io.File

class ImageVH(private val binding: RecyclerImageBinding) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.SSIV.setDoubleTapZoomDpi(320)
        binding.SSIV.setDoubleTapZoomDuration(200)
        binding.SSIV.setDoubleTapZoomStyle(SubsamplingScaleImageView.ZOOM_FOCUS_CENTER)
        binding.SSIV.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF

        binding.SSIV.setOnImageEventListener(object : DefaultOnImageEventListener() {

            override fun onImageLoadError(e: Exception?) {
                binding.Message.visibility = View.VISIBLE
            }
        })
    }

    fun bind(file: File?) {
        binding.SSIV.recycle()
        if (file != null) {
            binding.Message.visibility = View.GONE
            val source = ImageSource.uri(Uri.fromFile(file))
            binding.SSIV.setImage(source)
        } else binding.Message.visibility = View.VISIBLE
    }
}