package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.MenuHelper.Operation
import com.omgodse.notally.room.BaseNote

class Archived : NotallyFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes

    override fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.unarchive, R.drawable.unarchive) { model.restoreBaseNote(baseNote.id) })
        return operations
    }
}