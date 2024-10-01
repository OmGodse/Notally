package com.omgodse.notally.changehistory

abstract class ListValueChange<T>(
    internal val newValue: T,
    internal val oldValue: T,
    position: Int,
    internal val positionAfter: Int = position,
    ) : ListChange(position) {

    override fun redo() {
        update(position, newValue, false)
    }

    override fun undo() {
        update(positionAfter, oldValue, true)
    }

     abstract fun update(position: Int, value: T, isUndo: Boolean)

}
