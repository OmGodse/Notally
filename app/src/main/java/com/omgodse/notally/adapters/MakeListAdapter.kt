package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.ListItemBinding
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.viewholders.MakeListViewHolder
import com.omgodse.notally.xml.ListItem
import java.util.*

class MakeListAdapter(private val context: Context, var items: ArrayList<ListItem>) :
    RecyclerView.Adapter<MakeListViewHolder>() {

    var listItemListener: ListItemListener? = null

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: MakeListViewHolder, position: Int) {
        val listItem = items[position]
        holder.bind(listItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MakeListViewHolder {
        val binding = ListItemBinding.inflate(LayoutInflater.from(context), parent, false)
        return MakeListViewHolder(binding, listItemListener)
    }
}