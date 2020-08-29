package com.omgodse.notally.recyclerview

import androidx.recyclerview.widget.RecyclerView

interface ListItemListener {

    fun onMoveToNext(position: Int)

    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

    fun onItemTextChange(position: Int, newText: String)

    fun onItemCheckedChange(position: Int, checked: Boolean)
}