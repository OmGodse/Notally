package com.omgodse.notally.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import java.io.File
import java.util.*
import kotlin.collections.HashSet

abstract class BaseModel(app: Application) : AndroidViewModel(app) {

    var isNewNote = true
    var isFirstInstance = true

    var title = String()
    var timestamp = Date().time

    val labels = MutableLiveData(HashSet<String>())

    var file: File? = null
        set(value) {
            field = value
            setStateFromFile()
        }

    abstract fun saveNote()

    abstract fun shareNote()

    abstract fun setStateFromFile()
}