package com.omgodse.notally.changehistory

abstract class ValueChange<T>(protected val newValue: T, protected val oldValue: T) : Change {

    override fun redo() {
        update(newValue, false)
    }

    override fun undo() {
        update(newValue, true)
    }

    abstract fun update(value: T, isUndo: Boolean)
}
