package com.omgodse.notally.recyclerview

interface ListItemListener {

    fun delete(position: Int, force: Boolean): Boolean

    fun moveToNext(position: Int)

    fun add(position: Int, initialText: String = "", checked: Boolean = false, isChildItem: Boolean? = null, uncheckedPosition: Int = position)

    fun textChanged(position: Int, text: String)

    fun checkedChanged(position: Int, checked: Boolean): Int

    fun isChildItemChanged(position: Int, isChildItem: Boolean)
}