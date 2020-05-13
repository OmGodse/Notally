package com.omgodse.notally.interfaces

import androidx.recyclerview.widget.RecyclerView

interface ListItemListener {

    fun onMoveToNext(position: Int)

    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

    fun onItemDeleted(position: Int)

    fun onItemSwapped(fromPosition: Int, toPosition: Int)

    fun onItemTextChange(position: Int, newText: String)

    fun onItemCheckedChange(position: Int, checked: Boolean)
}