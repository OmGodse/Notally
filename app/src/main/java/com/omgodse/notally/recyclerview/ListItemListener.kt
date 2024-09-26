package com.omgodse.notally.recyclerview

import com.omgodse.notally.room.ListItem

interface ListItemListener {

    fun delete(position: Int, force: Boolean): Boolean

    fun moveToNext(position: Int)

    fun swap(positionFrom: Int, positionTo: Int): Boolean

    fun move(positionFrom: Int, positionTo: Int, byDrag: Boolean): Boolean
    fun revertMove(positionFrom: Int, positionTo: Int, isChildItemBefore: Boolean? = null)

    fun add(position: Int, initialText: String = "", checked: Boolean = false, isChildItem: Boolean? = null, uncheckedPosition: Int? = position, children: MutableList<ListItem>)

    fun textChanged(position: Int, text: String)

    fun checkedChanged(position: Int, checked: Boolean): Int

    fun isChildItemChanged(position: Int, isChildItem: Boolean)
}