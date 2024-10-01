package com.omgodse.notally.changehistory

import android.util.Log

class ChangeHistory(private val onStackChanged: (stackPointer: Int) -> Unit) {
    private val TAG = "ChangeHistory"
    private val changeStack = ArrayList<Change>()
    private var stackPointer = -1

    fun push(change: Change) {
        popRedos()
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

    fun reset() {
        stackPointer = -1
        changeStack.clear()
    }

    internal fun lookUp(position: Int = 0): Change {
        if (stackPointer - position < 0) {
            throw IllegalArgumentException("ChangeHistory only has $stackPointer changes!")
        }
        return changeStack[stackPointer - position]
    }

    private fun popRedos() {
        while (changeStack.size > stackPointer + 1) {
            changeStack.removeAt(stackPointer + 1)
        }
        //            changeStack.subList(stackPointer, changeStack.size).clear()

    }
}
