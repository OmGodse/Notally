package com.omgodse.notally.fragments

import android.app.Activity
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
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.xml.XMLReader
import java.io.File

class Notes : NotallyFragment() {

    private lateinit var exportHelper: ExportHelper

    override fun onResume() {
        super.onResume()
        val notesHelper = NotesHelper(mContext)
        val notePath = notesHelper.getNotePath()
        val sortedFiles = notesHelper.getSortedFilesList(notePath)
        if (sortedFiles?.size != noteAdapter.itemCount) {
            populateRecyclerView()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        exportHelper = ExportHelper(mContext, this)
        (mContext as MainActivity).binding.TakeNoteFAB.setOnClickListener {
            displayNoteTypes()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            if (uri != null){
                exportHelper.writeFileToStream(uri)
            }
        }
        else super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.search, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.Search) {
            findNavController().navigate(R.id.NotesFragmentToSearchFragment)
        }
        return super.onOptionsItemSelected(item)
    }


    override fun onNoteClicked(position: Int) {
        val file = noteAdapter.files[position]
        val isNote = XMLReader(file).isNote()
        val intent = if (isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, file.path)
        intent.putExtra(Constants.PreviousFragment, R.id.NotesFragment)
        startActivityForResult(intent, Constants.RequestCode)
    }

    override fun onNoteLongClicked(position: Int) {
        val file = noteAdapter.files[position]
        val notesHelper = NotesHelper(mContext)
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.share, R.drawable.share)
        menuHelper.addItem(R.string.labels, R.drawable.label)
        menuHelper.addItem(R.string.export, R.drawable.export)
        menuHelper.addItem(R.string.delete, R.drawable.delete)
        menuHelper.addItem(R.string.archive, R.drawable.archive)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.share) -> notesHelper.shareNote(file)
                    mContext.getString(R.string.labels) -> notesHelper.changeNoteLabel(file, noteAdapter)
                    mContext.getString(R.string.export) -> showExportDialog(file)
                    mContext.getString(R.string.delete) -> deleteNote(file)
                    mContext.getString(R.string.archive) -> archiveNote(file)
                }
            }
        })

        menuHelper.show()
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

    private fun showExportDialog(file: File) {
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.pdf, R.drawable.pdf)
        menuHelper.addItem(R.string.plain_text, R.drawable.plain_text)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.pdf) -> exportHelper.exportNoteToPDF(file)
                    mContext.getString(R.string.plain_text) -> exportHelper.exportNoteToPlainText(file)
                }
            }
        })

        menuHelper.show()
    }


    override fun getFolderPath() = NotesHelper(mContext).getNotePath()

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_notes)
}