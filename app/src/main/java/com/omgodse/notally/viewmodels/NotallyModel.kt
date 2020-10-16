package com.omgodse.notally.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.omgodse.notally.xml.BaseNote
import java.io.File
import java.util.*
import kotlin.collections.HashSet

abstract class NotallyModel(private val app: Application) : AndroidViewModel(app) {

    var isNewNote = true
    var isFirstInstance = true

    var title = String()
    var timestamp = Date().time

    val labels = MutableLiveData(HashSet<String>())

    internal lateinit var file: File

    fun setFile(file: File) {
        this.file = file
        if (file.exists()) {
            val baseNote = BaseNote.readFromFile(file)
            setStateFromBaseNote(baseNote)
        }
    }

    fun saveNote() {
        val currentNote = getBaseNote()
        if (file.exists()) {
            val savedNote = BaseNote.readFromFile(file)
            if (savedNote != currentNote) {
                currentNote.writeToFile()
            }
        } else currentNote.writeToFile()
    }


    abstract fun getBaseNote(): BaseNote

    abstract fun setStateFromBaseNote(baseNote: BaseNote)


    fun deleteBaseNoteForever() = file.delete()

    fun restoreBaseNote() = BaseNoteModel.restoreBaseNote(getBaseNote(), app)

    fun moveBaseNoteToArchive() = BaseNoteModel.moveBaseNoteToArchive(getBaseNote(), app)

    fun moveBaseNoteToDeleted() = BaseNoteModel.moveBaseNoteToDeleted(getBaseNote(), app)
}