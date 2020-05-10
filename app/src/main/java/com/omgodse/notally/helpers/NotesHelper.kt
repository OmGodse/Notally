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
import com.omgodse.notally.adapters.NoteAdapter
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.xml.XMLReader
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.io.File
import java.io.FileWriter
import java.io.StringWriter

class NotesHelper(val context: Context) {

    fun shareNote(file: File) {
        val xmlReader = XMLReader(file)
        val title = xmlReader.getTitle()
        val body = if (xmlReader.isNote()) {
            val spans = xmlReader.getSpans()
            xmlReader.getBody()
        } else {
            val listItems = xmlReader.getListItems()
            getBodyFromItems(listItems)
        }
        shareNote(title, body)
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


    fun restoreNote(file: File) = moveFile(file, getNotePath())

    fun moveNoteToDeleted(file: File) = moveFile(file, getDeletedPath())

    fun moveNoteToArchive(file: File) = moveFile(file, getArchivedPath())

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

    fun getSortedFilesList(filesPath: File): ArrayList<File>? {
        val settingsHelper = SettingsHelper(context)

        val listOfFiles = filesPath.listFiles()?.toCollection(ArrayList())
        listOfFiles?.sortWith(Comparator { firstFile, secondFile ->
            firstFile.name.compareTo(secondFile.name)
        })

        if (settingsHelper.getSortingPreferences() == context.getString(R.string.newestFirstKey))
            listOfFiles?.reverse()

        return listOfFiles
    }


    fun changeNoteLabel(file: File, noteAdapter: NoteAdapter) {
        val xmlReader = XMLReader(file)
        val previousLabels = xmlReader.getLabels()

        val allLabels = getSortedLabelsList().toTypedArray()

        val checkedLabels = getCheckedLabels(previousLabels)
        val createLabel = View.inflate(context, R.layout.add_label, null) as MaterialTextView

        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(context.getString(R.string.labels))

        if (allLabels.isNotEmpty()) {
            alertDialogBuilder.setMultiChoiceItems(allLabels, checkedLabels, null)
            alertDialogBuilder.setPositiveButton(R.string.save, null)
        } else alertDialogBuilder.setView(createLabel)

        alertDialogBuilder.setNegativeButton(R.string.cancel, null)

        val dialog = alertDialogBuilder.create()
        dialog.show()

        createLabel.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(file, noteAdapter)
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
            val selectedLabels = HashSet<String>()
            dialog.listView?.checkedItemPositions?.forEach { key, value ->
                if (value) {
                    val label = allLabels[key]
                    selectedLabels.add(label)
                }
            }
            dialog.dismiss()
            val fileWriter = FileWriter(file)

            val xmlWriter: XMLWriter
            if (xmlReader.isNote()) {
                xmlWriter = XMLWriter(XMLTags.Note)

                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setBody(xmlReader.getBody())
                xmlWriter.setSpans(xmlReader.getSpans())
                xmlWriter.setLabels(selectedLabels)
                xmlWriter.endNote()
            } else {
                xmlWriter = XMLWriter(XMLTags.List)
                xmlWriter.startNote()
                xmlWriter.setDateCreated(xmlReader.getDateCreated())
                xmlWriter.setTitle(xmlReader.getTitle())
                xmlWriter.setListItems(xmlReader.getListItems())
                xmlWriter.setLabels(selectedLabels)
                xmlWriter.endNote()
            }

            val note = xmlWriter.getNote()
            fileWriter.write(note)
            fileWriter.close()

            val position = noteAdapter.files.indexOf(file)
            noteAdapter.notifyItemChanged(position)
        }
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

    private fun displayAddLabelDialog(file: File, noteAdapter: NoteAdapter) {
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
                    changeNoteLabel(file, noteAdapter)
                } else textInputLayout.error = context.getString(R.string.label_exists)
            } else dialog.dismiss()
        }
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
}