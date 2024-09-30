package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem

class ListDeleteChange(
    position: Int,
    private val item: ListItem,
    private val listManager: ListManager
) : ListChange(position) {
    override fun redo() {
        listManager.delete(position, pushChange = false)
    }

    override fun undo() {
        listManager.add(position, item, pushChange = false)
    }

    override fun toString(): String {
        return "DeleteChange at $position"
    }
}