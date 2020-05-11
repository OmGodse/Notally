package com.omgodse.notally.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.viewmodels.NoteModel
import java.io.File

class DisplayLabel : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val label = arguments?.get(Constants.argLabelKey).toString()
        model.label = label
        model.fetchRelevantNotes(getPayload())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            Constants.ResultCodeEditedFile -> {
                val filePath = data?.getStringExtra(Constants.FilePath)

                filePath?.let {
                    val file = File(filePath)
                    val note = NotesHelper.convertFileToNote(file)

                    if (!note.labels.contains(model.label)){
                        model.fetchRelevantNotes(getPayload())
                    }
                    else super.onActivityResult(requestCode, resultCode, data)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }


    override fun getPayload() = NoteModel.LABELLED_NOTES

    override fun getObservable() = model.observableLabelledNotes


    override fun getFragmentID() = R.id.DisplayLabelFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_labels)

    override fun getSupportedOperations() : ArrayList<Operation> {
        return ArrayList()
    }
}