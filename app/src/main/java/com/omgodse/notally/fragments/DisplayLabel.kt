package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.livedata.Content

class DisplayLabel : NotallyFragment() {

    override fun getBackground() = R.drawable.label

    override fun getObservable(): Content<BaseNote> {
        val label = requireNotNull(requireArguments().getString(Constants.SelectedLabel))
        return model.getNotesByLabel(label)
    }
}