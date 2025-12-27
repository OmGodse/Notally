package com.omgodse.notally.fragments

import android.content.Context
import android.graphics.drawable.Drawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.shape.MaterialShapeDrawable
import com.google.android.material.shape.RelativeCornerSize
import com.google.android.material.shape.RoundedCornerTreatment
import com.google.android.material.shape.ShapeAppearanceModel
import com.omgodse.notally.R
import com.omgodse.notally.room.Folder

class Search : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.Notes?.background = getRippleDrawable(requireContext())
        binding?.Deleted?.background = getRippleDrawable(requireContext())
        binding?.Archived?.background = getRippleDrawable(requireContext())

        val checked = when (model.folder) {
            Folder.NOTES -> R.id.Notes
            Folder.DELETED -> R.id.Deleted
            Folder.ARCHIVED -> R.id.Archived
        }
        binding?.RadioGroup?.check(checked)

        binding?.RadioGroup?.visibility = View.VISIBLE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            binding?.RecyclerView?.scrollIndicators = View.SCROLL_INDICATOR_TOP
        }

        binding?.RadioGroup?.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.Notes -> model.folder = Folder.NOTES
                R.id.Deleted -> model.folder = Folder.DELETED
                R.id.Archived -> model.folder = Folder.ARCHIVED
            }
        }
    }

    private fun getRippleDrawable(context: Context): Drawable {
        val model = ShapeAppearanceModel.builder()
            .setAllCorners(RoundedCornerTreatment())
            .setAllCornerSizes(RelativeCornerSize(0.5f))
            .build()

        val base = MaterialShapeDrawable(model)
        base.strokeWidth = context.resources.displayMetrics.density
        base.strokeColor = ContextCompat.getColorStateList(context, R.color.chip_stroke)
        base.fillColor = ContextCompat.getColorStateList(context, R.color.chip_background)

        val ripple = requireNotNull(ContextCompat.getColorStateList(context, R.color.chip_ripple))
        return RippleDrawable(ripple, base, null)
    }


    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults
}