package com.omgodse.notally.helpers

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.view.LayoutInflater
import androidx.core.util.forEach
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.R
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.databinding.AddLabelBinding
import com.omgodse.notally.databinding.DialogInputBinding
import com.omgodse.notally.miscellaneous.getBody
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.ListItem

interface OperationsParent {

    fun accessContext(): Context

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit)


    fun shareNote(title: String?, body: CharSequence?) {
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        intent.putExtra(TakeNote.EXTRA_SPANNABLE, body)
        intent.putExtra(Intent.EXTRA_TEXT, body.toString())
        val chooser = Intent.createChooser(intent, accessContext().getString(R.string.share_note))
        accessContext().startActivity(chooser)
    }

    fun shareNote(title: String?, items: List<ListItem>?) = shareNote(title, items.getBody())


    fun labelNote(labels: List<String>, previousLabels: HashSet<String>, onUpdated: (labels: HashSet<String>) -> Unit) {
        val checkedLabels = labels.map { label -> previousLabels.contains(label) }.toBooleanArray()

        val inflater = LayoutInflater.from(accessContext())
        val addLabel = AddLabelBinding.inflate(inflater).root

        val builder = MaterialAlertDialogBuilder(accessContext())
            .setTitle(R.string.labels)
            .setNegativeButton(R.string.cancel, null)

        if (labels.isNotEmpty()) {
            builder.setMultiChoiceItems(labels.toTypedArray(), checkedLabels, null)
            builder.setPositiveButton(R.string.save, null)
        } else builder.setView(addLabel)

        val dialog = builder.create()

        addLabel.setOnClickListener {
            dialog.dismiss()
            displayAddLabelDialog(previousLabels, onUpdated)
        }

        dialog.setOnShowListener {
            dialog.getButton(DialogInterface.BUTTON_POSITIVE)?.setOnClickListener {
                val selectedLabels = HashSet<String>()
                dialog.listView.checkedItemPositions.forEach { key, value ->
                    if (value) {
                        val label = labels[key]
                        selectedLabels.add(label)
                    }
                }
                dialog.dismiss()
                onUpdated(selectedLabels)
            }
        }

        dialog.show()
    }

    private fun displayAddLabelDialog(previousLabels: HashSet<String>, onUpdated: (labels: HashSet<String>) -> Unit) {
        val inflater = LayoutInflater.from(accessContext())
        val binding = DialogInputBinding.inflate(inflater)

        val dialog = MaterialAlertDialogBuilder(accessContext())
            .setTitle(R.string.add_label)
            .setView(binding.root)
            .setNegativeButton(R.string.cancel, null)
            .setPositiveButton(R.string.save, null)
            .create()

        dialog.setOnShowListener {
            binding.edit.requestFocus()
            dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener {
                val value = binding.edit.text.toString().trim()
                if (value.isNotEmpty()) {
                    val label = Label(value)
                    insertLabel(label) { success ->
                        if (success) {
                            dialog.dismiss()
                            labelNote(listOf(value), previousLabels, onUpdated)
                        } else binding.root.error = accessContext().getString(R.string.label_exists)
                    }
                } else dialog.dismiss()
            }
        }

        dialog.show()
    }
}