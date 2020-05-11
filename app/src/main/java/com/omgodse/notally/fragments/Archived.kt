package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.viewmodels.NoteModel

class Archived : NotallyFragment() {

    override fun getPayload() = NoteModel.ARCHIVED_NOTES

    override fun getObservable() = model.observableArchivedNotes


    override fun getFragmentID() = R.id.ArchivedFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_archived)

    override fun getSupportedOperations() : ArrayList<Operation> {
        val supportedOperations = ArrayList<Operation>()
        supportedOperations.add(Operation(R.string.share, R.drawable.share))
        supportedOperations.add(Operation(R.string.labels, R.drawable.label))
        supportedOperations.add(Operation(R.string.unarchive, R.drawable.unarchive))
        return supportedOperations
    }
}