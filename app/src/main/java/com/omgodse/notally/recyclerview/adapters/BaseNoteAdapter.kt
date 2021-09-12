package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.viewholders.BaseNoteViewHolder
import com.omgodse.notally.room.BaseNote
import org.ocpsoft.prettytime.PrettyTime

class BaseNoteAdapter(private val settingsHelper: SettingsHelper, private val itemListener: ItemListener) :
    ListAdapter<BaseNote, BaseNoteViewHolder>(DiffCallback()) {

    private val prettyTime = PrettyTime()

    override fun onBindViewHolder(holder: BaseNoteViewHolder, position: Int) {
        val baseNote = getItem(position)
        holder.bind(baseNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseNoteViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerBaseNoteBinding.inflate(inflater, parent, false)
        return BaseNoteViewHolder(binding, settingsHelper, itemListener, prettyTime)
    }


    private class DiffCallback : DiffUtil.ItemCallback<BaseNote>() {

        override fun areItemsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: BaseNote, newItem: BaseNote): Boolean {
            return oldItem == newItem
        }
    }
}