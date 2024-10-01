package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem

class ListAddChange(
    position: Int,
    internal val itemBeforeInsert: ListItem,
    private val listManager: ListManager,
) : ListChange(position) {
    override fun redo() {
        listManager.add(position, item = itemBeforeInsert, pushChange = false)
    }

    override fun undo() {
        listManager.delete(
            position,
            childrenToDelete = itemBeforeInsert.children,
            pushChange = false,
        )
    }

    override fun toString(): String {
        return "Add at position: $position"
    }
}
