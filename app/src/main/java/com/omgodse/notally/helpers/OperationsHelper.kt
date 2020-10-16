package com.omgodse.notally.helpers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.text.InputType
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import androidx.core.util.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.DialogInputBinding
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.ListItem
import com.omgodse.notally.xml.Note

class OperationsHelper(private val context: Context) {

    fun shareNote(baseNote: BaseNote) {
        when (baseNote) {
            is Note -> shareNote(baseNote.title, baseNote.body.applySpans(baseNote.spans))
            is List -> shareNote(baseNote.title, baseNote.items)
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

    fun shareNote(title: String?, items: kotlin.collections.List<ListItem>?) = shareNote(title, getBodyFromItems(items))

    fun labelNote(previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
        val allLabels = BaseNoteModel.getSortedLabels(context).toTypedArray()

        val checkedLabels = getCheckedLabels(allLabels, previousLabels)
        val addLabel = MaterialTextView(ContextThemeWrapper(context, R.style.AddLabel))

        val alertDialogBuilder = MaterialAlertDialogBuilder(context)
        alertDialogBuilder.setTitle(R.string.labels)
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)

        if (allLabels.isNotEmpty()) {
            alertDialogBuilder.setMultiChoiceItems(allLabels, checkedLabels, null)
            alertDialogBuilder.setPositiveButton(R.string.save, null)
        } else alertDialogBuilder.setView(addLabel)

        val dialog = alertDialogBuilder.create()
        dialog.show()

        addLabel.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(allLabels, previousLabels, onLabelsUpdated)
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


    private fun displayAddLabelDialog(allLabels: Array<String>, previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
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
                if (!allLabels.contains(label)) {
                    insertLabel(allLabels, label)
                    dialog.dismiss()
                    labelNote(previousLabels, onLabelsUpdated)
                } else binding.TextInputLayout.error = context.getString(R.string.label_exists)
            } else dialog.dismiss()
        }
    }


    private fun insertLabel(allLabels: Array<String>, label: String) {
        val labels = allLabels.toHashSet()
        labels.add(label)
        BaseNoteModel.saveLabels(context, labels)
    }

    private fun getCheckedLabels(allLabels: Array<String>, labels: HashSet<String>): BooleanArray {
        val checkedLabels = BooleanArray(allLabels.size)
        allLabels.forEachIndexed { index, label ->
            checkedLabels[index] = labels.contains(label)
        }
        return checkedLabels
    }

    companion object {
        fun getBodyFromItems(items: kotlin.collections.List<ListItem>?) = buildString {
            items?.forEachIndexed { index, listItem ->
                appendLine("${(index + 1)}) ${listItem.body}")
            }
        }
    }
}