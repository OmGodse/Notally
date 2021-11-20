package com.omgodse.notally.fragments

import com.omgodse.notally.R

class Archived : NotallyFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes
}