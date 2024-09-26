package com.omgodse.notally.miscellaneous

import android.util.Log


class ChangeHistory(
    private val onStackChanged: (stackPointer: Int) -> Unit
) {
    private val TAG = "ChangeHistory"
    private val changeStack = ArrayList<Change>()
    private var stackPointer =-1

    fun addChange(change: Change) {
        invalidateRedos()
        changeStack.add(change)
        stackPointer++
        Log.d(TAG, "addChange: $change")
        onStackChanged.invoke(stackPointer)
    }

    fun redo() {
        stackPointer++
        if (stackPointer >= changeStack.size) {
            throw RuntimeException("There is no Change to redo!")
        }
        val makeListAction = changeStack[stackPointer]
        Log.d(TAG, "redo: $makeListAction")
        makeListAction.redo()
        onStackChanged.invoke(stackPointer)
    }

    fun undo() {
        if (stackPointer < 0) {
            throw RuntimeException("There is no Change to undo!")
        }
        val makeListAction = changeStack[stackPointer]
        Log.d(TAG, "undo: $makeListAction")
        makeListAction.undo()
        stackPointer--
        onStackChanged.invoke(stackPointer)
    }

    fun canRedo(): Boolean {
        return stackPointer >= -1 && stackPointer < changeStack.size - 1
    }

    fun canUndo(): Boolean {
        return stackPointer > -1
    }

    private fun invalidateRedos() {
        if (stackPointer > -1 && stackPointer + 1 < changeStack.size) {
            changeStack.subList(stackPointer + 1, changeStack.size).clear()
        }
    }

}