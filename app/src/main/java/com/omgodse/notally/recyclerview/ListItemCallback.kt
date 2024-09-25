package com.omgodse.notally.recyclerview

import androidx.recyclerview.widget.DiffUtil
import com.omgodse.notally.room.ListItem

class ListItemCallback(private val oldList: List<ListItem>, private val newList: List<ListItem>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition] === newList[newPosition]
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition] == newList[newPosition]

    }


}