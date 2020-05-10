package com.omgodse.notally.adapters

import android.content.Context
import android.text.InputType
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.ListItem
import com.omgodse.notally.viewholders.ListHolder
import java.util.*

class ListAdapter(private val context: Context, var items: ArrayList<ListItem>) :
    RecyclerView.Adapter<ListHolder>() {

    var listItemListener: ListItemListener? = null

    fun onItemDismiss(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ListHolder, position: Int) {
        val listItem = items[position]
        holder.listItem.setText(listItem.body)
        holder.checkBox.isChecked = listItem.checked
        holder.listItem.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ListHolder(view, listItemListener)
    }
}