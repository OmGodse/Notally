package com.omgodse.notally.fragments

import android.animation.LayoutTransition
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.helpers.ExportHelper
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.xml.XMLReader
import java.io.File

class Search : NotallyFragment() {

    private var keyword = String()
    private lateinit var exportHelper: ExportHelper

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        exportHelper = ExportHelper(mContext, this)
        binding.FrameLayout.layoutTransition = LayoutTransition()

        (mContext as MainActivity).binding.EnterSearchKeyword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(query: Editable?) {
                displaySearchResults(query.toString().trim())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == Constants.RequestCodeExportFile && resultCode == Activity.RESULT_OK){
            val uri = data?.data
            if (uri != null){
                exportHelper.writeFileToStream(uri)
            }
        }
        else if (resultCode == Constants.ResultCodeEditedFile){
            val filePath = data?.getStringExtra(Constants.FilePath)
            if (filePath != null) {
                val file = File(filePath)
                val position = noteAdapter.files.indexOf(file)
                if (!isFileMatch(file, keyword)) {
                    noteAdapter.files.remove(file)
                    noteAdapter.notifyItemRemoved(position)
                    confirmVisibility()
                } else noteAdapter.notifyItemChanged(position)
            }
        }
        else super.onActivityResult(requestCode, resultCode, data)
    }


    override fun onNoteClicked(position: Int) {
        val file = noteAdapter.files[position]
        val isNote = XMLReader(file).isNote()
        val intent = if (isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, file.path)
        intent.putExtra(Constants.PreviousFragment, R.id.SearchFragment)
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
                    mContext.getString(R.string.delete) -> deleteNote(file)
                    mContext.getString(R.string.archive) -> archiveNote(file)
                    mContext.getString(R.string.export) -> showExportDialog(file)
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

    private fun displaySearchResults(keyword: String) {
        if (keyword.isEmpty()) {
            noteAdapter.files = ArrayList()
        } else noteAdapter.files = getFiles(keyword)

        noteAdapter.notifyDataSetChanged()
        confirmVisibility()
    }

    private fun getFiles(keyword: String): ArrayList<File> {
        this.keyword = keyword
        val keywordFiles = ArrayList<File>()

        val notesHelper = NotesHelper(mContext)
        val notesPath = notesHelper.getNotePath()
        val settingsHelper = SettingsHelper(mContext)

        val files: ArrayList<File>? = notesPath.listFiles()?.toCollection(ArrayList())

        files?.forEach { file ->
            if (isFileMatch(file, keyword)) {
                keywordFiles.add(file)
            }
        }

        keywordFiles.sortWith(Comparator { firstFile, secondFile ->
            firstFile.name.compareTo(secondFile.name)
        })

        if (settingsHelper.getSortingPreferences() == mContext.getString(R.string.newestFirstKey))
            keywordFiles.reverse()

        return keywordFiles
    }

    private fun isFileMatch(file: File, keyword: String): Boolean {
        val xmlReader = XMLReader(file)

        val title = xmlReader.getTitle()
        val body = xmlReader.getBody()
        val labels = xmlReader.getLabels()
        val items = xmlReader.getListItems()

        labels.forEach { label ->
            if (label.contains(keyword, true)) {
                return true
            }
        }

        items.forEach { item ->
            if (item.body.contains(keyword, true)) {
                return true
            }
        }

        return if (body.contains(keyword, true)) {
            true
        } else title.contains(keyword, true)
    }


    override fun populateRecyclerView() {}

    override fun getFolderPath(): File? = null

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_search)
}