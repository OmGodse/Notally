package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsHelper
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class Archived : NotallyFragment() {

    override fun getObservable() = model.archivedNotes

    override fun getFragmentID() = R.id.ArchivedFragment

    override fun getBackground() = R.drawable.archive

    override fun getSupportedOperations(operationsHelper: OperationsHelper, baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.unarchive, R.drawable.unarchive) { model.restoreBaseNote(baseNote) })
        return operations
    }
}