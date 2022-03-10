package com.omgodse.notally.activities

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.viewbinding.ViewBinding
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.chip.ChipGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsParent
import com.omgodse.notally.miscellaneous.*
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.NotallyModel
import kotlinx.coroutines.launch

abstract class NotallyActivity : AppCompatActivity(), OperationsParent {

    internal abstract val type: Type
    internal abstract val binding: ViewBinding
    internal val model: NotallyModel by viewModels { NotallyModel.Factory(application, type) }

    override fun onBackPressed() {
        model.saveNote { super.onBackPressed() }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        model.saveNote {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        binding.root.isSaveFromParentEnabled = false

        if (model.isFirstInstance) {
            val selectedBaseNote = intent.getParcelableExtra<BaseNote>(Constants.SelectedBaseNote)
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
        if (menu != null) {
            val pin = menu.add(R.string.pin, R.drawable.pin) { item -> pin(item) }
            bindPinned(pin)

            menu.add(R.string.share, R.drawable.share) { share() }
            menu.add(R.string.labels, R.drawable.label) { label() }

            when (model.folder) {
                Folder.NOTES -> {
                    menu.add(R.string.delete, R.drawable.delete) { delete() }
                    menu.add(R.string.archive, R.drawable.archive) { archive() }
                    // TODO Step 1
                    menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever() }
                }
                Folder.DELETED -> {
                    menu.add(R.string.restore, R.drawable.restore) { restore() }
                    menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever() }

                }
                Folder.ARCHIVED -> {
                    menu.add(R.string.unarchive, R.drawable.unarchive) { restore() }

                }
            }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }


    override fun accessContext(): Context {
        return this
    }

    override fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) {
        model.insertLabel(label, onComplete)
    }


    abstract fun getLabelGroup(): ChipGroup


    open fun receiveSharedNote() {}


    private fun share() {

        // system converts the title and body to plain text when sharing notes ..
        val body = when (type) {
            //TODO Step 3
            Type.NOTE, Type.PHONENUMBER -> model.body
            Type.LIST -> model.items.getBody()

        }
        Operations.shareNote(this, model.title, body)
    }

    private fun label() {
        lifecycleScope.launch {
            val labels = model.getAllLabelsAsList()
            labelNote(labels, model.labels) { updatedLabels ->
                model.labels = updatedLabels
                getLabelGroup().bindLabels(updatedLabels)
            }
        }
    }

    private fun delete() {
        model.moveBaseNoteToDeleted()
        onBackPressed()
    }

    private fun restore() {
        model.restoreBaseNote()
        super.onBackPressed()
    }

    private fun archive() {
        model.moveBaseNoteToArchive()
        super.onBackPressed()
    }

    private fun deleteForever() {
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

    private fun pin(item: MenuItem) {
        model.pinned = !model.pinned
        bindPinned(item)
    }


    private fun bindPinned(item: MenuItem) {
        val icon: Int
        val title: Int
        if (model.pinned) {
            icon = R.drawable.unpin
            title = R.string.unpin
        } else {
            icon = R.drawable.pin
            title = R.string.pin
        }
        item.setTitle(title)
        item.setIcon(icon)
    }

    internal fun setupToolbar(toolbar: MaterialToolbar) {
        setSupportActionBar(toolbar)
        supportActionBar?.title = null
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}