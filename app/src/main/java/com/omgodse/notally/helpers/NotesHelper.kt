package com.omgodse.notally.helpers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.View
import androidx.core.util.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.miscellaneous.Note
import com.omgodse.notally.xml.XMLReader
import java.io.File
import java.io.StringWriter

class NotesHelper(val context: Context) {

    fun shareNote(note: Note) {
        val body = if (note.isNote) {
            note.body
        }
        else getBodyFromItems(note.items)
        shareNote(note.title, body)
    }

    fun shareNote(title: String?, body: String?) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, body)
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_note)))
    }

    fun shareNote(title: String?, items: ArrayList<ListItem>?) = shareNote(title, getBodyFromItems(items))

    fun getBodyFromItems(items: ArrayList<ListItem>?): String {
        val stringWriter = StringWriter()
        items?.forEachIndexed { index, listItem ->
            stringWriter.appendln("${(index + 1)}) ${listItem.body}")
        }
        return stringWriter.toString()
    }


    fun getNotePath() = getFolder("notes")

    fun getDeletedPath() = getFolder("deleted")

    fun getArchivedPath() = getFolder("archived")

    private fun getFolder(folderName: String): File {
        val folder = File(context.filesDir, folderName)
        if (!folder.exists()) {
            folder.mkdir()
        }
        return folder
    }


    fun restoreFile(file: File) = moveFile(file, getNotePath())

    fun moveFileToDeleted(file: File) = moveFile(file, getDeletedPath())

    fun moveFileToArchive(file: File) = moveFile(file, getArchivedPath())

    private fun moveFile (file: File, destinationPath: File) : Boolean {
        val destinationFile = File(destinationPath, file.name)
        destinationFile.writeText(file.readText())
        return file.delete()
    }


    fun getSortedLabelsList(): ArrayList<String> {
        val sharedPreferences = context.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val labels = sharedPreferences.getStringSet("labelItems", HashSet<String>()) as HashSet<String>
        val arrayList = ArrayList(labels)
        arrayList.sort()
        return arrayList
    }


    fun labelNote(previousLabels: HashSet<String>, listener: LabelListener) {
        val allLabels = getSortedLabelsList().toTypedArray()

        val checkedLabels = getCheckedLabels(previousLabels)
        val createLabel = View.inflate(context, R.layout.add_label, null) as MaterialTextView

        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(R.string.labels)
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)

        if (allLabels.isNotEmpty()) {
            alertDialogBuilder.setMultiChoiceItems(allLabels, checkedLabels, null)
            alertDialogBuilder.setPositiveButton(R.string.save, null)
        } else alertDialogBuilder.setView(createLabel)

        val dialog = alertDialogBuilder.create()
        dialog.show()

        createLabel.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(previousLabels, listener)
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
            val selectedLabels = HashSet<String>()
            dialog.listView.checkedItemPositions.forEach { key, value ->
                if (value) {
                    val label = allLabels[key]
                    selectedLabels.add(label)
                }
            }
            dialog.dismiss()
            listener.onUpdateLabels(selectedLabels)
        }
    }

    private fun getCheckedLabels(labels: HashSet<String>) : BooleanArray {
        val allLabels = getSortedLabelsList().toTypedArray()
        val checkedLabels = BooleanArray(allLabels.size)
        allLabels.forEachIndexed { index, label ->
            if (labels.contains(label)){
                checkedLabels[index] = true
            }
        }
        return checkedLabels
    }


    private fun insertLabel(label: String) {
        val priorLabels = getSortedLabelsList()

        val newLabels = HashSet(priorLabels)
        newLabels.add(label)

        val sharedPreferences = context.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putStringSet(Constants.labelItems, newLabels)
        editor.apply()
    }

    private fun displayAddLabelDialog(previousLabels: HashSet<String>, listener: LabelListener) {
        val priorLabels = getSortedLabelsList()

        val view = View.inflate(context, R.layout.dialog_add_label, null)
        val textInputLayout: TextInputLayout = view.findViewById(R.id.TextInputLayout)
        val textInputEditText: TextInputEditText = view.findViewById(R.id.TextInputEditText)

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.add_label)
        dialogBuilder.setView(view)
        dialogBuilder.setNegativeButton(R.string.cancel, null)
        dialogBuilder.setPositiveButton(R.string.save, null)

        val dialog = dialogBuilder.show()
        textInputEditText.requestFocus()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val label = textInputEditText.text.toString().trim()
            if (label.isNotEmpty()) {
                if (!priorLabels.contains(label)) {
                    insertLabel(label)
                    dialog.dismiss()
                    labelNote(previousLabels, listener)
                } else textInputLayout.error = context.getString(R.string.label_exists)
            } else dialog.dismiss()
        }
    }

    companion object {
        fun convertFileToNote(file: File) : Note {
            val xmlReader = XMLReader(file)

            val isNote = xmlReader.isNote()
            val title = xmlReader.getTitle()
            val timestamp = xmlReader.getDateCreated()
            val body = xmlReader.getBody()
            val items = xmlReader.getListItems()
            val spans = xmlReader.getSpans()
            val labels = xmlReader.getLabels()

            return Note(
                isNote,
                title,
                timestamp,
                body,
                items,
                spans,
                labels,
                file.path
            )
        }
    }
}