package com.omgodse.notally.room

data class ListItem(var body: String, var checked: Boolean, var isChildItem: Boolean, var uncheckedPosition: Int) : Cloneable{
    public override fun clone(): Any {
        return ListItem(body, checked, isChildItem, uncheckedPosition)
    }
}