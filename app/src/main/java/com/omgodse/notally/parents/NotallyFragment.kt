package com.omgodse.notally.parents

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.adapters.NoteAdapter
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.ExportHelper
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.miscellaneous.*
import com.omgodse.notally.viewmodels.NoteModel
import java.io.File

abstract class NotallyFragment : Fragment(), NoteListener {

    internal lateinit var mContext: Context
    internal lateinit var binding: FragmentNotesBinding

    private lateinit var noteAdapter: NoteAdapter
    private lateinit var exportHelper: ExportHelper

    internal val model: NoteModel by activityViewModels()

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        model.fetchRelevantNotes(getPayload())

        exportHelper = ExportHelper(mContext, this)

        noteAdapter = NoteAdapter(mContext)
        noteAdapter.noteListener = this
        binding.RecyclerView.adapter = noteAdapter
        binding.RecyclerView.setHasFixedSize(true)

        setupPadding()
        setupFrameLayout()
        setupLayoutManager()
        setupItemDecoration()

        populateRecyclerView()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentNotesBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val payload = getPayload()
        val filePath = data?.getStringExtra(Constants.FilePath)

        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK) {
            val uri = data?.data
            if (uri != null) {
                exportHelper.writeFileToStream(uri)
            }
        }
        else when (resultCode) {
            Constants.ResultCodeEditedFile -> model.handleNoteEdited(filePath, payload)
            Constants.ResultCodeCreatedFile -> {
                model.handleNoteCreated(filePath, payload)

                binding.RecyclerView.post {
                    val settingsHelper = SettingsHelper(mContext)
                    if (settingsHelper.getSortingPreferences() == mContext.getString(R.string.newestFirstKey)){
                        binding.RecyclerView.smoothScrollToPosition(0)
                    }
                    else binding.RecyclerView.smoothScrollToPosition(noteAdapter.itemCount)
                }

                if (filePath != null) {
                    val file = File(filePath)
                    val note = NotesHelper.convertFileToNote(file)
                    if (note.isEmpty()){
                        model.handleNoteDeleted(filePath, getPayload())
                        val rootView = (mContext as MainActivity).binding.CoordinatorLayout
                        Snackbar.make(rootView, R.string.discarded_empty_note, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }
            Constants.ResultCodeDeletedFile -> model.handleNoteDeleted(filePath, payload)
            Constants.ResultCodeArchivedFile -> model.handleNoteArchived(filePath, payload)
            Constants.ResultCodeRestoredFile -> model.handleNoteRestored(filePath, payload)
            Constants.ResultCodeDeletedForeverFile -> model.handleNoteDeletedForever(filePath, payload)
        }
    }


    override fun onNoteClicked(position: Int) {
        val note = noteAdapter.currentList[position]
        val intent = if (note.isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, note.filePath)
        intent.putExtra(Constants.PreviousFragment, getFragmentID())
        startActivityForResult(intent, Constants.RequestCode)
    }

    override fun onNoteLongClicked(position: Int) {
        if (getSupportedOperations().isNotEmpty()) {
            val note = noteAdapter.currentList[position]
            val notesHelper = NotesHelper(mContext)
            val menuHelper = MenuHelper(mContext)

            getSupportedOperations().forEach { operation ->
                menuHelper.addItem(operation.textId, operation.drawableId)
            }

            menuHelper.setListener(object : DialogListener {
                override fun onDialogItemClicked(label: String) {
                    when (label) {
                        mContext.getString(R.string.share) -> notesHelper.shareNote(note)
                        mContext.getString(R.string.labels) -> labelNote(note)
                        mContext.getString(R.string.export) -> showExportDialog(note)
                        mContext.getString(R.string.delete) -> model.handleNoteDeleted(note.filePath, getPayload())
                        mContext.getString(R.string.archive) -> model.handleNoteArchived(note.filePath, getPayload())
                        mContext.getString(R.string.restore) -> model.handleNoteRestored(note.filePath, getPayload())
                        mContext.getString(R.string.unarchive) -> model.handleNoteRestored(note.filePath, getPayload())
                        mContext.getString(R.string.delete_forever) -> confirmDeletion(note)
                    }
                }
            })

            menuHelper.show()
        }
    }


    private fun setupPadding() {
        val padding = mContext.resources.getDimensionPixelSize(R.dimen.recyclerViewPadding)
        val settingsHelper = SettingsHelper(mContext)
        val cardPreference = settingsHelper.getNoteTypePreferences()

        if (cardPreference == mContext.getString(R.string.elevatedKey)) {
            binding.RecyclerView.setPaddingRelative(padding, 0, padding, 0)
        }
    }

    private fun setupFrameLayout() {
        binding.FrameLayout.background = getBackground()
    }

    private fun setupLayoutManager() {
        val settingsHelper = SettingsHelper(mContext)
        val viewPreference = settingsHelper.getViewPreference()

        binding.RecyclerView.layoutManager = if (viewPreference == mContext.getString(R.string.gridKey)) {
            StaggeredGridLayoutManager(2, LinearLayout.VERTICAL)
        } else LinearLayoutManager(mContext)
    }

    private fun setupItemDecoration() {
        val settingsHelper = SettingsHelper(mContext)
        val cardMargin = mContext.resources.getDimensionPixelSize(R.dimen.cardMargin)
        val viewPreference = settingsHelper.getViewPreference()
        val cardPreference = settingsHelper.getNoteTypePreferences()

        if (cardPreference == mContext.getString(R.string.elevatedKey)) {
            if (viewPreference == mContext.getString(R.string.gridKey)) {
                binding.RecyclerView.addItemDecoration(ItemDecoration(cardMargin, 2))
            } else binding.RecyclerView.addItemDecoration(ItemDecoration(cardMargin, 1))
        }
    }


    private fun labelNote(note: Note) {
        val listener = object : LabelListener {
            override fun onUpdateLabels(labels: HashSet<String>) {
                model.editNoteLabel(note, labels, getPayload())
            }
        }

        val notesHelper = NotesHelper(mContext)
        notesHelper.labelNote(note.labels, listener)
    }

    private fun confirmDeletion(note: Note) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            model.handleNoteDeletedForever(note.filePath, getPayload())
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }

    private fun showExportDialog(note: Note) {
        val file = File(note.filePath)
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.pdf, R.drawable.pdf)
        menuHelper.addItem(R.string.html, R.drawable.html)
        menuHelper.addItem(R.string.plain_text, R.drawable.plain_text)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.pdf) -> exportHelper.exportFileToPDF(file)
                    mContext.getString(R.string.html) -> exportHelper.exportFileToHTML(file)
                    mContext.getString(R.string.plain_text) -> exportHelper.exportFileToPlainText(file)
                }
            }
        })

        menuHelper.show()
    }


    private fun populateRecyclerView() {
        getObservable()?.observe(viewLifecycleOwner, Observer { notes ->
            noteAdapter.submitCorrectList(notes)
            confirmVisibility(notes)
        })
    }

    private fun confirmVisibility(notes: List<Note>) {
        if (notes.isNotEmpty()) {
            binding.RecyclerView.visibility = View.VISIBLE
        } else {
            binding.RecyclerView.visibility = View.GONE
            (mContext as MainActivity).binding.AppBarLayout.setExpanded(true, true)
        }
    }


    abstract fun getFragmentID(): Int

    abstract fun getBackground(): Drawable?

    abstract fun getSupportedOperations(): ArrayList<Operation>


    abstract fun getPayload(): String

    abstract fun getObservable(): MutableLiveData<ArrayList<Note>>?
}