package com.omgodse.notally.parents

import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.adapters.NoteAdapter
import com.omgodse.notally.databinding.FragmentNotesBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.ItemDecoration
import java.io.File

abstract class NotallyFragment : Fragment(), NoteListener {

    internal lateinit var binding: FragmentNotesBinding
    internal lateinit var mContext: Context
    internal lateinit var noteAdapter: NoteAdapter

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        noteAdapter = NoteAdapter(mContext, ArrayList())
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
        when (resultCode) {
            Constants.ResultCodeEditedFile -> handleFileEdited(data)
            Constants.ResultCodeCreatedFile -> handleFileCreated(data)
            Constants.ResultCodeDeletedFile -> handleFileDeleted(data)
            Constants.ResultCodeArchivedFile -> handleFileArchived(data)
            Constants.ResultCodeRestoredFile -> handleFileRestored(data)
            Constants.ResultCodeDeletedForeverFile -> handleFileDeletedForever(data)
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


    private fun handleFileEdited(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            val position = noteAdapter.files.indexOf(file)
            noteAdapter.notifyItemChanged(position)
        }
    }

    private fun handleFileCreated(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            val settingsHelper = SettingsHelper(mContext)
            if (settingsHelper.getSortingPreferences() == getString(R.string.newestFirstKey)) {
                noteAdapter.files.add(0, file)
            } else noteAdapter.files.add(file)
            val position = noteAdapter.files.indexOf(file)
            noteAdapter.notifyItemInserted(position)
            binding.RecyclerView.smoothScrollToPosition(position)
            confirmVisibility()
        }
    }

    private fun handleFileArchived(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            archiveNote(file)
        }
    }

    private fun handleFileDeleted(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            deleteNote(file)
        }
    }

    private fun handleFileRestored(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            restoreNote(file)
        }
    }

    private fun handleFileDeletedForever(data: Intent?) {
        val filePath = data?.getStringExtra(Constants.FilePath)
        if (filePath != null) {
            val file = File(filePath)
            val position = noteAdapter.files.indexOf(file)
            noteAdapter.files.remove(file)
            noteAdapter.notifyItemRemoved(position)
            confirmVisibility()
        }
    }


    open fun confirmVisibility() {
        if (noteAdapter.itemCount > 0) {
            binding.RecyclerView.visibility = View.VISIBLE
        } else {
            binding.RecyclerView.visibility = View.GONE
            (mContext as MainActivity).binding.AppBarLayout.setExpanded(true, true)
        }
    }

    open fun populateRecyclerView() {
        val folderPath = getFolderPath()
        if (folderPath != null) {
            val notesHelper = NotesHelper(mContext)
            val listOfFiles: ArrayList<File>? = notesHelper.getSortedFilesList(folderPath)

            if (listOfFiles != null) {
                noteAdapter.files = listOfFiles
                noteAdapter.notifyDataSetChanged()
            }
            confirmVisibility()
        }
    }


    internal fun deleteNote(file: File) {
        val position = noteAdapter.files.indexOf(file)
        val notesHelper = NotesHelper(mContext)
        if (notesHelper.moveNoteToDeleted(file)) {
            noteAdapter.files.remove(file)
            noteAdapter.notifyItemRemoved(position)
            confirmVisibility()
        }
    }

    internal fun archiveNote(file: File) {
        val position = noteAdapter.files.indexOf(file)
        val notesHelper = NotesHelper(mContext)
        if (notesHelper.moveNoteToArchive(file)) {
            noteAdapter.files.remove(file)
            noteAdapter.notifyItemRemoved(position)
            confirmVisibility()
        }
    }

    internal fun restoreNote(file: File) {
        val position = noteAdapter.files.indexOf(file)
        val notesHelper = NotesHelper(mContext)
        if (notesHelper.restoreNote(file)) {
            noteAdapter.files.remove(file)
            noteAdapter.notifyItemRemoved(position)
            confirmVisibility()
        }
    }


    abstract fun getFolderPath(): File?

    abstract fun getBackground(): Drawable?


    abstract override fun onNoteClicked(position: Int)

    abstract override fun onNoteLongClicked(position: Int)
}