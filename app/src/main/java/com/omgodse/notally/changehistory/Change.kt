package com.omgodse.notally.changehistory

interface Change {
    fun redo()
    fun undo()
}