package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerBaseNoteBinding
import com.omgodse.notally.databinding.RecyclerHeaderBinding
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.viewholders.BaseNoteVH
import com.omgodse.notally.recyclerview.viewholders.HeaderVH
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Header
import com.omgodse.notally.room.Item
import com.omgodse.notally.room.ViewType
import org.ocpsoft.prettytime.PrettyTime
import java.text.SimpleDateFormat

class BaseNoteAdapter(
    private val dateFormat: String,
    private val maxItems: Int,
    private val maxLines: Int,
    private val formatter: SimpleDateFormat,
    private val listener: ItemListener
) : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback) {

    private val prettyTime = PrettyTime()

    override fun getItemViewType(position: Int): Int {
        return getItem(position).viewType.ordinal
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Header -> (holder as HeaderVH).bind(item)
            is BaseNote -> (holder as BaseNoteVH).bind(item)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (ViewType.values()[viewType]) {
            ViewType.NOTE -> {
                val binding = RecyclerBaseNoteBinding.inflate(inflater, parent, false)
                BaseNoteVH(binding, dateFormat, maxItems, maxLines, listener, prettyTime, formatter)
            }
            ViewType.HEADER -> {
                val binding = RecyclerHeaderBinding.inflate(inflater, parent, false)
                HeaderVH(binding)
            }
        }
    }


    private object DiffCallback : DiffUtil.ItemCallback<Item>() {

        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when (oldItem) {
                is BaseNote -> if (newItem is BaseNote) {
                    oldItem.id == newItem.id
                } else false
                is Header -> if (newItem is Header) {
                    oldItem.label == newItem.label
                } else false
            }
        }

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean {
            return when (oldItem) {
                is BaseNote -> if (newItem is BaseNote) {
                    oldItem == newItem
                } else false
                is Header -> if (newItem is Header) {
                    oldItem.label == newItem.label
                } else false
            }
        }
    }
}