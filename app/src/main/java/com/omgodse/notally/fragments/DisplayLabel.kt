package com.omgodse.notally.fragments

import androidx.lifecycle.LiveData
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.room.Item

class DisplayLabel : NotallyFragment() {

    override fun getBackground() = R.drawable.label

    override fun getObservable(): LiveData<List<Item>> {
        val label = requireNotNull(requireArguments().getString(Constants.SelectedLabel))
        return model.getNotesByLabel(label)
    }
}