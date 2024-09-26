package com.omgodse.notally.room

data class ListItem(var body: String, var checked: Boolean, var isChild: Boolean, var uncheckedPosition: Int?, var children: MutableList<ListItem>) : Cloneable {
    public override fun clone(): Any {
        return ListItem(body, checked, isChild, uncheckedPosition, children)
    }
}