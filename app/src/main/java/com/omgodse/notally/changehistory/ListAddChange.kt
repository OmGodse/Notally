package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem

class ListAddChange(
    position: Int,
    internal val newItem: ListItem,
    private val listManager: ListManager
) : ListChange(position) {
    override fun redo() {
        listManager.add(position, item = newItem, pushChange = false)
    }

    override fun undo() {
        listManager.delete(position, newItem = newItem, pushChange = false)
    }

    override fun toString(): String {
        return "Add at position: $position"
    }

}
