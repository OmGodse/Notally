package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.viewholders.BaseNoteVH
import com.omgodse.notally.room.BaseNote
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat

class BaseNoteAdapter(
    private val settingsHelper: SettingsHelper,
    private val formatter: SimpleDateFormat,
    private val itemListener: ItemListener
) : ListAdapter<BaseNote, BaseNoteVH>(DiffCallback()) {

    private val prettyTime = PrettyTime()

    override fun onBindViewHolder(holder: BaseNoteVH, position: Int) {
        val baseNote = getItem(position)
        holder.bind(baseNote)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BaseNoteVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerBaseNoteBinding.inflate(inflater, parent, false)
        return BaseNoteVH(binding, settingsHelper, itemListener, prettyTime, formatter, inflater)
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