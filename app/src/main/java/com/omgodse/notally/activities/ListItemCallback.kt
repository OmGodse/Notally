package com.omgodse.notally.activities

import androidx.recyclerview.widget.DiffUtil
import com.omgodse.notally.room.ListItem

class ListItemCallback(private val oldList: List<ListItem>, private val newList: List<ListItem>) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size
    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        return oldList[oldPosition].equals(newList[newPosition])
    }

    override fun areContentsTheSame(oldPosition: Int, newPosition: Int): Boolean {
        val (body1, checked1, isChildItem1) = oldList[oldPosition]
        val (body2, checked2, isChildItem2) = newList[newPosition]
        return body1.equals(body2) && checked1.equals(checked2) && isChildItem1.equals(isChildItem2)
    }


}