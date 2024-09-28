package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListMoveChange(
    positionFrom: Int,
    private val positionTo: Int,
    private val isChildBefore: Boolean,
    private val listManager: ListManager
) : ListChange(positionFrom) {
    override fun redo() {
        listManager.move(position, positionTo, false, pushChange = false)
    }

    override fun undo() {
        listManager.revertMove(position, positionTo, isChildBefore)
    }

    override fun toString(): String {
        return "MoveChange from: $position to: $positionTo isChildBefore: $isChildBefore"
    }
}