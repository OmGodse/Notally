package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsHelper
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class Deleted : NotallyFragment() {

    override fun getObservable() = model.deletedNotes

    override fun getFragmentID() = R.id.DeletedFragment

    override fun getBackground() = R.drawable.delete

    override fun getSupportedOperations(operationsHelper: OperationsHelper, baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.restore, R.drawable.restore) { model.restoreBaseNote(baseNote) })
        operations.add(Operation(R.string.delete_forever, R.drawable.delete) { confirmDeletion(baseNote) })
        return operations
    }
}