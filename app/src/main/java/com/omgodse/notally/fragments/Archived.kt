package com.omgodse.notally.fragments

import androidx.navigation.fragment.findNavController
import com.omgodse.notally.R

class Archived : NotallyFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes

    override fun navigateToNotes() {
        findNavController().navigate(R.id.ArchivedToNotes)
    }
}