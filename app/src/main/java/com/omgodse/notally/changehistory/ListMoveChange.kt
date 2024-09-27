package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListMoveChange(
    fromPosition: Int,
    private val toPosition: Int,
    private val isChildBefore: Boolean,
    private val listManager: ListManager
) : ListChange(fromPosition) {
    override fun redo() {
        listManager.move(position, toPosition, false)
    }

    override fun undo() {
        listManager.revertMove(position, toPosition, isChildBefore)
    }

    override fun toString(): String {
        return "MoveChange from: $position to: $toPosition isChildBefore: $isChildBefore"
    }
}