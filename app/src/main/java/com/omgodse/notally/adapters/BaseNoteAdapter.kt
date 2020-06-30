package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.xml.BaseNote
import com.omgodse.notally.xml.List
import com.omgodse.notally.xml.Note
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class BaseNoteAdapter(private val context: Context) :
    ListAdapter<BaseNote, BaseNoteAdapter.ViewHolder>(DiffCallback()) {

    var onNoteClicked: ((position: Int) -> Unit)? = null
    var onNoteLongClicked: ((position: Int) -> Unit)? = null

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (val baseNote = getItem(position)) {
            is Note -> setupTextNote(baseNote, holder)
            is List -> setupListNote(baseNote, holder)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false)
        return ViewHolder(view)
    }


    private fun setupTextNote(note: Note, holder: ViewHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.setVisible(false)
        holder.listItemsRemaining.setVisible(false)

        holder.displayTitle.text = note.title
        holder.displayBody.text = note.body.applySpans(note.spans)
        holder.displayDate.text = PrettyTime().format(Date(note.timestamp.toLong()))

        holder.displayTitle.setVisible(note.title.isNotEmpty())
        holder.displayBody.setVisible(note.body.isNotEmpty())

        if (note.isEmpty()) {
            holder.displayBody.setText(R.string.empty_note)
            holder.displayBody.setVisible(true)
        }

        for (label in note.labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
    }

    private fun setupListNote(list: List, holder: ViewHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.removeAllViews()

        holder.displayBody.setVisible(false)
        holder.displayList.setVisible(true)

        holder.displayTitle.text = list.title
        holder.displayDate.text = PrettyTime().format(Date(list.timestamp.toLong()))

        holder.displayTitle.setVisible(list.title.isNotEmpty())

        val settingsHelper = SettingsHelper(context)

        val limit = settingsHelper.getMaxItemsPreference().toInt()

        val filteredList = if (list.items.size >= limit) {
            list.items.subList(0, limit)
        } else list.items

        holder.listItemsRemaining.setVisible(list.items.size > limit)

        holder.listItemsRemaining.text = if (list.items.size > limit) {
            val itemsRemaining = list.items.size - limit
            if (itemsRemaining == 1) {
                context.getString(R.string.one_more_item)
            } else context.getString(R.string.more_items, itemsRemaining)
        } else null

        for (listItem in filteredList) {
            val view = View.inflate(context, R.layout.preview_item, null) as MaterialTextView
            view.text = listItem.body
            view.handleChecked(listItem.checked)
            holder.displayList.addView(view)
        }

        holder.displayList.setVisible(list.items.isNotEmpty())

        if (list.isEmpty()) {
            holder.displayBody.setText(R.string.empty_list)
            holder.displayBody.setVisible(true)
        }

        for (label in list.labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
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


    class DiffCallback : DiffUtil.ItemCallback<BaseNote>() {
        override fun areItemsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem == newItem
        }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val rootView = view as MaterialCardView

        val displayBody: MaterialTextView = view.findViewById(R.id.Note)
        val displayDate: MaterialTextView = view.findViewById(R.id.Date)
        val displayTitle: MaterialTextView = view.findViewById(R.id.Title)

        val labelGroup: ChipGroup = view.findViewById(R.id.LabelGroup)

        val displayList: LinearLayout = view.findViewById(R.id.LinearLayout)
        val listItemsRemaining: MaterialTextView = view.findViewById(R.id.ItemsRemaining)

        private val settingsHelper = SettingsHelper(context)

        init {
            displayBody.maxLines = settingsHelper.getMaxLinesPreference().toInt()

            displayDate.setVisible(settingsHelper.getShowDateCreatedPreference())

            when (settingsHelper.getNoteTypePreferences()) {
                context.getString(R.string.elevatedKey) -> {
                    rootView.cardElevation = context.resources.getDimension(R.dimen.cardElevation)
                    rootView.radius = context.resources.getDimension(R.dimen.cardCornerRadius)
                    rootView.strokeWidth = 0
                }
                context.getString(R.string.flatKey) -> {
                    rootView.cardElevation = 0f
                    rootView.radius = 0f
                    rootView.strokeWidth = 1
                }
            }
            rootView.setOnClickListener {
                onNoteClicked?.invoke(adapterPosition)
            }

            rootView.setOnLongClickListener {
                onNoteLongClicked?.invoke(adapterPosition)
                return@setOnLongClickListener true
            }
        }
    }
}