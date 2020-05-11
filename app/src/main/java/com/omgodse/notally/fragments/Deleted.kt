package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.viewmodels.NoteModel

class Deleted : NotallyFragment() {

    override fun getPayload() = NoteModel.DELETED_NOTES

    override fun getObservable() = model.observableDeletedNotes


    override fun getFragmentID() = R.id.DeletedFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_delete)

    override fun getSupportedOperations(): ArrayList<Operation> {
        val supportedOperations = ArrayList<Operation>()
        supportedOperations.add(Operation(R.string.restore, R.drawable.restore))
        supportedOperations.add(Operation(R.string.delete_forever, R.drawable.delete))
        return supportedOperations
    }
}