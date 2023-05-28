package com.omgodse.notally.fragments

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.TypedValue
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.navigation.fragment.findNavController
import com.google.android.material.shape.MaterialShapeDrawable
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.miscellaneous.add

class Notes : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupEditContainer()
        super.onViewCreated(view, savedInstanceState)

        binding?.TakeNote?.setOnClickListener {
            goToActivity(TakeNote::class.java)
        }
        binding?.MakeList?.setOnClickListener {
            goToActivity(MakeList::class.java)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.search, R.drawable.search) { findNavController().navigate(R.id.NotesToSearch) }
    }


    private fun setupEditContainer() {
        val container = requireNotNull(binding).EditContainer

        val drawable = MaterialShapeDrawable()
        val surface = TypedValue()
        requireContext().theme.resolveAttribute(com.google.android.material.R.attr.colorSurface, surface, true)

        drawable.elevation = container.elevation
        drawable.initializeElevationOverlay(requireContext())
        drawable.fillColor = ColorStateList.valueOf(surface.data)
        drawable.shadowCompatibilityMode = MaterialShapeDrawable.SHADOW_COMPAT_MODE_ALWAYS

        container.background = drawable
        container.visibility = View.VISIBLE
    }


    override fun getObservable() = model.baseNotes

    override fun getBackground() = R.drawable.notebook
}