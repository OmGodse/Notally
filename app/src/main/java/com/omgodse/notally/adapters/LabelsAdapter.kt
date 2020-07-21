package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import com.omgodse.notally.databinding.LabelItemBinding
import com.omgodse.notally.viewholders.LabelsViewHolder

class LabelsAdapter(private val context: Context) :
    ListAdapter<String, LabelsViewHolder>(DiffCallback()) {

    var onLabelClicked: ((position: Int) -> Unit)? = null
    var onLabelLongClicked: ((position: Int) -> Unit)? = null

    override fun onBindViewHolder(holder: LabelsViewHolder, position: Int) {
        val label = getItem(position)
        holder.bind(label)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LabelsViewHolder {
        val binding = LabelItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return LabelsViewHolder(binding, onLabelClicked, onLabelLongClicked)
    }


    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}