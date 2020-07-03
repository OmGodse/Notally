package com.omgodse.notally.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.omgodse.notally.helpers.NotesHelper
import java.io.File
import java.util.*
import kotlin.collections.HashSet

abstract class NotallyModel(app: Application) : AndroidViewModel(app) {

    var isNewNote = true
    var isFirstInstance = true

    var title = String()
    var timestamp = Date().time
    var timeModified = Date().time

    val labels = MutableLiveData(HashSet<String>())

    var file: File? = null
        set(value) {
            field = value
            setStateFromFile()
        }

    private val notesHelper = NotesHelper(app)

    abstract fun saveNote()

    abstract fun setStateFromFile()


    internal fun restoreFile() {
        file?.let {
            notesHelper.restoreFile(it)
        }
    }

    internal fun deleteFileForever() {
        file?.delete()
    }

    internal fun moveFileToArchive() {
        file?.let {
            notesHelper.moveFileToArchive(it)
        }
    }

    internal fun moveFileToDeleted() {
        file?.let {
            notesHelper.moveFileToDeleted(it)
        }
    }
}