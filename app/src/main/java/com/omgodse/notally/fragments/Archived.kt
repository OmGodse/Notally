package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.room.BaseNote

class Archived : NotallyFragment() {

    override fun getBackground() = R.drawable.archive

    override fun getObservable() = model.archivedNotes

    override fun showOperations(baseNote: BaseNote) {
        val unarchive = Operation(R.string.unarchive, R.drawable.unarchive) { model.restoreBaseNote(baseNote.id) }
        showMenu(unarchive)
    }
}