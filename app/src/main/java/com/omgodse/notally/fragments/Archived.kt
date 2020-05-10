package com.omgodse.notally.fragments

import android.content.Intent
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.xml.XMLReader

class Archived : NotallyFragment() {

    override fun onNoteClicked(position: Int) {
        val file = noteAdapter.files[position]
        val intent: Intent
        val isNote = XMLReader(file).isNote()
        intent = if (isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, file.path)
        intent.putExtra(Constants.PreviousFragment, R.id.ArchivedFragment)
        startActivityForResult(intent, Constants.RequestCode)
    }

    override fun onNoteLongClicked(position: Int) {
        val file = noteAdapter.files[position]
        val notesHelper = NotesHelper(mContext)

        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.share, R.drawable.share)
        menuHelper.addItem(R.string.labels, R.drawable.label)
        menuHelper.addItem(R.string.unarchive, R.drawable.unarchive)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.share) -> notesHelper.shareNote(file)
                    mContext.getString(R.string.labels) -> notesHelper.changeNoteLabel(file, noteAdapter)
                    mContext.getString(R.string.unarchive) -> restoreNote(file)
                }
            }
        })

        menuHelper.show()
    }


    override fun getFolderPath() = NotesHelper(mContext).getArchivedPath()

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_archived)
}