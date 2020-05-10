package com.omgodse.notally.viewholders

import android.content.Context
import android.view.View
import android.widget.LinearLayout
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.NoteListener

class NoteHolder(context: Context, view: View, noteListener: NoteListener?) :
    RecyclerView.ViewHolder(view) {

    private val rootView = view as MaterialCardView

    val displayBody: MaterialTextView = view.findViewById(R.id.Note)
    val displayDate: MaterialTextView = view.findViewById(R.id.Date)
    val displayTitle: MaterialTextView = view.findViewById(R.id.Title)

    val listItemsRemaining: MaterialTextView = view.findViewById(R.id.ItemsRemaining)

    val labelGroup: ChipGroup = view.findViewById(R.id.LabelGroup)
    val displayList: LinearLayout = view.findViewById(R.id.LinearLayout)

    private val settingsHelper = SettingsHelper(context)

    init {
        displayBody.maxLines = try {
            settingsHelper.getMaxLinesPreference().toInt()
        } catch (exception: Exception) {
            10
        }

        if (settingsHelper.getShowDateCreatedPreference()){
            displayDate.visibility = View.VISIBLE
        }
        else displayDate.visibility = View.GONE

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