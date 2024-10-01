package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListCheckedChange(
    checked: Boolean,
    position: Int,
    positionAfter: Int,
    private val listManager: ListManager,
) : ListBooleanChange(checked, position, positionAfter) {

    override fun update(position: Int, value: Boolean, isUndo: Boolean) {
        listManager.changeChecked(position, value, pushChange = false)
    }

    override fun toString(): String {
        return "CheckedChange pos: $position positionAfter: $positionAfter isChecked: $newValue"
    }
}
