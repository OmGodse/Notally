package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.room.Folder

class Search : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.filter, R.drawable.filter) { showFilterDialog() }
    }


    private fun showFilterDialog() {
        val items = Operations.createArray(requireContext(), R.string.notes, R.string.deleted, R.string.archived)
        val checked = when (model.folder) {
            Folder.NOTES -> 0
            Folder.DELETED -> 1
            Folder.ARCHIVED -> 2
        }
        MaterialAlertDialogBuilder(requireContext()).setTitle(R.string.filter)
            .setSingleChoiceItems(items, checked) { dialog, index ->
                dialog.cancel()
                when (index) {
                    0 -> model.folder = Folder.NOTES
                    1 -> model.folder = Folder.DELETED
                    2 -> model.folder = Folder.ARCHIVED
                }
            }.setNegativeButton(R.string.cancel, null).show()
    }


    override fun getBackground() = R.drawable.search

    override fun getObservable() = model.searchResults
}