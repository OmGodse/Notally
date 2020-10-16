package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.viewmodels.NotallyModel
import java.io.File
import java.util.*
import kotlin.collections.HashSet

abstract class NotallyActivity : AppCompatActivity() {

    internal abstract val model: NotallyModel
    internal lateinit var operationsHelper: OperationsHelper

    override fun onBackPressed() {
        model.saveNote()
        super.onBackPressed()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        model.saveNote()
        super.onSaveInstanceState(outState)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        operationsHelper = OperationsHelper(this)

        val filePathToEdit = intent.getStringExtra(Constants.FilePath)

        if (model.isFirstInstance) {
            if (filePathToEdit != null) {
                model.isNewNote = false
                model.setFile(File(filePathToEdit))
            } else {
                model.isNewNote = true
                val timestamp = Date().time
                val file = File(BaseNoteModel.getNotePath(this), "$timestamp.xml")
                model.setFile(file)

                val data = Intent()
                data.putExtra(Constants.FilePath, file.path)
                setResult(Constants.ResultCodeCreatedFile, data)
            }

            if (intent.action == Intent.ACTION_SEND) {
                receiveSharedNote()
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
            R.id.Share -> shareNote()
            R.id.Labels -> labelNote()
            R.id.Delete -> deleteNote()
            R.id.Archive -> archiveNote()
            R.id.Restore -> restoreNote()
            R.id.Unarchive -> restoreNote()
            R.id.DeleteForever -> deleteNoteForever()
        }
        return super.onOptionsItemSelected(item)
    }


    abstract fun shareNote()

    open fun receiveSharedNote() {}


    private fun labelNote() {
        operationsHelper.labelNote(model.labels.value ?: HashSet()) {
            model.labels.value = it
        }
    }

    private fun deleteNote() {
        model.moveBaseNoteToDeleted()
        super.onBackPressed()
    }

    private fun restoreNote() {
        model.restoreBaseNote()
        super.onBackPressed()
    }

    private fun archiveNote() {
        model.moveBaseNoteToArchive()
        super.onBackPressed()
    }

    private fun deleteNoteForever() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            model.deleteBaseNoteForever()
            super.onBackPressed()
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