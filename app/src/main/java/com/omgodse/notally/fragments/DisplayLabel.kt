package com.omgodse.notally.fragments

import android.content.Intent
import android.os.Bundle
import android.view.View
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.xml.XMLReader
import java.io.File

class DisplayLabel : NotallyFragment() {

    private lateinit var label: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        label = arguments?.get(Constants.argLabelKey).toString()
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (resultCode) {
            Constants.ResultCodeEditedFile -> {
                val filePath = data?.getStringExtra(Constants.FilePath)

                if (filePath != null) {
                    val file = File(filePath)
                    val position = noteAdapter.files.indexOf(file)
                    val fileLabels = XMLReader(file).getLabels()
                    if (!fileLabels.contains(label)) {
                        noteAdapter.files.remove(file)
                        noteAdapter.notifyItemRemoved(position)
                        confirmVisibility()
                    } else noteAdapter.notifyItemChanged(position)
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }


    override fun onNoteClicked(position: Int) {
        val file = noteAdapter.files[position]
        val intent: Intent
        val isNote = XMLReader(file).isNote()
        intent = if (isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, file.path)
        intent.putExtra(Constants.PreviousFragment, R.id.DisplayLabelFragment)
        startActivityForResult(intent, Constants.RequestCode)
    }

    override fun onNoteLongClicked(position: Int) {}


    private fun getSortedNotesWithLabel(): ArrayList<File>? {
        val notesHelper = NotesHelper(mContext)
        val settingsHelper = SettingsHelper(mContext)
        val notesPath = notesHelper.getNotePath()

        val labelledFiles = ArrayList<File>()

        val files = notesPath.listFiles()?.toCollection(ArrayList())

        files?.forEach { file ->
            val xmlReader = XMLReader(file)
            val labels = xmlReader.getLabels()
            if (labels.contains(label)) {
                labelledFiles.add(file)
            }
        }

        labelledFiles.sortWith(Comparator { firstFile, secondFile ->
            firstFile.name.compareTo(secondFile.name)
        })

        if (settingsHelper.getSortingPreferences() == getString(R.string.newestFirstKey))
            labelledFiles.reverse()

        return labelledFiles
    }


    override fun populateRecyclerView() {
        val listOfFiles = getSortedNotesWithLabel()

        if (listOfFiles != null) {
            noteAdapter.files = listOfFiles
            noteAdapter.notifyDataSetChanged()
        }

        confirmVisibility()
    }

    override fun getFolderPath(): File? = null

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_labels)
}