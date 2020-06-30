package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class Archived : NotallyFragment() {

    override fun getObservable() = model.archivedNotes

    override fun getFragmentID() = R.id.ArchivedFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_archived)

    override fun getSupportedOperations(notesHelper: NotesHelper, baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.share, R.drawable.share) { notesHelper.shareNote(baseNote) })
        operations.add(Operation(R.string.labels, R.drawable.label) { labelBaseNote(baseNote) })
        operations.add(Operation(R.string.unarchive, R.drawable.unarchive) { model.restoreFile(baseNote.filePath) })
        return operations
    }
}