package com.omgodse.notally.recyclerview.viewholders

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.miscellaneous.setVisible
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Type
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class BaseNoteViewHolder(
    private val binding: RecyclerBaseNoteBinding,
    private val settingsHelper: SettingsHelper,
    private val itemListener: ItemListener
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.Note.maxLines = settingsHelper.getMaxLines()
        binding.Date.setVisible(settingsHelper.getShowDateCreated())

        when (settingsHelper.getCardType()) {
            binding.root.context.getString(R.string.flatKey) -> setCardFlat()
            binding.root.context.getString(R.string.elevatedKey) -> setCardElevated()
        }

        binding.root.setOnClickListener {
            itemListener.onClick(adapterPosition)
        }

        binding.root.setOnLongClickListener {
            itemListener.onLongClick(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> bindNote(baseNote)
            Type.LIST -> bindList(baseNote)
        }
        binding.Pinned.setVisible(baseNote.pinned)
        binding.LabelGroup.bindLabels(baseNote.labels)
        if (baseNote.isEmpty()) {
            binding.Note.setText(baseNote.getEmptyMessage())
            binding.Note.setVisible(true)
        }
    }

    private fun bindNote(note: BaseNote) {
        binding.LinearLayout.setVisible(false)
        binding.ItemsRemaining.setVisible(false)

        binding.Title.text = note.title
        binding.Note.text = note.body.applySpans(note.spans)
        binding.Date.text = PrettyTime().format(Date(note.timestamp))

        binding.Title.setVisible(note.title.isNotEmpty())
        binding.Note.setVisible(note.body.isNotEmpty())
    }

    private fun bindList(list: BaseNote) {
        binding.LinearLayout.removeAllViews()

        binding.Note.setVisible(false)
        binding.LinearLayout.setVisible(true)

        binding.Title.text = list.title
        binding.Date.text = PrettyTime().format(Date(list.timestamp))

        binding.Title.setVisible(list.title.isNotEmpty())

        val maxItems = settingsHelper.getMaxItems()
        val filteredList = list.items.take(maxItems)

        binding.ItemsRemaining.setVisible(list.items.size > maxItems)

        binding.ItemsRemaining.text = if (list.items.size > maxItems) {
            val itemsRemaining = list.items.size - maxItems
            if (itemsRemaining == 1) {
                binding.root.context.getString(R.string.one_more_item)
            } else binding.root.context.getString(R.string.more_items, itemsRemaining)
        } else null

        for ((body, checked) in filteredList) {
            val view = View.inflate(binding.root.context, R.layout.list_item_preview, null) as MaterialTextView
            view.text = body
            view.handleChecked(checked)
            binding.LinearLayout.addView(view)
        }

        binding.LinearLayout.setVisible(list.items.isNotEmpty())
    }


    private fun setCardFlat() {
        binding.root.cardElevation = 0f
        binding.root.radius = 0f
        binding.root.strokeWidth = 1
    }

    private fun setCardElevated() {
        binding.root.cardElevation = binding.root.resources.getDimension(R.dimen.cardElevation)
        binding.root.radius = binding.root.resources.getDimension(R.dimen.cardCornerRadius)
        binding.root.strokeWidth = 0
    }

    private fun MaterialTextView.handleChecked(checked: Boolean) {
        if (checked) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        paint.isStrikeThruText = checked
    }
}