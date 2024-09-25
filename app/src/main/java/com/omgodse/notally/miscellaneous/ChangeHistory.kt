package com.omgodse.notally.miscellaneous


class ChangeHistory(
    private val onStackChanged: (stackPointer: Int) -> Unit
) {
    private val changeStack = ArrayList<Change>()
    private var stackPointer =-1

    fun addChange(change: Change) {
        invalidateRedos()
        changeStack.add(change)
        stackPointer++
    }

    fun redo() {
        stackPointer++
        if (stackPointer >= changeStack.size) {
            throw RuntimeException("There is no EditAction to redo!")
        }
        val makeListAction = changeStack[stackPointer]
        makeListAction.redo()
        onStackChanged.invoke(stackPointer)
    }

    fun undo() {
        if (stackPointer < 0) {
            throw RuntimeException("There is no EditAction to undo!")
        }
        val makeListAction = changeStack[stackPointer]
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
        onStackChanged.invoke(stackPointer)
    }

}