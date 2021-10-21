package com.omgodse.notally.recyclerview.viewholders

import android.view.LayoutInflater
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ListItemPreviewBinding
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Type
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class BaseNoteVH(
    private val binding: RecyclerBaseNoteBinding,
    private val settingsHelper: SettingsHelper,
    private val itemListener: ItemListener,
    private val prettyTime: PrettyTime,
    private val inflater: LayoutInflater
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.Note.maxLines = settingsHelper.getMaxLines()
        binding.Date.isVisible = settingsHelper.getShowDateCreated()

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

        binding.Pinned.isVisible = baseNote.pinned
        binding.LabelGroup.bindLabels(baseNote.labels)

        val date = Date(baseNote.timestamp)
        binding.Date.text = prettyTime.format(date)

        if (baseNote.isEmpty()) {
            binding.Note.setText(baseNote.getEmptyMessage())
            binding.Note.isVisible = true
        }
    }

    private fun bindNote(note: BaseNote) {
        binding.LinearLayout.isVisible = false
        binding.ItemsRemaining.isVisible = false

        binding.Title.text = note.title
        binding.Note.text = note.body.applySpans(note.spans)

        binding.Title.isVisible = note.title.isNotEmpty()
        binding.Note.isVisible = note.body.isNotEmpty()
    }

    private fun bindList(list: BaseNote) {
        binding.LinearLayout.removeAllViews()

        binding.Note.isVisible = false
        binding.LinearLayout.isVisible = true

        binding.Title.text = list.title
        binding.Title.isVisible = list.title.isNotEmpty()

        val maxItems = settingsHelper.getMaxItems()
        val filteredList = list.items.take(maxItems)

        binding.ItemsRemaining.isVisible = list.items.size > maxItems

        binding.ItemsRemaining.text = if (list.items.size > maxItems) {
            val itemsRemaining = list.items.size - maxItems
            if (itemsRemaining == 1) {
                binding.root.context.getString(R.string.one_more_item)
            } else binding.root.context.getString(R.string.more_items, itemsRemaining)
        } else null

        for (item in filteredList) {
            val view = ListItemPreviewBinding.inflate(inflater).root
            view.text = item.body
            view.handleChecked(item.checked)
            binding.LinearLayout.addView(view)
        }

        binding.LinearLayout.isVisible = list.items.isNotEmpty()
    }


    private fun MaterialTextView.handleChecked(checked: Boolean) {
        if (checked) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        paint.isStrikeThruText = checked
    }
}