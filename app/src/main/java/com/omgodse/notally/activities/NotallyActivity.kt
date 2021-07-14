package com.omgodse.notally.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.transition.AutoTransition
import androidx.transition.TransitionManager
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsParent
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Label
import com.omgodse.notally.viewmodels.NotallyModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

abstract class NotallyActivity : AppCompatActivity(), OperationsParent {

    internal abstract val model: NotallyModel

    override fun onBackPressed() {
        model.saveNote {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model.saveNote()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val selectedBaseNote = intent.getParcelableExtra<BaseNote>(Constants.SelectedBaseNote)

        if (model.isFirstInstance) {
            if (selectedBaseNote != null) {
                model.isNewNote = false
                model.setStateFromBaseNote(selectedBaseNote)
            } else model.isNewNote = true

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
        bindPinned(menu?.findItem(R.id.Pin))
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.Share -> shareNote()
            R.id.Labels -> labelNote()
            R.id.Pin -> pinNote(item)
            R.id.Delete -> deleteNote()
            R.id.Archive -> archiveNote()
            R.id.Restore -> restoreNote()
            R.id.Unarchive -> restoreNote()
            R.id.DeleteForever -> deleteNoteForever()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun accessContext(): Context {
        return this
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    abstract fun shareNote()

    abstract fun getLabelGroup(): ChipGroup

    abstract fun getPinnedIndicator(): TextView

    abstract fun getPinnedParent(): LinearLayout


    open fun receiveSharedNote() {}


    private fun labelNote() {
        lifecycleScope.launch {
            val labels = withContext(Dispatchers.IO) { model.getAllLabelsAsList() }
            labelNote(labels, model.labels) { updatedLabels ->
                model.labels = updatedLabels
                getLabelGroup().bindLabels(updatedLabels)
            }
        }
    }

    private fun deleteNote() {
        model.moveBaseNoteToDeleted()
        onBackPressed()
    }

    private fun restoreNote() {
        model.restoreBaseNote()
        onBackPressed()
    }

    private fun archiveNote() {
        model.moveBaseNoteToArchive()
        onBackPressed()
    }

    private fun deleteNoteForever() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { dialog, which ->
                model.deleteBaseNoteForever {
                    super.onBackPressed()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pinNote(item: MenuItem) {
        model.pinned = !model.pinned
        bindPinned(item, true)
    }


    private fun bindPinned(item: MenuItem?, fromUser: Boolean = false) {
        if (fromUser) {
            TransitionManager.beginDelayedTransition(getPinnedParent(), AutoTransition())
        }
        getPinnedIndicator().isVisible = model.pinned
        item?.title = if (model.pinned) {
            getString(R.string.unpin)
        } else {
            getString(R.string.pin)
        }
    }

    internal fun setupToolbar(toolbar: MaterialToolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}