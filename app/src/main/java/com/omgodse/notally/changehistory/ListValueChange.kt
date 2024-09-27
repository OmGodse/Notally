package com.omgodse.notally.changehistory

abstract class ListValueChange<T>(
    protected val newValue: T,
    protected val oldValue: T,
    position: Int,
    protected val positionAfter: Int = position,
    ) : ListChange(position) {

    override fun redo() {
        update(position, newValue, false)
    }

    override fun undo() {
        update(positionAfter, oldValue, true)
    }

     abstract fun update(position: Int, value: T, isUndo: Boolean)

}
