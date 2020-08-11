package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.View
import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class DisplayLabel : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val label = arguments?.get(Constants.argLabelKey).toString()
        model.label = label
        model.fetchLabelledNotes()
    }


    override fun getObservable() = model.labelledNotes

    override fun getFragmentID() = R.id.DisplayLabelFragment

    override fun getBackground() = R.drawable.colored_label

    override fun getSupportedOperations(notesHelper: NotesHelper, baseNote: BaseNote): ArrayList<Operation> {
        return ArrayList()
    }
}