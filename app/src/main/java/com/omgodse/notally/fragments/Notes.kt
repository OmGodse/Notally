package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.navigation.fragment.findNavController
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.room.BaseNote

class Notes : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        (requireContext() as MainActivity).binding.TakeNoteFAB.setOnClickListener {
            displayNoteTypes()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.Search) {
            findNavController().navigate(R.id.NotesToSearch)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
    }


    private fun displayNoteTypes() {
        val makeList = Operation(R.string.make_list, R.drawable.checkbox) { goToActivity(MakeList::class.java) }
        val takeNote = Operation(R.string.take_note, R.drawable.edit) { goToActivity(TakeNote::class.java) }
        showMenu(makeList, takeNote)
    }


    override fun getObservable() = model.baseNotes

    override fun getBackground() = R.drawable.notes

    override fun showOperations(baseNote: BaseNote) {
        val pin = if (baseNote.pinned) {
            Operation(R.string.unpin, R.drawable.pin) { model.unpinBaseNote(baseNote.id) }
        } else Operation(R.string.pin, R.drawable.pin) { model.pinBaseNote(baseNote.id) }
        val share = Operation(R.string.share, R.drawable.share) { shareBaseNote(baseNote) }
        val labels = Operation(R.string.labels, R.drawable.label) { labelBaseNote(baseNote) }
        val export = Operation(R.string.export, R.drawable.export) { exportBaseNote(baseNote) }

        val delete = Operation(R.string.delete, R.drawable.delete) { model.moveBaseNoteToDeleted(baseNote.id) }
        val archive = Operation(R.string.archive, R.drawable.archive) { model.moveBaseNoteToArchive(baseNote.id) }
        val moreOptions = Operation(R.string.more_options, R.drawable.more_options) { showMenu(delete, archive) }

        showMenu(pin, share, labels, export, moreOptions)
    }
}