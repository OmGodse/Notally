package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.MenuDialog.Operation
import com.omgodse.notally.room.BaseNote

class Deleted : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.DeleteAll) {
            confirmDeletionOfAllNotes()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.delete_all, menu)
    }


    private fun confirmDeletionOfAllNotes() {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_all_notes)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteAllBaseNotes()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun confirmDeletionOfSingleNote(baseNote: BaseNote) {
        MaterialAlertDialogBuilder(requireContext())
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever(baseNote)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }


    override fun getBackground() = R.drawable.delete

    override fun getObservable() = model.deletedNotes

    override fun getSupportedOperations(baseNote: BaseNote): ArrayList<Operation> {
        val operations = ArrayList<Operation>()
        operations.add(Operation(R.string.restore, R.drawable.restore) { model.restoreBaseNote(baseNote.id) })
        operations.add(Operation(R.string.delete_forever, R.drawable.delete) { confirmDeletionOfSingleNote(baseNote) })
        return operations
    }
}