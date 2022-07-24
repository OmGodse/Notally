package com.omgodse.notally.recyclerview

interface ListItemListener {

    fun moveToNext(position: Int)

    fun textChanged(position: Int, text: String)

    fun checkedChanged(position: Int, checked: Boolean)
}