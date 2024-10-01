package com.omgodse.notally.changehistory

import com.omgodse.notally.recyclerview.ListManager

class ListMoveChange(
    positionFrom: Int,
    private val positionTo: Int,
    private val isChildBefore: Boolean,
    private val hadChildren: Boolean,
    private val listManager: ListManager
) : ListChange(positionFrom) {
    override fun redo() {
        listManager.move(position, positionTo, pushChange = false)
    }

    override fun undo() {
        listManager.revertMove(position, positionTo, isChildBefore, hadChildren)
    }

    override fun toString(): String {
        return "MoveChange from: $position to: $positionTo isChildBefore: $isChildBefore hadChildren: $hadChildren"
    }
}