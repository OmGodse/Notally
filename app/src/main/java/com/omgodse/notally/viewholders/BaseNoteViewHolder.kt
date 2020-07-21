package com.omgodse.notally.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.databinding.RecyclerViewItemBinding
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class BaseNoteViewHolder(private val binding: RecyclerViewItemBinding,
                         maxLines: Int,
                         private val maxItems: Int,
                         noteType: String,
                         isDateVisible: Boolean,
                         onNoteClicked: ((position: Int) -> Unit)?,
                         onNoteLongClicked: ((position: Int) -> Unit)?) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.Note.maxLines = maxLines
        binding.Date.setVisible(isDateVisible)

        when (noteType) {
            binding.root.context.getString(R.string.flatKey) -> setCardFlat()
            binding.root.context.getString(R.string.elevatedKey) -> setCardElevated()
        }

        binding.root.setOnClickListener {
            onNoteClicked?.invoke(adapterPosition)
        }

        binding.root.setOnLongClickListener {
            onNoteLongClicked?.invoke(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(baseNote: BaseNote) {
        when (baseNote) {
            is Note -> bind(baseNote)
            is List -> bind(baseNote)
        }
        bindLabels(baseNote.labels)
    }

    private fun bind(note: Note) {
        binding.LinearLayout.setVisible(false)
        binding.ItemsRemaining.setVisible(false)

        binding.Title.text = note.title
        binding.Note.text = note.body.applySpans(note.spans)
        binding.Date.text = PrettyTime().format(Date(note.timestamp.toLong()))

        binding.Title.setVisible(note.title.isNotEmpty())
        binding.Note.setVisible(note.body.isNotEmpty())

        if (note.isEmpty()) {
            binding.Note.setText(R.string.empty_note)
            binding.Note.setVisible(true)
        }
    }

    private fun bind(list: List) {
        binding.LinearLayout.removeAllViews()

        binding.Note.setVisible(false)
        binding.LinearLayout.setVisible(true)

        binding.Title.text = list.title
        binding.Date.text = PrettyTime().format(Date(list.timestamp.toLong()))

        binding.Title.setVisible(list.title.isNotEmpty())

        val filteredList = list.items.take(maxItems)

        binding.ItemsRemaining.setVisible(list.items.size > maxItems)

        binding.ItemsRemaining.text = if (list.items.size > maxItems) {
            val itemsRemaining = list.items.size - maxItems
            if (itemsRemaining == 1) {
                binding.root.context.getString(R.string.one_more_item)
            } else binding.root.context.getString(R.string.more_items, itemsRemaining)
        } else null

        for (listItem in filteredList) {
            val view = View.inflate(binding.root.context, R.layout.preview_item, null) as MaterialTextView
            view.text = listItem.body
            view.handleChecked(listItem.checked)
            binding.LinearLayout.addView(view)
        }

        binding.LinearLayout.setVisible(list.items.isNotEmpty())

        if (list.isEmpty()) {
            binding.Note.setText(R.string.empty_list)
            binding.Note.setVisible(true)
        }
    }

    private fun bindLabels(labels: HashSet<String>) {
        binding.LabelGroup.removeAllViews()
        labels.forEach {
            val labelChip = View.inflate(binding.root.context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = it
            binding.LabelGroup.addView(labelChip)
        }
    }


    private fun setCardFlat() {
        binding.root.cardElevation = 0f
        binding.root.radius = 0f
        binding.root.strokeWidth = 1
    }

    private fun setCardElevated() {
        binding.root.cardElevation = binding.root.context.resources.getDimension(R.dimen.cardElevation)
        binding.root.radius = binding.root.context.resources.getDimension(R.dimen.cardCornerRadius)
        binding.root.strokeWidth = 0
    }


    private fun View.setVisible(visible: Boolean) {
        visibility = if (visible) {
            View.VISIBLE
        } else View.GONE
    }

    private fun MaterialTextView.handleChecked(checked: Boolean) {
        if (checked) {
            paint.isStrikeThruText = true
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else {
            paint.isStrikeThruText = false
            setCompoundDrawablesWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        }
    }
}