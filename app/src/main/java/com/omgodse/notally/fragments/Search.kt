package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.MenuDialog.Operation
import com.omgodse.notally.room.BaseNote

class Search : NotallyFragment() {

    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults

    override fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.share, R.drawable.share) { shareNote(baseNote) })
        operations.add(Operation(R.string.labels, R.drawable.label) { labelBaseNote(baseNote) })
        operations.add(Operation(R.string.export, R.drawable.export) { showExportDialog(baseNote) })
        operations.add(Operation(R.string.delete, R.drawable.delete) { model.moveBaseNoteToDeleted(baseNote.id) })
        operations.add(Operation(R.string.archive, R.drawable.archive) { model.moveBaseNoteToArchive(baseNote.id) })
        return operations
    }
}