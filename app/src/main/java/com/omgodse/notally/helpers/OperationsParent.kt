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
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.getBody
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.ListItem
import com.omgodse.notally.room.Type

interface OperationsParent {

    fun accessContext(): Context

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit)


    fun shareNote(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> shareNote(baseNote.title, baseNote.body.applySpans(baseNote.spans))
            Type.LIST -> shareNote(baseNote.title, baseNote.items)
        }
    }

    fun shareNote(title: String?, body: CharSequence?) {
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, title)
        shareIntent.putExtra(TakeNote.EXTRA_SPANNABLE, body)
        shareIntent.putExtra(Intent.EXTRA_TEXT, body.toString())
        accessContext().startActivity(Intent.createChooser(shareIntent, accessContext().getString(R.string.share_note)))
    }

    fun shareNote(title: String?, items: List<ListItem>?) = shareNote(title, items.getBody())


    fun labelNote(labels: List<String>, previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
        val checkedLabels = getCheckedLabels(labels, previousLabels)

        val inflater = LayoutInflater.from(accessContext())
        val addLabel = AddLabelBinding.inflate(inflater).root

        val alertDialogBuilder = MaterialAlertDialogBuilder(accessContext())
        alertDialogBuilder.setTitle(R.string.labels)
        alertDialogBuilder.setNegativeButton(R.string.cancel, null)

        if (labels.isNotEmpty()) {
            alertDialogBuilder.setMultiChoiceItems(labels.toTypedArray(), checkedLabels, null)
            alertDialogBuilder.setPositiveButton(R.string.save, null)
        } else alertDialogBuilder.setView(addLabel)

        val dialog = alertDialogBuilder.create()
        dialog.show()

        addLabel.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(previousLabels, onLabelsUpdated)
        }

        dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
            val selectedLabels = HashSet<String>()
            dialog.listView.checkedItemPositions.forEach { key, value ->
                if (value) {
                    val label = labels[key]
                    selectedLabels.add(label)
                }
            }
            dialog.dismiss()
            onLabelsUpdated(selectedLabels)
        }
    }

    private fun displayAddLabelDialog(previousLabels: HashSet<String>, onLabelsUpdated: (labels: HashSet<String>) -> Unit) {
        val inflater = LayoutInflater.from(accessContext())
        val binding = DialogInputBinding.inflate(inflater)

        binding.edit.inputType = InputType.TYPE_TEXT_FLAG_CAP_WORDS
        binding.edit.filters = arrayOf()

        val dialogBuilder = MaterialAlertDialogBuilder(accessContext())
        dialogBuilder.setTitle(R.string.add_label)
        dialogBuilder.setView(binding.root)
        dialogBuilder.setNegativeButton(R.string.cancel, null)
        dialogBuilder.setPositiveButton(R.string.save, null)

        val dialog = dialogBuilder.show()
        binding.edit.requestFocus()

        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
            val value = binding.edit.text.toString().trim()
            if (value.isNotEmpty()) {
                val label = Label(value)
                insertLabel(label) { success ->
                    if (success) {
                        dialog.dismiss()
                        labelNote(listOf(value), previousLabels, onLabelsUpdated)
                    } else binding.root.error = accessContext().getString(R.string.label_exists)
                }
            } else dialog.dismiss()
        }
    }

    companion object {

        private fun getCheckedLabels(labels: List<String>, previousLabels: HashSet<String>): BooleanArray {
            return labels.map { label -> previousLabels.contains(label) }.toBooleanArray()
        }
    }
}