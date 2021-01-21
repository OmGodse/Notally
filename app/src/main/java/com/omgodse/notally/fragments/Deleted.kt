package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.MenuHelper.Operation
import com.omgodse.notally.room.BaseNote

class Deleted : NotallyFragment() {

    override fun getBackground() = R.drawable.delete

    override fun getObservable() = model.deletedNotes

    override fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.restore, R.drawable.restore) { model.restoreBaseNote(baseNote.id) })
        operations.add(Operation(R.string.delete_forever, R.drawable.delete) { confirmDeletion(baseNote) })
        return operations
    }
}