package com.omgodse.notally.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.util.TypedValue
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityNotallyBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.viewmodels.NotallyModel
import com.omgodse.notally.widget.WidgetProvider
import kotlinx.coroutines.launch

abstract class NotallyActivity(private val type: Type) : AppCompatActivity() {

    internal lateinit var binding: ActivityNotallyBinding
    internal val model: NotallyModel by viewModels()

    override fun finish() {
        lifecycleScope.launch {
            model.saveNote()
            sendModificationBroadcast()
            super.finish()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong("id", model.id)
        lifecycleScope.launch {
            model.saveNote()
            sendModificationBroadcast()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model.type = type
        initialiseBinding()
        setContentView(binding.root)

        lifecycleScope.launch {
            if (model.isFirstInstance) {
                val persistedId = savedInstanceState?.getLong("id")
                val selectedId = intent.getLongExtra(Constants.SelectedBaseNote, 0L)
                val id = persistedId ?: selectedId
                if (id != 0L) {
                    model.isNewNote = false
                    model.setState(id)
                } else {
                    model.isNewNote = true
                    model.createBaseNote()

                    if (intent.action == Intent.ACTION_SEND) {
                        handleSharedNote()
                    }
                }
                model.isFirstInstance = false
            }

            setupToolbar()
            setupListeners()
            setStateFromModel()

            configureUI()
            binding.ScrollView.visibility = View.VISIBLE
        }
    }


    abstract fun configureUI()

    open fun setupListeners() {
        binding.EnterTitle.doAfterTextChanged { text ->
            model.title = requireNotNull(text).trim().toString()
        }
    }

    open fun setStateFromModel() {
        val formatter = BaseNoteModel.getDateFormatter(this)
        binding.DateCreated.text = formatter.format(model.timestamp)

        binding.EnterTitle.setText(model.title)
        Operations.bindLabels(binding.LabelGroup, model.labels, model.textSize)

        setupColor()
    }


    private fun sendModificationBroadcast() {
        val intent = Intent(this, WidgetProvider::class.java)
        intent.action = WidgetProvider.ACTION_NOTE_MODIFIED
        intent.putExtra(WidgetProvider.EXTRA_NOTE_ID, model.id)
        sendBroadcast(intent)
    }


    private fun handleSharedNote() {
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val string = intent.getStringExtra(Intent.EXTRA_TEXT)
        val charSequence = intent.getCharSequenceExtra(Operations.extraCharSequence)
        val body = charSequence ?: string

        if (body != null) {
            model.body = Editable.Factory.getInstance().newEditable(body)
        }
        if (title != null) {
            model.title = title
        }
    }


    private fun share() {
        val body = when (type) {
            Type.NOTE -> model.body
            Type.LIST -> Operations.getBody(model.items)
        }
        Operations.shareNote(this, model.title, body)
    }

    private fun label() {
        lifecycleScope.launch {
            val labels = model.getAllLabels()
            val onUpdated = { new: List<String> ->
                model.setLabels(new)
                Operations.bindLabels(binding.LabelGroup, model.labels, model.textSize)
            }
            val add = { Operations.displayAddLabelDialog(this@NotallyActivity, model::insertLabel) { label() } }
            Operations.labelNote(this@NotallyActivity, labels, model.labels, onUpdated, add)
        }
    }


    private fun delete() {
        model.folder = Folder.DELETED
        finish()
    }

    private fun restore() {
        model.folder = Folder.NOTES
        finish()
    }

    private fun archive() {
        model.folder = Folder.ARCHIVED
        finish()
    }

    private fun deleteForever() {
        MaterialAlertDialogBuilder(this)
            .setMessage(R.string.delete_note_forever)
            .setPositiveButton(R.string.delete) { _, _ ->
                lifecycleScope.launch {
                    model.delete()
                    sendModificationBroadcast()
                    super.finish()
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun pin(item: MenuItem) {
        model.pinned = !model.pinned
        bindPinned(item)
    }


    private fun setupColor() {
        val color = Operations.extractColor(model.color, this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            window.statusBarColor = color
        }
        binding.root.setBackgroundColor(color)
        binding.RecyclerView.setBackgroundColor(color)
        binding.Toolbar.backgroundTintList = ColorStateList.valueOf(color)
    }

    private fun setupToolbar() {
        binding.Toolbar.setNavigationOnClickListener { finish() }

        val menu = binding.Toolbar.menu
        val pin = menu.add(R.string.pin, R.drawable.pin) { item -> pin(item) }
        bindPinned(pin)

        menu.add(R.string.share, R.drawable.share) { share() }
        menu.add(R.string.labels, R.drawable.label) { label() }

        when (model.folder) {
            Folder.NOTES -> {
                menu.add(R.string.delete, R.drawable.delete) { delete() }
                menu.add(R.string.archive, R.drawable.archive) { archive() }
            }

            Folder.DELETED -> {
                menu.add(R.string.restore, R.drawable.restore) { restore() }
                menu.add(R.string.delete_forever, R.drawable.delete) { deleteForever() }
            }

            Folder.ARCHIVED -> {
                menu.add(R.string.delete, R.drawable.delete) { delete() }
                menu.add(R.string.unarchive, R.drawable.unarchive) { restore() }
            }
        }
    }

    private fun initialiseBinding() {
        binding = ActivityNotallyBinding.inflate(layoutInflater)
        when (type) {
            Type.NOTE -> {
                binding.AddItem.visibility = View.GONE
                binding.RecyclerView.visibility = View.GONE
            }

            Type.LIST -> {
                binding.EnterBody.visibility = View.GONE
            }
        }

        val title = TextSize.getEditTitleSize(model.textSize)
        val date = TextSize.getDisplayBodySize(model.textSize)
        val body = TextSize.getEditBodySize(model.textSize)

        binding.EnterTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, title)
        binding.DateCreated.setTextSize(TypedValue.COMPLEX_UNIT_SP, date)
        binding.EnterBody.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        binding.root.isSaveFromParentEnabled = false
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
}