package com.omgodse.notally.fragments

import com.omgodse.notally.R
import com.omgodse.notally.room.BaseNote

class Search : NotallyFragment() {

    private fun moreOperations(baseNote: BaseNote) {
        val delete = Operation(R.string.delete, R.drawable.delete) { model.moveBaseNoteToDeleted(baseNote.id) }
        val archive = Operation(R.string.archive, R.drawable.archive) { model.moveBaseNoteToArchive(baseNote.id) }
        showMenu(delete, archive)
    }


    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults

    override fun showOperations(baseNote: BaseNote) {
        val pin = if (baseNote.pinned) {
            Operation(R.string.unpin, R.drawable.pin) { model.unpinBaseNote(baseNote.id) }
        } else Operation(R.string.pin, R.drawable.pin) { model.pinBaseNote(baseNote.id) }
        val share = Operation(R.string.share, R.drawable.share) { shareBaseNote(baseNote) }
        val labels = Operation(R.string.labels, R.drawable.label) { labelBaseNote(baseNote) }
        val export = Operation(R.string.export, R.drawable.export) { exportBaseNote(baseNote) }
        val moreOptions = Operation(R.string.more_options, R.drawable.more_options) { moreOperations(baseNote) }
        showMenu(pin, share, labels, export, moreOptions)
    }
}