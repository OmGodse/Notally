package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.viewmodels.NotallyModel
import java.io.File
import java.util.*

abstract class NotallyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filePathToEdit = intent.getStringExtra(Constants.FilePath)
        val model = getViewModel()

        if (model.isFirstInstance) {
            if (filePathToEdit != null) {
                model.isNewNote = false
                model.file = File(filePathToEdit)
            } else {
                model.isNewNote = true
                val timestamp = Date().time
                val notesHelper = NotesHelper(this)
                val file = File(notesHelper.getNotePath(), "$timestamp.xml")
                model.file = file

                val data = Intent()
                data.putExtra(Constants.FilePath, file.path)
                setResult(Constants.ResultCodeCreatedFile, data)
            }
            model.isFirstInstance = false
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val menuId = when (intent.getIntExtra(Constants.PreviousFragment, R.id.NotesFragment)) {
            R.id.NotesFragment -> R.menu.notes
            R.id.ArchivedFragment -> R.menu.archived
            R.id.DeletedFragment -> R.menu.deleted
            else -> R.menu.notes
        }

        menuInflater.inflate(menuId, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.Labels -> labelNote()
            R.id.Share -> shareNote()
            R.id.Delete -> deleteNote()
            R.id.Archive -> archiveNote()
            R.id.Restore -> restoreNote()
            R.id.Unarchive -> restoreNote()
            R.id.DeleteForever -> deleteNoteForever()
        }
        return super.onOptionsItemSelected(item)
    }


    abstract fun shareNote()

    abstract fun getViewModel(): NotallyModel


    private fun labelNote() {
        val model = getViewModel()
        val notesHelper = NotesHelper(this)
        notesHelper.labelNote(model.labels.value ?: HashSet()) { labels ->
            model.labels.value = labels
        }
    }

    private fun deleteNote() {
        getViewModel().moveFileToDeleted()
        onBackPressed()
    }

    private fun restoreNote() {
        getViewModel().restoreFile()
        onBackPressed()
    }

    private fun archiveNote() {
        getViewModel().moveFileToArchive()
        onBackPressed()
    }

    private fun deleteNoteForever() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            getViewModel().deleteFileForever()
            onBackPressed()
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }


    internal fun setupToolbar(toolbar: MaterialToolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    companion object {
        const val DateFormat = "EEE d MMM yyyy"
    }
}