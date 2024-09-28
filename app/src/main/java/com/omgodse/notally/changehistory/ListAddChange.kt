package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListAddChange(
    position: Int,
    private val listManager: ListManager
) : ListChange(position) {
    override fun redo() {
        listManager.add(position, pushChange = false)
    }

    override fun undo() {
        listManager.delete(position, true, pushChange = false)
    }

    override fun toString(): String {
        return "Add at position: $position"
    }

}
