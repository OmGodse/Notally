package com.omgodse.notally.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.Folder
import com.omgodse.notally.room.Label
import com.omgodse.notally.room.NotallyDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.collections.HashSet

abstract class NotallyModel(app: Application) : AndroidViewModel(app) {

    private val database = NotallyDatabase.getDatabase(app)
    private val labelDao = database.labelDao
    private val baseNoteDao = database.baseNoteDao

    var id = 0L

    var isNewNote = true
    var isFirstInstance = true

    var folder = Folder.NOTES

    var title = String()
    var pinned = false

    var timestamp = Date().time
    var labels = HashSet<String>()


    abstract fun getBaseNote(): BaseNote

    open fun setStateFromBaseNote(baseNote: BaseNote) {
        id = baseNote.id
        folder = baseNote.folder

        title = baseNote.title
        pinned = baseNote.pinned
        timestamp = baseNote.timestamp
        labels = baseNote.labels
    }


    fun saveNote(onComplete: (() -> Unit)? = null) {
        viewModelScope.launch {
            id = withContext(Dispatchers.IO) { baseNoteDao.insertBaseNote(getBaseNote()) }
            onComplete?.invoke()
        }
    }

    fun deleteBaseNoteForever(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) { baseNoteDao.deleteBaseNote(getBaseNote()) }
            onComplete()
        }
    }

    fun insertLabel(label: Label, onComplete: (success: Boolean) -> Unit) = executeAsyncWithCallback({ labelDao.insertLabel(label) }, onComplete)


    fun restoreBaseNote() {
        folder = Folder.NOTES
    }

    fun moveBaseNoteToArchive() {
        folder = Folder.ARCHIVED
    }

    fun moveBaseNoteToDeleted() {
        folder = Folder.DELETED
    }


    suspend fun getAllLabelsAsList() = labelDao.getAllLabelsAsList()
}