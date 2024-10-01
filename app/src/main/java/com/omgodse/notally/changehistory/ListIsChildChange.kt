package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListIsChildChange(isChild: Boolean, position: Int, private val listManager: ListManager) :
    ListBooleanChange(isChild, position) {
    override fun update(position: Int, value: Boolean, isUndo: Boolean) {
        listManager.changeIsChild(position, value, pushChange = false)
    }

    override fun toString(): String {
        return "IsChildChange position: $position isChild: $newValue"
    }

}