package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.ListItemBinding
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.recyclerview.viewholders.MakeListViewHolder
import com.omgodse.notally.room.ListItem
import java.util.*

class MakeListAdapter(var items: ArrayList<ListItem>, private val listItemListener: ListItemListener) : RecyclerView.Adapter<MakeListViewHolder>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MakeListViewHolder, position: Int) {
        val listItem = items[position]
        holder.bind(listItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MakeListViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemBinding.inflate(inflater, parent, false)
        return MakeListViewHolder(binding, listItemListener)
    }
}