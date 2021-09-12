package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.RecyclerLabelBinding
import com.omgodse.notally.recyclerview.ItemListener
import com.omgodse.notally.recyclerview.viewholders.LabelsViewHolder
import com.omgodse.notally.room.Label

class LabelsAdapter(private val itemListener: ItemListener) : ListAdapter<Label, LabelsViewHolder>(DiffCallback()) {

    override fun onBindViewHolder(holder: LabelsViewHolder, position: Int) {
        val label = getItem(position)
        holder.bind(label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelsViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerLabelBinding.inflate(inflater, parent, false)
        return LabelsViewHolder(binding, itemListener)
    }


    private class DiffCallback : DiffUtil.ItemCallback<Label>() {

        override fun areItemsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: Label, newItem: Label): Boolean {
            return oldItem == newItem
        }
    }
}