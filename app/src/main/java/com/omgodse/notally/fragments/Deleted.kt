package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class Deleted : NotallyFragment() {

    override fun getObservable() = model.deletedNotes

    override fun getFragmentID() = R.id.DeletedFragment

    override fun getBackground() = R.drawable.colored_delete

    override fun getSupportedOperations(notesHelper: NotesHelper, baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.restore, R.drawable.restore) { model.restoreFile(baseNote.filePath) })
        operations.add(Operation(R.string.delete_forever, R.drawable.delete) { confirmDeletion(baseNote) })
        return operations
    }
}