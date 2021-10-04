package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.recyclerview.viewholders.MakeListVH
import com.omgodse.notally.room.ListItem
import java.util.*

class MakeListAdapter(
    private val items: ArrayList<ListItem>,
    private val listItemListener: ListItemListener
) : RecyclerView.Adapter<MakeListVH>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MakeListVH, position: Int) {
        val listItem = items[position]
        holder.bind(listItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MakeListVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerListItemBinding.inflate(inflater, parent, false)
        return MakeListVH(binding, listItemListener)
    }
}