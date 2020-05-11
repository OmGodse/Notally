package com.omgodse.notally.fragments

import android.content.Intent
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
import com.omgodse.notally.helpers.ExportHelper
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.viewmodels.NoteModel

class Notes : NotallyFragment() {

    private lateinit var exportHelper: ExportHelper

    override fun onResume() {
        super.onResume()
        model.fetchRelevantNotes(getPayload())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        exportHelper = ExportHelper(mContext, this)
        (mContext as MainActivity).binding.TakeNoteFAB.setOnClickListener {
            displayNoteTypes()
        }
    }


    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.Search) {
            findNavController().navigate(R.id.NotesFragmentToSearchFragment)
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
    }


    private fun displayNoteTypes() {
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.make_list, R.drawable.checkbox)
        menuHelper.addItem(R.string.take_note, R.drawable.edit)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.make_list) -> {
                        val intent = Intent(mContext, MakeList::class.java)
                        intent.putExtra(Constants.PreviousFragment, R.id.NotesFragment)
                        startActivityForResult(intent, Constants.RequestCode)
                    }
                    mContext.getString(R.string.take_note) -> {
                        val intent = Intent(mContext, TakeNote::class.java)
                        intent.putExtra(Constants.PreviousFragment, R.id.NotesFragment)
                        startActivityForResult(intent, Constants.RequestCode)
                    }
                }
            }
        })

        menuHelper.show()
    }


    override fun getPayload() = NoteModel.NOTES

    override fun getObservable() = model.observableNotes


    override fun getFragmentID() = R.id.NotesFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_notes)

    override fun getSupportedOperations() : ArrayList<Operation> {
        val supportedOperations = ArrayList<Operation>()
        supportedOperations.add(Operation(R.string.share, R.drawable.share))
        supportedOperations.add(Operation(R.string.labels, R.drawable.label))
        supportedOperations.add(Operation(R.string.export, R.drawable.export))
        supportedOperations.add(Operation(R.string.delete, R.drawable.delete))
        supportedOperations.add(Operation(R.string.archive, R.drawable.archive))
        return supportedOperations
    }
}