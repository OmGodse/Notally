package com.omgodse.notally.recyclerview.viewholders

import android.view.View
import android.widget.TextView
import androidx.core.view.children
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.R
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Type
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat
import java.util.*

class BaseNoteVH(
    private val binding: RecyclerBaseNoteBinding,
    private val settingsHelper: SettingsHelper,
    listener: ItemListener,
    private val prettyTime: PrettyTime,
    private val formatter: SimpleDateFormat,
) : RecyclerView.ViewHolder(binding.root) {

    init {
        binding.Note.maxLines = settingsHelper.getMaxLines()

        binding.CardView.setOnClickListener {
            listener.onClick(adapterPosition)
        }

        binding.CardView.setOnLongClickListener {
            listener.onLongClick(adapterPosition)
            return@setOnLongClickListener true
        }
    }

    fun bind(baseNote: BaseNote) {
        when (baseNote.type) {
            Type.NOTE -> bindNote(baseNote)
            Type.LIST -> bindList(baseNote)
        }

        binding.Title.text = baseNote.title
        binding.Title.isVisible = baseNote.title.isNotEmpty()

        val date = Date(baseNote.timestamp)
        if (settingsHelper.showDateCreated()) {
            when (settingsHelper.getDateFormat()) {
                SettingsHelper.DateFormat.relative -> binding.Date.text = prettyTime.format(date)
                SettingsHelper.DateFormat.absolute -> binding.Date.text = formatter.format(date)
            }
            binding.Date.visibility = View.VISIBLE
        } else binding.Date.visibility = View.GONE

        binding.LabelGroup.bindLabels(baseNote.labels)

        if (isEmpty(baseNote)) {
            binding.Note.setText(getEmptyMessage(baseNote))
            binding.Note.isVisible = true
        }
    }

    private fun bindNote(note: BaseNote) {
        binding.LinearLayout.isVisible = false

        binding.Note.text = note.body.applySpans(note.spans)
        binding.Note.isVisible = note.body.isNotEmpty()
    }

    private fun bindList(list: BaseNote) {
        binding.Note.isVisible = false

        if (list.items.isEmpty()) {
            binding.LinearLayout.visibility = View.GONE
        } else {
            binding.LinearLayout.visibility = View.VISIBLE

            val max = settingsHelper.getMaxItems()
            val filteredList = list.items.take(max)
            binding.LinearLayout.children.forEachIndexed { index, view ->
                if (view.id != R.id.ItemsRemaining) {
                    if (index < filteredList.size) {
                        val item = filteredList[index]
                        view as TextView
                        view.text = item.body
                        handleChecked(view, item.checked)
                        view.visibility = View.VISIBLE
                    } else view.visibility = View.GONE
                }
            }

            if (list.items.size > max) {
                binding.ItemsRemaining.visibility = View.VISIBLE
                val itemsRemaining = list.items.size - max
                binding.ItemsRemaining.text = if (itemsRemaining == 1) {
                    binding.root.context.getString(R.string.one_more_item)
                } else binding.root.context.getString(R.string.more_items, itemsRemaining)
            } else binding.ItemsRemaining.visibility = View.GONE
        }
    }


    private fun isEmpty(baseNote: BaseNote): Boolean {
        return when (baseNote.type) {
            Type.NOTE -> baseNote.title.isBlank() && baseNote.body.isBlank()
            Type.LIST -> baseNote.title.isBlank() && baseNote.items.isEmpty()
        }
    }

    private fun getEmptyMessage(baseNote: BaseNote): Int {
        return when (baseNote.type) {
            Type.NOTE -> R.string.empty_note
            Type.LIST -> R.string.empty_list
        }
    }

    private fun handleChecked(textView: TextView, checked: Boolean) {
        if (checked) {
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        textView.paint.isStrikeThruText = checked
    }
}