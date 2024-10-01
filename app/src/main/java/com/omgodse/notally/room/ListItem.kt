package com.omgodse.notally.room

data class ListItem(
    var body: String,
    var checked: Boolean,
    var isChild: Boolean,
    var uncheckedPosition: Int?,
    var children: MutableList<ListItem>,
    var id: Int = -1
) : Cloneable {
    public override fun clone(): Any {
        return ListItem(body, checked, isChild, uncheckedPosition, children.toMutableList(), id)
    }

    override fun toString(): String {
        return "${if (isChild) " >" else ""}${if (checked) "x" else ""} ${body}${
            if (children.isNotEmpty()) "(${
                children.map { it.body }.joinToString(",")
            })" else ""
        }"
    }
}