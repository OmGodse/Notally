package com.omgodse.notally.interfaces

interface NoteListener {

    fun onNoteClicked(position: Int)

    fun onNoteLongClicked(position: Int)
}