package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.MenuDialog.Operation
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.room.BaseNote

class DisplayLabel : NotallyFragment() {

    private val label by lazy { requireNotNull(requireArguments().getString(Constants.SelectedLabel)) }

    override fun getBackground() = R.drawable.label

    override fun getObservable() = model.getNotesByLabel(label)

    override fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation> {
        return ArrayList()
    }
}