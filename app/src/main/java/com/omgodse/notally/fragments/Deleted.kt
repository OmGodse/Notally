package com.omgodse.notally.fragments

import android.content.Intent
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.helpers.MenuHelper
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.DialogListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.xml.XMLReader
import java.io.File

class Deleted : NotallyFragment() {

    override fun onNoteClicked(position: Int) {
        val file = noteAdapter.files[position]
        val intent: Intent
        val isNote = XMLReader(file).isNote()
        intent = if (isNote) {
            Intent(mContext, TakeNote::class.java)
        } else Intent(mContext, MakeList::class.java)
        intent.putExtra(Constants.FilePath, file.path)
        intent.putExtra(Constants.PreviousFragment, R.id.DeletedFragment)
        startActivityForResult(intent, Constants.RequestCode)
    }

    override fun onNoteLongClicked(position: Int) {
        val file = noteAdapter.files[position]
        val menuHelper = MenuHelper(mContext)

        menuHelper.addItem(R.string.restore, R.drawable.restore)
        menuHelper.addItem(R.string.delete_forever, R.drawable.delete)

        menuHelper.setListener(object : DialogListener {
            override fun onDialogItemClicked(label: String) {
                when (label) {
                    mContext.getString(R.string.restore) -> restoreNote(file)
                    mContext.getString(R.string.delete_forever) -> confirmDeletion(file)
                }
            }
        })

        menuHelper.show()
    }


    private fun confirmDeletion(file: File) {
        val alertDialogBuilder = MaterialAlertDialogBuilder(mContext)
        alertDialogBuilder.setMessage(R.string.delete_note_forever)
        alertDialogBuilder.setPositiveButton(R.string.delete) { dialog, which ->
            if (file.delete()) {
                val position = noteAdapter.files.indexOf(file)
                noteAdapter.files.remove(file)
                noteAdapter.notifyItemRemoved(position)
                confirmVisibility()
            }
        }
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)
        alertDialogBuilder.show()
    }


    override fun getFolderPath(): File? = NotesHelper(mContext).getDeletedPath()

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_delete)
}