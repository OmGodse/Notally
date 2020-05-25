package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.miscellaneous.Note
import com.omgodse.notally.miscellaneous.NoteDiffCallback
import com.omgodse.notally.miscellaneous.applySpans
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

class NoteAdapter(private val context: Context) : ListAdapter<Note, NoteAdapter.NoteHolder>(NoteDiffCallback()) {

    var noteListener: NoteListener? = null

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val note = getItem(position)

        if (note.isNote) {
            setupTextNote(note, holder)
        } else setupListNote(note, holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false)
        return NoteHolder(view)
    }


    private fun setupTextNote(note: Note, holder: NoteHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.visibility = View.GONE
        holder.listItemsRemaining.visibility = View.GONE

        holder.displayTitle.text = note.title
        holder.displayBody.text = note.body.applySpans(note.spans)
        holder.displayDate.text = PrettyTime().format(Date(note.timestamp.toLong()))

        if (note.title.isEmpty()) {
            holder.displayTitle.visibility = View.GONE
        } else holder.displayTitle.visibility = View.VISIBLE

        if (note.body.isEmpty()) {
            holder.displayBody.visibility = View.GONE
        } else holder.displayBody.visibility = View.VISIBLE

        if (note.isEmpty()) {
            holder.displayBody.setText(R.string.empty_note)
            holder.displayBody.visibility = View.VISIBLE
        }

        for (label in note.labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
    }

    private fun setupListNote(note: Note, holder: NoteHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.removeAllViews()

        holder.displayBody.visibility = View.GONE
        holder.displayList.visibility = View.VISIBLE

        holder.displayTitle.text = note.title
        holder.displayDate.text = PrettyTime().format(Date(note.timestamp.toLong()))

        if (note.title.isEmpty()) {
            holder.displayTitle.visibility = View.GONE
        } else holder.displayTitle.visibility = View.VISIBLE

        val settingsHelper = SettingsHelper(context)

        val limit = try {
            settingsHelper.getMaxItemsPreference().toInt()
        } catch (exception: Exception) {
            4
        }

        val filteredList = if (note.items.size >= limit) {
            note.items.subList(0, limit)
        } else note.items

        holder.listItemsRemaining.visibility = if (note.items.size > limit) {
            View.VISIBLE
        } else View.GONE

        holder.listItemsRemaining.text = if (note.items.size > limit) {
            val itemsRemaining = note.items.size - limit
            if (itemsRemaining == 1) {
                context.getString(R.string.one_more_item)
            } else context.getString(R.string.more_items, itemsRemaining)
        } else null

        for (listItem in filteredList) {
            val view = View.inflate(context, R.layout.preview_item, null) as MaterialTextView
            view.text = listItem.body
            handleChecked(view, listItem.checked)
            holder.displayList.addView(view)
        }

        if (note.items.isEmpty()) {
            holder.displayList.visibility = View.GONE
        } else holder.displayList.visibility = View.VISIBLE

        if (note.isEmpty()) {
            holder.displayBody.setText(R.string.empty_list)
            holder.displayBody.visibility = View.VISIBLE
        }

        for (label in note.labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
    }

    private fun handleChecked(textView: MaterialTextView, checked: Boolean) {
        if (checked) {
            textView.paint.isStrikeThruText = true
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else {
            textView.paint.isStrikeThruText = false
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        }
    }


    inner class NoteHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val rootView = view as MaterialCardView

        val displayBody: MaterialTextView = view.findViewById(R.id.Note)
        val displayDate: MaterialTextView = view.findViewById(R.id.Date)
        val displayTitle: MaterialTextView = view.findViewById(R.id.Title)

        val labelGroup: ChipGroup = view.findViewById(R.id.LabelGroup)

        val displayList: LinearLayout = view.findViewById(R.id.LinearLayout)
        val listItemsRemaining: MaterialTextView = view.findViewById(R.id.ItemsRemaining)

        private val settingsHelper = SettingsHelper(context)

        init {
            displayBody.maxLines = try {
                settingsHelper.getMaxLinesPreference().toInt()
            } catch (exception: Exception) {
                10
            }

            if (settingsHelper.getShowDateCreatedPreference()) {
                displayDate.visibility = View.VISIBLE
            } else displayDate.visibility = View.GONE

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
                noteListener?.onNoteClicked(adapterPosition)
            }

            rootView.setOnLongClickListener {
                noteListener?.onNoteLongClicked(adapterPosition)
                return@setOnLongClickListener true
            }
        }

    }
}