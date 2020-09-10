package com.omgodse.notally.helpers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.InputType
import android.view.LayoutInflater
import androidx.core.util.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.AddLabelBinding
import com.omgodse.notally.databinding.DialogInputBinding
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.ListItem
import com.omgodse.notally.xml.Note
import java.io.File

class NotesHelper(val context: Context) {

    fun shareNote(baseNote: BaseNote) {
        when (baseNote) {
            is Note -> shareNote(baseNote)
            is List -> shareNote(baseNote)
        }
    }

    fun shareNote(title: String?, body: CharSequence?) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        shareIntent.putExtra(TakeNote.EXTRA_SPANNABLE, body)
        shareIntent.putExtra(Intent.EXTRA_TEXT, body.toString())
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_note)))
    }

    fun shareNote(title: String?, items: ArrayList<ListItem>?) = shareNote(title, getBodyFromItems(items))


    fun getBodyFromItems(items: kotlin.collections.List<ListItem>?) = buildString {
        items?.forEachIndexed { index, listItem ->
            appendLine("${(index + 1)}) ${listItem.body}")
        }
    }

    private fun shareNote(note: Note) = shareNote(note.title, note.body.applySpans(note.spans))

    private fun shareNote(list: List) = shareNote(list.title, getBodyFromItems(list.items))


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

    private fun moveFile(file: File, destinationPath: File): Boolean {
        val destinationFile = File(destinationPath, file.name)
        destinationFile.writeText(file.readText())
        return file.delete()
    }


    fun getSortedLabelsList(): ArrayList<String> {
        val sharedPreferences = context.getSharedPreferences(Constants.labelsPreferences, Context.MODE_PRIVATE)
        val labels = sharedPreferences.getStringSet(Constants.labelItems, HashSet<String>()) as HashSet<String>
        val arrayList = ArrayList(labels)
        arrayList.sort()
        return arrayList
    }


    fun labelNote(previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
        val allLabels = getSortedLabelsList().toTypedArray()

        val checkedLabels = getCheckedLabels(previousLabels)
        val binding = AddLabelBinding.inflate(LayoutInflater.from(context))

        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(R.string.labels)
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)

        if (allLabels.isNotEmpty()) {
            alertDialogBuilder.setMultiChoiceItems(allLabels, checkedLabels, null)
            alertDialogBuilder.setPositiveButton(R.string.save, null)
        } else alertDialogBuilder.setView(binding.root)

        val dialog = alertDialogBuilder.create()
        dialog.show()

        binding.root.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(previousLabels, onLabelsUpdated)
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
            onLabelsUpdated(selectedLabels)
        }
    }

    private fun displayAddLabelDialog(previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
        val priorLabels = getSortedLabelsList()

        val binding = DialogInputBinding.inflate(LayoutInflater.from(context))

        binding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        binding.edit.filters = arrayOf()

        val dialogBuilder = MaterialAlertDialogBuilder(context)
        dialogBuilder.setTitle(R.string.add_label)
        dialogBuilder.setView(binding.root)
        dialogBuilder.setNegativeButton(R.string.cancel, null)
        dialogBuilder.setPositiveButton(R.string.save, null)

        val dialog = dialogBuilder.show()
        binding.edit.requestFocus()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val label = binding.edit.text.toString().trim()
            if (label.isNotEmpty()) {
                if (!priorLabels.contains(label)) {
                    insertLabel(label)
                    dialog.dismiss()
                    labelNote(previousLabels, onLabelsUpdated)
                } else binding.TextInputLayout.error = context.getString(R.string.label_exists)
            } else dialog.dismiss()
        }
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

    private fun getCheckedLabels(labels: HashSet<String>): BooleanArray {
        val allLabels = getSortedLabelsList().toTypedArray()
        val checkedLabels = BooleanArray(allLabels.size)
        allLabels.forEachIndexed { index, label ->
            if (labels.contains(label)) {
                checkedLabels[index] = true
            }
        }
        return checkedLabels
    }
}