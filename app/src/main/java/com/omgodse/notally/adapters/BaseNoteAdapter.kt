package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.RecyclerViewItemBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.viewholders.BaseNoteViewHolder
import com.omgodse.notally.xml.BaseNote

class BaseNoteAdapter(private val context: Context) :
    ListAdapter<BaseNote, BaseNoteViewHolder>(DiffCallback()) {

    var onNoteClicked: ((position: Int) -> Unit)? = null
    var onNoteLongClicked: ((position: Int) -> Unit)? = null

    private val settingsHelper = SettingsHelper(context)
    private val maxLines = settingsHelper.getMaxLinesPreference()
    private val maxItems = settingsHelper.getMaxItemsPreference()
    private val noteType = settingsHelper.getNoteTypePreferences()
    private val isDateVisible = settingsHelper.getShowDateCreatedPreference()

    override fun onBindViewHolder(holder: BaseNoteViewHolder, position: Int) {
        val baseNote = getItem(position)
        holder.bind(baseNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseNoteViewHolder {
        val binding = RecyclerViewItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return BaseNoteViewHolder(binding, maxLines, maxItems, noteType, isDateVisible, onNoteClicked, onNoteLongClicked)
    }


    class DiffCallback : DiffUtil.ItemCallback<BaseNote>() {
        override fun areItemsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem.filePath == newItem.filePath
        }

        override fun areContentsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem == newItem
        }
    }
}