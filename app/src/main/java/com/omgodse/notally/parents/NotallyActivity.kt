package com.omgodse.notally.parents

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
import java.io.File
import java.util.*

abstract class NotallyActivity : AppCompatActivity() {

    internal var isNew = true
    internal lateinit var file: File

    override fun onBackPressed() {
        saveNote()
        super.onBackPressed()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val filePathToEdit = intent.getStringExtra(Constants.FilePath)

        if (filePathToEdit != null){
            isNew = false
            val data = Intent()
            data.putExtra(Constants.FilePath, filePathToEdit)
            setResult(Constants.ResultCodeEditedFile, data)
            file = File(filePathToEdit)
        } else {
            isNew = true
            val timestamp = Date().time
            val notesHelper = NotesHelper(this)
            file = File(notesHelper.getNotePath(), "$timestamp.xml")
            val data = Intent()
            data.putExtra(Constants.FilePath, file.path)
            setResult(Constants.ResultCodeCreatedFile, data)
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


    abstract fun saveNote()

    abstract fun shareNote()

    abstract fun labelNote()


    private fun deleteNote() {
        saveNote()
        val data = Intent()

        if (!isNew) {
            data.putExtra(Constants.FilePath, file.path)
        }

        setResult(Constants.ResultCodeDeletedFile, data)
        super.onBackPressed()
    }

    private fun restoreNote() {
        saveNote()
        val data = Intent()

        if (!isNew) {
            data.putExtra(Constants.FilePath, file.path)
        }

        setResult(Constants.ResultCodeRestoredFile, data)
        super.onBackPressed()
    }

    private fun archiveNote() {
        saveNote()
        val data = Intent()

        if (!isNew) {
            data.putExtra(Constants.FilePath, file.path)
        }

        setResult(Constants.ResultCodeArchivedFile, data)
        super.onBackPressed()
    }

    private fun deleteNoteForever() {
        val alertDialogBuilder = MaterialAlertDialogBuilder(this)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            val data = Intent()

            if (!isNew) {
                file.delete()
                data.putExtra(Constants.FilePath, file.path)
            }

            setResult(Constants.ResultCodeDeletedForeverFile, data)
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
        const val DateFormat = "EEE, d MMM yyyy"
    }
}